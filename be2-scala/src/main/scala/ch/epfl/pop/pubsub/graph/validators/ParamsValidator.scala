package ch.epfl.pop.pubsub.graph.validators

import ch.epfl.pop.model.network.JsonRpcRequest
import ch.epfl.pop.pubsub.graph.{GraphMessage, PipelineError}

object ParamsValidator extends MethodContentValidator {

  final def validationError(reason: String, rpcId: Option[Int]): PipelineError = super.validationError(reason, "MethodValidator", rpcId)

  private def validateGeneralParams(rpcMessage: JsonRpcRequest): GraphMessage = {
    if (!validateChannel(rpcMessage.getParamsChannel)) {
      Left(ParamsValidator.validationError("Channel validation failed", rpcMessage.id))
    } else {
      Right(rpcMessage)
    }
  }

  def validateBroadcast(rpcMessage: JsonRpcRequest): GraphMessage = validateGeneralParams(rpcMessage)

  def validateCatchup(rpcMessage: JsonRpcRequest): GraphMessage = validateGeneralParams(rpcMessage)

  def validatePublish(rpcMessage: JsonRpcRequest): GraphMessage = validateGeneralParams(rpcMessage)

  def validateSubscribe(rpcMessage: JsonRpcRequest): GraphMessage = validateGeneralParams(rpcMessage)

  def validateUnsubscribe(rpcMessage: JsonRpcRequest): GraphMessage = validateGeneralParams(rpcMessage)
}
