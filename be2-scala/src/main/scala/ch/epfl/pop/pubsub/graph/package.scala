package ch.epfl.pop.pubsub

import ch.epfl.pop.model.network.JsonRpcMessage

package object graph {
  type JsonString = String

  type GraphMessage = Either[PipelineError, JsonRpcMessage]

  def bindToPipe(rpcMessage: JsonRpcMessage, success: Boolean, error: PipelineError): GraphMessage = {
    if (success) Right(rpcMessage) else Left(error)
  }

  def bindToPipe[T](rpcMessage: JsonRpcMessage, opt: Option[T], pipelineError: PipelineError): GraphMessage = {
    if (opt.isEmpty) Left(pipelineError) else Right(rpcMessage)
  }
}
