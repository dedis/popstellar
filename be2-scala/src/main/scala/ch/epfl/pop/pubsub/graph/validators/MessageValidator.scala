package ch.epfl.pop.pubsub.graph.validators

import akka.pattern.AskableActorRef
import ch.epfl.pop.model.network.JsonRpcRequest
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.network.method.message.data.ObjectType
import ch.epfl.pop.model.objects.{Channel, Hash, PublicKey}
import ch.epfl.pop.pubsub.AskPatternConstants
import ch.epfl.pop.pubsub.graph.{GraphMessage, PipelineError, bindToPipe}
import ch.epfl.pop.storage.DbActor

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
    *   GraphMessage: passes the rpcMessages to Right if successful right with pipeline error
    */
  def runChecks(checks: GraphMessage*): GraphMessage = {
    if (checks.head.isRight && !checks.tail.isEmpty)
      runChecks(checks.tail: _*)
    else
      checks.head
  }

  def validateMessage(rpcMessage: JsonRpcRequest): GraphMessage = {

    val message: Message = rpcMessage.getParamsMessage.get
    val expectedId: Hash = Hash.fromStrings(message.data.toString, message.signature.toString)

    for {
      _ <- bindToPipe(
        rpcMessage,
        message.message_id == expectedId,
        validationError(
          "Invalid message_id",
          "MessageValidator",
          rpcMessage.id
        )
      )
      _ <- bindToPipe(
        rpcMessage,
        message.signature.verify(message.sender, message.data),
        validationError(
          "Invalid sender signature",
          "MessageValidator",
          rpcMessage.id
        )
      )
      result <- bindToPipe(
        rpcMessage,
        message.witness_signatures.forall(ws => ws.verify(message.message_id)),
        validationError("Invalid witness signature", "MessageValidator", rpcMessage.id)
      )
    } yield result
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
    bindToPipe(rpcMessage, validateAttendee(sender, channel, dbActor), error)
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
    bindToPipe(rpcMessage, validateOwner(sender, channel, dbActor), error)
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
      channelObjectType: ObjectType.ObjectType,
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
      channelObjectType: ObjectType.ObjectType,
      channel: Channel,
      dbActor: AskableActorRef = DbActor.getInstance,
      error: PipelineError
  ): GraphMessage = {
    bindToPipe(rpcMessage, validateChannelType(channelObjectType, channel, dbActor), error)
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
    *   GraphMessage: passes the rpcMessages to Right if successful right with pipeline error
    */
  def checkMsgSenderKey(
      rpcMessage: JsonRpcRequest,
      expectedKey: PublicKey,
      msgSenderKey: PublicKey,
      error: PipelineError
  ): GraphMessage = {
    bindToPipe(rpcMessage, expectedKey == msgSenderKey, error)
  }

}
