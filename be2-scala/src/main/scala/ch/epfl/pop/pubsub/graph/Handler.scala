package ch.epfl.pop.pubsub.graph

import akka.NotUsed
import akka.stream.scaladsl.Flow
import ch.epfl.pop.model.network.{JsonRpcRequest, JsonRpcResponse}
import ch.epfl.pop.pubsub.MessageRegistry

object Handler {
  private def handle(rpcRequest: JsonRpcRequest, messageRegistry: MessageRegistry): GraphMessage = {
    val (_object, action) = rpcRequest.getDecodedDataHeader
    messageRegistry.getHandler(_object, action) match {
      case Some(handler) => handler(rpcRequest)
      case _ => Right(PipelineError(
          ErrorCodes.SERVER_ERROR.id,
          s"MessageRegistry could not find any handler for JsonRpcRequest : $rpcRequest'",
          rpcRequest.getId
        ))
    }
  }

  // handles messages
  def handler(messageRegistry: MessageRegistry): Flow[GraphMessage, GraphMessage, NotUsed] = Flow[GraphMessage].map {
    case Left(rpcRequest: JsonRpcRequest) => handle(rpcRequest, messageRegistry)
    case Left(rpcResponse: JsonRpcResponse) => Right(PipelineError(
        ErrorCodes.SERVER_ERROR.id,
        "NOT IMPLEMENTED : Server does not allow JsonRpcResponse processing (handling) currently",
        rpcResponse.getId
      ))
    case graphMessage @ _ => graphMessage
  }
}
