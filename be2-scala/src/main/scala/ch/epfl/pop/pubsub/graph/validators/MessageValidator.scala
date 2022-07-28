package ch.epfl.pop.pubsub.graph.validators

import akka.pattern.AskableActorRef
import ch.epfl.pop.model.network.JsonRpcRequest
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.network.method.message.data.ObjectType
import ch.epfl.pop.model.objects.{Channel, Hash, PublicKey}
import ch.epfl.pop.pubsub.AskPatternConstants
import ch.epfl.pop.pubsub.graph.GraphMessage
import ch.epfl.pop.storage.DbActor

import scala.concurrent.Await
import scala.util.Success

object MessageValidator extends ContentValidator with AskPatternConstants {

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

  /** checks whether the channel of the JsonRpcRequest is of the given type
    *
    * @param channelObjectType
    *   the ObjectType the channel should be
    * @param channel
    *   the channel we want to check
    * @param dbActor
    *   the DbActor we use (by default the main one, obtained through getInstance)
    */
  def validateChannelType(channelObjectType: ObjectType.ObjectType, channel: Channel, dbActor: AskableActorRef = DbActor.getInstance): Boolean = {
    val ask = dbActor ? DbActor.ReadChannelData(channel)
    Await.ready(ask, duration).value.get match {
      case Success(DbActor.DbActorReadChannelDataAck(channelData)) => channelData.channelType == channelObjectType
      case _                                                       => false
    }
  }
}
