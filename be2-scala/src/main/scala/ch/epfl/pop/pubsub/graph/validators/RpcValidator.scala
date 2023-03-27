package ch.epfl.pop.pubsub.graph.validators

import ch.epfl.pop.model.network.{JsonRpcMessage, JsonRpcRequest, JsonRpcResponse}
import ch.epfl.pop.pubsub.graph.{GraphMessage, PipelineError}

object RpcValidator extends ContentValidator {
  final val JSON_RPC_VERSION: String = "2.0"

  private def validateGeneralRpc(rpcMessage: JsonRpcMessage, validationErrorFunction: String => PipelineError): GraphMessage = {
    if (rpcMessage.jsonrpc != JSON_RPC_VERSION) {
      Left(validationErrorFunction(s"json-rpc version should be '$JSON_RPC_VERSION' but is ${rpcMessage.jsonrpc}"))
    } else {
      Right(rpcMessage)
    }
  }

  def validateRpcRequest(rpcMessage: JsonRpcRequest): GraphMessage = {
    def validationError(reason: String): PipelineError = super.validationError(reason, "RpcRequest", rpcMessage.id)

    validateGeneralRpc(rpcMessage, validationError)
  }

  def validateRpcResponse(rpcMessage: JsonRpcResponse): GraphMessage = {
    def validationError(reason: String): PipelineError = super.validationError(reason, "RpcResponse", rpcMessage.id)

    validateGeneralRpc(rpcMessage, validationError)
  }
}
