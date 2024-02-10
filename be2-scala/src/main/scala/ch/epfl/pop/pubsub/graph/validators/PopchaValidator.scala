package ch.epfl.pop.pubsub.graph.validators

import akka.pattern.AskableActorRef
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.network.method.message.data.ObjectType
import ch.epfl.pop.model.network.method.message.data.popcha.Authenticate
import ch.epfl.pop.model.network.{JsonRpcMessage, JsonRpcRequest}
import ch.epfl.pop.model.objects._
import ch.epfl.pop.pubsub.PublishSubscribe
import ch.epfl.pop.pubsub.graph.validators.MessageValidator.{checkAttendee, checkChannelType, extractData}
import ch.epfl.pop.pubsub.graph.{GraphMessage, PipelineError}

/** Validator for Popcha messages' data contents
  */
case object PopchaValidator extends MessageDataContentValidator {

  private val popchaValidator = new PopchaValidator(PublishSubscribe.getDbActorRef)

  /** Validates a Popcha Authenticate message data content
    * @param rpcMessage
    *   Message received
    * @return
    *   A graph message representing the result of the validation
    */
  def validateAuthenticateRequest(rpcMessage: JsonRpcRequest): GraphMessage = popchaValidator.validateAuthenticateRequest(rpcMessage)
}

sealed class PopchaValidator(dbActorRef: => AskableActorRef) extends MessageDataContentValidator {

  def validateAuthenticateRequest(rpcMessage: JsonRpcRequest): GraphMessage = {
    def validationError(reason: String): PipelineError = super.validationError(reason, "Authenticate", rpcMessage.id)

    rpcMessage.getParamsMessage match {
      case Some(_: Message) =>
        val (authenticate, laoId, sender, channel) = extractData[Authenticate](rpcMessage)

        for {
          _ <- checkResponseMode(rpcMessage, authenticate.responseMode, validationError(s"Invalid response mode ${authenticate.responseMode}"))
          _ <- checkChannelType(rpcMessage, ObjectType.POPCHA, channel, dbActorRef, validationError(s"Incorrect channel $channel for popcha authentication message"))
          _ <- checkAttendee(rpcMessage, sender, channel, dbActorRef, validationError(s"User doesn't belong to the requested lao $laoId"))
          result <- checkIdentifierProof(
            rpcMessage,
            authenticate.identifier,
            authenticate.identifierProof,
            authenticate.nonce,
            validationError("Failed to verify the identifier proof with the given identifier/nonce pair")
          )
        } yield result

      case None => Left(validationErrorNoMessage(rpcMessage.id))
    }
  }

  private def checkResponseMode(rpcMessage: JsonRpcRequest, responseMode: String, error: PipelineError): GraphMessage = {
    if (responseMode == "query" || responseMode == "fragment")
      Right(rpcMessage)
    else
      Left(error)
  }

  private def checkIdentifierProof(rpcMessage: JsonRpcMessage, identifier: PublicKey, identifierProof: Signature, nonce: Base64Data, error: PipelineError): GraphMessage = {
    val verified = identifierProof.verify(identifier, nonce)
    if (verified)
      Right(rpcMessage)
    else
      Left(error)
  }
}
