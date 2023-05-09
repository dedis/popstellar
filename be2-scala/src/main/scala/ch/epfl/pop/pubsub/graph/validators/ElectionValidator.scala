package ch.epfl.pop.pubsub.graph.validators

import akka.pattern.AskableActorRef
import ch.epfl.pop.model.network.JsonRpcRequest
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.network.method.message.data.ObjectType
import ch.epfl.pop.model.network.method.message.data.election._
import ch.epfl.pop.model.objects.ElectionChannel._
import ch.epfl.pop.model.objects.{Channel, Hash}
import ch.epfl.pop.pubsub.graph.validators.MessageValidator._
import ch.epfl.pop.pubsub.graph.{ErrorCodes, GraphMessage, PipelineError}
import ch.epfl.pop.storage.DbActor

import scala.concurrent.Await
import scala.util.{Success, Failure}

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

  def validateSetupElection(rpcMessage: JsonRpcRequest): GraphMessage = {
    def validationError(reason: String): PipelineError = super.validationError(reason, "SetupElection", rpcMessage.id)

    rpcMessage.getParamsMessage match {
      case Some(_: Message) =>
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
      case _ => Left(validationErrorNoMessage(rpcMessage.id))
    }
  }

  def validateKeyElection(rpcMessage: JsonRpcRequest): GraphMessage = {
    def validationError(reason: String): PipelineError = super.validationError(reason, "KeyElection", rpcMessage.id)

    rpcMessage.getParamsMessage match {
      case Some(_: Message) =>
        val (keyElection, _, senderPK, channel) = extractData[KeyElection](rpcMessage)

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

      case _ => Left(validationErrorNoMessage(rpcMessage.id))
    }
  }

  def validateOpenElection(rpcMessage: JsonRpcRequest): GraphMessage = {
    def validationError(reason: String): PipelineError = super.validationError(reason, "OpenElection", rpcMessage.id)

    rpcMessage.getParamsMessage match {
      case Some(_: Message) =>
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
      case _ => Left(validationErrorNoMessage(rpcMessage.id))
    }
  }

  def validateCastVoteElection(rpcMessage: JsonRpcRequest): GraphMessage = {
    def validationError(reason: String): PipelineError = super.validationError(reason, "CastVoteElection", rpcMessage.id)

    rpcMessage.getParamsMessage match {
      case Some(_: Message) =>
        val (casteVote, laoId, senderPK, channel) = extractData[CastVoteElection](rpcMessage)

        val electionId = channel.extractChildChannel
        val questions = Await.ready(channel.getSetupMessage(dbActorRef), duration).value.get match {
          case Success(setupElection) => setupElection.questions
          case Failure(exception)     => return Left(validationError("Failed to get election questions: " + exception.getMessage))
          case err @ _                => return Left(validationError("Unknown error: " + err.toString))
        }

        runChecks(
          checkTimestampStaleness(
            rpcMessage,
            casteVote.created_at,
            validationError(s"stale 'created_at' timestamp (${casteVote.created_at})")
          ),
          checkId(
            rpcMessage,
            laoId,
            casteVote.lao,
            validationError("unexpected lao id")
          ),
          checkId(
            rpcMessage,
            electionId,
            casteVote.election,
            validationError("unexpected election id")
          ),
          checkValidateVoteElection(
            rpcMessage,
            casteVote.election,
            casteVote.votes,
            questions,
            validationError(s"invalid votes")
          ),
          // check open and end constraints
          checkElectionStarted(
            rpcMessage,
            channel,
            dbActorRef,
            validationError(s"This election has not started yet")
          ),
          checkElectionNotEnded(
            rpcMessage,
            channel,
            dbActorRef,
            validationError(s"This election has already ended")
          ),
          checkAttendee(
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
            validationError(s"trying to send a CastVoteElection message on a wrong type of channel $channel")
          )
        )

      case _ => Left(validationErrorNoMessage(rpcMessage.id))
    }
  }

// TODO: Proper validation
  def validateResultElection(rpcMessage: JsonRpcRequest): GraphMessage = {
    Right(rpcMessage)
  }

  def validateEndElection(rpcMessage: JsonRpcRequest): GraphMessage = {
    def validationError(reason: String): PipelineError = super.validationError(reason, "EndElection", rpcMessage.id)

    rpcMessage.getParamsMessage match {
      case Some(_: Message) =>
        val (endElection, laoId, senderPK, channel) = extractData[EndElection](rpcMessage)

        val electionId = channel.extractChildChannel

        val firstCheck = runChecks(
          checkTimestampStaleness(
            rpcMessage,
            endElection.created_at,
            validationError(s"stale 'created_at' timestamp (${endElection.created_at})")
          ),
          checkId(
            rpcMessage,
            electionId,
            endElection.election,
            validationError("unexpected election id")
          ),
          checkId(
            rpcMessage,
            laoId,
            endElection.lao,
            validationError("unexpected lao id")
          ),
          checkOwner(
            rpcMessage,
            senderPK,
            channel,
            dbActorRef,
            validationError(s"invalid sender $senderPK")
          ),
          checkChannelType(
            rpcMessage,
            ObjectType.ELECTION,
            channel,
            dbActorRef,
            validationError(s"trying to send a EndElection message on a wrong type of channel $channel")
          )
        )

        // Until runChecks() can be set to be call-by-name this two part check is required to pass the unit tests
        lazy val secondCheck = checkVoteResults(
          rpcMessage,
          channel,
          endElection.registered_votes,
          validationError(s"Incorrect verification hash")
        )

        if (firstCheck.isRight)
          secondCheck
        else
          firstCheck

      case _ => Left(validationErrorNoMessage(rpcMessage.id))
    }
  }

  /** checks if the election has not ended
    *
    * @param rpcMessage
    *   the rpc message to validate
    * @param electionChannel
    *   the election channel
    * @param dbActorRef
    *   the dbActor to ask for end election message
    * @param error
    *   the error to forward in case the election has ended
    * @return
    *   GraphMessage: passes the rpcMessages to Right if successful else Left with pipeline error
    */
  private def checkElectionNotEnded(
      rpcMessage: JsonRpcRequest,
      electionChannel: Channel,
      dbActorRef: AskableActorRef,
      error: PipelineError
  ): GraphMessage =
    Await.result(electionChannel.extractMessages[EndElection](dbActorRef), duration) match {
      case h :: _ => Left(error)
      case _      => Right(rpcMessage)
    }

  /** checks if the votes are valid: valid question ids, valid ballot options and valid vote ids
    *
    * @param rpcMessage
    *   rpc message to validate
    * @param electionId
    *   the election id needed to compute votes ids
    * @param votes
    *   the votes to validate
    * @param questions
    *   the questions asked to the voters
    * @param error
    *   the error to forward in case of invalid vote(s)
    * @return
    *   GraphMessage: passes the rpcMessages to Right if successful else Left with pipeline error
    */
  private def checkValidateVoteElection(
      rpcMessage: JsonRpcRequest,
      electionId: Hash,
      votes: List[VoteElection],
      questions: List[ElectionQuestion],
      error: PipelineError
  ): GraphMessage = {

    val q2Ballots: Map[Hash, List[String]] = questions.map(question => question.id -> question.ballot_options).toMap
    val questionsId: List[Hash] = questions.map(_.id)

    val areVotesValid = votes.forall(vote =>
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

    if (areVotesValid)
      Right(rpcMessage)
    else
      Left(error)
  }

  /** checks if the election has started
    *
    * @param rpcMessage
    *   the rpc message to validate
    * @param electionChannel
    *   the election channel
    * @param dbActorRef
    *   the dbActor to ask for open election messages
    * @param error
    *   the error to forward in case the election hasn't started
    * @return
    *   GraphMessage: passes the rpcMessages to Right if successful else Left with pipeline error
    */
  private def checkElectionStarted(
      rpcMessage: JsonRpcRequest,
      electionChannel: Channel,
      dbActorRef: AskableActorRef,
      error: PipelineError
  ): GraphMessage =
    Await.result(electionChannel.extractMessages[OpenElection](dbActorRef), duration) match {
      case h :: _ => Right(rpcMessage)
      case _      => Left(error)
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
    *   GraphMessage: passes the rpcMessages to Right if successful else Left with pipeline error
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
      Right(rpcMessage)
    else
      Left(error)
  }

  /** checks if castVotes hash computation match the expected hash
    *
    * @param rpcMessage
    *   the rpc message to validate
    * @param channel
    *   the channel to pull the votes from
    * @param expectedHash
    *   The hash of the concatenated votes (i.e. registered_votes)
    * @param error
    *   the error to forward in case of incoherence between expectedHash and the computed hash from castVotes
    * @return
    *   GraphMessage: passes the rpcMessages to Right if successful else Left with pipeline error
    */
  private def checkVoteResults(rpcMessage: JsonRpcRequest, channel: Channel, expectedHash: Hash, error: PipelineError): GraphMessage = {
    val castVotes = Await.result(channel.getLastVotes(dbActorRef), duration)
    val votes: List[VoteElection] = castVotes.flatMap(_.votes)
    val sortedVotes: List[VoteElection] = votes.sortBy(_.id.toString)
    val computedHash = Hash.fromStrings(sortedVotes.map(_.id.toString): _*)

    if (computedHash == expectedHash)
      Right(rpcMessage)
    else
      Left(error)
  }
}
