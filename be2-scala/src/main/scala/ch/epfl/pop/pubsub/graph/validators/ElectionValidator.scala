package ch.epfl.pop.pubsub.graph.validators

import akka.pattern.AskableActorRef
import ch.epfl.pop.model.network.JsonRpcRequest
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.network.method.message.data.ObjectType
import ch.epfl.pop.model.network.method.message.data.election._
import ch.epfl.pop.model.objects.ElectionChannel._
import ch.epfl.pop.model.objects.{Base64Data, Channel, Hash}
import ch.epfl.pop.pubsub.graph.validators.MessageValidator._
import ch.epfl.pop.pubsub.graph.{ErrorCodes, GraphMessage, PipelineError}
import ch.epfl.pop.storage.DbActor

import scala.concurrent.Await

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

  def validateKeyElection(rpcMessage: JsonRpcRequest): GraphMessage = electionValidator.validateKeyElection(rpcMessage)
}

sealed class ElectionValidator(dbActorRef: => AskableActorRef) extends MessageDataContentValidator with EventValidator {

  override val EVENT_HASH_PREFIX: String = "Election"

  private val HASH_ERROR: Hash = Hash(Base64Data(""))

  def validateSetupElection(rpcMessage: JsonRpcRequest): GraphMessage = {
    def validationError(reason: String): PipelineError = super.validationError(reason, "SetupElection", rpcMessage.id)

    rpcMessage.getParamsMessage match {
      case Some(message: Message) =>
        val (setupElection, _, senderPK, channel) = extractData[SetupElection](rpcMessage)

        val electionId = channel.extractChildChannel
        val expectedHash: Hash = Hash.fromStrings(EVENT_HASH_PREFIX, electionId.toString, setupElection.created_at.toString, setupElection.name)

        runChecks(
          checkTimestampStaleness(
            rpcMessage,
            setupElection.created_at,
            validationError(s"stale 'created_at' timestamp (${setupElection.created_at})")
          ),
          checkTimestampOrder(
            rpcMessage,
            setupElection.created_at,
            setupElection.start_time,
            validationError(
              s"'start_time' (${setupElection.start_time}) timestamp is smaller than 'created_at' (${setupElection.created_at})"
            )
          ),
          checkTimestampOrder(
            rpcMessage,
            setupElection.start_time,
            setupElection.end_time,
            validationError(
              s"'end_time' (${setupElection.end_time}) timestamp is smaller than 'start_time' (${setupElection.start_time})"
            )
          ),
          checkId(
            rpcMessage,
            expectedHash,
            setupElection.id,
            validationError("unexpected id")
          ),
          checkQuestionId(
            rpcMessage,
            setupElection.questions,
            setupElection.id,
            validationError("unexpected question ids")
          ),
          checkOwner(
            rpcMessage,
            senderPK,
            channel,
            dbActorRef,
            validationError(s"invalid sender $senderPK")
          ),
          // note: the SetupElection is the only message sent to the main channel, others are sent in an election channel
          checkChannelType(
            rpcMessage,
            ObjectType.LAO,
            channel,
            dbActorRef,
            validationError(s"trying to send a SetupElection message on a wrong type of channel $channel")
          )
        )

      case _ => Right(validationErrorNoMessage(rpcMessage.id))
    }
  }

  def validateKeyElection(rpcMessage: JsonRpcRequest): GraphMessage = {
    def validationError(reason: String): PipelineError = super.validationError(reason, "KeyElection", rpcMessage.id)

    rpcMessage.getParamsMessage match {
      case Some(message: Message) =>
        val (keyElection, laoId, senderPK, channel) = extractData[KeyElection](rpcMessage)

        val electionId = channel.extractChildChannel

        runChecks(
          checkChannelType(
            rpcMessage,
            ObjectType.ELECTION,
            channel,
            dbActorRef,
            validationError(s"trying to send a KeyElection message on a wrong type of channel $channel")
          ),
          checkId(
            rpcMessage,
            electionId,
            keyElection.election,
            validationError("Unexpected election id")
          ),
          checkOwner(
            rpcMessage,
            senderPK,
            channel,
            dbActorRef,
            validationError(s"Sender $senderPK has an invalid PoP token.")
          )
        )

      case _ => Right(validationErrorNoMessage(rpcMessage.id))
    }
  }

  def validateOpenElection(rpcMessage: JsonRpcRequest): GraphMessage = {
    def validationError(reason: String): PipelineError = super.validationError(reason, "OpenElection", rpcMessage.id)

    rpcMessage.getParamsMessage match {
      case Some(message: Message) =>
        val (openElection, laoId, senderPK, channel) = extractData[OpenElection](rpcMessage)

        val electionId = channel.extractChildChannel

        runChecks(
          checkTimestampStaleness(
            rpcMessage,
            openElection.opened_at,
            validationError(s"stale 'opened_at' timestamp (${openElection.opened_at})")
          ),
          checkId(
            rpcMessage,
            laoId,
            openElection.lao,
            validationError("Unexpected lao id")
          ),
          checkId(
            rpcMessage,
            electionId,
            openElection.election,
            validationError("Unexpected election id")
          ),
          checkOwner(
            rpcMessage,
            senderPK,
            channel,
            dbActorRef,
            validationError(s"Sender $senderPK has an invalid PoP token.")
          ),
          checkChannelType(
            rpcMessage,
            ObjectType.ELECTION,
            channel,
            dbActorRef,
            validationError(
              s"trying to send a OpenElection message on a wrong type of channel $channel"
            )
          )
        )

      case _ => Right(validationErrorNoMessage(rpcMessage.id))
    }
  }

  def validateCastVoteElection(rpcMessage: JsonRpcRequest): GraphMessage = {
    def validationError(reason: String): PipelineError = super.validationError(reason, "CastVoteElection", rpcMessage.id)

    rpcMessage.getParamsMessage match {
      case Some(message: Message) =>
        val data: CastVoteElection = message.decodedData.get.asInstanceOf[CastVoteElection]

        val channel: Channel = rpcMessage.getParamsChannel
        val questions = Await.result(channel.getSetupMessage(dbActorRef), duration).questions
        val sender: PublicKey = message.sender

        if (!validateTimestampStaleness(data.created_at)) {
          Right(validationError(s"stale 'created_at' timestamp (${data.created_at})"))
        } else if (channel.extractChildChannel != data.election) {
          Right(validationError("unexpected election id"))
        } else if (channel.decodeChannelLaoId.getOrElse(HASH_ERROR) != data.lao) {
          Right(validationError("unexpected lao id"))
        } else if (!validateVoteElection(data.election, data.votes, questions)) {
          Right(validationError(s"invalid votes"))
          // check open and end constraints
        } else if (getOpenMessage(channel).isEmpty) {
          Right(validationError(s"This election has not started yet"))
        } else if (getEndMessage(channel).isDefined) {
          Right(validationError(s"This election has already ended"))
        } else if (!validateAttendee(sender, channel, dbActorRef)) {
          Right(validationError(s"Sender ${sender} has an invalid PoP token."))
        } else if (!validateChannelType(ObjectType.ELECTION, channel, dbActorRef)) {
          Right(validationError(s"trying to send a CastVoteElection message on a wrong type of channel $channel"))
        } else {
          Left(rpcMessage)
        }
      case _ => Right(validationErrorNoMessage(rpcMessage.id))
    }
  }

  // checks if the votes are valid: valid question ids, valid ballot options and valid vote ids
  private def validateVoteElection(electionId: Hash, votes: List[VoteElection], questions: List[ElectionQuestion]): Boolean = {
    val q2Ballots: Map[Hash, List[String]] = questions.map(question => question.id -> question.ballot_options).toMap
    val questionsId: List[Hash] = questions.map(_.id)

    votes.forall(vote =>
      vote.vote match {
        case Some(Left(index)) =>
          questionsId.contains(vote.question) &&
            index < q2Ballots(vote.question).size &&
            vote.id == Hash.fromStrings("Vote", electionId.toString, vote.question.toString, index.toString)
        case Some(Right(encryptedVote)) =>
          questionsId.contains(vote.question) && vote.id == Hash.fromStrings("Vote", electionId.toString, vote.question.toString, encryptedVote.toString)
        case _ => false
      }
    )
  }

  private def getOpenMessage(electionChannel: Channel): Option[OpenElection] =
    Await.result(electionChannel.extractMessages[OpenElection](dbActorRef), duration) match {
      case h :: _ => Some(h._2)
      case _      => None
    }

  private def getEndMessage(electionChannel: Channel): Option[EndElection] =
    Await.result(electionChannel.extractMessages[EndElection](dbActorRef), duration) match {
      case h :: _ => Some(h._2)
      case _      => None
    }

  // not implemented since the back end does not receive a ResultElection message coming from the front end
  def validateResultElection(rpcMessage: JsonRpcRequest): GraphMessage = {
    Right(PipelineError(ErrorCodes.SERVER_ERROR.id, "NOT IMPLEMENTED: ElectionValidator cannot handle ResultElection messages yet", rpcMessage.id))
  }

  /** checks if the question ids are valid wrt protocol
    *
    * @param rpcMessage
    *   rpc message to validate
    * @param questions
    *   the question to compute the id from
    * @param electionId
    *   the election_id needed to compute the questions id
    * @param error
    *   the error to forward in case of invalid id
    * @return
    *   GraphMessage: passes the rpcMessages to Left if successful right with pipeline error
    */
  private def checkQuestionId(
      rpcMessage: JsonRpcRequest,
      questions: List[ElectionQuestion],
      electionId: Hash,
      error: PipelineError
  ): GraphMessage = {
    val isQuestionIdValid: Boolean =
      questions.forall(q =>
        q.id == Hash.fromStrings("Question", electionId.toString, q.question)
      )

    if (isQuestionIdValid)
      Left(rpcMessage)
    else
      Right(error)
  }

  /** @param castVotes
    *   List of pairs of messages and castVote data
    * @param checkHash
    *   The hash of the concatenated votes (i.e. registered_votes)
    * @return
    *   True if the hashes are the same, false otherwise
    */
  private def compareResults(castVotes: List[CastVoteElection], checkHash: Hash): Boolean = {
    val votes: List[VoteElection] = castVotes.flatMap(_.votes)
    val sortedVotes: List[VoteElection] = votes.sortBy(_.id.toString)
    val computedHash = Hash.fromStrings(sortedVotes.map(_.id.toString): _*)
    computedHash == checkHash
  }
}
