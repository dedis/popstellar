package ch.epfl.pop.pubsub.graph.validators

import ch.epfl.pop.model.network.JsonRpcRequest
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.network.method.message.data.coin.PostTransaction
import ch.epfl.pop.model.objects.Transaction
import ch.epfl.pop.pubsub.graph.validators.MessageValidator._
import ch.epfl.pop.pubsub.graph.{GraphMessage, PipelineError}

case object CoinValidator extends MessageDataContentValidator {
  def validatePostTransaction(rpcMessage: JsonRpcRequest): GraphMessage = {
    def validationError(reason: String): PipelineError = super.validationError(reason, "PostTransaction", rpcMessage.id)
    rpcMessage.getParamsMessage match {
      case Some(message: Message) =>
        val data: PostTransaction = message.decodedData.get.asInstanceOf[PostTransaction]

        runChecks(
          checkId(
            rpcMessage,
            data.transactionId,
            data.transaction.transactionId,
            validationError("incorrect transaction id")
          ),
          checkTransactionSignatures(
            rpcMessage,
            data.transaction,
            validationError("bad signature")
          ),
          checkSumOutputs(
            rpcMessage,
            data.transaction,
            s => validationError(s)
          )
        )

      case _ => Right(validationErrorNoMessage(rpcMessage.id))
    }
  }

  /** @param rpcMessage
    *   rpc message to validate
    * @param transaction
    *   The transaction to be checked
    * @param error
    *   the error to forward in case the transaction signatures are not valid.
    * @return
    *   : GraphMessage passes the rpcMessages to Left if successful, else right with pipeline error
    */
  private def checkTransactionSignatures(rpcMessage: JsonRpcRequest, transaction: Transaction, error: PipelineError): GraphMessage = {
    if (transaction.checkSignatures())
      Left(rpcMessage)
    else {
      Right(error)
    }
  }

  /** @param rpcMessage
    *   rpc message to validate
    * @param transaction
    *   The transaction to be checked
    * @param validationError
    *   the error to forward in case the transaction's sum is not valid.
    * @return
    *   GraphMessage passes the rpcMessages to Left if successful, else right with pipeline error
    */
  private def checkSumOutputs(rpcMessage: JsonRpcRequest, transaction: Transaction, validationError: String => PipelineError): GraphMessage = {
    transaction.sumOutputs() match {
      case Left(err) => Right(validationError(err.getMessage))
      case Right(_)  => Left(rpcMessage)
    }
  }

}
