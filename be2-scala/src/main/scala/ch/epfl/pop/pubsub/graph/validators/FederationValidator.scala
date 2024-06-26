package ch.epfl.pop.pubsub.graph.validators

import akka.pattern.AskableActorRef
import ch.epfl.pop.model.network.JsonRpcRequest
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.network.method.message.data.ObjectType
import ch.epfl.pop.model.network.method.message.data.federation.*
import ch.epfl.pop.pubsub.PublishSubscribe
import ch.epfl.pop.pubsub.graph.validators.MessageValidator.{checkChallengeSenderKey, checkChannelType, checkOrganizerChallengeSender, checkOwner, checkResultChallengeSenderKey, extractData, runChecks}
import ch.epfl.pop.pubsub.graph.{GraphMessage, PipelineError}

object FederationValidator extends MessageDataContentValidator {

  val federationValidator = new FederationValidator(PublishSubscribe.getDbActorRef)

  def validateFederationChallengeRequest(rpcMessage: JsonRpcRequest): GraphMessage = federationValidator.validateFederationChallengeRequest(rpcMessage)

  def validateFederationExpect(rpcMessage: JsonRpcRequest): GraphMessage = federationValidator.validateFederationExpect(rpcMessage)

  def validateFederationInit(rpcMessage: JsonRpcRequest): GraphMessage = federationValidator.validateFederationInit(rpcMessage)

  def validateFederationChallenge(rpcMessage: JsonRpcRequest): GraphMessage = federationValidator.validateFederationChallenge(rpcMessage)

  def validateFederationResult(rpcMessage: JsonRpcRequest): GraphMessage = federationValidator.validateFederationResult(rpcMessage)

}

sealed class FederationValidator(dbActorRef: => AskableActorRef) extends MessageDataContentValidator {

  private val CHALLENGE_VALUE_REGEX = "^[0-9a-fA-F]{64}$".r
  private val SERVER_ADDRESS_REGEX = "^(ws|wss)://.*(:\\\\d{0,5})?/.*$".r
  private val RESULT_STATUS_REGEX = "^failure|success$".r

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

  def validateFederationExpect(rpcMessage: JsonRpcRequest): GraphMessage = {
    def validationError(reason: String): PipelineError = super.validationError(reason, "FederationExpect", rpcMessage.id)

    rpcMessage.getParamsMessage match {
      case Some(message: Message) =>
        val (federationExpect, laoId, senderPk, channel) = extractData[FederationExpect](rpcMessage)

        runChecks(
          checkStringPattern(
            rpcMessage,
            federationExpect.serverAddress,
            SERVER_ADDRESS_REGEX,
            validationError(s"invalid server address")
          ),
          checkOrganizerChallengeSender(
            rpcMessage,
            federationExpect.challenge,
            channel,
            dbActorRef,
            validationError(s"Sender of the challenge in the federationExpect message is not the organizer")
          ),
          checkExpectChallenge(
            rpcMessage,
            federationExpect,
            dbActorRef,
            validationError(s"the challenge in the federationExpect does not match the challenge in the db")
          ),
          checkChannelType(
            rpcMessage,
            ObjectType.federation,
            channel,
            dbActorRef,
            validationError(s"trying to send a federationExpect message on a wrong type of channel $channel")
          ),
          checkOwner(
            rpcMessage,
            senderPk,
            channel,
            dbActorRef,
            validationError(s"Sender $senderPk of the federationExpect is not the organizer")
          )
        )
      case _ => Left(validationErrorNoMessage(rpcMessage.id))

    }
  }

  def validateFederationInit(rpcMessage: JsonRpcRequest): GraphMessage = {
    def validationError(reason: String): PipelineError = super.validationError(reason, "FederationInit", rpcMessage.id)

    rpcMessage.getParamsMessage match {

      case Some(message: Message) =>
        val (federationInit, laoId, senderPk, channel) = extractData[FederationInit](rpcMessage)
        val challenge = FederationChallenge.buildFromJson(federationInit.challenge.data.decodeToString())
        runChecks(
          checkStringPattern(
            rpcMessage,
            federationInit.serverAddress,
            SERVER_ADDRESS_REGEX,
            validationError(s"invalid server address")
          ),
          checkStringPattern(
            rpcMessage,
            challenge.value.toString,
            CHALLENGE_VALUE_REGEX,
            validationError("Invalid challenge value")
          ),
          checkOrganizerChallengeSender(
            rpcMessage,
            federationInit.challenge,
            channel,
            dbActorRef,
            validationError(s"Sender of the challenge in the federationInit message is not the organizer")
          ),
          checkChannelType(
            rpcMessage,
            ObjectType.federation,
            channel,
            dbActorRef,
            validationError(s"trying to send a federationInit message on a wrong type of channel $channel")
          ),
          checkOwner(
            rpcMessage,
            senderPk,
            channel,
            dbActorRef,
            validationError(s"Sender $senderPk of the federationInit is not the organizer")
          )
        )
      case _ => Left(validationErrorNoMessage(rpcMessage.id))
    }
  }

  def validateFederationChallenge(rpcMessage: JsonRpcRequest): GraphMessage = {
    def validationError(reason: String): PipelineError = super.validationError(reason, "FederationChallenge", rpcMessage.id)

    rpcMessage.getParamsMessage match {

      case Some(message: Message) =>
        val (federationChallenge, laoId, senderPk, channel) = extractData[FederationChallenge](rpcMessage)
        runChecks(
          checkTimestampStaleness(
            rpcMessage,
            federationChallenge.validUntil,
            validationError(s"stale 'valid_until' timestamp (${federationChallenge.validUntil})")
          ),
          checkValidUntil(
            rpcMessage,
            federationChallenge.validUntil,
            validationError(s"the challenge is not valid")
          ),
          checkStringPattern(
            rpcMessage,
            federationChallenge.value.toString,
            CHALLENGE_VALUE_REGEX,
            validationError(s"challenge value should contain 64 hexadecimal characters i.e represent a 32 hexadecimal byte array")
          ),
          checkChallengeSenderKey(
            rpcMessage,
            senderPk,
            dbActorRef,
            validationError(s"the sender of the challenge should be the organizer")
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

  def validateFederationResult(rpcMessage: JsonRpcRequest): GraphMessage = {
    def validationError(reason: String): PipelineError = super.validationError(reason, "FederationResult", rpcMessage.id)

    rpcMessage.getParamsMessage match {
      case Some(message: Message) =>
        val (federationResult, laoId, senderPk, channel) = extractData[FederationResult](rpcMessage)

        runChecks(
          checkStringPattern(
            rpcMessage,
            federationResult.status,
            RESULT_STATUS_REGEX,
            validationError(s"invalid result status")
          ),
          checkResultChallengeSenderKey(
            rpcMessage,
            federationResult,
            federationResult.challenge.sender,
            dbActorRef,
            validationError(s"the challenge message sender in federationResult is not the organizer and/or the publicKey in federationResult is not the sender of FederationInit")
          ),
          checkInitChallenge(
            rpcMessage,
            federationResult,
            dbActorRef,
            validationError(s"the challenge in the federationResult does not match the challenge in federationInit")
          ),
          checkChannelType(
            rpcMessage,
            ObjectType.federation,
            channel,
            dbActorRef,
            validationError(s"trying to send a federationResult message on a wrong type of channel $channel")
          )
        )
      case _ => Left(validationErrorNoMessage(rpcMessage.id))
    }
  }

}
