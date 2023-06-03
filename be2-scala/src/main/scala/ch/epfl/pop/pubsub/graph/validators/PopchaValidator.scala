package ch.epfl.pop.pubsub.graph.validators

import akka.pattern.AskableActorRef
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.network.method.message.data.popcha.Authenticate
import ch.epfl.pop.model.network.{JsonRpcMessage, JsonRpcRequest}
import ch.epfl.pop.model.objects._
import ch.epfl.pop.pubsub.graph.validators.MessageValidator.{checkAttendee, extractData}
import ch.epfl.pop.pubsub.graph.{GraphMessage, PipelineError}
import ch.epfl.pop.storage.DbActor
import ch.epfl.pop.storage.DbActor.{DbActorReadUserAuthenticationAck, ReadUserAuthenticated}

import scala.concurrent.Await
import scala.util.Success

/** Validator for Popcha messages' data contents
  */
case object PopchaValidator extends MessageDataContentValidator {

  private val popchaValidator = new PopchaValidator(DbActor.getInstance)

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
          _ <- checkChannelName(rpcMessage, channel, laoId, validationError(s"Incorrect channel $channel for lao $laoId"))
          _ <- checkAttendee(rpcMessage, sender, channel, dbActorRef, validationError(s"User doesn't belong to the requested lao $laoId"))
          _ <- checkIdentifierProof(
            rpcMessage,
            authenticate.identifier,
            authenticate.identifierProof,
            authenticate.nonce,
            validationError("Failed to verify the identifier proof with the given identifier/nonce pair")
          )
          result <- checkNoDuplicateIdentifiers(
            rpcMessage,
            sender,
            authenticate.identifier,
            authenticate.clientId,
            validationError(s"Failed to verify that pop token $sender didn't have another user authenticate to client ${authenticate.clientId}")
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

  private def checkChannelName(rpcMessage: JsonRpcMessage, channel: Channel, laoId: Hash, error: PipelineError): GraphMessage = {
    val expectedChannelName = Channel.ROOT_CHANNEL_PREFIX + laoId + Channel.POPCHA_AUTHENTICATION_LOCATION
    if (channel.channel == expectedChannelName)
      Right(rpcMessage)
    else
      Left(error)
  }

  private def checkIdentifierProof(rpcMessage: JsonRpcMessage, identifier: PublicKey, identifierProof: Signature, nonce: String, error: PipelineError): GraphMessage = {
    val verified = identifierProof.verify(identifier, Base64Data.encode(nonce))
    if (verified)
      Right(rpcMessage)
    else
      Left(error)
  }

  private def checkNoDuplicateIdentifiers(rpcMessage: JsonRpcMessage, user: PublicKey, identifier: PublicKey, clientId: String, error: PipelineError): GraphMessage = {
    val ask = dbActorRef ? ReadUserAuthenticated(identifier, clientId)
    Await.ready(ask, duration).value.get match {
      case Success(DbActorReadUserAuthenticationAck(optUser)) => optUser match {
          case Some(userId) if userId == user => Right(rpcMessage)
          case None                           => Right(rpcMessage)
          case _                              => Left(error)
        }
      case _ => Left(error)
    }
  }
}
