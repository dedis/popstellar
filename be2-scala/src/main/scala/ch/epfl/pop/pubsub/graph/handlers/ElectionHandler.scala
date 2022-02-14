package ch.epfl.pop.pubsub.graph.handlers

import ch.epfl.pop.model.network.JsonRpcRequest
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.network.method.message.data.ObjectType
import ch.epfl.pop.model.network.method.message.data.election.SetupElection
import ch.epfl.pop.model.objects.{Channel, DbActorNAckException, Hash}
import ch.epfl.pop.pubsub.graph.{DbActor, ErrorCodes, GraphMessage, PipelineError}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.util.Failure

object ElectionHandler extends MessageHandler {

  def handleSetupElection(rpcMessage: JsonRpcRequest): GraphMessage = {
    //FIXME: add election info to election channel/electionData
    val message: Message = rpcMessage.getParamsMessage.get
    val electionId: Hash = message.decodedData.get.asInstanceOf[SetupElection].id
    val electionChannel: Channel = Channel(s"${rpcMessage.getParamsChannel.channel}${Channel.SEPARATOR}$electionId")

    val askWrite = (dbActor ? DbActor.Write(rpcMessage.getParamsChannel, message))
    val askCreateChannel = (dbActor ? DbActor.CreateChannel(electionChannel, ObjectType.ELECTION))

    val resFuture: Future[GraphMessage] = (for{
      resultWrite <- askWrite
      resultCreateChannel <- askCreateChannel
    } yield(resultWrite, resultCreateChannel)).map{
      case (DbActor.DbActorWriteAck(), DbActor.DbActorAck()) => Left(rpcMessage)
      case (Failure(e: DbActorNAckException), _) => Right(PipelineError(e.getCode, e.getMessage, rpcMessage.id))
      case (_, Failure(e: DbActorNAckException)) => Right(PipelineError(e.getCode, e.getMessage, rpcMessage.id))
      case _ => Right(PipelineError(ErrorCodes.SERVER_ERROR.id, "Database actor returned an unknown answer", rpcMessage.id))
    }
    Await.result(resFuture, duration)
  }

  def handleCastVoteElection(rpcMessage: JsonRpcRequest): GraphMessage = {
    // no need to propagate here, hence the use of dbAskWrite
    val ask: Future[GraphMessage] = dbAskWrite(rpcMessage)
    Await.result(ask, duration)
  }

  def handleResultElection(rpcMessage: JsonRpcRequest): GraphMessage = Right(
    PipelineError(ErrorCodes.SERVER_ERROR.id, "NOT IMPLEMENTED: ElectionHandler cannot handle ResultElection messages yet", rpcMessage.id)
  )

  def handleEndElection(rpcMessage: JsonRpcRequest): GraphMessage = Right(
    PipelineError(ErrorCodes.SERVER_ERROR.id, "NOT IMPLEMENTED: ElectionHandler cannot handle EndElection messages yet", rpcMessage.id)
  )
}
