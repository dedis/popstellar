package ch.epfl.pop.pubsub

import ch.epfl.pop.model.network.JsonRpcMessage

package object graph {
  type JsonString = String
  type GraphMessage = Either[JsonRpcMessage, PipelineError] // FIXME convention states that Right(_) is the success value

  def bindToPipe(rpcMessage: JsonRpcMessage, success: Boolean, error: PipelineError) = {
    if (success) Left(rpcMessage) else Right(error)
  }
}
