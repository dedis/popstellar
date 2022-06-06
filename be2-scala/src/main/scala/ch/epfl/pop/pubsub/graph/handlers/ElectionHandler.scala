package ch.epfl.pop.pubsub.graph.handlers

import akka.pattern.AskableActorRef
import ch.epfl.pop.json.MessageDataProtocol.{KeyElectionFormat, resultElectionFormat}
import ch.epfl.pop.model.network.JsonRpcRequest
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.network.method.message.data.ObjectType
import ch.epfl.pop.model.network.method.message.data.election.VersionType._
import ch.epfl.pop.model.network.method.message.data.election._
import ch.epfl.pop.model.objects.ElectionChannel._
import ch.epfl.pop.model.objects._
import ch.epfl.pop.pubsub.graph.{ErrorCodes, GraphMessage, PipelineError}
import ch.epfl.pop.storage.DbActor
import ch.epfl.pop.storage.DbActor.DbActorReadElectionDataAck

import java.nio.ByteBuffer
import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}

/**
 * ElectionHandler object uses the db instance from the MessageHandler
 */
object ElectionHandler extends MessageHandler {
  final lazy val handlerInstance = new ElectionHandler(super.dbActor)

  def handleSetupElection(rpcMessage: JsonRpcRequest): GraphMessage = handlerInstance.handleSetupElection(rpcMessage)

  def handleOpenElection(rpcMessage: JsonRpcRequest): GraphMessage = handlerInstance.handleOpenElection(rpcMessage)

  def handleCastVoteElection(rpcMessage: JsonRpcRequest): GraphMessage = handlerInstance.handleCastVoteElection(rpcMessage)

  def handleResultElection(rpcMessage: JsonRpcRequest): GraphMessage = handlerInstance.handleResultElection(rpcMessage)

  def handleEndElection(rpcMessage: JsonRpcRequest): GraphMessage = handlerInstance.handleEndElection(rpcMessage)

  def handleKeyElection(rpcMessage: JsonRpcRequest): GraphMessage = handlerInstance.handleKeyElection(rpcMessage)
}

class ElectionHandler(dbRef: => AskableActorRef) extends MessageHandler {

  /**
   *
   * Overrides default DbActor with provided parameter
   */
  override final val dbActor: AskableActorRef = dbRef

  def handleSetupElection(rpcMessage: JsonRpcRequest): GraphMessage = {
    //FIXME: add election info to election channel/electionData
    val message: Message = rpcMessage.getParamsMessage.get
    val data: SetupElection = message.decodedData.get.asInstanceOf[SetupElection]
    val electionId: Hash = data.id
    val electionChannel: Channel = Channel(s"${rpcMessage.getParamsChannel.channel}${Channel.CHANNEL_SEPARATOR}$electionId")
    val keyPair = KeyPair()

    //need to write and propagate the election message
    val combined = for {
      _ <- dbActor ? DbActor.WriteAndPropagate(rpcMessage.getParamsChannel, message)
      _ <- dbActor ? DbActor.CreateChannel(electionChannel, ObjectType.ELECTION)
      _ <- dbActor ? DbActor.WriteAndPropagate(electionChannel, message)
      _ <- dbActor ? DbActor.CreateElectionData(electionId, keyPair)
    } yield ()

    Await.ready(combined, duration).value match {
      case Some(Success(_)) =>
        //generating a key pair in case the version is secret-ballot, to send then the public key to the frontend
        data.version match {
          case OPEN_BALLOT => Left(rpcMessage)
          case SECRET_BALLOT =>
            val keyElection: KeyElection = KeyElection(electionId, keyPair.publicKey)
            val broadcastKey: Base64Data = Base64Data.encode(KeyElectionFormat.write(keyElection).toString)
            Await.result(dbBroadcast(rpcMessage, rpcMessage.getParamsChannel, broadcastKey, electionChannel), duration)
        }
      case Some(Failure(ex: DbActorNAckException)) => Right(PipelineError(ex.code, s"handleSetupElection failed : ${ex.message}", rpcMessage.getId))
      case reply => Right(PipelineError(ErrorCodes.SERVER_ERROR.id, s"handleSetupElection failed : unexpected DbActor reply '$reply'", rpcMessage.getId))
    }
  }

  def handleKeyElection(rpcMessage: JsonRpcRequest): GraphMessage = {
    val ask: Future[GraphMessage] = dbAskWritePropagate(rpcMessage)
    Await.result(ask, duration)
  }

  def handleOpenElection(rpcMessage: JsonRpcRequest): GraphMessage = {
    //checks first if the election is created (i.e. if the channel election exists)
    val combined = for {
      _ <- dbActor ? DbActor.ChannelExists(rpcMessage.getParamsChannel)
      _ <- dbAskWritePropagate(rpcMessage)
    } yield ()
    Await.ready(combined, duration).value.get match {
      case Success(_) => Left(rpcMessage)
      case Failure(ex: DbActorNAckException) => Right(PipelineError(ex.code, s"handleOpenElection failed : ${ex.message}", rpcMessage.getId))
      case reply => Right(PipelineError(ErrorCodes.SERVER_ERROR.id, s"handleOpenElection failed : unknown DbActor reply $reply", rpcMessage.getId))
    }
  }

  def handleCastVoteElection(rpcMessage: JsonRpcRequest): GraphMessage = {
    val ask: Future[GraphMessage] = dbAskWritePropagate(rpcMessage)
    Await.result(ask, duration)
  }

  def handleResultElection(rpcMessage: JsonRpcRequest): GraphMessage = Right(
    PipelineError(ErrorCodes.SERVER_ERROR.id, "NOT IMPLEMENTED: ElectionHandler cannot handle ResultElection messages yet", rpcMessage.id)
  )

  def handleEndElection(rpcMessage: JsonRpcRequest): GraphMessage = {
    val witnessSignatures = rpcMessage.getParamsMessage match {
      case Some(it) => it.witness_signatures.map(_.signature)
      case _ => Nil
    }
    val electionChannel: Channel = rpcMessage.getParamsChannel
    val combined = for {
      electionQuestionResults <- createElectionQuestionResults(electionChannel)
      // propagate the endElection message
      _ <- dbAskWritePropagate(rpcMessage)
      //data to be broadcast
      resultElection: ResultElection = ResultElection(electionQuestionResults, witnessSignatures)
      data: Base64Data = Base64Data.encode(resultElectionFormat.write(resultElection).toString)
      // create & propagate the resultMessage
      _ <- dbBroadcast(rpcMessage, electionChannel, data, electionChannel)
    } yield ()
    Await.ready(combined, duration).value match {
      case Some(Success(_)) => Left(rpcMessage)
      case _ => Right(PipelineError(ErrorCodes.SERVER_ERROR.id, s"handleEndElection unknown error", rpcMessage.getId))
    }
  }

  private def createElectionQuestionResults(electionChannel: Channel): Future[List[ElectionQuestionResult]] = {
    for {
      votesList <- electionChannel.getLastVotes(dbActor)
      castsVotesElections = votesList.map(_._2)
      setupMessage <- electionChannel.getSetupMessage(dbActor)
      questionToBallots = setupMessage.questions.map(question => question.id -> question.ballot_options).toMap
      DbActorReadElectionDataAck(electionData) <- dbActor ? DbActor.ReadElectionData(setupMessage.id)
    } yield {
      val resultsTable = mutable.HashMap.from(for {
        (question, ballots) <- questionToBallots
      } yield question -> ballots.map(_ -> 0).toMap)
      for {
        castVoteElection <- castsVotesElections
        voteElection <- castVoteElection.votes
        voteIndex = electionChannel.getVoteIndex(electionData, voteElection.vote)
      } {
        val question = voteElection.question
        val ballots = questionToBallots(question).toArray
        val ballot = ballots.apply(voteIndex)
        val questionResult = resultsTable(question)
        resultsTable.update(question, questionResult.updated(ballot, questionResult(ballot) + 1))
      }
      (for {
        (qid, ballotToCount) <- resultsTable
        electionBallotVotes = List.from(for {
          (ballot, count) <- ballotToCount
        } yield ElectionBallotVotes(ballot, count))
      } yield ElectionQuestionResult(qid, electionBallotVotes)).toList
    }
  }
}
