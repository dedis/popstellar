package ch.epfl.pop.pubsub.graph.validators

import ch.epfl.pop.model.network.JsonRpcRequest
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.network.method.message.data.ObjectType
import ch.epfl.pop.model.network.method.message.data.coin.PostTransaction
import ch.epfl.pop.model.objects.{Channel, Hash, Transaction}
import ch.epfl.pop.pubsub.graph.validators.MessageValidator._
import ch.epfl.pop.pubsub.graph.{GraphMessage, PipelineError}

import scala.concurrent._

case object CoinValidator extends MessageDataContentValidator {
  def validatePostTransaction(rpcMessage: JsonRpcRequest): GraphMessage = {
    def validationError(reason: String): PipelineError = super.validationError(reason, "PostTransaction", rpcMessage.id)

    rpcMessage.getParamsMessage match {
      case Some(message: Message) =>
        val data: PostTransaction = message.decodedData.get.asInstanceOf[PostTransaction]

        runChecks(
          checkTransactionId(rpcMessage, data.transactionId, data.transaction.transactionId, validationError("incorrect transaction id")),
          checkTransactionSignatures(rpcMessage, data.transaction, validationError("bad signature")),
          checkSumOutputs(rpcMessage, data.transaction)
        )

      case _ => Right(validationErrorNoMessage(rpcMessage.id))
    }
  }
  private def checkTransactionId(rpcMessage: JsonRpcRequest, transactionId: Hash, expectedTransactionId: Hash, error: PipelineError): GraphMessage = {
    if (transactionId == expectedTransactionId)
      Left(rpcMessage)
    else {
      Right(error)
    }
  }
  private def checkTransactionSignatures(rpcMessage: JsonRpcRequest, transaction: Transaction, error: PipelineError): GraphMessage = {
    if (transaction.checkSignatures())
      Left(rpcMessage)
    else {
      Right(error)
    }
  }
  private def checkSumOutputs(rpcMessage: JsonRpcRequest, transaction: Transaction): GraphMessage = {
    def validationError(reason: String): PipelineError = super.validationError(reason, "PostTransaction", rpcMessage.id)
    transaction.sumOutputs() match {
      case Left(err) => Right(validationError(err.getMessage()))
      case Right(_)  => Left(rpcMessage)
    }
  }

}
