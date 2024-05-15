package ch.epfl.pop.pubsub.graph.validators

import akka.pattern.AskableActorRef
import ch.epfl.pop.model.network.JsonRpcRequest
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.network.method.message.data.ObjectType
import ch.epfl.pop.model.network.method.message.data.federation.{FederationChallenge, FederationChallengeRequest, FederationInit}
import ch.epfl.pop.pubsub.PublishSubscribe
import ch.epfl.pop.pubsub.graph.validators.MessageValidator.{checkChannelType, checkMsgSenderKey, checkMsgServerKey, checkOwner, extractData, runChecks}
import ch.epfl.pop.pubsub.graph.{GraphMessage, PipelineError}

object FederationValidator extends MessageDataContentValidator  with EventValidator {

  val federationValidator = new FederationValidator(PublishSubscribe.getDbActorRef)

  def validateFederationChallenge(rpcMessage: JsonRpcRequest): GraphMessage = federationValidator.validateFederationChallenge(rpcMessage)

  def validateFederationChallengeRequest(rpcMessage: JsonRpcRequest): GraphMessage = federationValidator.validateChallengeRequest(rpcMessage)

  def validateFederationInit(rpcMessage: JsonRpcRequest): GraphMessage = federationValidator.validateFederationInit(rpcMessage)

  def validateFederationExpect(rpcMessage: JsonRpcRequest): GraphMessage = federationValidator.validateFederationExpect(rpcMessage)

  def validateFederationResult(rpcMessage: JsonRpcRequest): GraphMessage = federationValidator.validateFederationResult(rpcMessage)

}

sealed class FederationValidator(dbActorRef: => AskableActorRef) extends MessageDataContentValidator with EventValidator{

  private val CHALLENGE_VALUE_REGEX = "^[0-9a-fA-F]{64}$".r
  private val SERVER_ADDRESS_REGEX = "^(ws|wss)://.*(:\\\\d{0,5})?/.*$".r

  def validateFederationChallenge(rpcMessage: JsonRpcRequest): GraphMessage = {
    def validationError(reason: String): PipelineError = super.validationError(reason, "FederationChallenge", rpcMessage.id)

    rpcMessage.getParamsMessage match {

      case Some(message: Message) =>
        val (federationChallenge, laoId, senderPk, channel) = extractData[FederationChallenge](rpcMessage)
        val ask = dbActor
        runChecks(
          checkTimestampStaleness(
            rpcMessage,
            federationChallenge.valid_until,
            validationError(s"stale 'valid_until' timestamp (${federationChallenge.valid_until})")
          ),
          checkStringPattern(
            rpcMessage,
            federationChallenge.value.toString,
            CHALLENGE_VALUE_REGEX,
            validationError(s"challenge value should contain 64 hexadecimal characters i.e represent a 32 hexadecimal byte array")
          ),
          checkMsgServerKey(
            rpcMessage,
            senderPk,
            validationError(s"invalid sender $senderPk")
          ),
          checkChannelType(
            rpcMessage,
            ObjectType.federation,
            channel,
            dbActorRef,
            validationError(s"trying to send a challenge message on a wrong type of channel $channel")
          )
        )
      case _ => Left(validationErrorNoMessage(rpcMessage.id))
    }
  }

  def validateFederationChallengeRequest(rpcMessage: JsonRpcRequest): GraphMessage = {
    def validationError(reason: String): PipelineError = super.validationError(reason, "FederationChallengeRequest", rpcMessage.id)

    rpcMessage.getParamsMessage match {

      case Some(message: Message) =>
        val (federationChallengeRequest, laoId, senderPk, channel) = extractData[FederationChallengeRequest](rpcMessage)

        runChecks(
          checkTimestampStaleness(
            rpcMessage,
            federationChallengeRequest.timestamp,
            validationError(s"stale timestamp (${federationChallengeRequest.timestamp})")
        ),
          checkChannelType(
            rpcMessage,
            ObjectType.federation,
            channel,
            dbActorRef,
            validationError(s"trying to send a challenge request message on a wrong type of channel $channel")
          ),
          checkOwner(
            rpcMessage,
            senderPk,
            channel,
            dbActorRef,
            validationError(s"Sender $senderPk of the challenge request is not the organizer")
          )
        ) 
      case _ => Left(validationErrorNoMessage(rpcMessage.id))   
    }
  }
  
  def validateFederationInit(rpcMessage: JsonRpcRequest): GraphMessage = {
    def validationError(reason: String): PipelineError = super.validationError(reason, "FederationInit", rpcMessage.id)
    
    rpcMessage.getParamsMessage match {

      case Some(message : Message) =>
        
        val (federationInit, laoId, senderPk, channel) = extractData[FederationInit](rpcMessage)
        
        runChecks(
          checkDifferentId(
            rpcMessage,
            laoId,
            federationInit.lao_id,
            validationError(s"Needing a different lao to federate")
          ),
          checkStringPattern(
            rpcMessage,
            federationInit.server_address,
            SERVER_ADDRESS_REGEX,
            validationError(s"invalid server address")
          ),
          checkChannelType(
            rpcMessage,
            ObjectType.federation,
            channel,
            dbActorRef,
            validationError(s"trying to send a federation init message on a wrong type of channel $channel")
          ),
          checkDifferentKeys( // need to find a way to check exactly 
            rpcMessage,
            federationInit.public_key,
            senderPk,
            validationError(s"Needing the public key of the other organizer to federate")
          )
          
        )
        
    }
  }
  def validateFederationExpect(rpcMessage: JsonRpcRequest): GraphMessage = {
    def validationError(reason: String): PipelineError = super.validationError(reason, "FederationExpect", rpcMessage.id)
  }
  def validateFederationResult(rpcMessage: JsonRpcRequest): GraphMessage = {
    def validationError(reason: String): PipelineError = super.validationError(reason, "FederationResult", rpcMessage.id)
  }


}
