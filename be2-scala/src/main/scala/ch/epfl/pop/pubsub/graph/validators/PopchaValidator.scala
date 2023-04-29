package ch.epfl.pop.pubsub.graph.validators

import ch.epfl.pop.model.network.JsonRpcRequest
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.network.method.message.data.popcha.Authenticate
import ch.epfl.pop.pubsub.graph.validators.CoinValidator.validationErrorNoMessage
import ch.epfl.pop.pubsub.graph.{GraphMessage, PipelineError}

case object PopchaValidator extends MessageDataContentValidator {
  def validateAuthenticateRequest(rpcMessage: JsonRpcRequest): GraphMessage = {
    def validationError(reason: String): PipelineError = super.validationError(reason, "CreateLao", rpcMessage.id)

    rpcMessage.getParamsMessage match {
      case Some(message: Message) =>
        val authenticate = message.decodedData.get.asInstanceOf[Authenticate]
        Right(rpcMessage)
      case None => Left(validationErrorNoMessage(rpcMessage.id))
    }
  }
}
