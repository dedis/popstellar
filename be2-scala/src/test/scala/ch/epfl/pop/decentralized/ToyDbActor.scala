package ch.epfl.pop.decentralized

import akka.actor.{Actor, ActorLogging}
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.network.method.message.data.ObjectType
import ch.epfl.pop.model.objects.{Base64Data, Channel, ChannelData, Hash}
import ch.epfl.pop.storage.DbActor
final case class ToyDbActor() extends Actor {

  final val CHANNEL1_NAME: String = "/root/wex/lao1Id"
  final val CHANNEL2_NAME: String = "/root/wex/lao2Id"
  final val MESSAGE1_ID: Hash = Hash(Base64Data.encode("message1Id"))
  final val MESSAGE2_ID: Hash = Hash(Base64Data.encode("message2Id"))
  final val MESSAGE3_ID: Hash = Hash(Base64Data.encode("message3Id"))
  final val MESSAGE4_ID: Hash = Hash(Base64Data.encode("message4Id"))
  final val MESSAGE5_ID: Hash = Hash(Base64Data.encode("message5Id"))
  final val MESSAGE1: Message = Message(null, null, null, MESSAGE1_ID, null, null)
  final val MESSAGE4: Message = Message(null, null, null, MESSAGE4_ID, null, null)
  override def receive: Receive = {
    case DbActor.GetAllChannels() =>
      sender() ! DbActor.DbActorGetAllChannelsAck(Set(Channel(CHANNEL1_NAME), Channel(CHANNEL2_NAME)))
    case DbActor.ReadChannelData(channel) =>
      if (channel.channel.equals(CHANNEL1_NAME)) {
        sender() ! DbActor.DbActorReadChannelDataAck(ChannelData(ObjectType.LAO, List(MESSAGE1_ID)))
      } else {
        sender() ! DbActor.DbActorReadChannelDataAck(ChannelData(ObjectType.LAO, List(MESSAGE4_ID)))
      }
    case DbActor.Catchup(channel) =>
      if (channel.channel.equals(CHANNEL1_NAME)) {
        sender() ! DbActor.DbActorCatchupAck(List(MESSAGE1))
      } else {
        sender() ! DbActor.DbActorCatchupAck(List(MESSAGE4))
      }
  }
}
