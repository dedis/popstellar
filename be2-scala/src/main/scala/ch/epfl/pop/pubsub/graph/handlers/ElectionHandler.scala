package ch.epfl.pop.pubsub.graph.handlers

import akka.pattern.AskableActorRef
import ch.epfl.pop.json.MessageDataProtocol.{KeyElectionFormat, resultElectionFormat}
import ch.epfl.pop.model.network.JsonRpcRequest
import ch.epfl.pop.model.network.method.message.data.election.VersionType
import ch.epfl.pop.model.network.method.message.data.election.*
import ch.epfl.pop.model.objects.ElectionChannel.*
import ch.epfl.pop.model.objects.*
import ch.epfl.pop.pubsub.graph.{ErrorCodes, GraphMessage, PipelineError}
import ch.epfl.pop.storage.DbActor
import ch.epfl.pop.storage.DbActor.DbActorReadElectionDataAck

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Right, Success}

/** ElectionHandler object uses the db instance from the MessageHandler
  */
object ElectionHandler extends MessageHandler {

  private val serverUnexpectedAnswer: String = "The server is doing something unexpected"

  def handleSetupElection(rpcMessage: JsonRpcRequest): GraphMessage = {
    // FIXME: add election info to election channel/electionData

    // need to write and propagate the election message
    val combined = for {
      case (_, message, Some(data)) <- extractParameters[SetupElection](rpcMessage, serverUnexpectedAnswer)
      electionId: Hash = data.id
      electionChannel: Channel = Channel(s"${rpcMessage.getParamsChannel.channel}${Channel.CHANNEL_SEPARATOR}$electionId")
      keyPair = KeyPair()
      _ <- dbActor ? DbActor.WriteSetupElectionMessage(electionChannel, message)
      _ <- dbActor ? DbActor.CreateElectionData(rpcMessage.extractLaoId, electionId, keyPair)
    } yield (data, electionId, keyPair, electionChannel)

    Await.ready(combined, duration).value match {
      case Some(Success((data, electionId, keyPair, electionChannel))) =>
        // generating a key pair in case the version is secret-ballot, to send then the public key to the frontend
        data.version match {
          case VersionType.OPEN_BALLOT => Right(rpcMessage)
          case VersionType.SECRET_BALLOT =>
            val keyElection: KeyElection = KeyElection(electionId, keyPair.publicKey)
            Await.result(
              broadcast(rpcMessage, rpcMessage.getParamsChannel, KeyElectionFormat.write(keyElection), electionChannel),
              duration
            )
          case version => Left(PipelineError(ErrorCodes.INVALID_ACTION.id, s"handleSetupElection failed: unknown election type $version", rpcMessage.getId))
        }
      case Some(Failure(ex: DbActorNAckException)) => Left(PipelineError(ex.code, s"handleSetupElection failed : ${ex.message}", rpcMessage.getId))
      case reply                                   => Left(PipelineError(ErrorCodes.SERVER_ERROR.id, s"handleSetupElection failed : unexpected DbActor reply '$reply'", rpcMessage.getId))
    }
  }

  def handleKeyElection(rpcMessage: JsonRpcRequest): GraphMessage = {
    val ask: Future[GraphMessage] = dbAskWritePropagate(rpcMessage)
    Await.result(ask, duration)
  }

  def handleOpenElection(rpcMessage: JsonRpcRequest): GraphMessage = {
    // checks first if the election is created (i.e. if the channel election exists)
    val combined = for {
      _ <- dbActor ? DbActor.ChannelExists(rpcMessage.getParamsChannel)
      _ <- dbAskWritePropagate(rpcMessage)
    } yield ()
    Await.ready(combined, duration).value.get match {
      case Success(_)                        => Right(rpcMessage)
      case Failure(ex: DbActorNAckException) => Left(PipelineError(ex.code, s"handleOpenElection failed : ${ex.message}", rpcMessage.getId))
      case reply                             => Left(PipelineError(ErrorCodes.SERVER_ERROR.id, s"handleOpenElection failed : unknown DbActor reply $reply", rpcMessage.getId))
    }
  }

  def handleCastVoteElection(rpcMessage: JsonRpcRequest): GraphMessage = {
    val ask: Future[GraphMessage] = dbAskWritePropagate(rpcMessage)
    Await.result(ask, duration)
  }

  def handleResultElection(rpcMessage: JsonRpcRequest): GraphMessage = {
    val ask: Future[GraphMessage] = dbAskWritePropagate(rpcMessage)
    Await.result(ask, duration)
  }

  def handleEndElection(rpcMessage: JsonRpcRequest): GraphMessage = {
    val witnessSignatures = rpcMessage.getParamsMessage match {
      case Some(it) => it.witness_signatures.map(_.signature)
      case _        => Nil
    }
    val electionChannel: Channel = rpcMessage.getParamsChannel
    val combined = for {
      case (_, Some(laoId)) <- extractLaoChannel(rpcMessage, "There is an issue with the id of the LAO")

      electionQuestionResults <- createElectionQuestionResults(electionChannel, laoId)
      // propagate the endElection message
      _ <- dbAskWritePropagate(rpcMessage)
      // data to be broadcast
      resultElection: ResultElection = ResultElection(electionQuestionResults, witnessSignatures)
      // create & propagate the resultMessage
      _ <- broadcast(rpcMessage, electionChannel, resultElectionFormat.write(resultElection), electionChannel)
    } yield ()
    Await.ready(combined, duration).value match {
      case Some(Success(_)) => Right(rpcMessage)
      case _                => Left(PipelineError(ErrorCodes.SERVER_ERROR.id, s"handleEndElection unknown error", rpcMessage.getId))
    }
  }

  /** Helper function to create the list of ElectionQuestionResult
    *
    * @param electionChannel
    *   : the Channel in which we read the data
    * @return
    *   the list of ElectionQuestionResult wrapped in a [[scala.concurrent.Future]]
    */
  private def createElectionQuestionResults(electionChannel: Channel, laoId: Hash): Future[List[ElectionQuestionResult]] = {
    for {
      // get the last votes of the CastVotes messages
      castsVotesElections <- electionChannel.getLastVotes(dbActor)
      // get the setupElection message of the channel
      setupMessage <- electionChannel.getSetupMessage(dbActor)
      // associate the questions ids to their ballots
      questionToBallots = setupMessage.questions.map(question => question.id -> question.ballot_options).toMap
      case DbActorReadElectionDataAck(electionData) <- dbActor ? DbActor.ReadElectionData(laoId, setupMessage.id)
    } yield {
      // set up the table of results
      val resultsTable = mutable.HashMap.from(for {
        (question, ballots) <- questionToBallots
      } yield question -> ballots.map(_ -> 0).toMap)
      for {
        castVoteElection <- castsVotesElections
        voteElection <- castVoteElection.votes
        // get the index of the vote
        voteIndex = electionChannel.getVoteIndex(electionData, voteElection.vote)
      } {
        val question = voteElection.question
        val ballots = questionToBallots(question).toArray
        val ballot = ballots.apply(voteIndex)
        val questionResult = resultsTable(question)
        // update the results by adding a vote to the corresponding ballot
        resultsTable.update(question, questionResult.updated(ballot, questionResult(ballot) + 1))
      }
      (for {
        // from the results saved in resultsTable, we construct the ElectionQuestionResult
        (qid, ballotToCount) <- resultsTable
        electionBallotVotes = List.from(for {
          (ballot, count) <- ballotToCount
        } yield ElectionBallotVotes(ballot, count))
      } yield ElectionQuestionResult(qid, electionBallotVotes)).toList
    }
  }
}
