package ch.epfl.pop.pubsub.graph.validators

import ch.epfl.pop.model.network.JsonRpcRequest
import ch.epfl.pop.pubsub.graph.{ErrorCodes, GraphMessage, PipelineError}

import scala.util.matching.Regex

object ParamsValidator extends MethodContentValidator {

  final def validationError(reason: String, rpcId: Option[Int]): PipelineError = super.validationError(reason, "MethodValidator", rpcId)

  private def validateGeneralParams(rpcMessage: JsonRpcRequest): GraphMessage = {
    if (!validateChannel(rpcMessage.getParamsChannel)) {
      Left(ParamsValidator.validationError("Channel validation failed", rpcMessage.id))
    } else {
      Right(rpcMessage)
    }
  }

  private def validateGeneralParamsPagedCatchup(rpcMessage: JsonRpcRequest): GraphMessage = {
    if (!validateChannel(rpcMessage.getParamsChannel)) {
      Left(ParamsValidator.validationError("Channel validation failed", rpcMessage.id))
    } else {
      val pattern: Regex = "^/root(/[^/]+)/social/(chirps(/[^/]+)|profile(/[^/]+){2})$".r

      pattern.findFirstMatchIn(rpcMessage.getParamsChannel.toString) match {
        case Some(_) => Right(rpcMessage)
        case None =>
          Left(PipelineError(ErrorCodes.INVALID_ACTION.id, "Paging is not supported on this channel", rpcMessage.id))
      }
    }
  }

  def validateBroadcast(rpcMessage: JsonRpcRequest): GraphMessage = validateGeneralParams(rpcMessage)

  def validateCatchup(rpcMessage: JsonRpcRequest): GraphMessage = validateGeneralParams(rpcMessage)
  def validatePagedCatchup(rpcMessage: JsonRpcRequest): GraphMessage = validateGeneralParamsPagedCatchup(rpcMessage)
  def validatePublish(rpcMessage: JsonRpcRequest): GraphMessage = validateGeneralParams(rpcMessage)

  def validateSubscribe(rpcMessage: JsonRpcRequest): GraphMessage = validateGeneralParams(rpcMessage)

  def validateUnsubscribe(rpcMessage: JsonRpcRequest): GraphMessage = validateGeneralParams(rpcMessage)
}
