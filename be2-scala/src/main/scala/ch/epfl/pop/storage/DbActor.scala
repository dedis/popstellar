package ch.epfl.pop.storage

import akka.actor.{Actor, ActorLogging, ActorRef, Status}
import akka.event.{Logging, LoggingReceive}
import akka.pattern.AskableActorRef
import ch.epfl.pop.decentralized.ConnectionMediator
import ch.epfl.pop.json.MessageDataProtocol
import ch.epfl.pop.json.MessageDataProtocol.*
import ch.epfl.pop.model.network.method.Rumor
import ch.epfl.pop.json.MessageDataProtocol.GreetLaoFormat
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.network.method.message.data.federation.FederationChallenge
import ch.epfl.pop.model.network.method.message.data.lao.GreetLao
import ch.epfl.pop.model.network.method.message.data.{ActionType, ObjectType}
import ch.epfl.pop.model.network.method.{Rumor, RumorState}
import ch.epfl.pop.model.network.method.message.data.socialMedia.{AddChirp, AddReaction, DeleteChirp, DeleteReaction}
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
import scala.concurrent.Await
import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.util.{Failure, Success, Try}
import spray.json.*
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

      case _ => log.error(s"Trying to write an ElectionSetup message on an invalid channel ${channel.channel}. Not writing message ${message.message_id} in memory")
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
        log.error(s"Trying to read an ElectionSetup message from an invalid channel ${channel.channel}.")
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
            log.error(s"Unable to parse header. Therefore unable to decode message data: $ex")
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

    // returns true if chirp one is older than chirp two by timestamp and false otherwise
    def compareChirpsTimestamps(chirpOneId: Hash, chirpTwoId: Hash, allChirpsList: List[Message]): Boolean = {
      try {
        val chirpOne = allChirpsList.find(msg => msg.message_id == chirpOneId).get
        val chirpOneObj = AddChirp.buildFromJson(chirpOne.data.decodeToString())

        val chirpTwo = allChirpsList.find(msg => msg.message_id == chirpTwoId).get
        val chirpTwoObj = AddChirp.buildFromJson(chirpTwo.data.decodeToString())

        if (chirpOneObj.timestamp.time > chirpTwoObj.timestamp.time) {
          false
        } else {
          true
        }
      } catch {
        case ex: spray.json.DeserializationException => false
      }

    }

    val noCreateLAOMessageError = "No create_lao message was found in the db."

    val laoID = channel.decodeChannelLaoId match {
      case Some(id) => id
      case None     => Hash(Base64Data(""))
    }
    val reactionsChannel = Channel.apply(s"/root/$laoID/social/reactions")

    val reactionsChannelData: ChannelData = readChannelData(reactionsChannel)

    var reactionsList = readCreateLao(reactionsChannel) match {
      case Some(msg) =>
        msg :: buildCatchupList(reactionsChannelData.messages, Nil, reactionsChannel)

      case None =>
        if (reactionsChannel.isMainLaoChannel) {
          log.error(s"$noCreateLAOMessageError")
        }
        buildCatchupList(reactionsChannelData.messages, Nil, reactionsChannel)
    }

    var reactionsChannelDataIDs = reactionsChannelData.messages
    for reaction: Message <- reactionsList do
      try {
        val reactionObj = DeleteReaction.buildFromJson(reaction.data.decodeToString())
        reactionsChannelDataIDs = reactionsChannelDataIDs.filter(msg => msg != reactionObj.reaction_id)
      } catch {
        case ex: spray.json.DeserializationException =>
      }

    reactionsList = readCreateLao(reactionsChannel) match {
      case Some(msg) =>
        msg :: buildCatchupList(reactionsChannelDataIDs, Nil, reactionsChannel)

      case None =>
        if (reactionsChannel.isMainLaoChannel) {
          log.error(s"$noCreateLAOMessageError")
        }
        buildCatchupList(reactionsChannelDataIDs, Nil, reactionsChannel)
    }

    val chirpsChannel = Channel.apply(s"/root/$laoID/social/chirps")

    var allChirpsList = catchupChannel(chirpsChannel)

    // check in place to remove create lao message if any from the list
    try {
      AddChirp.buildFromJson(allChirpsList.head.data.decodeToString())
    } catch {
      case ex: spray.json.DeserializationException =>
        try {
          DeleteChirp.buildFromJson(allChirpsList.head.data.decodeToString())
        } catch {
          case ex: spray.json.DeserializationException => allChirpsList = allChirpsList.slice(1, allChirpsList.length)
        }
    }

    var count = 0
    for chirp <- allChirpsList do {
      try {
        val chirpObj = DeleteChirp.buildFromJson(chirp.data.decodeToString())
        if (chirpObj.action.toString == "delete") {
          allChirpsList = allChirpsList.filter(msg => msg.message_id != chirpObj.chirp_id)
          allChirpsList = allChirpsList.filter(msg => msg != chirp)
          reactionsList = reactionsList.filter(reaction =>
            try {
              AddReaction.buildFromJson(reaction.data.decodeToString()).chirp_id != chirpObj.chirp_id
            } catch {
              case ex: spray.json.DeserializationException => true // do not filter
            }
          )
        }
      } catch {
        case ex: spray.json.DeserializationException =>
      }
    }

    val chirpScores = collection.mutable.Map[Hash, Int]()

    for chirp <- allChirpsList do
      chirpScores(chirp.message_id) = 0

    for reaction: Message <- reactionsList do
      try {
        val reactionObj = AddReaction.buildFromJson(reaction.data.decodeToString())
        if reactionObj.action.toString == "add" then
          if reactionObj.reaction_codepoint == "👍" then
            if (chirpScores.contains(reactionObj.chirp_id)) {
              chirpScores(reactionObj.chirp_id) += 1
            } else {
              chirpScores(reactionObj.chirp_id) = 1
            }
          else if reactionObj.reaction_codepoint == "👎" then
            if (chirpScores.contains(reactionObj.chirp_id)) {
              chirpScores(reactionObj.chirp_id) -= 1
            } else {
              chirpScores(reactionObj.chirp_id) = -1
            }
          else if reactionObj.reaction_codepoint == "❤️" then
            if (chirpScores.contains(reactionObj.chirp_id)) {
              chirpScores(reactionObj.chirp_id) += 1
            } else {
              chirpScores(reactionObj.chirp_id) = 1
            }
      } catch {
        case ex: spray.json.DeserializationException =>
      }

    var first = new Hash(Base64Data(""))
    var second = new Hash(Base64Data(""))
    var third = new Hash(Base64Data(""))
    var temp = new Hash(Base64Data(""))
    for (chirpId, score) <- chirpScores do
      if first.base64Data.toString == "" then
        first = chirpId
      else if score > chirpScores(first) || (score == chirpScores(first) && compareChirpsTimestamps(chirpId, first, allChirpsList)) then
        temp = first
        first = chirpId
        third = second
        second = temp
      else if second.base64Data.toString == "" then
        second = chirpId
      else if score > chirpScores(second) || (score == chirpScores(second) && compareChirpsTimestamps(chirpId, second, allChirpsList)) then
        temp = second
        second = chirpId
        third = temp
      else if third.base64Data.toString == "" then
        third = chirpId
      else if score > chirpScores(third) || (score == chirpScores(third) && compareChirpsTimestamps(chirpId, third, allChirpsList)) then
        third = chirpId

    var topThreeChirps: List[Hash] = List(third, second, first)
    if (first.base64Data.toString == "") {
      topThreeChirps = List()
    } else if (second.base64Data.toString == "") {
      topThreeChirps = List(first)
    } else if (third.base64Data.toString == "") {
      topThreeChirps = List(second, first)
    }

    var catchupList: List[Message] = List.empty
    for id <- topThreeChirps do
      catchupList = allChirpsList.find(msg => msg.message_id == id).get :: catchupList

    if (!checkChannelExistence(channel)) {
      createChannel(channel, ObjectType.chirp)
    }

    this.synchronized {
      val channelData = ChannelData(ObjectType.chirp, topThreeChirps)
      storage.write(
        (storage.CHANNEL_DATA_KEY + channel.toString, channelData.toJsonString)
      )
      for message: Message <- catchupList do
        storage.write(
          (storage.DATA_KEY + s"$channel${Channel.DATA_SEPARATOR}${message.message_id}", message.toJsonString)
        )
    }

    readGreetLao(channel) match {
      case Some(msg) => catchupList
      case None      => catchupList
    }
  }

  private def updateNumberOfNewChirpsReactions(channel: Channel, resetToZero: Boolean): Unit = {
    val laoID = channel.decodeChannelLaoId match {
      case Some(id) => id
      case None     => Hash(Base64Data(""))
    }
    readNumberOfReactions(laoID) match {
      case Some(numberOfReactionsFromDb: NumberOfChirpsReactionsData) =>
        if (resetToZero) {
          val numberOfReactions = NumberOfChirpsReactionsData(0)
          writeNumberOfReactions(numberOfReactions, laoID)
        } else {
          val numberOfNewChirpsReactionsInt = numberOfReactionsFromDb.numberOfChirpsReactions
          val updatedNumberOfChirpsReactions = NumberOfChirpsReactionsData(numberOfNewChirpsReactionsInt + 1)
          writeNumberOfReactions(updatedNumberOfChirpsReactions, laoID)
        }
      case None =>
        val numberOfReactions = NumberOfChirpsReactionsData(1)
        writeNumberOfReactions(numberOfReactions, laoID)
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
              log.error(s"message_id '$head' is listed in channel '$fromChannel' but not stored in db. This entry will be ignored.")
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
        val numberOfNewChirpsReactionsInt = readNumberOfReactions(laoID) match {
          case Some(numberOfReactions) => numberOfReactions.numberOfChirpsReactions
          case None                    => 0
        }
        if (LocalDateTime.now().isAfter(topChirpsTimestamp.plusSeconds(5)) || numberOfNewChirpsReactionsInt >= 5) {
          if (numberOfNewChirpsReactionsInt >= 5) {
            updateNumberOfNewChirpsReactions(channel, true)
          }
          this.topChirpsTimestamp = LocalDateTime.now()
          getTopChirps(channel, buildCatchupList)
        } else {
          var catchupList: List[Message] = List.empty
          val chirpsChannel = Channel.apply(s"/root/$laoID/social/chirps")
          var allChirpsList = catchupChannel(chirpsChannel)
          allChirpsList = allChirpsList.slice(1, allChirpsList.length)

          for id <- readChannelData(channel).messages do
            catchupList = allChirpsList.find(msg => msg.message_id == id).get :: catchupList

          catchupList
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
            log.error("No create_lao message was found in the db")
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
              log.error(s"Critical error encountered: message_id '$head' is listed in channel '$channelToPage' but not stored in db. This entry will be ignored.")
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

    val noCreateLAOMessageError = "Critical error encountered: no create_lao message was found in the db"

    if (chirpsPattern.findFirstMatchIn(channel.toString).isDefined) {
      val chirpsChannel = Channel.apply(s"/root/$laoID/social/chirps")

      val channelData: ChannelData = readChannelData(chirpsChannel)

      var pagedCatchupList = readCreateLao(chirpsChannel) match {
        case Some(msg) =>
          msg :: buildPagedCatchupList(channelData.messages, Nil, chirpsChannel)

        case None =>
          if (chirpsChannel.isMainLaoChannel) {
            log.error(noCreateLAOMessageError)
          }
          buildPagedCatchupList(channelData.messages, Nil, chirpsChannel)
      }

      beforeMessageID match {
        case Some(msgID) =>
          var indexOfMessage = -1
          var count = 0
          for msg <- pagedCatchupList do
            if (msg.message_id.toString == msgID) {
              indexOfMessage = count
            }
            count = count + 1

          if (indexOfMessage != -1) {
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

        case null =>
          var startingIndex = pagedCatchupList.length - numberOfMessages
          if (startingIndex < 0) {
            startingIndex = 0
          }
          pagedCatchupList = pagedCatchupList.slice(startingIndex, pagedCatchupList.length)
      }
      readGreetLao(chirpsChannel) match {
        case Some(msg) => pagedCatchupList
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
            log.error(noCreateLAOMessageError)
          }
          buildPagedCatchupList(channelData.messages, Nil, profileChannel)
      }

      beforeMessageID match {
        case Some(msgID) =>
          var indexOfMessage = -1
          var count = 0
          for msg <- pagedCatchupList do
            if (msg.message_id.toString == msgID) {
              indexOfMessage = count
            }
            count = count + 1

          if (indexOfMessage != -1) {
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

        case null =>
          var startingIndex = pagedCatchupList.length - numberOfMessages
          if (startingIndex < 0) {
            startingIndex = 0
          }
          pagedCatchupList = pagedCatchupList.slice(startingIndex, pagedCatchupList.length)
      }
      readGreetLao(profileChannel) match {
        case Some(msg) => pagedCatchupList
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
        log.error(s"Encountered a problem while reading the message having as id '$messageId'. Signature $signature was not added to message $messageId.")
        throw DbActorNAckException(ErrorCodes.SERVER_ERROR.id, s"Could not read message of message id $messageId")
      case Failure(ex) => throw ex
    }
  }

  @throws[DbActorNAckException]
  private def generateLaoDataKey(channel: Channel): String = {
    channel.decodeChannelLaoId match {
      case Some(data) => storage.DATA_KEY + s"${Channel.ROOT_CHANNEL_PREFIX}$data$LAO_DATA_LOCATION"
      case None =>
        log.error(s"Encountered a problem while decoding LAO channel from '$channel'. LAO data key for channel ${channel.channel} was not generated.")
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

  private def generateNumberOfReactionsKey(laoID: Hash): String = {
    s"${storage.NUMBER_OF_REACTIONS_KEY}$laoID"
  }

  @throws[DbActorNAckException]
  private def readNumberOfReactions(laoID: Hash): Option[NumberOfChirpsReactionsData] = {
    val numberOfReactionsKey = generateNumberOfReactionsKey(laoID)
    Try(storage.read(numberOfReactionsKey)) match {
      case Success(Some(json)) => Some(NumberOfChirpsReactionsData.buildFromJson(json))
      case Success(None)       => None
      case Failure(ex)         => throw ex
    }
  }

  private def writeNumberOfReactions(numberOfReactions: NumberOfChirpsReactionsData, laoID: Hash): Unit = {
    this.synchronized {
      storage.write(generateNumberOfReactionsKey(laoID) -> numberOfReactions.toJsonString)

    }
  }

  @throws[DbActorNAckException]
  private def readFederationMessages(channel: Channel, key: String): Option[Message] = {
    channel.extractLaoChannel match {
      case Some(mainLaoChannel) =>
        storage.read(storage.DATA_KEY + key + channel.toString) match {
          case Some(msgId) => read(mainLaoChannel, Hash(Base64Data(msgId)))
          case _           => None
        }
      case _ =>
        log.info("Error : Trying to read a federationMessage from an invalid channel")
        None
    }
  }

  @throws[DbActorNAckException]
  private def readFederationChallenge(channel: Channel, laoId: Hash): Option[Message] = {
    readFederationMessages(channel, storage.FEDERATION_CHALLENGE_KEY + laoId.toString)
  }

  @throws[DbActorNAckException]
  private def readFederationExpect(channel: Channel, laoId: Hash): Option[Message] = {
    readFederationMessages(channel, storage.FEDERATION_EXPECT_KEY + laoId.toString)
  }

  @throws[DbActorNAckException]
  private def readFederationInit(channel: Channel, laoId: Hash): Option[Message] = {
    readFederationMessages(channel, storage.FEDERATION_INIT_KEY + laoId.toString)
  }

  @throws[DbActorNAckException]
  private def readFederationResult(channel: Channel, laoId: Hash): Option[Message] = {
    readFederationMessages(channel, storage.FEDERATION_RESULT_KEY + laoId.toString)
  }

  @throws[DbActorNAckException]
  private def readFederationTokensExchange(channel: Channel, laoId: Hash): Option[Message] = {
    readFederationMessages(channel, storage.FEDERATION_TOKENS_EXCHANGE_KEY + laoId.toString)
  }

  @throws[DbActorNAckException]
  private def writeFederationMessages(channel: Channel, key: String, message: Message): Unit = {
    channel.extractLaoChannel match {
      case Some(mainLaoChannel) =>
        createChannel(channel, ObjectType.federation)
        storage.write((storage.DATA_KEY + key + channel.toString, message.message_id.toString()))
        writeAndPropagate(mainLaoChannel, message)

      case _ => log.info("Error : Trying to write a federationMessage on an invalid channel")
    }
  }

  @throws[DbActorNAckException]
  private def writeFederationChallenge(channel: Channel, laoId: Hash, message: Message): Unit = {
    writeFederationMessages(channel, storage.FEDERATION_CHALLENGE_KEY + laoId.toString, message)
  }

  @throws[DbActorNAckException]
  private def writeFederationExpect(channel: Channel, laoId: Hash, message: Message): Unit = {
    writeFederationMessages(channel, storage.FEDERATION_EXPECT_KEY + laoId.toString, message)
  }

  @throws[DbActorNAckException]
  private def writeFederationInit(channel: Channel, laoId: Hash, message: Message): Unit = {
    writeFederationMessages(channel, storage.FEDERATION_INIT_KEY + laoId.toString, message)
  }

  @throws[DbActorNAckException]
  private def writeFederationResult(channel: Channel, laoId: Hash, message: Message): Unit = {
    writeFederationMessages(channel, storage.FEDERATION_RESULT_KEY + laoId.toString, message)
  }

  @throws[DbActorNAckException]
  private def writeFederationTokensExchange(channel: Channel, laoId: Hash, message: Message): Unit = {
    writeFederationMessages(channel, storage.FEDERATION_TOKENS_EXCHANGE_KEY + laoId.toString, message)
  }

  @throws[DbActorNAckException]
  private def deleteFederationChallenge(channel: Channel, laoId: Hash): Unit = {
    val key = storage.DATA_KEY + storage.FEDERATION_CHALLENGE_KEY + laoId.toString + channel.toString
    this.synchronized {
      Try(storage.read(key)) match {
        case Success(Some(_)) => storage.delete(key)
        case Success(None)    => /* Do Nothing */
        case Failure(ex)      => throw ex
      }
    }
  }

  override def receive: Receive = LoggingReceive {
    case Write(channel, message) =>
      log.info(s"Received a WRITE request on channel '$channel' for message ${message.message_id}")
      Try(write(channel, message)) match {
        case Success(_) => sender() ! DbActorAck()
        case failure    => sender() ! failure.recover(Status.Failure(_))
      }

    case Read(channel, messageId) =>
      log.info(s"Received a READ request for message_id '$messageId' from channel '$channel'")
      Try(read(channel, messageId)) match {
        case Success(opt) => sender() ! DbActorReadAck(opt)
        case failure      => sender() ! failure.recover(Status.Failure(_))
      }

    case ReadChannelData(channel) =>
      log.info(s"Received a ReadChannelData request for channel '$channel'")
      Try(readChannelData(channel)) match {
        case Success(channelData) => sender() ! DbActorReadChannelDataAck(channelData)
        case failure              => sender() ! failure.recover(Status.Failure(_))
      }

    case ReadElectionData(laoId, electionId) =>
      log.info(s"Received a ReadElectionData request for election '$electionId' in lao_id $laoId")
      Try(readElectionData(laoId, electionId)) match {
        case Success(electionData) => sender() ! DbActorReadElectionDataAck(electionData)
        case failure               => sender() ! failure.recover(Status.Failure(_))
      }

    case ReadLaoData(channel) =>
      log.info(s"Received a ReadLaoData request for channel ${channel.channel}")
      Try(readLaoData(channel)) match {
        case Success(laoData) => sender() ! DbActorReadLaoDataAck(laoData)
        case failure          => sender() ! failure.recover(Status.Failure(_))
      }

    case WriteLaoData(channel, message, address) =>
      log.info(s"Received a WriteLaoData request for channel $channel and message_id ${message.message_id}")
      Try(writeLaoData(channel, message, address)) match {
        case Success(_) => sender() ! DbActorAck()
        case failure    => sender() ! failure.recover(Status.Failure(_))
      }

    case WriteCreateLaoMessage(channel, message) =>
      log.info(s"Received a WriteCreateLaoMessage request for channel ${channel.channel} and message_id ${message.message_id}")
      Try(writeCreateLao(channel, message)) match {
        case Success(_) => sender() ! DbActorAck()
        case failure    => sender() ! failure.recover(Status.Failure(_))
      }

    case WriteSetupElectionMessage(channel, message) =>
      log.info(s"Received a WriteSetupElectionMessage request on channel ${channel.channel} and message_id ${message.message_id}")
      Try(writeSetupElectionMessage(channel, message)) match {
        case Success(_) => sender() ! DbActorAck()
        case failure    => sender() ! failure.recover(Status.Failure(_))
      }

    case ReadSetupElectionMessage(channel) =>
      log.info(s"Received a ReadSetupElectionMessage request on channel ${channel.channel}")
      Try(readSetupElectionMessage(channel)) match {
        case Success(msg) => sender() ! DbActorReadAck(msg)
        case failure      => sender() ! failure.recover(Status.Failure(_))
      }

    case Catchup(channel) =>
      log.info(s"Received a CATCHUP request for channel '$channel'")
      Try(catchupChannel(channel)) match {
        case Success(messages) => sender() ! DbActorCatchupAck(messages)
        case failure           => sender() ! failure.recover(Status.Failure(_))
      }

    case PagedCatchup(channel, numberOfMessages, beforeMessageID) =>
      log.info(s"Received a PagedCatchup request for channel '$channel' for '$numberOfMessages' messages before message ID: '$beforeMessageID")
      Try(pagedCatchupChannel(channel, numberOfMessages, beforeMessageID)) match {
        case Success(messages) => sender() ! DbActorCatchupAck(messages)
        case failure           => sender() ! failure.recover(Status.Failure(_))
      }

    case GetAllChannels() =>
      log.info(s"Received a GetAllChannels request")
      Try(getAllChannels) match {
        case Success(setOfChannels) => sender() ! DbActorGetAllChannelsAck(setOfChannels)
        case failure                => sender() ! failure.recover(Status.Failure(_))
      }

    case WriteAndPropagate(channel, message) =>
      log.info(s"Received a WriteAndPropagate request on channel '$channel' for message_id ${message.message_id}")
      Try(writeAndPropagate(channel, message)) match {
        case Success(_) => sender() ! DbActorAck()
        case failure    => sender() ! failure.recover(Status.Failure(_))
      }

    case CreateChannel(channel, objectType) =>
      log.info(s"Received an CreateChannel request for channel '$channel' of type '$objectType'")
      Try(createChannel(channel, objectType)) match {
        case Success(_) => sender() ! DbActorAck()
        case failure    => sender() ! failure.recover(Status.Failure(_))
      }

    case CreateElectionData(laoId, id, keyPair) =>
      log.info(s"Received an CreateElection request for election '$id'" +
        s"\n\tprivate key = ${keyPair.privateKey.toString}" +
        s"\n\tpublic key = ${keyPair.publicKey.toString}")
      Try(createElectionData(laoId, id, keyPair)) match {
        case Success(_) => sender() ! DbActorAck()
        case failure    => sender() ! failure.recover(Status.Failure(_))
      }

    case CreateChannelsFromList(list) =>
      log.info(s"Received a CreateChannelsFromList request for list $list")
      Try(createChannels(list)) match {
        case Success(_) => sender() ! DbActorAck()
        case failure    => sender() ! failure.recover(Status.Failure(_))
      }

    case ChannelExists(channel) =>
      log.info(s"Received an ChannelExists request for channel '$channel'")
      if (checkChannelExistence(channel)) {
        sender() ! DbActorAck()
      } else {
        sender() ! Status.Failure(DbActorNAckException(ErrorCodes.INVALID_ACTION.id, s"channel '$channel' does not exist in db"))
      }

    case AssertChannelMissing(channel) =>
      log.info(s"Received an AssertChannelMissing request for channel '$channel'")
      if (checkChannelExistence(channel)) {
        sender() ! Status.Failure(DbActorNAckException(ErrorCodes.INVALID_ACTION.id, s"channel '$channel' already exists in db"))
      } else {
        sender() ! DbActorAck()
      }

    case AddWitnessSignature(channel, messageId, signature) =>
      log.info(s"Received an AddWitnessSignature request for message_id '$messageId' on channel ${channel.channel} with signature ${signature.toString}")
      Try(addWitnessSignature(channel, messageId, signature)) match {
        case Success(witnessMessage) => sender() ! DbActorAddWitnessSignatureAck(witnessMessage)
        case failure                 => sender() ! failure.recover(Status.Failure(_))
      }

    case ReadRollCallData(laoId) =>
      log.info(s"Received an ReadRollCallData request for RollCall '$laoId'")
      Try(readRollCallData(laoId)) match {
        case Success(rollcallData) => sender() ! DbActorReadRollCallDataAck(rollcallData)
        case failure               => sender() ! failure.recover(Status.Failure(_))
      }

    case WriteRollCallData(laoId, message) =>
      log.info(s"Received a WriteRollCallData request for RollCall id $laoId and message_id ${message.message_id}")
      Try(writeRollCallData(laoId, message)) match {
        case Success(_) => sender() ! DbActorAck()
        case failure    => sender() ! failure.recover(Status.Failure(_))
      }

    case WriteUserAuthenticated(popToken, clientId, user) =>
      log.info(s"Received a WriteUserAuthenticated request for user $user, id $popToken and clientId $clientId")
      Try(storage.write(generateAuthenticatedKey(popToken, clientId) -> user.base64Data.toString())) match {
        case Success(_) => sender() ! DbActorAck()
        case failure    => sender() ! failure.recover(Status.Failure(_))
      }

    case ReadUserAuthenticated(popToken, clientId) =>
      log.info(s"Received a ReadUserAuthenticated request for pop token $popToken and clientId $clientId")
      Try(storage.read(generateAuthenticatedKey(popToken, clientId))) match {
        case Success(Some(id)) => sender() ! DbActorReadUserAuthenticationAck(Some(PublicKey(Base64Data(id))))
        case Success(None)     => sender() ! DbActorReadUserAuthenticationAck(None)
        case failure           => sender() ! failure.recover(Status.Failure(_))
      }

    case ReadServerPublicKey() =>
      log.info(s"Received a ReadServerPublicKey request")
      Try(readServerPublicKey()) match {
        case Success(publicKey) => sender() ! DbActorReadServerPublicKeyAck(publicKey)
        case failure            => sender() ! failure.recover(Status.Failure(_))
      }

    case ReadServerPrivateKey() =>
      log.info(s"Received a ReadServerPrivateKey request")
      Try(readServerPrivateKey()) match {
        case Success(privateKey) => sender() ! DbActorReadServerPrivateKeyAck(privateKey)
        case failure             => sender() ! failure.recover(Status.Failure(_))
      }

    case GenerateHeartbeat() =>
      log.info(s"Received a GenerateHeartbeat request")
      Try(generateHeartbeat()) match {
        case Success(heartbeat) => sender() ! DbActorGenerateHeartbeatAck(heartbeat)
        case failure            => sender() ! failure.recover(Status.Failure(_))
      }

    case WriteRumor(rumor) =>
      log.info(s"Received a WriteRumor request")
      Try(writeRumor(rumor)) match {
        case Success(_) => sender() ! DbActorAck()
        case failure    => sender() ! failure.recover(Status.Failure(_))
      }

    case ReadRumor(desiredRumor) =>
      log.info(s"Received a ReadRumor request for rumor $desiredRumor")
      Try(readRumor(desiredRumor)) match {
        case Success(foundRumor) => sender() ! DbActorReadRumor(foundRumor)
        case failure             => sender() ! failure.recover(Status.Failure(_))
      }

    case ReadRumorData(senderPk) =>
      log.info(s"Received a ReadRumorData request for sender_pk $senderPk")
      Try(readRumorData(senderPk)) match {
        case Success(foundRumorIds) => sender() ! DbActorReadRumorData(foundRumorIds)
        case failure                => sender() ! failure.recover(Status.Failure(_))
      }

    case GenerateRumorStateAns(rumorState: RumorState) =>
      log.info(s"Received a GenerateRumorStateAns request based on rumor_state ${rumorState.state}")
      Try(generateRumorStateAns(rumorState)) match {
        case Success(rumorList) => sender() ! DbActorGenerateRumorStateAns(rumorList)
        case failure            => sender() ! failure.recover(Status.Failure(_))
      }

    case GetRumorState() =>
      log.info(s"Received a GetRumorState request")
      Try(getRumorState) match
        case Success(rumorState) => sender() ! DbActorGetRumorStateAck(rumorState)
        case failure             => sender() ! failure.recover(Status.Failure(_))

    case UpdateNumberOfNewChirpsReactions(channel: Channel) =>
      updateNumberOfNewChirpsReactions(channel, false)

    case ReadFederationChallenge(channel, laoId) =>
      log.info(s"Actor $self (db) received a ReadFederationChallenge request")
      Try(readFederationChallenge(channel, laoId)) match {
        case Success(message) => sender() ! DbActorReadAck(message)
        case failure          => sender() ! failure.recover(Status.Failure(_))
      }

    case ReadFederationExpect(channel, laoId) =>
      log.info(s"Actor $self (db) received a ReadFederationExpect request")
      Try(readFederationExpect(channel, laoId)) match {
        case Success(message) => sender() ! DbActorReadAck(message)
        case failure          => sender() ! failure.recover(Status.Failure(_))
      }

    case ReadFederationInit(channel, laoId) =>
      log.info(s"Actor $self (db) received a ReadFederationInit request")
      Try(readFederationInit(channel, laoId)) match {
        case Success(message) => sender() ! DbActorReadAck(message)
        case failure          => sender() ! failure.recover(Status.Failure(_))
      }

    case ReadFederationResult(channel, laoId) =>
      log.info(s"Actor $self (db) received a ReadFederationResult request")
      Try(readFederationResult(channel, laoId)) match {
        case Success(message) => sender() ! DbActorReadAck(message)
        case failure          => sender() ! failure.recover(Status.Failure(_))
      }

    case ReadFederationTokensExchange(channel, laoId) =>
      log.info(s"Actor $self (db) received a ReadFederationResult request")
      Try(readFederationResult(channel, laoId)) match {
        case Success(message) => sender() ! DbActorReadAck(message)
        case failure          => sender() ! failure.recover(Status.Failure(_))
      }  

    case WriteFederationChallenge(channel, laoId, message) =>
      log.info(s"Actor $self (db) received a WriteFederationChallenge request")
      Try(writeFederationChallenge(channel, laoId, message)) match {
        case Success(_) => sender() ! DbActorAck()
        case failure    => sender() ! failure.recover(Status.Failure(_))
      }

    case WriteFederationExpect(channel, laoId, message) =>
      log.info(s"Actor $self (db) received a WriteFederationExpect request")
      Try(writeFederationExpect(channel, laoId, message)) match {
        case Success(_) => sender() ! DbActorAck()
        case failure    => sender() ! failure.recover(Status.Failure(_))
      }

    case WriteFederationInit(channel, laoId, message) =>
      log.info(s"Actor $self (db) received a WriteFederationInit request")
      Try(writeFederationInit(channel, laoId, message)) match {
        case Success(_) => sender() ! DbActorAck()
        case failure    => sender() ! failure.recover(Status.Failure(_))
      }

    case WriteFederationResult(channel, laoId, message) =>
      log.info(s"Actor $self (db) received a WriteFederationResult request")
      Try(writeFederationResult(channel, laoId, message)) match {
        case Success(_) => sender() ! DbActorAck()
        case failure    => sender() ! failure.recover(Status.Failure(_))
      }

    case WriteFederationTokensExchange(channel, laoId, message) =>
      log.info(s"Actor $self (db) received a WriteFederationTokensExchange request")
      Try(writeFederationTokensExchange(channel, laoId, message)) match {
        case Success(_) => sender() ! DbActorAck()
        case failure    => sender() ! failure.recover(Status.Failure(_))
      }  

    case DeleteFederationChallenge(channel, laoId) =>
      log.info(s"Actor $self (db) received a DeleteFederationChallenge request")
      Try(deleteFederationChallenge(channel, laoId)) match {
        case Success(_) => sender() ! DbActorAck()
        case failure    => sender() ! failure.recover(Status.Failure(_))
      }

    case m =>
      log.warning(s"Received an unknown message $m")
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

  /** Requests the db to build out rumorState +
    */
  final case class GetRumorState() extends Event

  /** Requests the Db for the challenge stored
    * @param channel
    *   the channel in which the challenge is being sent
    * @param laoId
    *   the id of the lao in which the challenge message is
    */
  final case class ReadFederationChallenge(channel: Channel, laoId: Hash) extends Event

  /** Requests the Db for the federationExpect message
    * @param channel
    *   the channel in which the federationExpect is being sent
    * @param laoId
    *   the id of the lao in which the federationExpect message is
    */
  final case class ReadFederationExpect(channel: Channel, laoId: Hash) extends Event

  /** Requests the Db for the federationInit message
    *
    * @param channel
    *   the channel in which the federationInit is being sent
    * @param laoId
    *   the id of the lao in which the federationInit message is
    */
  final case class ReadFederationInit(channel: Channel, laoId: Hash) extends Event

  /** Requests the Db for the federationResult message
    *
    * @param channel
    *   the channel in which the federationResult is being sent
    * @param laoId
    *   the id of the lao in which the federationResult message is
    */
  final case class ReadFederationResult(channel: Channel, laoId: Hash) extends Event

  /**
   * Requests the Db for the federationTokensExchange message 
   * @param channel the channel in which the federationTokensExchange is being sent
   * @param laoId the id of the lao in which the federationTokensExchange message is
   */
  final case class ReadFederationTokensExchange(channel: Channel, laoId: Hash) extends Event

  /** Requests the Db to write the challenge
    * @param channel
    *   the channel in which the challenge is being sent
    * @param laoId
    *   the id of the lao in which the challenge message is
    * @param message
    *   the challenge message
    */
  final case class WriteFederationChallenge(channel: Channel, laoId: Hash, message: Message) extends Event

  /** Requests the Db to write the federationExpect
    *
    * @param channel
    *   the channel in which the federationExpect is being sent
    * @param laoId
    *   the id of the lao in which the federationExpect message is
    * @param message
    *   the federationExpect message
    */
  final case class WriteFederationExpect(channel: Channel, laoId: Hash, message: Message) extends Event

  /** Requests the Db to write the federationInit
    *
    * @param channel
    *   the channel in which the federationInit is being sent
    * @param laoId
    *   the id of the lao in which the federationInit message is
    * @param message
    *   the federationInit message
    */
  final case class WriteFederationInit(channel: Channel, laoId: Hash, message: Message) extends Event

  /** Requests the Db to write the federationResult
    *
    * @param channel
    *   the channel in which the federationResult is being sent
    * @param laoId
    *   the id of the lao in which the federationResult message is
    * @param message
    *   the federationResult message
    */
  final case class WriteFederationResult(channel: Channel, laoId: Hash, message: Message) extends Event

  /**
   * Requests the Db to write the federationTokensExchange
   * @param channel the channel in which the federationTokensExchange is being sent
   * @param laoId the id of the lao in which the federationTokensExchange message is
   * @param message the federationTokensExchange message
   */
  final case class WriteFederationTokensExchange(channel: Channel, laoId: Hash, message: Message) extends Event

  /** Requests the Db to delete the challenge
    * @param channel
    *   the channel in which the challenge is being sent
    * @param laoId
    *   the id of the lao in which the challenge message is
    */
  final case class DeleteFederationChallenge(channel: Channel, laoId: Hash) extends Event

  /** Requests the Db to update the number of chirps reaction events since last top chirps request
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

  /** Response for a [[GetRumorState]] +
    */
  final case class DbActorGetRumorStateAck(rumorState: RumorState) extends DbActorMessage

  /** Response for a general db actor ACK
    */
  final case class DbActorAck() extends DbActorMessage

}
