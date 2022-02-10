package ch.epfl.pop.pubsub.graph.handlers

import akka.NotUsed
import akka.stream.scaladsl.Flow
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.network.method.message.data.ObjectType
import ch.epfl.pop.model.network.method.message.data.election.SetupElection
import ch.epfl.pop.model.network.requests.election.{JsonRpcRequestCastVoteElection, JsonRpcRequestEndElection, JsonRpcRequestResultElection, JsonRpcRequestSetupElection}
import ch.epfl.pop.model.network.{JsonRpcRequest, JsonRpcResponse}
import ch.epfl.pop.model.objects.{Channel, Hash}
import ch.epfl.pop.pubsub.graph.{DbActor, ErrorCodes, GraphMessage, PipelineError}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}

object ElectionHandler extends MessageHandler {

  override val handler: Flow[GraphMessage, GraphMessage, NotUsed] = Flow[GraphMessage].map {
    case Left(jsonRpcMessage) => jsonRpcMessage match {
      case message@(_: JsonRpcRequestSetupElection) => handleSetupElection(message)
      case message@(_: JsonRpcRequestResultElection) => handleResultElection(message)
      case message@(_: JsonRpcRequestEndElection) => handleEndElection(message)
      case message@(_: JsonRpcRequestCastVoteElection) => handleCastVoteElection(message)
      case _ => Right(PipelineError(
        ErrorCodes.SERVER_ERROR.id,
        "Internal server fault: ElectionHandler was given a message it could not recognize",
        jsonRpcMessage match {
          case r: JsonRpcRequest => r.id
          case r: JsonRpcResponse => r.id
          case _ => None
        }
      ))
    }
    case graphMessage@_ => graphMessage
  }

  def handleSetupElection(rpcMessage: JsonRpcRequest): GraphMessage = {
    //FIXME: add election info to election channel/electionData
    val message: Message = rpcMessage.getParamsMessage.get
    val electionId: Hash = message.decodedData.get.asInstanceOf[SetupElection].id
    val electionChannel: Channel = Channel(s"${rpcMessage.getParamsChannel.channel}${Channel.SEPARATOR}$electionId")

    val ask: Future[GraphMessage] = (dbActor ? DbActor.Write(rpcMessage.getParamsChannel, message)).map {
      case DbActor.DbActorWriteAck() => Await.result((dbActor ? DbActor.CreateChannel(electionChannel, ObjectType.ELECTION)).map {
        case DbActor.DbActorAck() => Left(rpcMessage)
        case DbActor.DbActorNAck(code, description) => Right(PipelineError(code, description, rpcMessage.id))
      }, duration)
      case DbActor.DbActorNAck(code, description) => Right(PipelineError(code, description, rpcMessage.id))
      case _ => Right(PipelineError(ErrorCodes.SERVER_ERROR.id, "Database actor returned an unknown answer", rpcMessage.id))
    }

    Await.result(ask, duration)
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
