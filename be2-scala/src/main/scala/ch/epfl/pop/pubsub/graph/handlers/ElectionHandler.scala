package ch.epfl.pop.pubsub.graph.handlers

import ch.epfl.pop.model.network.JsonRpcRequest
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.network.method.message.data.ObjectType
import ch.epfl.pop.model.network.method.message.data.election.{CastVoteElection, SetupElection, VoteElection}
import ch.epfl.pop.model.objects.{Channel, ChannelData, DbActorNAckException, Hash}
import ch.epfl.pop.pubsub.graph.{ErrorCodes, GraphMessage, PipelineError}
import ch.epfl.pop.storage.DbActor

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}

object ElectionHandler extends MessageHandler {

  def handleSetupElection(rpcMessage: JsonRpcRequest): GraphMessage = {
    //FIXME: add election info to election channel/electionData
    val message: Message = rpcMessage.getParamsMessage.get
    val electionId: Hash = message.decodedData.get.asInstanceOf[SetupElection].id
    val electionChannel: Channel = Channel(s"${rpcMessage.getParamsChannel.channel}${Channel.CHANNEL_SEPARATOR}$electionId")

    val combined = for {
      _ <- dbActor ? DbActor.WriteAndPropagate(rpcMessage.getParamsChannel, message)
      _ <- dbActor ? DbActor.CreateChannel(electionChannel, ObjectType.ELECTION)
    } yield ()

    Await.ready(combined, duration).value match {
      case Some(Success(_)) =>
        Left(rpcMessage)
      case Some(Failure(ex: DbActorNAckException)) =>
        Right(PipelineError(ex.code, s"handleSetupElection failed : ${ex.message}", rpcMessage.getId))
      case reply =>
        Right(PipelineError(ErrorCodes.SERVER_ERROR.id, s"handleSetupElection failed : unexpected DbActor reply '$reply'", rpcMessage.getId))
    }
  }

  def handleOpenElection(rpcMessage: JsonRpcRequest): GraphMessage = {
    val ask: Future[GraphMessage] = dbAskWritePropagate(rpcMessage)
    println("open election abc")
    Await.result(ask, duration)
  }

  def handleCastVoteElection(rpcMessage: JsonRpcRequest): GraphMessage = {
    // no need to propagate here, hence the use of dbAskWrite
    val ask: Future[GraphMessage] = dbAskWrite(rpcMessage)
    rpcMessage.getParamsMessage match {
      case Some(message: Message) =>
        val data: CastVoteElection = message.decodedData.get.asInstanceOf[CastVoteElection]
        val electionId: Hash = data.election
        val electionChannel: Channel = Channel(s"${rpcMessage.getParamsChannel.channel}${Channel.CHANNEL_SEPARATOR}$electionId")
        dbActor ? DbActor.ReadChannelData(electionChannel) transformWith {
          case Success(c: ChannelData) =>
            // val decodedMessages = c.messages.map(message.decodedData.get.asInstanceOf[SetupElection].id)
            Future.successful(c.messages)
          case _ => Future.failed(new Exception())
        }
      case _ => ???
    }

    Await.result(ask, duration)
  }

  def handleResultElection(rpcMessage: JsonRpcRequest): GraphMessage = {
    val message: Message = rpcMessage.getParamsMessage.get
    val electionId: Hash = message.decodedData.get.asInstanceOf[SetupElection].id
    val electionChannel: Channel = Channel(s"${rpcMessage.getParamsChannel.channel}${Channel.CHANNEL_SEPARATOR}$electionId")

    val electionsData = for {
      channel <- dbActor ? DbActor.ReadChannelData(electionChannel)
    } yield channel
    Await.ready(electionsData, duration).value match {
      case Some(Success(electionsData: ChannelData)) =>
        var result = for (message<-electionsData.messages) yield message.base64Data
        Left(rpcMessage)
      case Some(Failure(ex: DbActorNAckException)) =>
        Right(PipelineError(ex.code, s"handleSetupElection failed : ${ex.message}", rpcMessage.getId))
      case reply =>
        Right(PipelineError(ErrorCodes.SERVER_ERROR.id, s"handleSetupElection failed : unexpected DbActor reply '$reply'", rpcMessage.getId))
    }

  }

  def handleEndElection(rpcMessage: JsonRpcRequest): GraphMessage = {
    //no need to propagate the results, we only need to write the results in the db
    val ask: Future[GraphMessage] = dbAskWritePropagate(rpcMessage)
    Await.result(ask, duration)
  }
}
