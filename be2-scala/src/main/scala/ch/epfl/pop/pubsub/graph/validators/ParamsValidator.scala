package ch.epfl.pop.pubsub.graph.validators

import ch.epfl.pop.model.network.JsonRpcRequest
import ch.epfl.pop.pubsub.graph.{GraphMessage, PipelineError}

object ParamsValidator extends MethodContentValidator {

  def validationError(reason: String, rpcId: Option[Int]): PipelineError = super.validationError(reason, "MethodValidator", rpcId)

  private def validateGeneralParams(rpcMessage: JsonRpcRequest): GraphMessage = {
    if (!validateChannel(rpcMessage.getParamsChannel)) {
      Right(ParamsValidator.validationError("Channel validation failed", rpcMessage.id))
    } else {
      Left(rpcMessage)
    }
  }

  def validateBroadcast(rpcMessage: JsonRpcRequest): GraphMessage = validateGeneralParams(rpcMessage)

  def validateCatchup(rpcMessage: JsonRpcRequest): GraphMessage = validateGeneralParams(rpcMessage)

  def validatePublish(rpcMessage: JsonRpcRequest): GraphMessage = validateGeneralParams(rpcMessage)

  def validateSubscribe(rpcMessage: JsonRpcRequest): GraphMessage = validateGeneralParams(rpcMessage)

  def validateUnsubscribe(rpcMessage: JsonRpcRequest): GraphMessage = validateGeneralParams(rpcMessage)
}
