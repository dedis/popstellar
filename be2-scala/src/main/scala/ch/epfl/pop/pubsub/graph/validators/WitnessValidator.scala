package ch.epfl.pop.pubsub.graph.validators

import ch.epfl.pop.model.network.JsonRpcRequest
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.network.method.message.data.ObjectType
import ch.epfl.pop.model.network.method.message.data.witness.WitnessMessage
import ch.epfl.pop.model.objects.{Channel, Hash, PublicKey, Signature}
import ch.epfl.pop.pubsub.graph.validators.MessageValidator.{validateChannelType, validateOwner}
import ch.epfl.pop.pubsub.graph.{GraphMessage, PipelineError}


case object WitnessValidator extends MessageDataContentValidator {
  def validateWitnessMessage(rpcMessage: JsonRpcRequest): GraphMessage = {
    def validationError(reason: String): PipelineError = super.validationError(reason, "WitnessMessage", rpcMessage.id)

    rpcMessage.getParamsMessage match {
      case Some(message: Message) =>
        val data: WitnessMessage = message.decodedData.get.asInstanceOf[WitnessMessage]
        val signature: Signature = data.signature
        val messageId: Hash = data.message_id
        val sender: PublicKey = message.sender

        val channel: Channel = rpcMessage.getParamsChannel

        //check if the signature in the message received is valid
        if (!signature.verify(sender, messageId.base64Data)) {
          Right(validationError("verification of the signature over the message id failed"))
        } else if (!validateOwner(sender, channel)) {
          Right(validationError(s"invalid sender $sender"))
        } else if (!validateChannelType(ObjectType.LAO, channel)) {
          Right(validationError(s"trying to send a WitnessMessage message on a wrong type of channel $channel"))
        } else {
          Left(rpcMessage)
        }
      case _ => Right(validationErrorNoMessage(rpcMessage.id))
    }
  }
}
