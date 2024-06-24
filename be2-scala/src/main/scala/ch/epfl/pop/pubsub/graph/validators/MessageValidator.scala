package ch.epfl.pop.pubsub.graph.validators

import akka.pattern.AskableActorRef
import ch.epfl.pop.model.network.JsonRpcRequest
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.network.method.message.data.ObjectType
import ch.epfl.pop.model.network.method.message.data.federation.{FederationExpect, FederationInit, FederationResult}
import ch.epfl.pop.model.objects.{Channel, Hash, PublicKey}
import ch.epfl.pop.pubsub.AskPatternConstants
import ch.epfl.pop.pubsub.graph.{ErrorCodes, GraphMessage, PipelineError}
import ch.epfl.pop.storage.DbActor

import scala.annotation.tailrec
import scala.concurrent.Await
import scala.util.Success

object MessageValidator extends ContentValidator with AskPatternConstants {

  /** Extracts the useful parameters from a rpc message
    *
    * @param rpcMessage
    *   rpc message which contains the parameters to extract
    * @return
    *   the decoded data, the id of the LAO, the PublicKey of the sender and the channel
    */
  def extractData[T](rpcMessage: JsonRpcRequest): (T, Hash, PublicKey, Channel) = {
    val message: Message = rpcMessage.getParamsMessage.get
    val data: T = message.decodedData.get.asInstanceOf[T]
    val laoId: Hash = rpcMessage.extractLaoId
    val sender: PublicKey = message.sender
    val channel: Channel = rpcMessage.getParamsChannel

    (data, laoId, sender, channel)
  }

  /** Runs the multiple checks of the validators
    *
    * @param checks
    *   checks which return a GraphMessage
    * @return
    *   GraphMessage: passes the rpcMessages to Right if successful Left with pipeline error
    */
  @tailrec
  def runChecks(checks: GraphMessage*): GraphMessage = {
    if (checks.head.isRight && checks.tail.nonEmpty)
      runChecks(checks.tail: _*)
    else
      checks.head
  }

  def validateMessage(rpcMessage: JsonRpcRequest): GraphMessage = {

    val message: Message = rpcMessage.getParamsMessage.get
    val expectedId: Hash = Hash.fromStrings(message.data.toString, message.signature.toString)

    if (message.message_id != expectedId) {
      Left(validationError("Invalid message_id", "MessageValidator", rpcMessage.id))
    } else if (!message.signature.verify(message.sender, message.data)) {
      Left(validationError("Invalid sender signature", "MessageValidator", rpcMessage.id))
    } else if (!message.witness_signatures.forall(ws => ws.verify(message.message_id))) {
      Left(validationError("Invalid witness signature", "MessageValidator", rpcMessage.id))
    } else {
      Right(rpcMessage)
    }
  }

  /** checks whether the sender of the JsonRpcRequest is in the attendee list inside the LAO's data
    *
    * @param sender
    *   the sender we want to verify
    * @param channel
    *   the channel we want the LaoData for
    * @param dbActor
    *   the AskableActorRef we use (by default the main DbActor, obtained through getInstance)
    */
  def validateAttendee(sender: PublicKey, channel: Channel, dbActor: AskableActorRef = DbActor.getInstance): Boolean = {
    val ask = dbActor ? DbActor.ReadLaoData(channel)
    Await.ready(ask, duration).value.get match {
      case Success(DbActor.DbActorReadLaoDataAck(laoData)) => laoData.attendees.contains(sender)
      case _                                               => false
    }
  }

  // Same as validateAttendee except that it returns a GraphMessage
  def checkAttendee(
      rpcMessage: JsonRpcRequest,
      sender: PublicKey,
      channel: Channel,
      dbActor: AskableActorRef = DbActor.getInstance,
      error: PipelineError
  ): GraphMessage = {
    if (validateAttendee(sender, channel, dbActor))
      Right(rpcMessage)
    else
      Left(error)
  }

  /** checks whether the sender of the JsonRpcRequest is the LAO owner
    *
    * @param sender
    *   the sender we want to verify
    * @param channel
    *   the channel we want the LaoData for
    * @param dbActor
    *   the DbActor we use (by default the main one, obtained through getInstance)
    */
  def validateOwner(sender: PublicKey, channel: Channel, dbActor: AskableActorRef = DbActor.getInstance): Boolean = {
    val ask = dbActor ? DbActor.ReadLaoData(channel)
    Await.ready(ask, duration).value.get match {
      case Success(DbActor.DbActorReadLaoDataAck(laoData)) => laoData.owner == sender
      case _                                               => false
    }
  }

  // Same as validateOwner except that it returns a GraphMessage
  def checkOwner(
      rpcMessage: JsonRpcRequest,
      sender: PublicKey,
      channel: Channel,
      dbActor: AskableActorRef = DbActor.getInstance,
      error: PipelineError
  ): GraphMessage = {
    if (validateOwner(sender, channel, dbActor))
      Right(rpcMessage)
    else
      Left(error)
  }

  /** checks whether the channel of the JsonRpcRequest is of the given type
    *
    * @param channelObjectType
    *   the ObjectType the channel should be
    * @param channel
    *   the channel we want to check
    * @param dbActor
    *   the DbActor we use (by default the main one, obtained through getInstance)
    */
  def validateChannelType(
      channelObjectType: ObjectType,
      channel: Channel,
      dbActor: AskableActorRef = DbActor.getInstance
  ): Boolean = {
    val ask = dbActor ? DbActor.ReadChannelData(channel)
    Await.ready(ask, duration).value.get match {
      case Success(DbActor.DbActorReadChannelDataAck(channelData)) => channelData.channelType == channelObjectType
      case _                                                       => false
    }
  }

  // Same as validateChannelType except that it returns a GraphMessage
  def checkChannelType(
      rpcMessage: JsonRpcRequest,
      channelObjectType: ObjectType,
      channel: Channel,
      dbActor: AskableActorRef = DbActor.getInstance,
      error: PipelineError
  ): GraphMessage = {
    if (validateChannelType(channelObjectType, channel, dbActor))
      Right(rpcMessage)
    else
      Left(error)
  }

  /** Checks if the msg senderPK is the expected one
    *
    * @param rpcMessage
    *   rpc message to validate
    * @param expectedKey
    *   the expected key
    * @param msgSenderKey
    *   the rpc message senderPK
    * @param error
    *   the error to forward in case the senderPK doesn't match the expected one
    * @return
    *   GraphMessage: passes the rpcMessages to Right if successful Left with pipeline error
    */
  def checkMsgSenderKey(
      rpcMessage: JsonRpcRequest,
      expectedKey: PublicKey,
      msgSenderKey: PublicKey,
      error: PipelineError
  ): GraphMessage = {
    if (expectedKey == msgSenderKey)
      Right(rpcMessage)
    else
      Left(error)
  }

  /** Checks if the organizer is the sender of the challenge in FederationExpect/FederationInit messages
    * @param rpcMessage
    *   rpc message to validate
    * @param challenge
    *   the challenge message to verify its sender
    * @param channel
    *   the channel we want the LaoData for
    * @param dbActor
    *   the DbActor we use (by default the main one, obtained through getInstance)
    * @param error
    *   the error to forward in case the sender publicKey doesn't match the expected one
    * @return
    *   GraphMessage: passes the rpcMessage to Right if successful Left with pipeline error
    */
  def checkOrganizerChallengeSender(rpcMessage: JsonRpcRequest, challenge: Message, channel: Channel, dbActor: AskableActorRef = DbActor.getInstance, error: PipelineError): GraphMessage = {
    val sender = challenge.sender
    if (validateOwner(sender, channel, dbActor))
      Right(rpcMessage)
    else
      Left(error)
  }

  /** Checks if the challenge message sender from server 1 to server 2 is the organizer initiating the federation
    * @param rpcMessage
    *   rpc message to validate
    * @param msgSenderKey
    *   the sender to verify
    * @param dbActor
    *   the DbActor we use (by default the main one, obtained through getInstance)
    * @param error
    *   the error to forward in case the sender publicKey doesn't match the expected one
    * @return
    *   GraphMessage: passes the rpcMessage to Right if successful Left with pipeline error
    */
  def checkChallengeSenderKey(rpcMessage: JsonRpcRequest, msgSenderKey: PublicKey, dbActor: AskableActorRef = DbActor.getInstance, error: PipelineError): GraphMessage =
    val channel = rpcMessage.getParamsChannel
    val laoId = rpcMessage.extractLaoId
    val ask = dbActor ? DbActor.ReadFederationExpect(channel, laoId)
    Await.ready(ask, duration).value.get match {
      case Success(DbActor.DbActorReadAck(Some(message))) =>
        val expectedKey = message.decodedData.get.asInstanceOf[FederationExpect].publicKey
        checkMsgSenderKey(rpcMessage, expectedKey, msgSenderKey, error)
      case Success(DbActor.DbActorReadAck(None)) => Left(PipelineError(ErrorCodes.INVALID_ACTION.id, s"didn't receive federationExpect before", rpcMessage.getId))
      case _                                                      => Left(PipelineError(ErrorCodes.SERVER_ERROR.id, s"Unexpected server behavior", rpcMessage.getId))
    }

  /** Checks if the challenge message sender in the federationResult message from server 2 to server 1 is the organizer expecting the federation and in case of success it also checks if the publicKey in the federationResult is the one of the organizer initiating the federation
    * @param rpcMessage
    *   rpc message to validate
    * @param msgSenderKey
    *   the sender to verify
    * @param dbActor
    *   the DbActor we use (by default the main one, obtained through getInstance)
    * @param error
    *   the error to forward in case the sender publicKey doesn't match the expected one
    * @return
    *   GraphMessage: passes the rpcMessage to Right if successful Left with pipeline error
    */
  def checkResultChallengeSenderKey(rpcMessage: JsonRpcRequest, result: FederationResult, msgSenderKey: PublicKey, dbActor: AskableActorRef = DbActor.getInstance, error: PipelineError): GraphMessage = {
    val channel = rpcMessage.getParamsChannel
    val laoId = rpcMessage.extractLaoId
    val ask = dbActor ? DbActor.ReadFederationInit(channel, laoId)
    Await.ready(ask, duration).value.get match {
      case Success(DbActor.DbActorReadAck(Some(message))) =>
        val init = message.decodedData.get.asInstanceOf[FederationInit]
        val expectedKey = init.publicKey
        val initSender = message.sender
        result.status match {
          case "success" =>
            if (expectedKey == msgSenderKey && initSender == result.publicKey.get)
              Right(rpcMessage)
            else
              Left(error)
          case "failure" => checkMsgSenderKey(rpcMessage, expectedKey, msgSenderKey, error)
          case _         => Left(PipelineError(ErrorCodes.INVALID_ACTION.id, s"invalid status", rpcMessage.getId))
        }
      case Success(DbActor.DbActorReadAck(None)) => Left(PipelineError(ErrorCodes.INVALID_ACTION.id, s"didn't receive federationInit before", rpcMessage.getId))
      case _                                                      => Left(PipelineError(ErrorCodes.SERVER_ERROR.id, s"Unexpected server behavior", rpcMessage.getId))
    }

  }

}
