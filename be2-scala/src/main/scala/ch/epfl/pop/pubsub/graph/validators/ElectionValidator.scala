package ch.epfl.pop.pubsub.graph.validators

import akka.pattern.AskableActorRef
import ch.epfl.pop.model.network.JsonRpcRequest
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.network.method.message.data.ObjectType
import ch.epfl.pop.model.network.method.message.data.election._
import ch.epfl.pop.model.objects.ElectionChannel._
import ch.epfl.pop.model.objects.{Channel, DbActorNAckException, Hash, LaoData, Timestamp}
import ch.epfl.pop.pubsub.PublishSubscribe
import ch.epfl.pop.pubsub.graph.validators.ElectionValidator.{FAILED_TO_GET_QUESTION_ERROR_MESSAGE, UNKNOWN_ERROR_MESSAGE}
import ch.epfl.pop.pubsub.graph.validators.MessageValidator._
import ch.epfl.pop.pubsub.graph.{ErrorCodes, GraphMessage, PipelineError}
import ch.epfl.pop.storage.DbActor

import scala.concurrent.Await
import scala.util.{Failure, Success}

//Similarly to the handlers, we create a ElectionValidator object which creates a ElectionValidator class instance.
//The defaults dbActorRef is used in the object, but the class can now be mocked with a custom dbActorRef for testing purpose
object ElectionValidator extends MessageDataContentValidator with EventValidator {

  val electionValidator = new ElectionValidator(PublishSubscribe.getDbActorRef)

  override val EVENT_HASH_PREFIX: String = electionValidator.EVENT_HASH_PREFIX

  private val UNKNOWN_ERROR_MESSAGE: String = "Unknown error: "

  private val FAILED_TO_GET_QUESTION_ERROR_MESSAGE: String = "Failed to get election questions: "

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

        val setupElectionTimeStamp = Await.ready(channel.getSetupMessage(dbActorRef), duration).value.get match {
          case Success(setupElection: SetupElection) => setupElection.created_at
          case Failure(exception)                    => return Left(validationError(FAILED_TO_GET_QUESTION_ERROR_MESSAGE + exception.getMessage))
          case err @ _                               => return Left(validationError(UNKNOWN_ERROR_MESSAGE + err.toString))
        }

        runChecks(
          checkTimestampStaleness(
            rpcMessage,
            openElection.opened_at,
            validationError(s"stale 'opened_at' timestamp (${openElection.opened_at})")
          ),
          checkTimestampOrder(
            rpcMessage,
            setupElectionTimeStamp,
            openElection.opened_at,
            validationError("trying to open an election before seting it up.")
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
          case Failure(exception)     => return Left(validationError(FAILED_TO_GET_QUESTION_ERROR_MESSAGE + exception.getMessage))
          case err @ _                => return Left(validationError(UNKNOWN_ERROR_MESSAGE + err.toString))
        }
        val openingTimeStamp = Await.result(channel.extractMessages[OpenElection](dbActorRef), duration) match {
          case openElection: List[(Message, OpenElection)] =>
            openElection.head._2.opened_at
          case _ => Timestamp(casteVote.created_at.time + 1L)
        }
        runChecks(
          checkTimestampStaleness(
            rpcMessage,
            casteVote.created_at,
            validationError(s"stale 'created_at' timestamp (${casteVote.created_at})")
          ),
          checkTimestampOrder(
            rpcMessage,
            openingTimeStamp,
            casteVote.created_at,
            validationError("trying to cast a vote before opening the election.")
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

  def validateResultElection(rpcMessage: JsonRpcRequest): GraphMessage = {
    def validationError(reason: String): PipelineError = super.validationError(reason, "ResultElection", rpcMessage.id)
    rpcMessage.getParamsMessage match {
      case Some(_: Message) =>
        val (resultElection, _, _, channel) = extractData[ResultElection](rpcMessage)
        val electionQuestionResult = resultElection.questions
        val electionId = channel.extractChildChannel
        val electionQuestions = Await.ready(channel.getSetupMessage(dbActorRef), duration).value.get match {
          case Success(setupElection) => setupElection.questions
          case Failure(exception)     => return Left(validationError(FAILED_TO_GET_QUESTION_ERROR_MESSAGE + exception.getMessage))
          case err @ _                => return Left(validationError(UNKNOWN_ERROR_MESSAGE + err.toString))
        }
        runChecks(
          checkNumberOfVotes(
            rpcMessage,
            channel,
            electionQuestionResult,
            dbActorRef,
            validationError(s"trying to send a ResultElection message with an invalid number of votes.")
          ),
          checkElectionEnded(
            rpcMessage,
            channel,
            dbActorRef,
            validationError("trying to send a ResultElection while Election is not ended yet.")
          ),
          checkResultQuestionIds(
            rpcMessage,
            electionQuestions,
            resultElection.questions,
            validationError(s"trying to send a ResultElection message with invalid question ids.")
          ),
          checkResultBallotOptions(
            rpcMessage,
            electionQuestions,
            electionQuestionResult,
            validationError(s"trying to send a ResultElection message with invalid ballot options.")
          )
        )

      case _ => Left(validationErrorNoMessage(rpcMessage.id))
    }
  }

  def validateEndElection(rpcMessage: JsonRpcRequest): GraphMessage = {
    def validationError(reason: String): PipelineError = super.validationError(reason, "EndElection", rpcMessage.id)

    rpcMessage.getParamsMessage match {
      case Some(_: Message) =>
        val (endElection, laoId, senderPK, channel) = extractData[EndElection](rpcMessage)

        val electionId = channel.extractChildChannel

        val setupElectionTimeStamp = Await.ready(channel.getSetupMessage(dbActorRef), duration).value.get match {
          case Success(setupElection: SetupElection) => setupElection.created_at
          case Failure(exception)                    => return Left(validationError(FAILED_TO_GET_QUESTION_ERROR_MESSAGE + exception.getMessage))
          case err @ _                               => return Left(validationError(UNKNOWN_ERROR_MESSAGE + err.toString))
        }
        print(setupElectionTimeStamp)
        print(endElection.created_at)

        val firstCheck = runChecks(
          checkTimestampStaleness(
            rpcMessage,
            endElection.created_at,
            validationError(s"stale 'created_at' timestamp (${endElection.created_at})")
          ),
          checkTimestampOrder(
            rpcMessage,
            setupElectionTimeStamp,
            endElection.created_at,
            validationError("trying to end an election before setting it up")
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

  /** Checks if the election has ended
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
  private def checkElectionEnded(
      rpcMessage: JsonRpcRequest,
      electionChannel: Channel,
      dbActorRef: AskableActorRef,
      error: PipelineError
  ): GraphMessage =
    Await.result(electionChannel.extractMessages[EndElection](dbActorRef), duration) match {
      case h :: _ => Right(rpcMessage)
      case _      => Left(error)
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
    val isQuestionIdValid: Boolean = {
      questions.forall(q =>
        q.id == Hash.fromStrings("Question", electionId.toString, q.question)
      )
    }

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

  /** checks that for each question, the number of votes is between 0 and the number of attendees to the election.
    *
    * @param rpcMessage
    *   the rpc message to validate.
    * @param result
    *   the list of the election results.
    * @param dbActorRef
    *   the dbActor to ask for the number of attendees.
    * @param channel
    *   the channel of the corresponding LaoData
    * @param error
    *   the error to forward when the number of votes is incoherent.
    * @return
    *   GraphMessage: passes the rpcMessages to Right if successful else Left with pipeline error
    */
  private def checkNumberOfVotes(rpcMessage: JsonRpcRequest, channel: Channel, result: List[ElectionQuestionResult], dbActorRef: AskableActorRef, error: PipelineError): GraphMessage = {
    val askLaoData = dbActorRef ? DbActor.ReadLaoData(channel)
    Await.ready(askLaoData, duration).value match {
      case Some(Success(DbActor.DbActorReadLaoDataAck(laoData: LaoData))) =>
        val numberOfAttendees = laoData.attendees.size
        val correct: Boolean =
          result.forall(electionQuestionResult =>
            (countNumberOfVotes(electionQuestionResult) <= numberOfAttendees) && allBallotCountsArePositive(electionQuestionResult)
          )
        if (correct) {
          Right(rpcMessage)
        } else {
          Left(error)
        }
      case Some(Failure(ex: DbActorNAckException)) => Left(PipelineError(ex.code, s"validation of ResultElection failed, could not retrieve laoData : ${ex.message}", rpcMessage.getId))
      case reply                                   => Left(PipelineError(ErrorCodes.SERVER_ERROR.id, s"ElectionValidator failed : unexpected DbActor reply '$reply'", rpcMessage.getId))
    }
  }

  /** @param electionQuestionResult
    *   The ElectionQuestionResult the ElectionValidator received.
    * @return
    *   The total number number of votes for the question.
    */
  private def countNumberOfVotes(electionQuestionResult: ElectionQuestionResult): Int = {
    electionQuestionResult.result.map(_.count).sum
  }

  /** @param electionQuestionResult
    *   The ElectionQuestionResult the ElectionValidator received.
    * @return
    *   True if all the ballot counts are greater that 0, false otherwise.
    */
  private def allBallotCountsArePositive(electionQuestionResult: ElectionQuestionResult): Boolean = {
    electionQuestionResult.result.forall(electionBallotVotes => electionBallotVotes.count >= 0)
  }

  /** checks that the question ids received with the ResultElection are coherent with the ones received in the SetupElection
    * @param rpcMessage
    *   the rpc message to validate.
    * @param questions
    *   The list of ElectionQuestions that have been sent with the SetupElection.
    * @param result
    *   The list of ElectionQuestionResults the ElectionValidator received.
    * @param error
    *   The error to forward when the ballot options are incoherent.
    * @return
    *   GraphMessage: passes the rpcMessages to Right if successful else Left with pipeline error.
    */
  private def checkResultQuestionIds(
      rpcMessage: JsonRpcRequest,
      questions: List[ElectionQuestion],
      result: List[ElectionQuestionResult],
      error: PipelineError
  ): GraphMessage = {
    val setupElectionIds = questions.map(_.id).toSet
    val resultElectionIds = result.map(_.id).toSet
    if (setupElectionIds == resultElectionIds) {
      Right(rpcMessage)
    } else
      Left(error)
  }

  /** checks that for the given ElectionResult, the ballot options match the ballot options sent in the SetupElection message.
    * @param rpcMessage
    *   The rpc message to validate.
    * @param questions
    *   The list of ElectionQuestions that have been sent with the SetupElection.
    * @param result
    *   The list of ElectionQuestionResults the ElectionValidator received.
    * @param error
    *   The error to forward in case where ballot options are incoherent.
    * @return
    *   GraphMessage: passes the rpcMessages to Right if successful else Left with pipeline error.
    */
  private def checkResultBallotOptions(
      rpcMessage: JsonRpcRequest,
      questions: List[ElectionQuestion],
      result: List[ElectionQuestionResult],
      error: PipelineError
  ): GraphMessage = {
    var isResultBallotValid = true
    isResultBallotValid = result.forall(question => {
      val matchingQuestion = questions.find(_.id == question.id)
      matchingQuestion.isDefined && question.result.toSet[ElectionBallotVotes].map(_.ballot_option) == matchingQuestion.get.ballot_options.toSet
    })
    if (isResultBallotValid) {
      Right(rpcMessage)
    } else
      Left(error)
  }

}
