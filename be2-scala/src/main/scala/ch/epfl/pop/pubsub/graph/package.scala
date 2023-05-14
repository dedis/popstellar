package ch.epfl.pop.pubsub

import ch.epfl.pop.json.HighLevelProtocol
import ch.epfl.pop.model.network.{JsonRpcMessage, JsonRpcRequest, JsonRpcResponse}

package object graph {
  type JsonString = String
  type GraphMessage = Either[PipelineError, JsonRpcMessage]

  def prettyPrinter(graphMessage: GraphMessage): String = {
    graphMessage match {
      case Right(jsonRpcMessage: JsonRpcMessage) =>
        jsonRpcMessage match {
          case jsonRpcRequest: JsonRpcRequest   => HighLevelProtocol.jsonRpcRequestFormat.write(jsonRpcRequest).prettyPrint
          case jsonRpcResponse: JsonRpcResponse => HighLevelProtocol.jsonRpcResponseFormat.write(jsonRpcResponse).prettyPrint
        }
      case Left(pipelineError) => pipelineError.toString
    }
  }
}
