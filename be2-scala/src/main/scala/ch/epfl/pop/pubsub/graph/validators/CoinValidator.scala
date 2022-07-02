package ch.epfl.pop.pubsub.graph.validators

import ch.epfl.pop.model.network.JsonRpcRequest
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.network.method.message.data.ObjectType
import ch.epfl.pop.model.network.method.message.data.coin.PostTransaction
import ch.epfl.pop.model.objects.Channel
import ch.epfl.pop.pubsub.graph.validators.MessageValidator._
import ch.epfl.pop.pubsub.graph.{GraphMessage, PipelineError}

import scala.concurrent._


case object CoinValidator extends MessageDataContentValidator {
  def validatePostTransaction(rpcMessage: JsonRpcRequest): GraphMessage = {
    def validationError(reason: String): PipelineError = super.validationError(reason, "PostTransaction", rpcMessage.id)


    rpcMessage.getParamsMessage match {
      case Some(message: Message) =>
        val data: PostTransaction = message.decodedData.get.asInstanceOf[PostTransaction]
        if (data.transactionId != data.transaction.transactionId) {
          Right(validationError("incorrect transaction id"))
        } else if (!data.transaction.checkSignatures()) {
          Right(validationError("bad signature"))
        } else {
          data.transaction.sumOutputs() match {
            case Left(err) => Right(validationError(err.getMessage()))
            case Right(_) => Left(rpcMessage)
          }
        }

      case _ => Right(validationErrorNoMessage(rpcMessage.id))
    }
  }
}
