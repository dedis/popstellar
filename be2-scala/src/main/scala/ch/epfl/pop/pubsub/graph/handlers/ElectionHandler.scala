package ch.epfl.pop.pubsub.graph.handlers
import akka.NotUsed
import akka.stream.scaladsl.Flow
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.network.requests.election.{JsonRpcRequestEndElection, JsonRpcRequestResultElection, JsonRpcRequestSetupElection}
import ch.epfl.pop.model.network.{JsonRpcRequest, JsonRpcResponse}
import ch.epfl.pop.pubsub.graph.{DbActor, ErrorCodes, GraphMessage, PipelineError}

import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global

object ElectionHandler extends MessageHandler {

  override val handler: Flow[GraphMessage, GraphMessage, NotUsed] = Flow[GraphMessage].map {
    case Left(jsonRpcMessage) => jsonRpcMessage match {
      case message@(_: JsonRpcRequestSetupElection) => handleSetupElection(message)
      case message@(_: JsonRpcRequestResultElection) => handleResultElection(message)
      case message@(_: JsonRpcRequestEndElection) => handleEndElection(message)
      case _ => Right(PipelineError(
        ErrorCodes.SERVER_ERROR.id,
        "Internal server fault: LaoHandler was given a message it could not recognize",
        jsonRpcMessage match {
          case r: JsonRpcRequest => r.id
          case r: JsonRpcResponse => r.id
          case _ => None
        }
      ))
    }
    case graphMessage@_ => graphMessage
  }

  def handleSetupElection(rpcMessage: JsonRpcRequestSetupElection): GraphMessage = {
    val message: Message = rpcMessage.getParamsMessage.get

    val f: Future[GraphMessage] = (dbActor ? DbActor.Write(rpcMessage.getParamsChannel, message)).map {
      case DbActor.DbActorWriteAck => Left(rpcMessage)
      case DbActor.DbActorNAck(code, description) => Right(PipelineError(code, description, rpcMessage.id))
      case _ => Right(PipelineError(ErrorCodes.SERVER_ERROR.id, "Database actor returned an unknown answer", rpcMessage.id))
    }

    Await.result(f, duration)
  }

  def handleResultElection(rpcMessage: JsonRpcRequest): GraphMessage = Right(
    PipelineError(ErrorCodes.SERVER_ERROR.id, "NOT IMPLEMENTED: ElectionHandler cannot handle ResultElection messages yet", rpcMessage.id)
  )

  def handleEndElection(rpcMessage: JsonRpcRequest): GraphMessage = Right(
    PipelineError(ErrorCodes.SERVER_ERROR.id, "NOT IMPLEMENTED: ElectionHandler cannot handle EndElection messages yet", rpcMessage.id)
  )
}
