package ch.epfl.pop.pubsub.graph.validators

import akka.pattern.AskableActorRef
import ch.epfl.pop.model.network.method.message.data.witness.WitnessMessage
import ch.epfl.pop.model.network.{JsonRpcMessage, JsonRpcRequest}
import ch.epfl.pop.model.objects.{Channel, Hash, PublicKey, WitnessSignaturePair}
import ch.epfl.pop.pubsub.graph.validators.MessageValidator._
import ch.epfl.pop.pubsub.graph.{GraphMessage, PipelineError}
import ch.epfl.pop.storage.DbActor

//Similarly to the handlers, we create a WitnessValidator object which creates a WitnessValidator class instance.
//The defaults dbActorRef is used in the object, but the class can now be mocked with a custom dbActorRef for testing purpose
object WitnessValidator {
  lazy val witnessValidator = new WitnessValidator(DbActor.getInstance)
  def validateWitnessMessage(rpcMessage: JsonRpcRequest): GraphMessage = witnessValidator.validateWitnessMessage(rpcMessage)
}

sealed class WitnessValidator(dbActorRef: => AskableActorRef) extends MessageDataContentValidator {
  type TestChange = Either[PipelineError, JsonRpcMessage]

  def bindToPipe[T](rpcMessage: JsonRpcMessage, opt: Option[T], pipelineError: PipelineError): TestChange = {
    if (opt.isEmpty) Left(pipelineError) else Right(rpcMessage)
  }

  def revert(graphMessage: GraphMessage): TestChange = {
    graphMessage match {
      case Left(message) => Right(message)
      case Right(err)    => Left(err)
    }
  }

  def undoRevert(testChange: TestChange): GraphMessage = {
    testChange match {
      case Left(err) => Right(err)
      case Right(x)  => Left(x)
    }
  }

  def checkWitnessesSignaturesTest(
      rpcMessage: JsonRpcRequest,
      witnessesKeyPairs: List[WitnessSignaturePair],
      id: Hash,
      error: PipelineError
  ): TestChange = {
    revert(checkWitnessesSignatures(rpcMessage, witnessesKeyPairs, id, error))
  }

  def checkOwnerTest(
      rpcMessage: JsonRpcRequest,
      sender: PublicKey,
      channel: Channel,
      dbActor: AskableActorRef = DbActor.getInstance,
      error: PipelineError
  ): TestChange = {
    revert(checkOwner(rpcMessage, sender, channel, dbActor, error))
  }
  def validateWitnessMessage(rpcMessage: JsonRpcRequest): GraphMessage = {
    undoRevert(validateWitnessMessageTest(rpcMessage))
  }

  def validateWitnessMessageTest(rpcMessage: JsonRpcRequest): TestChange = {
    def validationError(reason: String): PipelineError = super.validationError(reason, "WitnessMessage", rpcMessage.id)

    for {
      _ <- bindToPipe(rpcMessage, rpcMessage.getParamsMessage, validationErrorNoMessage(rpcMessage.id))
      (data, _, sender, channel) = extractData[WitnessMessage](rpcMessage)
      witnessSignaturePair = WitnessSignaturePair(sender, data.signature)
      _ <- checkWitnessesSignaturesTest(rpcMessage, List(witnessSignaturePair), data.message_id, validationError("verification of the signature over the message id failed"))
      result <- checkOwnerTest(rpcMessage, sender, channel, dbActorRef, validationError(s"invalid sender $sender"))
    } yield result
  }
}
