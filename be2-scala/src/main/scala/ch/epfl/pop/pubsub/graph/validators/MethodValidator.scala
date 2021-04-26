package ch.epfl.pop.pubsub.graph.validators

import ch.epfl.pop.model.network.JsonRpcRequest
import ch.epfl.pop.pubsub.graph.{GraphMessage, PipelineError}

object MethodValidator extends MethodContentValidator {

  final def validationError(reason: String): PipelineError = super.validationError(reason, "MethodValidator")

  private def validateGeneralMethod(rpcMessage: JsonRpcRequest): GraphMessage = {
    if (!validateChannel(rpcMessage.getParamsChannel)) {
      Right(MethodValidator.validationError("Channel validation failed"))
    } else {
      Left(rpcMessage)
    }
  }

  def validateBroadcast(rpcMessage: JsonRpcRequest): GraphMessage = validateGeneralMethod(rpcMessage)
  def validateCatchup(rpcMessage: JsonRpcRequest): GraphMessage = validateGeneralMethod(rpcMessage)
  def validatePublish(rpcMessage: JsonRpcRequest): GraphMessage = validateGeneralMethod(rpcMessage)
  def validateSubscribe(rpcMessage: JsonRpcRequest): GraphMessage = validateGeneralMethod(rpcMessage)
  def validateUnsubscribe(rpcMessage: JsonRpcRequest): GraphMessage = validateGeneralMethod(rpcMessage)
}
