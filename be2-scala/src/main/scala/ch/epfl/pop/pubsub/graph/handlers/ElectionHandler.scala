package ch.epfl.pop.pubsub.graph.handlers

import akka.pattern.AskableActorRef
import ch.epfl.pop.json.MessageDataProtocol.resultElectionFormat
import ch.epfl.pop.model.network.JsonRpcRequest
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.network.method.message.data.ObjectType
import ch.epfl.pop.model.network.method.message.data.election._
import ch.epfl.pop.model.objects._
import ch.epfl.pop.pubsub.graph.ElectionHelper.{getLastVotes, getSetupMessage}
import ch.epfl.pop.pubsub.graph.{ErrorCodes, GraphMessage, PipelineError}
import ch.epfl.pop.storage.DbActor

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
    val electionId: Hash = message.decodedData.get.asInstanceOf[SetupElection].id
    val electionChannel: Channel = Channel(s"${rpcMessage.getParamsChannel.channel}${Channel.CHANNEL_SEPARATOR}$electionId")

    //need to write and propagate the election message
    val combined = for {
      _ <- dbActor ? DbActor.WriteAndPropagate(rpcMessage.getParamsChannel, message)
      _ <- dbActor ? DbActor.CreateChannel(electionChannel, ObjectType.ELECTION)
      _ <- dbActor ? DbActor.WriteAndPropagate(electionChannel, message)
    } yield ()

    Await.ready(combined, duration).value match {
      case Some(Success(_)) => Left(rpcMessage)
      case Some(Failure(ex: DbActorNAckException)) => Right(PipelineError(ex.code, s"handleSetupElection failed : ${ex.message}", rpcMessage.getId))
      case reply => Right(PipelineError(ErrorCodes.SERVER_ERROR.id, s"handleSetupElection failed : unexpected DbActor reply '$reply'", rpcMessage.getId))
    }
  }

  def handleOpenElection(rpcMessage: JsonRpcRequest): GraphMessage = {
    //checks first if the election is created (i.e. if the channel election exists)
    val askChannelExist = dbActor ? DbActor.ChannelExists(rpcMessage.getParamsChannel)
    Await.ready(askChannelExist, duration).value.get match {
      case Success(_) =>
        val askWritePropagate: Future[GraphMessage] = dbAskWritePropagate(rpcMessage)
        Await.result(askWritePropagate, duration)
      case Failure(ex: DbActorNAckException) => Right(PipelineError(ex.code, s"handleOpenElection failed : ${ex.message}", rpcMessage.getId))
      case reply => Right(PipelineError(ErrorCodes.SERVER_ERROR.id, s"handleOpenElection failed : unknown DbActor reply $reply", rpcMessage.getId))
    }
  }

  def handleCastVoteElection(rpcMessage: JsonRpcRequest): GraphMessage = {
    // no need to propagate here, hence the use of dbAskWrite
    val ask: Future[GraphMessage] = dbAskWritePropagate(rpcMessage)
    Await.result(ask, duration)
  }

  def handleResultElection(rpcMessage: JsonRpcRequest): GraphMessage = Right(
    PipelineError(ErrorCodes.SERVER_ERROR.id, "NOT IMPLEMENTED: ElectionHandler cannot handle ResultElection messages yet", rpcMessage.id)
  )

  def handleEndElection(rpcMessage: JsonRpcRequest): GraphMessage = {
    //no need to propagate the results, we only need to write the results in the db
    Await.ready(dbAskWritePropagate(rpcMessage), duration).value match {
      case Some(Success(_)) =>
        val electionChannel = rpcMessage.getParamsChannel
        val results = getResults(electionChannel)
        val witnessSignatures = rpcMessage.getParamsMessage match {
          case Some(it) => it.witness_signatures.map(_.signature)
          case _ => Nil
        }
        val resultElection: ResultElection = ResultElection(results, witnessSignatures)
        val data: Base64Data = Base64Data.encode(resultElectionFormat.write(resultElection).toString())
        val askLaoData = dbActor ? DbActor.ReadLaoData(rpcMessage.getParamsChannel)
        Await.ready(askLaoData, duration).value match {
          case Some(Success(DbActor.DbActorReadLaoDataAck(laoData))) =>
            val signature: Signature = laoData.privateKey.signData(data)
            val sender: PublicKey = laoData.publicKey
            val msgId: Hash = Hash.fromStrings(data.toString, signature.toString)
            val message: Message = new Message(data,
              sender,
              signature,
              msgId,
              List.empty, Some(resultElection))
            val propagation = dbActor ? DbActor.WriteAndPropagate(electionChannel, message)
            Await.ready(propagation, duration).value match {
              case Some(Success(_)) =>
                Left(rpcMessage)
              case Some(Failure(ex: DbActorNAckException)) =>
                Right(PipelineError(ex.code, s"handleEndElection failed : ${
                  ex.message
                }", rpcMessage.getId))
              case reply =>
                Right(PipelineError(ErrorCodes.SERVER_ERROR.id, s"handleEndElection failed : unexpected DbActor reply '$reply'", rpcMessage.getId))
            }
          case _ => Right(PipelineError(ErrorCodes.SERVER_ERROR.id, s"handleEndElection failed : couldn't read LAO data", rpcMessage.getId))
        }
      case _ => Right(PipelineError(ErrorCodes.SERVER_ERROR.id, s"handleEndElection failed : askWritePropagate failed", rpcMessage.getId))
    }
  }

  private def getResults(electionChannel: Channel): List[ElectionQuestionResult] = {
    val castsVotesElections: List[CastVoteElection] = getLastVotes(electionChannel).map(_._2)
    val question2Ballots = getSetupMessage(electionChannel, dbActor).questions.map(question => question.id -> question.ballot_options).toMap
    // results is a map [Question ID -> [Ballot name -> count]]
    val r = question2Ballots.keys.map(question => question -> question2Ballots(question).map(_ -> 0).toMap).toMap
    val results = mutable.HashMap[Hash, Map[String, Int]]() ++ r
    for (castVoteElection <- castsVotesElections;
         voteElection <- castVoteElection.votes) {
      val question = voteElection.question //.asInstanceOf[ElectionQuestion]
      val ballots = question2Ballots(question)
      val ballot = voteElection.vote match {
        case Some(List(index)) => ballots.toArray.apply(index)
        case _ => "<NOT_VOTED>"
      }
      println(f"Your ballot is $ballot !")
      if (results.contains(question)) {
        val questionresult = results(question)
        if (questionresult.contains(ballot))
          results.update(question, questionresult.updated(ballot, questionresult(ballot) + 1))
        else
          results.update(question, questionresult.updated(ballot, 1))
      }
      else
        results.update(question, Map(ballot -> 1))
    }
    println(results)
    results.map(tuple => {
      val (qid, ballot2count) = tuple
      ElectionQuestionResult(qid, ballot2count.map(tuple => ElectionBallotVotes(tuple._1, tuple._2)).toList)
    }).toList
  }
}
