package ch.epfl.pop.storage

import akka.actor.{Actor, ActorLogging, ActorRef, Status}
import akka.event.LoggingReceive
import akka.pattern.AskableActorRef
import ch.epfl.pop.json.MessageDataProtocol
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.network.method.message.data.ActionType.ActionType
import ch.epfl.pop.model.objects.Channel.{CHANNEL_SEPARATOR, ROLL_CALL_DATA_PREFIX, ROOT_CHANNEL_PREFIX}
import ch.epfl.pop.model.network.method.message.data.{ActionType, ObjectType}
import ch.epfl.pop.model.objects._
import ch.epfl.pop.pubsub.graph.{ErrorCodes, JsonString}
import ch.epfl.pop.pubsub.{MessageRegistry, PubSubMediator, PublishSubscribe}
import ch.epfl.pop.storage.DbActor._

import scala.util.{Failure, Success, Try}

final case class DbActor(
    private val mediatorRef: ActorRef,
    private val registry: MessageRegistry,
    private val storage: Storage = new DiskStorage()
) extends Actor with ActorLogging {

  override def postStop(): Unit = {
    storage.close()
    super.postStop()
  }

  /* --------------- Functions handling messages DbActor may receive --------------- */

  @throws[DbActorNAckException]
  private def write(channel: Channel, message: Message): Unit = {
    // determine data object type. Assumption: all payloads carry header fields.
    val (_object, action) = MessageDataProtocol.parseHeader(message.data.decodeToString()).get

    // create channel if missing. If already present => createChannel does nothing
    createChannel(channel, _object)

    this.synchronized {
      val channelData: ChannelData = readChannelData(channel)
      storage.write(
        (channel.toString, channelData.addMessage(message.message_id).toJsonString),
        (s"$channel${Channel.DATA_SEPARATOR}${message.message_id}", message.toJsonString)
      )
    }
  }

  @throws[DbActorNAckException]
  private def read(channel: Channel, messageId: Hash): Option[Message] = {
    Try(storage.read(s"$channel${Channel.DATA_SEPARATOR}$messageId")) match {
      case Success(Some(json)) =>
        val msg = Message.buildFromJson(json)
        val data: JsonString = msg.data.decodeToString()
        MessageDataProtocol.parseHeader(data) match {
          case Success((_object, action)) =>
            val builder = registry.getBuilder(_object, action).get
            Some(msg.copy(decodedData = Some(builder(data))))
          case Failure(ex) =>
            log.error(s"Unable to decode message data: $ex")
            Some(msg)
        }
      case Success(None) => None
      case Failure(ex)   => throw ex
    }
  }

  @throws[DbActorNAckException]
  private def readChannelData(channel: Channel): ChannelData = {
    Try(storage.read(channel.toString)) match {
      case Success(Some(json)) => ChannelData.buildFromJson(json)
      case Success(None)       => throw DbActorNAckException(ErrorCodes.SERVER_ERROR.id, s"ChannelData for channel $channel not in the database")
      case Failure(ex)         => throw ex
    }
  }

  @throws[DbActorNAckException]
  private def readElectionData(electionId: Hash, laoId : Hash): ElectionData = {
    Try(storage.read(s"${ROOT_CHANNEL_PREFIX}${laoId.toString}/private/${electionId.toString}")) match {
      case Success(Some(json)) => ElectionData.buildFromJson(json)
      case Success(None)       => throw DbActorNAckException(ErrorCodes.SERVER_ERROR.id, s"ElectionData for election $electionId not in the database")
      case Failure(ex)         => throw ex
    }
  }

  @throws[DbActorNAckException]
  private def readLaoData(channel: Channel): LaoData = {
    Try(storage.read(generateLaoDataKey(channel))) match {
      case Success(Some(json)) => LaoData.buildFromJson(json)
      case Success(None)       => LaoData()
      case Failure(ex)         => throw ex
    }
  }

  @throws[DbActorNAckException]
  private def writeLaoData(channel: Channel, message: Message, address: Option[String]): Unit = {
    this.synchronized {
      val laoData: LaoData = Try(readLaoData(channel)) match {
        case Success(data) => data
        case Failure(_)    => LaoData()
      }
      val laoDataKey: String = generateLaoDataKey(channel)
      storage.write(laoDataKey -> laoData.updateWith(message, address).toJsonString)
    }
  }

  @throws[DbActorNAckException]
  private def catchupChannel(channel: Channel): List[Message] = {

    @scala.annotation.tailrec
    def buildCatchupList(msgIds: List[Hash], acc: List[Message]): List[Message] = {
      msgIds match {
        case Nil => acc
        case head :: tail =>
          Try(read(channel, head)).recover(_ => None) match {
            case Success(Some(msg)) => buildCatchupList(tail, msg :: acc)
            case _ =>
              log.error(s"/!\\ Critical error encountered: message_id '$head' is listed in channel '$channel' but not stored in db")
              buildCatchupList(tail, acc)
          }
      }
    }

    val channelData: ChannelData = readChannelData(channel)
    buildCatchupList(channelData.messages, Nil)
  }

  @throws[DbActorNAckException]
  private def writeAndPropagate(channel: Channel, message: Message): Unit = {
    write(channel, message)
    mediatorRef ! PubSubMediator.Propagate(channel, message)
  }

  @throws[DbActorNAckException]
  private def createChannel(channel: Channel, objectType: ObjectType.ObjectType): Unit = {
    if (!checkChannelExistence(channel)) {
      val pair = channel.toString -> ChannelData(objectType, List.empty).toJsonString
      storage.write(pair)
    }
  }

  @throws[DbActorNAckException]
  private def createElectionData(electionId: Hash, keyPair: KeyPair): Unit = {
    val channel = Channel(s"${ROOT_CHANNEL_PREFIX}private/${electionId.toString}")
    if (!checkChannelExistence(channel)) {
      val pair = channel.toString -> ElectionData(electionId, keyPair).toJsonString
      storage.write(pair)
    }
  }

  @throws[DbActorNAckException]
  private def createChannels(channels: List[(Channel, ObjectType.ObjectType)]): Unit = {

    @scala.annotation.tailrec
    def filterExistingChannels(
        list: List[(Channel, ObjectType.ObjectType)],
        acc: List[(Channel, ObjectType.ObjectType)]
    ): List[(Channel, ObjectType.ObjectType)] = {
      list match {
        case Nil => acc
        case head :: tail =>
          if (checkChannelExistence(head._1)) {
            filterExistingChannels(tail, acc)
          } else {
            filterExistingChannels(tail, head :: acc)
          }
      }
    }

    // removing channels already present in the db from the list
    val filtered: List[(Channel, ObjectType.ObjectType)] = filterExistingChannels(channels, Nil)
    // creating ChannelData from the filtered input
    val mapped: List[(String, String)] = filtered.map { case (c, o) => (c.toString, ChannelData(o, List.empty).toJsonString) }

    Try(storage.write(mapped: _*))
  }

  private def checkChannelExistence(channel: Channel): Boolean = {
    Try(storage.read(channel.toString)) match {
      case Success(option) => option.isDefined
      case _               => false
    }
  }

  @throws[DbActorNAckException]
  private def addWitnessSignature(channel: Channel, messageId: Hash, signature: Signature): Message = {
    Try(read(channel, messageId)) match {
      case Success(Some(msg)) =>
        msg.addWitnessSignature(WitnessSignaturePair(msg.sender, signature))
      case Success(None) =>
        log.error(s"Actor $self (db) encountered a problem while reading the message having as id '$messageId'")
        throw DbActorNAckException(ErrorCodes.SERVER_ERROR.id, s"Could not read message of message id $messageId")
      case Failure(ex) => throw ex
    }
  }

  @throws[DbActorNAckException]
  private def generateLaoDataKey(channel: Channel): String = {
    channel.decodeChannelLaoId match {
      case Some(data) => s"${Channel.ROOT_CHANNEL_PREFIX}$data${Channel.LAO_DATA_LOCATION}"
      case None =>
        log.error(s"Actor $self (db) encountered a problem while decoding LAO channel from '$channel'")
        throw DbActorNAckException(ErrorCodes.SERVER_ERROR.id, s"Could not extract the LAO id for channel $channel")
    }
  }

  // generates the key of the RollCallData to store in the database
  private def generateRollCallDataKey(laoId: Hash): String = {
    s"${ROLL_CALL_DATA_PREFIX}${laoId.toString}"
  }

  @throws[DbActorNAckException]
  private def readRollCallData(laoId: Hash): RollCallData = {
    Try(storage.read(generateRollCallDataKey(laoId))) match {
      case Success(Some(json)) => RollCallData.buildFromJson(json)
      case Success(None)       => throw DbActorNAckException(ErrorCodes.SERVER_ERROR.id, s"ReadRollCallData for RollCAll $laoId not in the database")
      case Failure(ex)         => throw ex
    }
  }

  @throws[DbActorNAckException]
  private def writeRollCallData(laoId: Hash, message: Message): Unit = {
    this.synchronized {
      val rollCallData: RollCallData = Try(readRollCallData(laoId)) match {
        case Success(data) => data
        case Failure(_)    => RollCallData(Hash(Base64Data("")), ActionType.CREATE)
      }
      val rollCallDataKey: String = generateRollCallDataKey(laoId)
      storage.write(rollCallDataKey -> rollCallData.updateWith(message).toJsonString)
    }
  }

  override def receive: Receive = LoggingReceive {
    case Write(channel, message) =>
      log.info(s"Actor $self (db) received a WRITE request on channel '$channel'")
      Try(write(channel, message)) match {
        case Success(_) => sender() ! DbActorAck()
        case failure    => sender() ! failure.recover(Status.Failure(_))
      }

    case Read(channel, messageId) =>
      log.info(s"Actor $self (db) received a READ request for message_id '$messageId' from channel '$channel'")
      Try(read(channel, messageId)) match {
        case Success(opt) => sender() ! DbActorReadAck(opt)
        case failure      => sender() ! failure.recover(Status.Failure(_))
      }

    case ReadChannelData(channel) =>
      log.info(s"Actor $self (db) received a ReadChannelData request from channel '$channel'")
      Try(readChannelData(channel)) match {
        case Success(channelData) => sender() ! DbActorReadChannelDataAck(channelData)
        case failure              => sender() ! failure.recover(Status.Failure(_))
      }

    case ReadElectionData(electionId, laoId) =>
      log.info(s"Actor $self (db) received a ReadElectionData request for election '$electionId'")
      Try(readElectionData(electionId, laoId)) match {
        case Success(electionData) => sender() ! DbActorReadElectionDataAck(electionData)
        case failure               => sender() ! failure.recover(Status.Failure(_))
      }

    case ReadLaoData(channel) =>
      log.info(s"Actor $self (db) received a ReadLaoData request")
      Try(readLaoData(channel)) match {
        case Success(laoData) => sender() ! DbActorReadLaoDataAck(laoData)
        case failure          => sender() ! failure.recover(Status.Failure(_))
      }

    case WriteLaoData(channel, message, address) =>
      log.info(s"Actor $self (db) received a WriteLaoData request for channel $channel")
      Try(writeLaoData(channel, message, address)) match {
        case Success(_) => sender() ! DbActorAck()
        case failure    => sender() ! failure.recover(Status.Failure(_))
      }

    case Catchup(channel) =>
      log.info(s"Actor $self (db) received a CATCHUP request for channel '$channel'")
      Try(catchupChannel(channel)) match {
        case Success(messages) => sender() ! DbActorCatchupAck(messages)
        case failure           => sender() ! failure.recover(Status.Failure(_))
      }

    case WriteAndPropagate(channel, message) =>
      log.info(s"Actor $self (db) received a WriteAndPropagate request on channel '$channel'")
      Try(writeAndPropagate(channel, message)) match {
        case Success(_) => sender() ! DbActorAck()
        case failure    => sender() ! failure.recover(Status.Failure(_))
      }

    case CreateChannel(channel, objectType) =>
      log.info(s"Actor $self (db) received an CreateChannel request for channel '$channel' of type '$objectType'")
      Try(createChannel(channel, objectType)) match {
        case Success(_) => sender() ! DbActorAck()
        case failure    => sender() ! failure.recover(Status.Failure(_))
      }

    case CreateElectionData(id, keyPair) =>
      log.info(s"Actor $self (db) received an CreateElection request for election '$id'" +
        s"\n\tprivate key = ${keyPair.privateKey.toString}" +
        s"\n\tpublic key = ${keyPair.publicKey.toString}")
      Try(createElectionData(id, keyPair)) match {
        case Success(_) => sender() ! DbActorAck()
        case failure    => sender() ! failure.recover(Status.Failure(_))
      }

    case CreateChannelsFromList(list) =>
      log.info(s"Actor $self (db) received a CreateChannelsFromList request for list $list")
      Try(createChannels(list)) match {
        case Success(_) => sender() ! DbActorAck()
        case failure    => sender() ! failure.recover(Status.Failure(_))
      }

    case ChannelExists(channel) =>
      log.info(s"Actor $self (db) received an ChannelExists request for channel '$channel'")
      if (checkChannelExistence(channel)) {
        sender() ! DbActorAck()
      } else {
        sender() ! Status.Failure(DbActorNAckException(ErrorCodes.INVALID_ACTION.id, s"channel '$channel' does not exist in db"))
      }

    case AssertChannelMissing(channel) =>
      log.info(s"Actor $self (db) received an AssertChannelMissing request for channel '$channel'")
      if (checkChannelExistence(channel)) {
        sender() ! Status.Failure(DbActorNAckException(ErrorCodes.INVALID_ACTION.id, s"channel '$channel' already exists in db"))
      } else {
        sender() ! DbActorAck()
      }

    case AddWitnessSignature(channel, messageId, signature) =>
      log.info(s"Actor $self (db) received an AddWitnessSignature request for message_id '$messageId'")
      Try(addWitnessSignature(channel, messageId, signature)) match {
        case Success(witnessMessage) => sender() ! DbActorAddWitnessSignatureAck(witnessMessage)
        case failure                 => sender() ! failure.recover(Status.Failure(_))
      }

    case ReadRollCallData(laoId) =>
      log.info(s"Actor $self (db) received an ReadRollCallData request for RollCall '$laoId'")
      Try(readRollCallData(laoId)) match {
        case Success(rollcallData) => sender() ! DbActorReadRollCallDataAck(rollcallData)
        case failure               => sender() ! failure.recover(Status.Failure(_))
      }

    case WriteRollCallData(laoId, message) =>
      log.info(s"Actor $self (db) received a WriteRollCallData request for RollCall id $laoId")
      Try(writeRollCallData(laoId, message)) match {
        case Success(_) => sender() ! DbActorAck()
        case failure    => sender() ! failure.recover(Status.Failure(_))
      }

    case m =>
      log.info(s"Actor $self (db) received an unknown message")
      sender() ! Status.Failure(DbActorNAckException(ErrorCodes.INVALID_ACTION.id, s"database actor received a message '$m' that it could not recognize"))
  }
}

object DbActor {

  final lazy val INSTANCE: AskableActorRef = PublishSubscribe.getDbActorRef

  def getInstance: AskableActorRef = INSTANCE

  // DbActor Events correspond to messages the actor may receive
  sealed trait Event

  /** Request to write a message in the database
    *
    * @param channel
    *   the channel where the message should be published
    * @param message
    *   the message to write in the database
    */
  final case class Write(channel: Channel, message: Message) extends Event

  /** Request to read a specific message with id <messageId> from <channel>
    *
    * @param channel
    *   the channel where the message was published
    * @param messageId
    *   the id of the message (message_id) we want to read
    */
  final case class Read(channel: Channel, messageId: Hash) extends Event

  /** Request to read the channelData from <channel>, with key laoId/channel
    *
    * @param channel
    *   the channel we need the data for
    */
  final case class ReadChannelData(channel: Channel) extends Event

  /** Request to read the ElectionData for election <id>
    *
    * @param electionId
    *   the election unique id
    */
  final case class ReadElectionData(electionId: Hash, laoId : Hash) extends Event

  /** Request to read the laoData of the LAO, with key laoId
    *
    * @param channel
    *   the channel we need the LAO's data for
    */
  final case class ReadLaoData(channel: Channel) extends Event

  /** Request to update the laoData of the LAO, with key laoId and given message
    *
    * @param channel
    *   the channel part of the LAO which data we need to update
    * @param message
    *   the message we use to update it
    */
  final case class WriteLaoData(channel: Channel, message: Message, address: Option[String]) extends Event

  /** Request to read all messages from a specific <channel>
    *
    * @param channel
    *   the channel where the messages should be fetched
    */
  final case class Catchup(channel: Channel) extends Event

  /** Request to write a <message> in the database and propagate said message to clients subscribed to the <channel>
    *
    * @param channel
    *   the channel where the message should be published
    * @param message
    *   the message to write in db and propagate to clients
    */
  final case class WriteAndPropagate(channel: Channel, message: Message) extends Event

  /** Request to create channel <channel> in the db with a type
    *
    * @param channel
    *   channel to create
    * @param objectType
    *   channel type
    */
  final case class CreateChannel(channel: Channel, objectType: ObjectType.ObjectType) extends Event

  /** Request to create election data in the db with an id and a keypair
    *
    * @param id
    *   unique id of the election
    * @param keyPair
    *   the keypair of the election
    */
  final case class CreateElectionData(id: Hash, keyPair: KeyPair) extends Event

  /** Request to create List of channels in the db with given types
    *
    * @param list
    *   list from which channels are created
    */
  final case class CreateChannelsFromList(list: List[(Channel, ObjectType.ObjectType)]) extends Event

  /** Request to check if channel <channel> exists in the db
    *
    * @param channel
    *   targeted channel
    * @note
    *   db answers with a simple boolean
    */
  final case class ChannelExists(channel: Channel) extends Event

  /** Request to check if channel <channel> is missing in the db
    *
    * @param channel
    *   targeted channel
    * @note
    *   db answers with a simple boolean
    */
  final case class AssertChannelMissing(channel: Channel) extends Event

  /** Request to append witness <signature> to a stored message with message_id <messageId>
    *
    * @param channel
    *   channel in which the messages are being sent
    * @param messageId
    *   message_id of the targeted message
    * @param signature
    *   signature to append to the witness signature list of the message
    */
  final case class AddWitnessSignature(channel: Channel, messageId: Hash, signature: Signature) extends Event

  /** Request to read the rollcallData of the LAO, with key laoId
    *
    * @param laoId
    *   the channel we need the Rollcall's data for
    */
  final case class ReadRollCallData(laoId: Hash) extends Event

  /** Request to write the rollcallData of the LAO, with key laoId
    *
    * @param laoId
    *   the channel we need the Rollcall's data for
    * @param message
    *   rollcall message sent through the channel
    */
  final case class WriteRollCallData(laoId: Hash, message: Message) extends Event

  /** Request to create a rollcall data in the db with state and updateId
    *
    * @param laoId
    *   unique id of the lao in which the rollcall messages are
    * @param updateId
    *   the updateId of the last rollcall message
    * @param state
    *   the state of the last rollcall message, i.e., CREATE, OPEN or CLOSE
    */
  final case class CreateRollCallData(laoId: Hash, updateId: Hash, state: ActionType) extends Event

  // DbActor DbActorMessage correspond to messages the actor may emit
  sealed trait DbActorMessage

  /** Response for a [[Read]] db request Receiving [[DbActorReadAck]] works as an acknowledgement that the read request was successful
    *
    * @param message
    *   requested message
    */
  final case class DbActorReadAck(message: Option[Message]) extends DbActorMessage

  /** Response for a [[ReadChannelData]] db request Receiving [[DbActorReadChannelDataAck]] works as an acknowledgement that the request was successful
    *
    * @param channelData
    *   requested channel data
    */
  final case class DbActorReadChannelDataAck(channelData: ChannelData) extends DbActorMessage

  /** Response for a [[ReadElectionData]] db request Receiving [[DbActorReadElectionDataAck]] works as an acknowledgement that the request was successful
    *
    * @param electionData
    *   requested channel data
    */
  final case class DbActorReadElectionDataAck(electionData: ElectionData) extends DbActorMessage

  /** Response for a [[ReadLaoData]] db request Receiving [[DbActorReadLaoDataAck]] works as an acknowledgement that the request was successful
    *
    * @param laoData
    *   requested lao data
    */
  final case class DbActorReadLaoDataAck(laoData: LaoData) extends DbActorMessage

  /** Response for a [[AddWitnessSignature]] db request Receiving [[DbActorAddWitnessSignatureAck]] works as an acknowledgement that the request was successful
    *
    * @param witnessMessage
    *   requested message witnessed
    */
  final case class DbActorAddWitnessSignatureAck(witnessMessage: Message) extends DbActorMessage

  /** Response for a [[Catchup]] db request Receiving [[DbActorCatchupAck]] works as an acknowledgement that the catchup request was successful
    *
    * @param messages
    *   requested messages
    */
  final case class DbActorCatchupAck(messages: List[Message]) extends DbActorMessage

  /** Response for a [[ReadRollcallData]] db request Receiving [[DbActorReadRollcallDataAck]] works as an acknowledgement that the request was successful
    *
    * @param rollcallData
    *   requested channel data
    */
  final case class DbActorReadRollCallDataAck(rollcallData: RollCallData) extends DbActorMessage

  /** Response for a general db actor ACK
    */
  final case class DbActorAck() extends DbActorMessage

}
