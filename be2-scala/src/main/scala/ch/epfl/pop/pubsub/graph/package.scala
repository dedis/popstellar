package ch.epfl.pop.pubsub

import ch.epfl.pop.model.network.JsonRpcMessage

package object graph {
  type JsonString = String

  type GraphMessage = Either[PipelineError, JsonRpcMessage]

  def bindToPipe(rpcMessage: JsonRpcMessage, success: Boolean, error: PipelineError) = {
    if (success) Left(rpcMessage) else Right(error)
  }
}
