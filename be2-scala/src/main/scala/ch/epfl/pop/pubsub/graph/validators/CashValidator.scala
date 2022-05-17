package ch.epfl.pop.pubsub.graph.validators

import ch.epfl.pop.model.network.JsonRpcRequest
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.network.method.message.data.ObjectType
import ch.epfl.pop.model.network.method.message.data.cash.PostTransaction
import ch.epfl.pop.model.objects.Channel
import ch.epfl.pop.pubsub.graph.validators.MessageValidator._
import ch.epfl.pop.pubsub.graph.{GraphMessage, PipelineError}

import scala.concurrent._


case object CashValidator extends MessageDataContentValidator {
  def validatePostTransaction(rpcMessage: JsonRpcRequest): GraphMessage = {
    def validationError(reason: String): PipelineError = super.validationError(reason, "PostTransaction", rpcMessage.id)

    rpcMessage.getParamsMessage match {
      case Some(message: Message) =>
        Left(rpcMessage)

      case _ => Right(validationErrorNoMessage(rpcMessage.id))
    }
  }
}
