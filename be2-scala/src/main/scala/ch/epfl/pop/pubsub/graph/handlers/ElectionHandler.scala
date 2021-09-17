package ch.epfl.pop.pubsub.graph.handlers
import akka.NotUsed
import akka.stream.scaladsl.Flow
import ch.epfl.pop.model.network.requests.election.{JsonRpcRequestEndElection, JsonRpcRequestResultElection, JsonRpcRequestSetupElection}
import ch.epfl.pop.model.network.{JsonRpcRequest, JsonRpcResponse}
import ch.epfl.pop.pubsub.graph.{ErrorCodes, GraphMessage, PipelineError}

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

  // FIXME election stuff
  def handleSetupElection(rpcMessage: JsonRpcRequest): GraphMessage = Left(rpcMessage)

  def handleResultElection(rpcMessage: JsonRpcRequest): GraphMessage = Left(rpcMessage)

  def handleEndElection(rpcMessage: JsonRpcRequest): GraphMessage = Left(rpcMessage)
}
