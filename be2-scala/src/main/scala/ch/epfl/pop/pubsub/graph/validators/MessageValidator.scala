package ch.epfl.pop.pubsub.graph.validators

import ch.epfl.pop.model.network.JsonRpcRequest
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.objects.Hash
import ch.epfl.pop.pubsub.graph.{ErrorCodes, GraphMessage, PipelineError}

object MessageValidator extends ContentValidator {
  /**
   * Creates a validation error message for reason <reason> that happened in
   * validator module <validator> with optional error code <errorCode>
   *
   * @param reason the reason of the validation error
   * @param validator validator module where the error occurred
   * @param errorCode error code related to the error
   * @return a description of the error and where it occurred
   */
  override def validationError(reason: String, validator: String, rpcId: Option[Int], errorCode: ErrorCodes.ErrorCodes = ErrorCodes.INVALID_DATA): PipelineError =
    super.validationError(reason, validator, rpcId, errorCode)

  def validateMessage(rpcMessage: JsonRpcRequest): GraphMessage = {

    val message: Message = rpcMessage.getParamsMessage.get
    val expectedId: Hash = Hash.fromStrings(message.data.toString, message.signature.toString)

    if (message.message_id != expectedId) {
      Right(validationError("Invalid message_id", "MessageValidator", rpcMessage.id))
    } else if (!message.signature.verify(message.sender, message.data)) {
      Right(validationError("Invalid sender signature", "MessageValidator", rpcMessage.id))
    } else if (!message.witness_signatures.forall(ws => ws.verify(message.message_id))) {
      Right(validationError("Invalid witness signature", "MessageValidator", rpcMessage.id))
    } else {
      Left(rpcMessage)
    }
  }
}
