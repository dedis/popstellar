package ch.epfl.pop.pubsub.graph.validators

import akka.pattern.AskableActorRef
import ch.epfl.pop.model.network.JsonRpcRequest
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.network.method.message.data.ObjectType
import ch.epfl.pop.model.network.method.message.data.election.{CastVoteElection, EndElection, OpenElection, ResultElection, SetupElection}
import ch.epfl.pop.model.objects.{Base64Data, Channel, Hash, PublicKey}
import ch.epfl.pop.pubsub.graph.validators.MessageValidator._
import ch.epfl.pop.pubsub.graph.{ErrorCodes, GraphMessage, PipelineError}
import ch.epfl.pop.storage.DbActor
import ch.epfl.pop.storage.DbActor.DbActorReadAck

import scala.collection.mutable
import scala.concurrent.Await
import scala.util.Success

//Similarly to the handlers, we create a ElectionValidator object which creates a ElectionValidator class instance.
//The defaults dbActorRef is used in the object, but the class can now be mocked with a custom dbActorRef for testing purpose
object ElectionValidator extends MessageDataContentValidator with EventValidator {

  val electionValidator = new ElectionValidator(DbActor.getInstance)

  override val EVENT_HASH_PREFIX: String = electionValidator.EVENT_HASH_PREFIX

  def validateSetupElection(rpcMessage: JsonRpcRequest): GraphMessage = electionValidator.validateSetupElection(rpcMessage)

  def validateOpenElection(rpcMessage: JsonRpcRequest): GraphMessage = electionValidator.validateOpenElection(rpcMessage)

  def validateCastVoteElection(rpcMessage: JsonRpcRequest): GraphMessage = electionValidator.validateCastVoteElection(rpcMessage)

  def validateResultElection(rpcMessage: JsonRpcRequest): GraphMessage = electionValidator.validateResultElection(rpcMessage)

  def validateEndElection(rpcMessage: JsonRpcRequest): GraphMessage = electionValidator.validateEndElection(rpcMessage)
}

sealed class ElectionValidator(dbActorRef: => AskableActorRef) extends MessageDataContentValidator with EventValidator {

  override val EVENT_HASH_PREFIX: String = "Election"

  private val HASH_ERROR: Hash = Hash(Base64Data(""))

  def validateSetupElection(rpcMessage: JsonRpcRequest): GraphMessage = {
    def validationError(reason: String): PipelineError = super.validationError(reason, "SetupElection", rpcMessage.id)

    rpcMessage.getParamsMessage match {
      case Some(message: Message) =>
        val data: SetupElection = message.decodedData.get.asInstanceOf[SetupElection]

        val laoId: Hash = rpcMessage.extractLaoId
        val expectedHash: Hash = Hash.fromStrings(EVENT_HASH_PREFIX, laoId.toString, data.created_at.toString, data.name)

        val sender: PublicKey = message.sender
        val channel: Channel = rpcMessage.getParamsChannel

        if (!validateTimestampStaleness(data.created_at)) {
          Right(validationError(s"stale 'created_at' timestamp (${data.created_at})"))
        } else if (!validateTimestampOrder(data.created_at, data.start_time)) {
          Right(validationError(s"'start_time' (${data.start_time}) timestamp is smaller than 'created_at' (${data.created_at})"))
        } else if (!validateTimestampOrder(data.start_time, data.end_time)) {
          Right(validationError(s"'end_time' (${data.end_time}) timestamp is smaller than 'start_time' (${data.start_time})"))
        } else if (expectedHash != data.id) {
          Right(validationError("unexpected id"))
        } else if (!validateOwner(sender, channel, dbActorRef)) {
          Right(validationError(s"invalid sender $sender"))
        } //note: the SetupElection is the only message sent to the main channel, others are sent in an election channel
        else if (!validateChannelType(ObjectType.LAO, channel, dbActorRef)) {
          Right(validationError(s"trying to send a SetupElection message on a wrong type of channel $channel"))
        } else {
          Left(rpcMessage)
        }

      case _ => Right(validationErrorNoMessage(rpcMessage.id))
    }
  }

  def validateOpenElection(rpcMessage: JsonRpcRequest): GraphMessage = {
    def validationError(reason: String): PipelineError = super.validationError(reason, "OpenElection", rpcMessage.id)

    rpcMessage.getParamsMessage match {
      case Some(message: Message) =>
        val data: OpenElection = message.decodedData.get.asInstanceOf[OpenElection]

        val channel: Channel = rpcMessage.getParamsChannel

        val electionId: Hash = channel.extractChildChannel
        val sender: PublicKey = message.sender

        val laoId: Hash = channel.decodeChannelLaoId.getOrElse(HASH_ERROR)

        if (!validateTimestampStaleness(data.opened_at)) {
          Right(validationError(s"stale 'opened_at' timestamp (${data.opened_at})"))
        } else if (electionId !=  data.election) {
          Right(validationError("Unexpected election id"))
        } else if (laoId != data.lao) {
          Right(validationError("Unexpected lao id"))
        } else if (!validateOwner(sender, channel, dbActorRef)) {
          Right(validationError(s"Sender $sender has an invalid PoP token."))
        } else if (!validateChannelType(ObjectType.ELECTION, channel, dbActorRef)) {
          Right(validationError(s"trying to send a OpenElection message on a wrong type of channel $channel"))
        } else {
          Left(rpcMessage)
        }

      case _ => Right(validationErrorNoMessage(rpcMessage.id))
    }
  }

  def validateCastVoteElection(rpcMessage: JsonRpcRequest): GraphMessage = {
    def validationError(reason: String): PipelineError = super.validationError(reason, "CastVoteElection", rpcMessage.id)

    rpcMessage.getParamsMessage match {
      case Some(message: Message) =>
        val data: CastVoteElection = message.decodedData.get.asInstanceOf[CastVoteElection]

        val channel: Channel = rpcMessage.getParamsChannel

        val electionId: Hash = channel.extractChildChannel
        val sender: PublicKey = message.sender

        val laoId: Hash = channel.decodeChannelLaoId.getOrElse(HASH_ERROR)

        val setupMessage: SetupElection = getSetupMessage(channel)
        val questions = setupMessage.questions
        val q2Ballots = questions.map(question => question.id -> question.ballot_options).toMap

        if (!validateTimestampStaleness(data.created_at)) {
          Right(validationError(s"stale 'created_at' timestamp (${data.created_at})"))
        } else if (electionId != data.election) {
          Right(validationError("unexpected election id"))
        } else if (laoId != data.lao) {
          Right(validationError("unexpected lao id"))
          //  check it the question id exists
        } else if (!data.votes.map(_.question).forall(question => q2Ballots.contains(question))) {
          Right(validationError(s"Incorrect parameter questionId"))
        } else if (!data.votes.forall(
          voteElection => {
            val vote = voteElection.vote match {
              case Some(v :: _) => v
              case _ => -1
            }
            // check if the ballot is available
            vote < q2Ballots(voteElection.question).size
          }
        )) {
          Right(validationError(s"Incorrect parameter ballot"))
          // check for question id duplication
        } else if (data.votes.map(_.question).distinct.length != data.votes.length) {
          Right(validationError(s"The castvote contains twice the same question id"))
          // check open and end constraints
        } else if (getOpenMessage(channel).isEmpty) {
          Right(validationError(s"This election has not started yet"))
        } else if (getEndMessage(channel).isDefined) {
          Right(validationError(s"This election has already ended"))
        } else if (!validateAttendee(sender, channel, dbActorRef)) {
          Right(validationError(s"Sender $sender has an invalid PoP token."))
        } else if (!validateChannelType(ObjectType.ELECTION, channel, dbActorRef)) {
          Right(validationError(s"trying to send a CastVoteElection message on a wrong type of channel $channel"))
        } else {
          Left(rpcMessage)
        }
      case _ => Right(validationErrorNoMessage(rpcMessage.id))
    }
  }

  private def getOpenMessage(electionChannel: Channel): Option[OpenElection] = {
    var result: Option[OpenElection] = None
    Await.ready(dbActor ? DbActor.ReadChannelData(electionChannel), duration).value match {
      case Some(Success(DbActor.DbActorReadChannelDataAck(channelData))) =>
        for (message <- channelData.messages) {
          (Await.ready(dbActor ? DbActor.Read(electionChannel, message), duration).value match {
            case Some(Success(value)) => value
            case _ => None
          }) match {
            case DbActorReadAck(Some(message)) =>
              try {
                result = Some(OpenElection.buildFromJson(message.data.decodeToString()))
              } catch {
                case exception: Throwable => print(exception)
              }
          }
        }
      case _ =>
    }
    result
  }

  private def getEndMessage(electionChannel: Channel): Option[EndElection] = {
    var result: Option[EndElection] = None
    Await.ready(dbActor ? DbActor.ReadChannelData(electionChannel), duration).value match {
      case Some(Success(DbActor.DbActorReadChannelDataAck(channelData))) =>
        for (message <- channelData.messages) {
          (Await.ready(dbActor ? DbActor.Read(electionChannel, message), duration).value match {
            case Some(Success(value)) => value
            case _ => None
          }) match {
            case DbActorReadAck(Some(message)) =>
              try {
                result = Some(EndElection.buildFromJson(message.data.decodeToString()))
              } catch {
                case exception: Throwable => print(exception)
              }
          }
        }
      case _ =>
    }
    result
  }

  //not implemented since the back end does not recieve a ResultElection message coming from the front end
  def validateResultElection(rpcMessage: JsonRpcRequest): GraphMessage = {
    Right(PipelineError(ErrorCodes.SERVER_ERROR.id, "NOT IMPLEMENTED: ElectionHandler cannot handle ResultElection messages yet", rpcMessage.id))
    //TODO: needs to be implemented when server to server communication will be done
    /*rpcMessage.getParamsMessage match {
      case Some(message) =>
        val data: ResultElection = message.decodedData.get.asInstanceOf[ResultElection]

        val sender: PublicKey = message.sender
        val channel: Channel = rpcMessage.getParamsChannel

        if (!validateOwner(sender, channel, dbActorRef)) {
          Right(validationError(s"invalid sender $sender"))
        } else if (!validateChannelType(ObjectType.ELECTION, channel, dbActorRef)) {
          Right(validationError(s"trying to send a ResultElection message on a wrong type of channel $channel"))
        } else {
          Left(rpcMessage)
        }
      case _ => Right(validationErrorNoMessage(rpcMessage.id))
    }*/
  }

  def validateEndElection(rpcMessage: JsonRpcRequest): GraphMessage = {
    def validationError(reason: String): PipelineError = super.validationError(reason, "EndElection", rpcMessage.id)

    rpcMessage.getParamsMessage match {
      case Some(message: Message) =>
        val data: EndElection = message.decodedData.get.asInstanceOf[EndElection]

        val channel: Channel = rpcMessage.getParamsChannel

        val electionId: Hash = channel.extractChildChannel

        val sender: PublicKey = message.sender

        val laoId: Hash = channel.decodeChannelLaoId.getOrElse(HASH_ERROR)

        if (!validateTimestampStaleness(data.created_at)) {
          Right(validationError(s"stale 'created_at' timestamp (${data.created_at})"))
        } else if (electionId != data.election) {
          Right(validationError("unexpected election id"))
        } else if (laoId != data.lao) {
          Right(validationError("unexpected lao id"))
        } else if (!validateOwner(sender, channel, dbActorRef)) {
          Right(validationError(s"invalid sender $sender"))
        } else if (!validateChannelType(ObjectType.ELECTION, channel, dbActorRef)) {
          Right(validationError(s"trying to send a EndElection message on a wrong type of channel $channel"))
          // TODO : check it - is this check ok ?
          /*} else if (!compareResults(getLastVotes(channel).flatMap(_.votes.map(_.id)), data.registered_votes)) {
            Right(validationError(s"Incorrect verification hash"))*/
        } else {
          Left(rpcMessage)
        }

      case _ => Right(validationErrorNoMessage(rpcMessage.id))
    }
  }

  def getSetupMessage(electionChannel: Channel, dbActor: AskableActorRef = DbActor.getInstance): SetupElection = {
    var result: SetupElection = null
    Await.ready(dbActor ? DbActor.ReadChannelData(electionChannel), duration).value match {
      case Some(Success(DbActor.DbActorReadChannelDataAck(channelData))) =>
        for (message <- channelData.messages) {
          (Await.ready(dbActor ? DbActor.Read(electionChannel, message), duration).value match {
            case Some(Success(value)) => value
            case _ => None
          }) match {
            case DbActorReadAck(Some(message)) =>
              try {
                result = SetupElection.buildFromJson(message.data.decodeToString())
              } catch {
                case exception: Throwable => print(exception)
              }
          }
        }
      case _ =>
    }
    result
  }

  def getLastVotes(electionChannel: Channel): List[CastVoteElection] =
    Await.ready(dbActor ? DbActor.ReadChannelData(electionChannel), duration).value match {
      case Some(Success(DbActor.DbActorReadChannelDataAck(channelData))) =>
        val messages: List[Hash] = channelData.messages
        val lastVotes: mutable.HashMap[PublicKey, CastVoteElection] = new mutable.HashMap()
        for (messageIdHash <- messages) {
          val dbAnswer =
            Await.ready(dbActor ? DbActor.Read(channel = electionChannel, messageId = messageIdHash), duration).value match {
              case Some(Success(value)) => value
              case _ => None
            }
          dbAnswer match {
            case DbActorReadAck(Some(message)) =>
              val sender = message.sender
              // FIXME : the message contains the correct base64-encoded json in data, but message.decodedData return None
              /*message.decodedData match {
                case Some(castVote: CastVoteElection) =>
                  if (!lastVotes.contains(sender) || !(castVote.created_at < lastVotes(sender).created_at))
                    lastVotes.update(sender, castVote)
                case _ =>
                  ()
              }*/
              try {
                val castVote: CastVoteElection = CastVoteElection.buildFromJson(message.data.decodeToString())
                if (!lastVotes.contains(sender) || !(castVote.created_at < lastVotes(sender).created_at))
                  lastVotes.update(sender, castVote)
              } catch {
                case exception: Throwable => print(exception)
              }
            case _ =>
          }
        }
        println(lastVotes)
        lastVotes.values.toList
      case _ => Nil
    }

  private def compareResults(messages: List[Hash], checkHash: Hash): Boolean =
    Hash(Base64Data(messages.map(_.toString).sorted.foldLeft("")(_ + _))) == checkHash
}
