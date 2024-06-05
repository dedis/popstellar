package ch.epfl.pop.storage

import akka.actor.{Actor, ActorLogging, ActorRef, Status}
import akka.event.LoggingReceive
import akka.pattern.AskableActorRef
import ch.epfl.pop.decentralized.ConnectionMediator
import ch.epfl.pop.json.MessageDataProtocol
import ch.epfl.pop.json.MessageDataProtocol.GreetLaoFormat
import ch.epfl.pop.model.network.method.{Rumor, RumorState}
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.network.method.message.data.lao.GreetLao
import ch.epfl.pop.model.network.method.message.data.{ActionType, ObjectType}
import ch.epfl.pop.model.network.method.message.data.socialMedia.AddReaction
import ch.epfl.pop.model.objects.*
import ch.epfl.pop.model.objects.Channel.{LAO_DATA_LOCATION, ROOT_CHANNEL, ROOT_CHANNEL_PREFIX}
import ch.epfl.pop.pubsub.graph.AnswerGenerator.timout
import ch.epfl.pop.pubsub.graph.{ErrorCodes, JsonString}
import ch.epfl.pop.pubsub.{MessageRegistry, PubSubMediator, PublishSubscribe}
import ch.epfl.pop.storage.DbActor.*
import com.google.crypto.tink.subtle.Ed25519Sign

import java.time.LocalDateTime
import java.util.concurrent.TimeUnit
import scala.collection.immutable.HashMap
import scala.collection.mutable.ListBuffer
import scala.concurrent.Await
import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.util.{Failure, Success, Try}
import scala.util.matching.Regex

final case class DbActor(
    private val mediatorRef: ActorRef,
    private val registry: MessageRegistry,
    private val storage: Storage = new DiskStorage(),
    private var topChirpsTimestamp: LocalDateTime = LocalDateTime.now()
) extends Actor with ActorLogging {

  override def postStop(): Unit = {
    storage.close()
    super.postStop()
  }

  private val duration: FiniteDuration = Duration(1, TimeUnit.SECONDS)
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
        (storage.CHANNEL_DATA_KEY + channel.toString, channelData.addMessage(message.message_id).toJsonString),
        (storage.DATA_KEY + s"$channel${Channel.DATA_SEPARATOR}${message.message_id}", message.toJsonString)
      )
    }
  }

  @throws[DbActorNAckException]
  private def writeCreateLao(channel: Channel, message: Message): Unit = {
    createChannel(channel, ObjectType.lao)
    storage.write((storage.DATA_KEY + storage.CREATE_LAO_KEY + channel.toString, message.message_id.toString()))
    write(Channel.ROOT_CHANNEL, message)
  }

  // this function (and its write companion) is necessary so that createLao messages appear in their correct channels (/root)
  // while also being able to find the message when running a catchup on lao_channel
  // as the client needs it to connect
  @throws[DbActorNAckException]
  private def readCreateLao(channel: Channel): Option[Message] = {
    storage.read(storage.DATA_KEY + storage.CREATE_LAO_KEY + channel.toString) match {
      case Some(msg_id) => read(Channel.ROOT_CHANNEL, Hash(Base64Data(msg_id)))
      case _            => None
    }
  }

  @throws[DbActorNAckException]
  private def writeSetupElectionMessage(channel: Channel, message: Message): Unit = {
    channel.extractLaoChannel match {
      case Some(mainLaoChan) =>
        createChannel(channel, ObjectType.election)
        storage.write((storage.DATA_KEY + storage.SETUP_ELECTION_KEY + channel.toString, message.message_id.toString()))
        writeAndPropagate(mainLaoChan, message)

      case _ => log.info("Error: Trying to write an ElectionSetup message on an invalid channel")
    }
  }

  // This function (its write companion) is necessary so SetupElection messages are stored on the main lao channel
  // while being easy to find from their related election channel
  @throws[DbActorNAckException]
  private def readSetupElectionMessage(channel: Channel): Option[Message] = {
    channel.extractLaoChannel match {
      case Some(mainLaoChannel) =>
        storage.read(storage.DATA_KEY + storage.SETUP_ELECTION_KEY + channel.toString) match {
          case Some(msg_id) => read(mainLaoChannel, Hash(Base64Data(msg_id)))
          case _            => None
        }

      case _ =>
        log.info("Error: Trying to read an ElectionSetup message from an invalid channel")
        None
    }
  }

  @throws[DbActorNAckException]
  private def read(channel: Channel, messageId: Hash): Option[Message] = {
    Try(storage.read(storage.DATA_KEY + s"$channel${Channel.DATA_SEPARATOR}$messageId")) match {
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
    Try(storage.read(storage.CHANNEL_DATA_KEY + channel.toString)) match {
      case Success(Some(json)) => ChannelData.buildFromJson(json)
      case Success(None)       => throw DbActorNAckException(ErrorCodes.SERVER_ERROR.id, s"ChannelData for channel $channel not in the database")
      case Failure(ex)         => throw ex
    }
  }

  @throws[DbActorNAckException]
  private def readElectionData(laoId: Hash, electionId: Hash): ElectionData = {
    Try(storage.read(storage.DATA_KEY + s"$ROOT_CHANNEL_PREFIX${laoId.toString}/private/${electionId.toString}")) match {
      case Success(Some(json)) => ElectionData.buildFromJson(json)
      case Success(None)       => throw DbActorNAckException(ErrorCodes.SERVER_ERROR.id, s"ElectionData for election $electionId not in the database")
      case Failure(ex)         => throw ex
    }
  }

  @throws[DbActorNAckException]
  private def readLaoData(channel: Channel): LaoData = {
    Try(storage.read(generateLaoDataKey(channel))) match {
      case Success(Some(json)) => LaoData.buildFromJson(json)
      case Success(None)       => throw DbActorNAckException(ErrorCodes.SERVER_ERROR.id, s"LaoData for channel $channel not in the database")
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
  private def readGreetLao(channel: Channel): Option[Message] = {
    val connectionMediator: AskableActorRef = PublishSubscribe.getConnectionMediatorRef
    val laoData = Try(readLaoData(channel)) match {
      case Success(data) => data
      case Failure(_)    => return None
    }

    val askAddresses = Await.ready(connectionMediator ? ConnectionMediator.ReadPeersClientAddress(), duration).value.get
    val addresses = askAddresses match {
      case Success(ConnectionMediator.ReadPeersClientAddressAck(result)) => result
      case _                                                             => List.empty
    }

    val laoChannel = channel.extractLaoChannel.get
    val laoID = laoChannel.extractChildChannel
    val greetLao = GreetLao(laoID, laoData.owner, laoData.address, addresses)

    // Need to build a signed message
    val jsData = GreetLaoFormat.write(greetLao)
    val encodedData = Base64Data.encode(jsData.toString)
    val signature = laoData.privateKey.signData(encodedData)
    val id = Hash.fromStrings(encodedData.toString, signature.toString)

    Some(Message(encodedData, laoData.publicKey, signature, id, List.empty))
  }

  private def getTopChirps(channel: Channel, buildCatchupList: (msgIds: List[Hash], acc: List[Message], fromChannel: Channel) => List[Message]): List[Message] = {
    val laoID = channel.decodeChannelLaoId match {
      case Some(id) => id
      case None     => Hash(Base64Data(""))
    }
    val reactionsChannel = Channel.apply(s"/root/$laoID/social/reactions")

    val channelData: ChannelData = readChannelData(reactionsChannel)

    val reactionsList = readCreateLao(reactionsChannel) match {
      case Some(msg) =>
        msg :: buildCatchupList(channelData.messages, Nil, reactionsChannel)

      case None =>
        if (reactionsChannel.isMainLaoChannel) {
          log.error("Critical error encountered: no create_lao message was found in the db")
        }
        buildCatchupList(channelData.messages, Nil, reactionsChannel)
    }

    val chirpScores = collection.mutable.Map[Hash, Int]()

    for reaction <- reactionsList do
      val reactionObj = AddReaction.buildFromJson(reaction.toJsonString)
      if reactionObj.action.toString == "add" then
        if reactionObj.reaction_codepoint == "👍" then
          chirpScores(reactionObj.chirp_id) += 1
        else if reactionObj.reaction_codepoint == "👎" then
          chirpScores(reactionObj.chirp_id) -= 1
        else if reactionObj.reaction_codepoint == "❤️" then
          chirpScores(reactionObj.chirp_id) += 1

    var first = new Hash(Base64Data(""))
    var second = new Hash(Base64Data(""))
    var third = new Hash(Base64Data(""))
    var temp = new Hash(Base64Data(""))
    for (chirpId, score) <- chirpScores do
      if first.base64Data.toString == "" then
        first = chirpId
      else if score > chirpScores(first) then
        temp = first
        first = chirpId
        third = second
        second = temp
      else if second.base64Data.toString == "" then
        second = chirpId
      else if score > chirpScores(second) then
        temp = second
        second = chirpId
        third = temp
      else if third.base64Data.toString == "" then
        third = chirpId
      else if score > chirpScores(third) then
        third = chirpId

    val chirpsChannel = Channel.apply(s"/root/$laoID/social/chirps")
    val topThreeChirps: List[Hash] = List(first, second, third)
    val catchupList = readCreateLao(chirpsChannel) match {
      case Some(msg) =>
        msg :: buildCatchupList(topThreeChirps, Nil, chirpsChannel)

      case None =>
        if (chirpsChannel.isMainLaoChannel) {
          log.error("Critical error encountered: no create_lao message was found in the db")
        }
        buildCatchupList(topThreeChirps, Nil, chirpsChannel)
    }

    if (!checkChannelExistence(channel)) {
      createChannel(channel, ObjectType.chirp)
    }

    this.synchronized {
      storage.write((storage.CHANNEL_DATA_KEY + channel.toString, ChannelData(ObjectType.chirp, topThreeChirps).toJsonString))
    }

    readGreetLao(channel) match {
      case Some(msg) => msg :: catchupList
      case None      => catchupList
    }
  }

  private def updateNumberOfNewChirpsReactions(channel: Channel, resetToZero: Boolean): Unit = {
    val laoID = channel.decodeChannelLaoId match {
      case Some(id) => id
      case None     => Hash(Base64Data(""))
    }
    val newReactionsChannel = Channel.apply(s"/root/$laoID/social/top_chirps/number_of_new_reactions")
    if (!checkChannelExistence(newReactionsChannel)) {
      val numberOfReactions: JsonString = "0"
      val pair = (storage.CHANNEL_DATA_KEY + newReactionsChannel.toString, numberOfReactions)
      storage.write(pair)
    } else {
      if (resetToZero) {
        val numberOfReactionsInt = 0
        val pair = (storage.CHANNEL_DATA_KEY + newReactionsChannel.toString, numberOfReactionsInt.toString)
        storage.write(pair)
      } else {
        val numberOfReactions = storage.read(storage.CHANNEL_DATA_KEY + newReactionsChannel.toString)
        val numberOfReactionsInt = numberOfReactions.toString.toInt + 1
        val pair = (storage.CHANNEL_DATA_KEY + newReactionsChannel.toString, numberOfReactionsInt.toString)
        storage.write(pair)
      }
    }
  }

  @throws[DbActorNAckException]
  private def catchupChannel(channel: Channel): List[Message] = {

    @scala.annotation.tailrec
    def buildCatchupList(msgIds: List[Hash], acc: List[Message], fromChannel: Channel): List[Message] = {
      msgIds match {
        case Nil => acc
        case head :: tail =>
          Try(read(fromChannel, head)).recover(_ => None) match {
            case Success(Some(msg)) => buildCatchupList(tail, msg :: acc, fromChannel)
            case _ =>
              log.error(s"/!\\ Critical error encountered: message_id '$head' is listed in channel '$fromChannel' but not stored in db")
              buildCatchupList(tail, acc, fromChannel)
          }
      }
    }

    val topChirpsPattern: Regex = "^/root(/[^/]+)/social/top_chirps$".r

    val laoID = channel.decodeChannelLaoId match {
      case Some(id) => id
      case None     => Hash(Base64Data(""))
    }

    if (topChirpsPattern.findFirstMatchIn(channel.toString).isDefined) {
      if (!checkChannelExistence(channel) || readChannelData(channel).messages.isEmpty) {
        getTopChirps(channel, buildCatchupList)
      } else {
        val newReactionsChannel = Channel.apply(s"/root/$laoID/social/top_chirps/number_of_new_reactions")
        var numberOfNewChirpsReactionsInt = 0
        if (checkChannelExistence(newReactionsChannel)) {
          val numberOfNewChirpsReactions = storage.read(storage.CHANNEL_DATA_KEY + newReactionsChannel.toString)
          numberOfNewChirpsReactionsInt = numberOfNewChirpsReactions.toString.toInt
        }
        if (LocalDateTime.now().isAfter(topChirpsTimestamp.plusSeconds(5)) || numberOfNewChirpsReactionsInt >= 5) {
          if (numberOfNewChirpsReactionsInt >= 5) {
            updateNumberOfNewChirpsReactions(channel, true)
          }
          this.topChirpsTimestamp = LocalDateTime.now()
          getTopChirps(channel, buildCatchupList)
        } else {
          buildCatchupList(readChannelData(channel).messages, Nil, channel)
        }
      }
    } else {
      // regular catchup (not top chirps)
      val channelData: ChannelData = readChannelData(channel)

      val catchupList = readCreateLao(channel) match {
        case Some(msg) =>
          msg :: buildCatchupList(channelData.messages, Nil, channel)

        case None =>
          if (channel.isMainLaoChannel) {
            log.error("Critical error encountered: no create_lao message was found in the db")
          }
          buildCatchupList(channelData.messages, Nil, channel)
      }

      readGreetLao(channel) match {
        case Some(msg) => msg :: catchupList
        case None      => catchupList
      }
    }
  }

  @throws[DbActorNAckException]
  private def pagedCatchupChannel(channel: Channel, numberOfMessages: Int, beforeMessageID: Option[String] = None): List[Message] = {

    @scala.annotation.tailrec
    def buildPagedCatchupList(msgIds: List[Hash], acc: List[Message], channelToPage: Channel): List[Message] = {
      msgIds match {
        case Nil => acc
        case head :: tail =>
          Try(read(channelToPage, head)).recover(_ => None) match {
            case Success(Some(msg)) => buildPagedCatchupList(tail, msg :: acc, channelToPage)
            case _ =>
              log.error(s"/!\\ Critical error encountered: message_id '$head' is listed in channel '$channelToPage' but not stored in db")
              buildPagedCatchupList(tail, acc, channelToPage)
          }
      }
    }

    val chirpsPattern: Regex = "^/root(/[^/]+)/social/chirps(/[^/]+)$".r

    val profilePattern: Regex = "^/root(/[^/]+)/social/profile(/[^/]+){2}$".r

    val laoID = channel.decodeChannelLaoId match {
      case Some(id) => id
      case None     => Hash(Base64Data(""))
    }

    if (chirpsPattern.findFirstMatchIn(channel.toString).isDefined) {
      val chirpsChannel = Channel.apply(s"/root/$laoID/social/chirps")

      val channelData: ChannelData = readChannelData(chirpsChannel)

      var pagedCatchupList = readCreateLao(chirpsChannel) match {
        case Some(msg) =>
          msg :: buildPagedCatchupList(channelData.messages, Nil, chirpsChannel)

        case None =>
          if (chirpsChannel.isMainLaoChannel) {
            log.error("Critical error encountered: no create_lao message was found in the db")
          }
          buildPagedCatchupList(channelData.messages, Nil, chirpsChannel)
      }

      beforeMessageID match {
        case Some(msgID) =>
          val indexOfMessage = pagedCatchupList.indexOf(msgID)
          if (indexOfMessage != -1 && indexOfMessage != 0) {
            var startingIndex = indexOfMessage - numberOfMessages
            if (startingIndex < 0) {
              startingIndex = 0
            }
            pagedCatchupList = pagedCatchupList.slice(startingIndex, indexOfMessage)
          }

        case None =>
          var startingIndex = pagedCatchupList.length - numberOfMessages
          if (startingIndex < 0) {
            startingIndex = 0
          }
          pagedCatchupList = pagedCatchupList.slice(startingIndex, pagedCatchupList.length)
      }
      readGreetLao(chirpsChannel) match {
        case Some(msg) => msg :: pagedCatchupList
        case None      => pagedCatchupList
      }
    } else if (profilePattern.findFirstMatchIn(channel.toString).isDefined) {
      val profilePublicKey = channel.toString.split("/")(5)
      val profileChannel = Channel.apply(s"/root/$laoID/social/$profilePublicKey")

      val channelData: ChannelData = readChannelData(profileChannel)

      var pagedCatchupList = readCreateLao(profileChannel) match {
        case Some(msg) =>
          msg :: buildPagedCatchupList(channelData.messages, Nil, profileChannel)

        case None =>
          if (profileChannel.isMainLaoChannel) {
            log.error("Critical error encountered: no create_lao message was found in the db")
          }
          buildPagedCatchupList(channelData.messages, Nil, profileChannel)
      }

      beforeMessageID match {
        case Some(msgID) =>
          val indexOfMessage = pagedCatchupList.indexOf(msgID)
          if (indexOfMessage != -1 && indexOfMessage != 0) {
            var startingIndex = indexOfMessage - numberOfMessages
            if (startingIndex < 0) {
              startingIndex = 0
            }
            pagedCatchupList = pagedCatchupList.slice(startingIndex, indexOfMessage)
          }

        case None =>
          var startingIndex = pagedCatchupList.length - numberOfMessages
          if (startingIndex < 0) {
            startingIndex = 0
          }
          pagedCatchupList = pagedCatchupList.slice(startingIndex, pagedCatchupList.length)
      }
      readGreetLao(profileChannel) match {
        case Some(msg) => msg :: pagedCatchupList
        case None      => pagedCatchupList
      }
    } else {
      List()
    }
  }

  @throws[DbActorNAckException]
  private def getAllChannels: Set[Channel] = {
    storage.filterKeysByPrefix(storage.CHANNEL_DATA_KEY)
      .map(key => Channel(key.replaceFirst(storage.CHANNEL_DATA_KEY, "")))
  }

  @throws[DbActorNAckException]
  private def writeAndPropagate(channel: Channel, message: Message): Unit = {
    write(channel, message)
    mediatorRef ! PubSubMediator.Propagate(channel, message)
  }

  @throws[DbActorNAckException]
  private def createChannel(channel: Channel, objectType: ObjectType): Unit = {
    if (!checkChannelExistence(channel)) {
      val pair = (storage.CHANNEL_DATA_KEY + channel.toString) -> ChannelData(objectType, List.empty).toJsonString
      storage.write(pair)
    }
  }

  @throws[DbActorNAckException]
  private def createElectionData(laoId: Hash, electionId: Hash, keyPair: KeyPair): Unit = {
    val channel = Channel(s"$ROOT_CHANNEL_PREFIX${laoId.toString}/private/${electionId.toString}")
    if (!checkChannelExistence(channel)) {
      val pair = (storage.DATA_KEY + channel.toString) -> ElectionData(electionId, keyPair).toJsonString
      storage.write(pair)
    }
  }

  @throws[DbActorNAckException]
  private def createChannels(channels: List[(Channel, ObjectType)]): Unit = {

    @scala.annotation.tailrec
    def filterExistingChannels(
        list: List[(Channel, ObjectType)],
        acc: List[(Channel, ObjectType)]
    ): List[(Channel, ObjectType)] = {
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
    val filtered: List[(Channel, ObjectType)] = filterExistingChannels(channels, Nil)
    // creating ChannelData from the filtered input
    val mapped: List[(String, String)] = filtered.map { case (c, o) => (storage.CHANNEL_DATA_KEY + c.toString, ChannelData(o, List.empty).toJsonString) }

    Try(storage.write(mapped: _*))
  }

  private def checkChannelExistence(channel: Channel): Boolean = {
    Try(storage.read(storage.CHANNEL_DATA_KEY + channel.toString)) match {
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
      case Some(data) => storage.DATA_KEY + s"${Channel.ROOT_CHANNEL_PREFIX}$data$LAO_DATA_LOCATION"
      case None =>
        log.error(s"Actor $self (db) encountered a problem while decoding LAO channel from '$channel'")
        throw DbActorNAckException(ErrorCodes.SERVER_ERROR.id, s"Could not extract the LAO id for channel $channel")
    }
  }

  // generates the key of the RollCallData to store in the database
  private def generateRollCallDataKey(laoId: Hash): String = {
    storage.DATA_KEY + s"$ROOT_CHANNEL_PREFIX${laoId.toString}/rollcall"
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
        case Failure(_)    => RollCallData(Hash(Base64Data("")), ActionType.create)
      }
      val rollCallDataKey: String = generateRollCallDataKey(laoId)
      storage.write(rollCallDataKey -> rollCallData.updateWith(message).toJsonString)
    }
  }

  private def generateAuthenticatedKey(popToken: PublicKey, client: String): String = {
    storage.AUTHENTICATED_KEY + popToken.base64Data.toString + Channel.DATA_SEPARATOR + client
  }

  @throws[DbActorNAckException]
  private def readServerPublicKey(): PublicKey = {
    Try(storage.read(storage.SERVER_PUBLIC_KEY + storage.DEFAULT)) match {
      case Success(Some(key)) => PublicKey(Base64Data(key))
      case Success(None) =>
        val (publicKey, _) = generateKeyPair()
        publicKey
      case Failure(ex) => throw ex
    }
  }

  @throws[DbActorNAckException]
  private def readServerPrivateKey(): PrivateKey = {
    Try(storage.read(storage.SERVER_PRIVATE_KEY + storage.DEFAULT)) match {
      case Success(Some(key)) => PrivateKey(Base64Data(key))
      case Success(None) =>
        val (_, privateKey) = generateKeyPair()
        privateKey
      case Failure(ex) => throw ex
    }
  }

  @throws[DbActorNAckException]
  private def generateKeyPair(): (PublicKey, PrivateKey) = {
    val keyPair: Ed25519Sign.KeyPair = Ed25519Sign.KeyPair.newKeyPair
    val publicKey = PublicKey(Base64Data.encode(keyPair.getPublicKey))
    val privateKey = PrivateKey(Base64Data.encode(keyPair.getPrivateKey))

    storage.write((storage.SERVER_PUBLIC_KEY + storage.DEFAULT, publicKey.base64Data.data))
    storage.write((storage.SERVER_PRIVATE_KEY + storage.DEFAULT, privateKey.base64Data.data))
    (publicKey, privateKey)
  }

  @throws[DbActorNAckException]
  private def generateHeartbeat(): HashMap[Channel, Set[Hash]] = {
    val setOfChannels = getAllChannels
    if (setOfChannels.isEmpty) return HashMap()
    val heartbeatMap: HashMap[Channel, Set[Hash]] = setOfChannels.foldLeft(HashMap.empty[Channel, Set[Hash]]) {
      (acc, channel) =>
        readChannelData(channel).messages.toSet match {
          case setOfIds if setOfIds.nonEmpty => acc + (channel -> setOfIds)
          case _                             => acc
        }
    }
    heartbeatMap
  }

  private def generateRumorKey(senderPk: PublicKey, rumorId: Int): String = {
    s"${storage.RUMOR_KEY}${senderPk.base64Data.data}${Channel.DATA_SEPARATOR}$rumorId"
  }

  private def generateRumorDataKey(senderPk: PublicKey): String = {
    s"${storage.RUMOR_DATA_KEY}${senderPk.base64Data.data}"
  }

  @throws[DbActorNAckException]
  private def readRumorData(senderPk: PublicKey): RumorData = {
    Try(storage.read(generateRumorDataKey(senderPk))) match {
      case Success(Some(json)) => RumorData.buildFromJson(json)
      case Success(None)       => RumorData(List.empty)
      case Failure(ex)         => throw ex
    }
  }

  @throws[DbActorNAckException]
  private def readRumor(desiredRumor: (PublicKey, Int)): Option[Rumor] = {
    val rumorKey = generateRumorKey(desiredRumor._1, desiredRumor._2)
    Try(storage.read(rumorKey)) match {
      case Success(Some(json)) => Some(Rumor.buildFromJson(json))
      case Success(None)       => None
      case Failure(ex)         => throw ex
    }
  }

  @throws[DbActorNAckException]
  private def writeRumor(rumor: Rumor): Unit = {
    this.synchronized {
      val rumorData: RumorData = Try(readRumorData(rumor.senderPk)) match {
        case Success(data) => data
        case Failure(_)    => RumorData(List.empty)
      }
      storage.write(generateRumorDataKey(rumor.senderPk) -> rumorData.updateWith(rumor.rumorId).toJsonString)
      storage.write(generateRumorKey(rumor.senderPk, rumor.rumorId) -> rumor.toJsonString)
    }
  }

  private def getRumorState: RumorState = {
    val allPublicKeys = storage.filterKeysByPrefix(storage.RUMOR_DATA_KEY).map(key => PublicKey(Base64Data(key.replaceFirst(storage.RUMOR_DATA_KEY, ""))))
    val allRumorData = allPublicKeys.flatMap {
      publicKey =>
        Try(readRumorData(publicKey)) match
          case Success(rumorData: RumorData) => Some(publicKey -> rumorData.lastRumorId())
          case Failure(ex)                   => None
    }.toMap
    RumorState(allRumorData)
  }

  private def generateRumorStateAns(rumorState: RumorState): List[Rumor] = {
    val localRumorState = getRumorState
    val missingRumors = rumorState.isMissingRumorsFrom(localRumorState)
    missingRumors.flatMap { (publicKey, rumorIdList) =>
      rumorIdList.map { id =>
        readRumor((publicKey, id)).get
      }
    }.toList
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

    case ReadElectionData(laoId, electionId) =>
      log.info(s"Actor $self (db) received a ReadElectionData request for election '$electionId'")
      Try(readElectionData(laoId, electionId)) match {
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

    case WriteCreateLaoMessage(channel, message) =>
      log.info(s"Actor $self (db) received a WriteCreateLaoMessage request")
      Try(writeCreateLao(channel, message)) match {
        case Success(_) => sender() ! DbActorAck()
        case failure    => sender() ! failure.recover(Status.Failure(_))
      }

    case WriteSetupElectionMessage(channel, message) =>
      log.info(s"Actor $self (db) received a WriteSetupElectionMessage request")
      Try(writeSetupElectionMessage(channel, message)) match {
        case Success(_) => sender() ! DbActorAck()
        case failure    => sender() ! failure.recover(Status.Failure(_))
      }

    case ReadSetupElectionMessage(channel) =>
      log.info(s"Actor $self (db) received a ReadSetupElectionMessage request")
      Try(readSetupElectionMessage(channel)) match {
        case Success(msg) => sender() ! DbActorReadAck(msg)
        case failure      => sender() ! failure.recover(Status.Failure(_))
      }

    case Catchup(channel) =>
      log.info(s"Actor $self (db) received a CATCHUP request for channel '$channel'")
      Try(catchupChannel(channel)) match {
        case Success(messages) => sender() ! DbActorCatchupAck(messages)
        case failure           => sender() ! failure.recover(Status.Failure(_))
      }

    case PagedCatchup(channel, numberOfMessages, beforeMessageID) =>
      log.info(s"Actor $self (db) received a PagedCatchup request for channel '$channel' for '$numberOfMessages' messages before message ID: '$beforeMessageID")
      Try(pagedCatchupChannel(channel, numberOfMessages, beforeMessageID)) match {
        case Success(messages) => sender() ! DbActorCatchupAck(messages)
        case failure           => sender() ! failure.recover(Status.Failure(_))
      }

    case GetAllChannels() =>
      log.info(s"Actor $self (db) receveid a GetAllChannels request")
      Try(getAllChannels) match {
        case Success(setOfChannels) => sender() ! DbActorGetAllChannelsAck(setOfChannels)
        case failure                => sender() ! failure.recover(Status.Failure(_))
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

    case CreateElectionData(laoId, id, keyPair) =>
      log.info(s"Actor $self (db) received an CreateElection request for election '$id'" +
        s"\n\tprivate key = ${keyPair.privateKey.toString}" +
        s"\n\tpublic key = ${keyPair.publicKey.toString}")
      Try(createElectionData(laoId, id, keyPair)) match {
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

    case WriteUserAuthenticated(popToken, clientId, user) =>
      log.info(s"Actor $self (db) received a WriteUserAuthenticated request for user $user, id $popToken and clientId $clientId")
      Try(storage.write(generateAuthenticatedKey(popToken, clientId) -> user.base64Data.toString())) match {
        case Success(_) => sender() ! DbActorAck()
        case failure    => sender() ! failure.recover(Status.Failure(_))
      }

    case ReadUserAuthenticated(popToken, clientId) =>
      log.info(s"Actor $self (db) received a ReadUserAuthenticated request for pop token $popToken and clientId $clientId")
      Try(storage.read(generateAuthenticatedKey(popToken, clientId))) match {
        case Success(Some(id)) => sender() ! DbActorReadUserAuthenticationAck(Some(PublicKey(Base64Data(id))))
        case Success(None)     => sender() ! DbActorReadUserAuthenticationAck(None)
        case failure           => sender() ! failure.recover(Status.Failure(_))
      }

    case ReadServerPublicKey() =>
      log.info(s"Actor $self (db) received a ReadServerPublicKey request")
      Try(readServerPublicKey()) match {
        case Success(publicKey) => sender() ! DbActorReadServerPublicKeyAck(publicKey)
        case failure            => sender() ! failure.recover(Status.Failure(_))
      }

    case ReadServerPrivateKey() =>
      log.info(s"Actor $self (db) received a ReadServerPrivateKey request")
      Try(readServerPrivateKey()) match {
        case Success(privateKey) => sender() ! DbActorReadServerPrivateKeyAck(privateKey)
        case failure             => sender() ! failure.recover(Status.Failure(_))
      }

    case GenerateHeartbeat() =>
      log.info(s"Actor $self (db) received a GenerateHeartbeat request")
      Try(generateHeartbeat()) match {
        case Success(heartbeat) => sender() ! DbActorGenerateHeartbeatAck(heartbeat)
        case failure            => sender() ! failure.recover(Status.Failure(_))
      }

    case WriteRumor(rumor) =>
      log.info(s"Actor $self (db) received a WriteRumor request")
      Try(writeRumor(rumor)) match {
        case Success(_) => sender() ! DbActorAck()
        case failure    => sender() ! failure.recover(Status.Failure(_))
      }

    case ReadRumor(desiredRumor) =>
      log.info(s"Actor $self (db) received a ReadRumor request")
      Try(readRumor(desiredRumor)) match {
        case Success(foundRumor) => sender() ! DbActorReadRumor(foundRumor)
        case failure             => sender() ! failure.recover(Status.Failure(_))
      }

    case ReadRumorData(senderPk) =>
      log.info(s"Actor $self (db) received a ReadRumorData request")
      Try(readRumorData(senderPk)) match {
        case Success(foundRumorIds) => sender() ! DbActorReadRumorData(foundRumorIds)
        case failure                => sender() ! failure.recover(Status.Failure(_))
      }

    case GenerateRumorStateAns(rumorState: RumorState) =>
      log.info(s"Actor $self (db) received a GenerateRumorStateAns request")
      Try(generateRumorStateAns(rumorState)) match {
        case Success(rumorList) => sender() ! DbActorGenerateRumorStateAns(rumorList)
        case failure            => sender() ! failure.recover(Status.Failure(_))
      }

    case UpdateNumberOfNewChirpsReactions(channel: Channel) =>
      updateNumberOfNewChirpsReactions(channel, false)

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
  final case class ReadElectionData(laoId: Hash, electionId: Hash) extends Event

  /** Request to read the laoData of the LAO, with key laoId
    *
    * @param channel
    *   the channel we need the LAO's data for
    */
  final case class ReadLaoData(channel: Channel) extends Event

  /** Request to write a "CreateLao" message in the db
    *
    * @param channel
    *   the channel part of the LAO the CreateLao refers to
    * @param message
    *   the actual CreateLao message we want to write in the db
    */
  final case class WriteCreateLaoMessage(channel: Channel, message: Message) extends Event

  /** Request to write a "SetupElection" message in the db
    *
    * @param channel
    *   the channel the SetupElection refers to
    * @param message
    *   the actual SetupElection message we want to write in the db
    */
  final case class WriteSetupElectionMessage(channel: Channel, message: Message) extends Event

  /** Request to read a "SetupElection" message from the db
    *
    * @param channel
    *   the channel the SetupElection refers to
    */
  final case class ReadSetupElectionMessage(channel: Channel) extends Event

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

  /** Request to read a number of messages (<numberOfMessages>) messages from a specific <channel> before a certain message ID <beforeMessageID> or the latest messages if <beforeMessageID> is not specified
    *
    * @param channel
    *   the channel where the messages should be fetched
    */
  final case class PagedCatchup(channel: Channel, numberOfMessages: Int, beforeMessageID: Option[String] = None) extends Event

  /** Request to get all locally stored channels
    */
  final case class GetAllChannels() extends Event

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
  final case class CreateChannel(channel: Channel, objectType: ObjectType) extends Event

  /** Request to create election data in the db with an id and a keypair
    *
    * @param id
    *   unique id of the election
    * @param keyPair
    *   the keypair of the election
    */
  final case class CreateElectionData(laoId: Hash, id: Hash, keyPair: KeyPair) extends Event

  /** Request to create List of channels in the db with given types
    *
    * @param list
    *   list from which channels are created
    */
  final case class CreateChannelsFromList(list: List[(Channel, ObjectType)]) extends Event

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

  /** Registers an authentication of a user on a client using a given identifier
    * @param popToken
    *   pop token to use for authentication
    * @param clientId
    *   client where the user authenticates on
    * @param user
    *   public key of the popcha long term identifier of the user
    */
  final case class WriteUserAuthenticated(popToken: PublicKey, clientId: String, user: PublicKey) extends Event

  /** Reads the authentication information registered for the given pop token regarding the given client
    * @param popToken
    *   pop token that may have been used for authentication
    * @param clientId
    *   client where the user may have authenticated on
    */
  final case class ReadUserAuthenticated(popToken: PublicKey, clientId: String) extends Event

  /** Request for the server public key, created both private and public key if none is found
    */
  final case class ReadServerPublicKey() extends Event

  /** Request for the server private key, created both private and public key if none is found
    */
  final case class ReadServerPrivateKey() extends Event

  /** Request to generate a local heartbeat */
  final case class GenerateHeartbeat() extends Event

  /** Writes the given rumor in Db and updates RumorData accordingly
    * @param rumor
    *   rumor to write in memory
    */
  final case class WriteRumor(rumor: Rumor) extends Event

  /** Requests the Db for rumors corresponding to keys {server public key:rumor id}
    * @param desiredRumor
    *   Map of server public keys and list of desired rumor id for each
    */
  final case class ReadRumor(desiredRumor: (PublicKey, Int)) extends Event

  /** Requests the Db for the list of rumorId received for a senderPk
    * @param senderPk
    *   Public key that we want to request
    */
  final case class ReadRumorData(senderPk: PublicKey) extends Event

  /** Requests the db to build a list of rumors that we have that are missing to the rumorState
    * @param rumorState
    *   Map of last seen rumorId per Publickey that we want to complete
    */

  final case class GenerateRumorStateAns(rumorState: RumorState) extends Event

  /** Requests the Db for the update the number of chirps reaction events since last top chirps request
    *
    * @param channel
    *   Channel containing the number of chirps reaction events since last top chirps request
    */
  final case class UpdateNumberOfNewChirpsReactions(channel: Channel) extends Event

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

  /** Response for [[GetAllChannels]] db request Receiving [[DbActorGetAllChannelsAck]] works as an acknowledgement that the getAllChannels request was successful
    *
    * @param setOfChannels
    *   requested channels
    */
  final case class DbActorGetAllChannelsAck(setOfChannels: Set[Channel]) extends DbActorMessage

  /** Response for a [[ReadRollCallData]] db request Receiving [[DbActorReadRollCallDataAck]] works as an acknowledgement that the request was successful
    *
    * @param rollcallData
    *   requested channel data
    */
  final case class DbActorReadRollCallDataAck(rollcallData: RollCallData) extends DbActorMessage

  /** Response for [[ReadUserAuthenticated]]
    *
    * @param user
    *   Some(user) if a user was registered on the client specified for the given pop token, [[None]] otherwise
    */
  final case class DbActorReadUserAuthenticationAck(user: Option[PublicKey]) extends DbActorMessage

  /** Response for a [[ReadServerPublicKey]] db request
    */
  final case class DbActorReadServerPublicKeyAck(publicKey: PublicKey) extends DbActorMessage

  /** Response for a [[ReadServerPrivateKey]] db request
    */
  final case class DbActorReadServerPrivateKeyAck(privateKey: PrivateKey) extends DbActorMessage

  /** Response for a [[GenerateHeartbeat]] db request Receiving [[DbActorGenerateHeartbeatAck]] works as an acknowledgement that the request was successful
    *
    * @param heartbeatMap
    *   requested heartbeat as a map from the channels to message ids
    */
  final case class DbActorGenerateHeartbeatAck(heartbeatMap: HashMap[Channel, Set[Hash]]) extends DbActorMessage

  /** Response for a [[ReadRumor]]
    */
  final case class DbActorReadRumor(foundRumor: Option[Rumor]) extends DbActorMessage

  /** Response for a [[ReadRumorData]]
    */
  final case class DbActorReadRumorData(rumorIds: RumorData) extends DbActorMessage

  /** Response for a [[GenerateRumorStateAns]]
    */
  final case class DbActorGenerateRumorStateAns(rumorList: List[Rumor]) extends DbActorMessage

  /** Response for a general db actor ACK
    */
  final case class DbActorAck() extends DbActorMessage

}
