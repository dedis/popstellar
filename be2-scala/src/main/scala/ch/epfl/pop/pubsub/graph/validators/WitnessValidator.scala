package ch.epfl.pop.pubsub.graph.validators

import akka.pattern.AskableActorRef
import ch.epfl.pop.model.network.JsonRpcRequest
import ch.epfl.pop.model.network.method.message.data.witness.WitnessMessage
import ch.epfl.pop.model.objects.WitnessSignaturePair
import ch.epfl.pop.pubsub.graph.validators.MessageValidator._
import ch.epfl.pop.pubsub.graph.{GraphMessage, PipelineError, bindToPipe}
import ch.epfl.pop.storage.DbActor

//Similarly to the handlers, we create a WitnessValidator object which creates a WitnessValidator class instance.
//The defaults dbActorRef is used in the object, but the class can now be mocked with a custom dbActorRef for testing purpose
object WitnessValidator {
  lazy val witnessValidator = new WitnessValidator(DbActor.getInstance)
  def validateWitnessMessage(rpcMessage: JsonRpcRequest): GraphMessage = witnessValidator.validateWitnessMessage(rpcMessage)
}

sealed class WitnessValidator(dbActorRef: => AskableActorRef) extends MessageDataContentValidator {
  def validateWitnessMessage(rpcMessage: JsonRpcRequest): GraphMessage = {
    def validationError(reason: String): PipelineError = super.validationError(reason, "WitnessMessage", rpcMessage.id)

    for {
      _ <- bindToPipe(rpcMessage, rpcMessage.getParamsMessage, validationErrorNoMessage(rpcMessage.id))
      (data, _, sender, channel) = extractData[WitnessMessage](rpcMessage)
      witnessSignaturePair = WitnessSignaturePair(sender, data.signature)
      _ <- checkWitnessesSignatures(
        rpcMessage,
        List(witnessSignaturePair),
        data.message_id,
        validationError(
          "verification of the signature over the message id failed"
        )
      )
      result <- checkOwner(rpcMessage, sender, channel, dbActorRef, validationError(s"invalid sender $sender"))
    } yield result
  }
}
