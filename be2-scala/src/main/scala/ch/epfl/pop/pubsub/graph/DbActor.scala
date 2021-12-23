package ch.epfl.pop.pubsub.graph

import java.io.File
import java.nio.charset.StandardCharsets

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.event.LoggingReceive
import akka.pattern.AskableActorRef
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.network.method.message.data.{MessageData, ObjectType}
import ch.epfl.pop.model.objects.{Channel, Hash, Signature, LaoData, ChannelData}
import ch.epfl.pop.pubsub.{AskPatternConstants, PubSubMediator, PublishSubscribe}
import org.iq80.leveldb.impl.Iq80DBFactory.factory
import org.iq80.leveldb.{DB, DBIterator, Options, WriteBatch}

import scala.collection.mutable
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success, Try}
import org.slf4j.LoggerFactory
import java.io.IOException

object DbActor extends AskPatternConstants {

  final val DATABASE_MAX_CHANNELS: Int = 1 << 10
  final lazy val INSTANCE: AskableActorRef = PublishSubscribe.getDbActorRef

  // DbActor Events correspond to messages the actor may receive
  sealed trait Event

  /**
   * Request to write a message in the database
   *
   * @param channel the channel where the message should be published
   * @param message the message to write in the database
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

  /** Request to read the laoData of the LAO, with key laoid/
    *
    * @param channel
    *   the channel we need the LAO's data for
    */
  final case class ReadLaoData(channel: Channel) extends Event

  /** Request to read all messages from a specific <channel>
    *
    * @param channel
    *   the channel where the messages should be fetched
    */
  final case class Catchup(channel: Channel) extends Event

  /**
   * Request to write a <message> in the database and propagate said message
   * to clients subscribed to the <channel>
   *
   * @param channel the channel where the message should be published
   * @param message the message to write in db and propagate to clients
   *
   * @note DbActor will answer with a [[DbActorWriteAck]] if successful since the propagation cannot fail
   */
  final case class WriteAndPropagate(channel: Channel, message: Message) extends Event

  /**
   * Request to create channel <channel> in the db with a type
   *
   * @param channel channel to create
   */
  final case class CreateChannel(channel: Channel, objectType: ObjectType.ObjectType) extends Event

  /** Request to check if channel <channel> exists in the db
    *
    * @param channel
    *   targeted channel
    * @note
    *   db answers with a simple boolean
    */
  final case class ChannelExists(channel: Channel) extends Event

  /** Request to append witness <signature> to a stored message with message_id
    * <messageId>
    *
    * @param messageId
    *   message_id of the targeted message
    * @param signature
    *   signature to append to the witness signature list of the message
    */
  final case class AddWitnessSignature(messageId: Hash, signature: Signature)
      extends Event

  // DbActor DbActorMessage correspond to messages the actor may emit
  sealed trait DbActorMessage

  /** Response for a [[Write]] db request Receiving [[DbActorWriteAck]] works as
    * an acknowledgement that the write request was successful
    */
  final case class DbActorWriteAck() extends DbActorMessage

  /** Response for a [[Read]] db request Receiving [[DbActorReadAck]] works as
    * an acknowledgement that the read request was successful
    *
    * @param message
    *   requested message
    */
  final case class DbActorReadAck(message: Option[Message])
      extends DbActorMessage

  /** Response for a [[ReadChannelData]] db request Receiving [[DbActorReadChannelDataAck]] works as
    * an acknowledgement that the request was successful
    *
    * @param channelData
    *   requested channel data
    */
  final case class DbActorReadChannelDataAck(channelData: Option[ChannelData])
      extends DbActorMessage

  /** Response for a [[ReadLaoData]] db request Receiving [[DbActorReadLaoDataAck]] works as
    * an acknowledgement that the request was successful
    *
    * @param laoData
    *   requested lao data
    */
  final case class DbActorReadLaoDataAck(laoData: Option[LaoData])
      extends DbActorMessage

  /** Response for a [[Catchup]] db request Receiving [[DbActorCatchupAck]]
    * works as an acknowledgement that the catchup request was successful
    *
    * @param messages
    *   requested messages
    */
  final case class DbActorCatchupAck(messages: List[Message])
      extends DbActorMessage

  /** Response for a general db actor ACK
    */
  final case class DbActorAck() extends DbActorMessage

  /** Response for a negative db request
    *
    * @param code
    *   error code corresponding to the error encountered
    * @param description
    *   description of the error encountered
    */
  final case class DbActorNAck(code: Int, description: String)
      extends DbActorMessage

  def getInstance: AskableActorRef = INSTANCE

  /**
   * Creates a new [[DbActor]] which is aware of channels already stored in the db
   *
   * @param mediatorRef reference pointing towards the pub sub mediator
   * @return the newly created [[DbActor]]
   */
  def apply(mediatorRef: ActorRef, DATABASE_FOLDER: String = "database"): DbActor = {

    /* Create a logger for this DB actor"*/
    val logger = LoggerFactory.getLogger("DBLogger")

    val options: Options = new Options()
    options.createIfMissing(true)

    val db: DB =
      try { factory.open(new File(DATABASE_FOLDER), options) }
      catch {
        case e: IOException => {
          logger.error("Could not open database folder {}", DATABASE_FOLDER)
          throw e
        }

      }
    DbActor(mediatorRef, db, DATABASE_FOLDER)
  }

  // FIXME: find way to keep track of db size
  sealed case class DbActor(mediatorRef: ActorRef, db: DB, DATABASE_FOLDER: String) extends Actor with ActorLogging {

    //Close the db and release the resources right before stopping the actor
    override def postStop() = {
      db.close();
    }

    //Map which serves as a cache of keys made from channels with generateLaoDataKey mapped to their LaoDatas (instead of going to the database)
    private val laoDataCache: collection.mutable.Map[String, LaoData] = collection.mutable.Map[String, LaoData]()

    //Generic helper function to check if a key-value pair is valid inside a map (cache in this context)
    private def keyValuePairIsValid[K, V](key: K, cache: collection.mutable.Map[K, V]): Boolean = {
      cache.keySet.contains(key) && cache.get(key) != None && cache.get(key).get != null
    }

    //helper function to determine if a channel should be created during write operations
    private def writeCreateNewChannel(channel: Channel, message: Message): DbActorMessage = {

      Try(db.get(channel.toString.getBytes)) match {
        case Success(bytes) if bytes != null =>
          log.info(s"Channel '$channel' already exists.")
          DbActorAck()
        case _ =>
          val objectType = message.decodedData match {
            case Some(data) if (data._object == ObjectType.ELECTION || data._object == ObjectType.CHIRP) => data._object
            case _ => ObjectType.LAO
          }
          // for now, we don't have meetup or roll call channels, so we just create Lao channels instead, easy to change if needed
          createChannel(channel, objectType) match {
            case DbActorAck() => DbActorAck()
            case _ =>
              log.info(s"Actor $self (db) encountered a problem while creating channel '$channel'")
              DbActorNAck(ErrorCodes.SERVER_ERROR.id, s"Error when creating channel '$channel'")
          }
      }
    }



    //helper functions for the database key generation, could make them public later if needed elsewhere
    private def generateMessageKey(channel: Channel, messageId: Hash): String = channel + (Channel.SEPARATOR + messageId.toString)

    //may return null if the extractLaoId fails
    private def generateLaoDataKey(channel: Channel): String = {
      channel.decodeSubChannel match {
        case Some(data) => new String(data, StandardCharsets.UTF_8) + Channel.LAO_DATA_LOCATION
        case None =>
          log.info(s"Actor $self (db) encountered a problem while decoding subchannel from '$channel'")
          null
      }
    }

    //helper functions to reduce complexity
    private def writeBatch(channel: Channel, messageId: Hash, batch: WriteBatch): DbActorMessage = {
      Try(db.write(batch)) match {
        case Success(_) =>
          log.info(s"Actor $self (db) wrote batch and message_id '$messageId' on channel '$channel'") //change with object and objectId/name/smth like that
          DbActorWriteAck()
        case Failure(exception) =>
          log.error(s"Actor $self (db) encountered a problem while writing message_id '$messageId' and batch on channel '$channel' because of the batch write")
          DbActorNAck(ErrorCodes.SERVER_ERROR.id, exception.getMessage)
      }
    }

    //extra cases for other data than LaoData can be added here easily later on
    private def addToBatchThenWrite(channel: Channel, message: Message, batch: WriteBatch): DbActorMessage = {
      val messageId: Hash = message.message_id
      if (LaoData.isAffectedBy(message)){
        val laoDataKey: String = generateLaoDataKey(channel)
        Try(db.get(laoDataKey.getBytes)) match {
          case Success(bt) =>
            //FIXME: use some sort of compare-and-swap to prevent concurrency issues with LaoData modification if needed
            if(bt != null){
              val laoJson = new String(bt, StandardCharsets.UTF_8)
              val laoDataNew: LaoData = LaoData.buildFromJson(laoJson).updateWith(message)
              batch.put(laoDataKey.getBytes, laoDataNew.toJsonString.getBytes)
              laoDataCache.put(laoDataKey, laoDataNew)
            } else if (bt == null) {
              val laoDataNew: LaoData = LaoData.emptyLaoData.updateWith(message)
              batch.put(laoDataKey.getBytes, laoDataNew.toJsonString.getBytes)
              laoDataCache.put(laoDataKey, laoDataNew)
            }
            //allows writing all data atomically
            writeBatch(channel, messageId, batch)
          case Failure(exception) =>
            log.error(s"Actor $self (db) encountered a problem with the data of the LAO in the database.")
            DbActorNAck(ErrorCodes.SERVER_ERROR.id, exception.getMessage)
        }
      } else{
        writeBatch(channel, messageId, batch)
      }
    }

    // only for the messages
    private def write(channel: Channel, message: Message): DbActorMessage = {

      val messageId: Hash = message.message_id

      //if needed, it is easy to remove the "write-through" functionality (creating a new channel if it does not exist) by simply returning a DbActorNAck if the channel doesn't exist instead of using this function
      writeCreateNewChannel(channel, message) match {
        case nack@DbActorNAck(id, message) => nack
        case _ => Try(db.get(channel.toString.getBytes)) match {
          case Success(bytes) if bytes != null => Try(db.createWriteBatch()) match {
            case Success(batch) => {
              // this will be done whether the message affects LaoData or not
              val json = new String(bytes, StandardCharsets.UTF_8)
              batch.put(channel.toString.getBytes, ChannelData.buildFromJson(json).addMessage(messageId).toJsonString.getBytes)
              batch.put(generateMessageKey(channel, messageId).getBytes, message.toJsonString.getBytes)
              addToBatchThenWrite(channel, message, batch)
            }
            case Failure(exception) =>
              log.error(s"Actor $self (db) encountered a problem while writing message_id '$messageId' and object ${ChannelData.getName} on channel '$channel' because of a batch creation")
              DbActorNAck(ErrorCodes.SERVER_ERROR.id, exception.getMessage)
          }
          case Failure(exception) =>
            log.error(s"Actor $self (db) encountered a problem while writing message_id '$messageId' on channel '$channel'")
            DbActorNAck(ErrorCodes.SERVER_ERROR.id, exception.getMessage)
          case _ =>
            log.error(s"Actor $self (db) encountered a problem while writing message_id '$messageId' on channel '$channel', as the channel does not existand wasn't created")
            DbActorNAck(ErrorCodes.SERVER_ERROR.id, s"Channel $channel does not exist and could not be created")
        }
      }
    }

    private def read(channel: Channel, messageId: Hash): DbActorMessage = {
      Try(db.get(channel.toString.getBytes)) match {
        case Success(b) if b != null => {
          Try (db.get(generateMessageKey(channel, messageId).getBytes)) match {
            case Success(bytes) if bytes != null =>
              val json = new String(bytes, StandardCharsets.UTF_8)
              DbActorReadAck(Some(Message.buildFromJson(json)))
            case _ =>
              DbActorNAck(ErrorCodes.SERVER_ERROR.id, "Unknown read database error")
          }
        }
        case _ => DbActorReadAck(None)
      }
    }

    private def readChannelData(channel: Channel): DbActorMessage = {
      Try (db.get(channel.toString.getBytes)) match {
        case Success(bytes) if bytes != null =>
          val json = new String(bytes, StandardCharsets.UTF_8)
          DbActorReadChannelDataAck(Some(ChannelData.buildFromJson(json)))
        case _ =>
          DbActorNAck(ErrorCodes.SERVER_ERROR.id, s"Channel data not found for channel $channel")
      }
    }

    private def readLaoData(channel: Channel): DbActorMessage = {
      val dataKey: String = generateLaoDataKey(channel)
      dataKey match {
        case null => DbActorReadLaoDataAck(None)
        case _ => {
          if(keyValuePairIsValid[String, LaoData](dataKey, laoDataCache)){
            //with our implementation, if a key is in the map, there is always a LaoData value attached to it
            DbActorReadLaoDataAck(Some(laoDataCache.get(dataKey).get))
          } else{
            Try(db.get(dataKey.getBytes)) match {
              case Success(bytes) if bytes != null =>
                val json = new String(bytes, StandardCharsets.UTF_8)
                val laoData: LaoData = LaoData.buildFromJson(json)
                // we add the data to the cache if it's missing
                laoDataCache.put(dataKey, laoData)
                DbActorReadLaoDataAck(Some(laoData))
              case _ =>
                DbActorNAck(ErrorCodes.SERVER_ERROR.id, s"Lao data not found for channel $channel")
            }
          }
        }
      }
    }

    private def catchup(channel: Channel): DbActorMessage = {
      @scala.annotation.tailrec
      def buildCatchupList(msgIds: List[Hash], acc: List[Message]): List[Message] = {
        msgIds match {
          case Nil => acc
          case head::tail => {
            val message: Message = Message.buildFromJson(new String(db.get(generateMessageKey(channel, head).getBytes), StandardCharsets.UTF_8))
            buildCatchupList(tail, message :: acc)
          }
        }
      }

      Try(db.get(channel.toString.getBytes)) match {
        case Success(bytes) if bytes != null =>
          readChannelData(channel) match {
            case DbActorReadChannelDataAck(Some(data)) =>
              val messageIds: List[Hash] = data.messages
              val result: List[Message] = buildCatchupList(messageIds, List.empty)
              DbActorCatchupAck(result)
            case _ =>
              DbActorNAck(ErrorCodes.SERVER_ERROR.id, s"Could not access the channel's ($channel) messages")
          }
        case _ =>
          DbActorNAck(
            ErrorCodes.INVALID_RESOURCE.id,
            s"Database cannot catchup from a channel $channel that does not exist in db"
          )
      }
    }

    private def createChannel(channel: Channel, objectType: ObjectType.ObjectType): DbActorMessage = {
      Try(db.get(channel.toString.getBytes)) match {
        case Success(bytes) if bytes != null =>
          log.info(s"Database cannot create an already existing channel $channel with a value")
          DbActorNAck(ErrorCodes.INVALID_RESOURCE.id, s"Database cannot create an already existing channel ($channel)")
        case _ =>
          val errorMessageCreate: String = s"Error while creating channel $channel in the database"
          Try(db.put(channel.toString.getBytes, ChannelData(objectType, List.empty).toJsonString.getBytes)) match {
            case Success(_) => DbActorAck()
            case _ =>
              log.error(errorMessageCreate)
              DbActorNAck(ErrorCodes.SERVER_ERROR.id, errorMessageCreate)
          }
      }
    }


    private def channelExists(channel: Channel): DbActorMessage = {
      Try(db.get(channel.toString.getBytes)) match {
        case Success(bytes) if bytes != null => DbActorAck()
        case _ => DbActorNAck(ErrorCodes.INVALID_RESOURCE.id, s"Channel '$channel' does not exist in db")
      }
    }


    override def receive: Receive = LoggingReceive {
      case Write(channel, message) =>
        log.info(s"Actor $self (db) received a WRITE request on channel '$channel'")
        sender ! write(channel, message)

      case Read(channel, messageId) =>
        log.info(s"Actor $self (db) received a READ request for message_id '$messageId' from channel '$channel'")
        sender ! read(channel, messageId)

      case ReadChannelData(channel) =>
        log.info(s"Actor $self (db) received a ReadChannelData request from channel '$channel'")
        sender ! readChannelData(channel)

      case ReadLaoData(channel) =>
        log.info(s"Actor $self (db) received a ReadLaoData request")
        sender ! readLaoData(channel)

      case Catchup(channel) =>
        log.info(s"Actor $self (db) received a CATCHUP request for channel '$channel'")
        sender ! catchup(channel)

      case WriteAndPropagate(channel, message) =>
        log.info(s"Actor $self (db) received a WriteAndPropagate request on channel '$channel'")
        val answer: DbActorMessage = write(channel, message)
        mediatorRef ! PubSubMediator.Propagate(channel, message)
        sender ! answer

      case CreateChannel(channel, objectType) =>
        log.info(s"Actor $self (db) received an CreateChannel request for channel '$channel' of type '$objectType'")
        sender ! createChannel(channel, objectType)

      case ChannelExists(channel) =>
        log.info(s"Actor $self (db) received an ChannelExists request for channel '$channel'")
        sender ! channelExists(channel)

      case AddWitnessSignature(messageId, _) =>
        log.info(
          s"Actor $self (db) received an AddWitnessSignature request for message_id '$messageId'"
        )
        sender ! DbActorNAck(
          ErrorCodes.SERVER_ERROR.id,
          s"NOT IMPLEMENTED: database actor cannot handle AddWitnessSignature requests yet"
        )

      case m @ _ =>
        log.info(s"Actor $self (db) received an unknown message")
        sender ! DbActorNAck(
          ErrorCodes.SERVER_ERROR.id,
          s"database actor received a message '$m' that it could not recognize"
        )
    }
  }

}
