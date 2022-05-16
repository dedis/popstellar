package ch.epfl.pop.model.objects

import ch.epfl.pop.json.MessageDataProtocol._
import ch.epfl.pop.model.network.Parsable
import ch.epfl.pop.model.network.method.message.data.ObjectType
import spray.json._


//the general ObjectType will be LAO (for all but election and chirp channels for now)
final case class ChannelData(
                              channelType: ObjectType.ObjectType,
                              messages: List[Hash],
                              privateKey: PrivateKey
                            ) {
  def toJsonString: String = {
    val that: ChannelData = this // tricks the compiler into inferring the right type
    that.toJson.toString
  }

  def update(messageId: Hash, key: PrivateKey): ChannelData = {
    this.channelType match {
      case ObjectType.ELECTION => ChannelData(channelType, messageId :: messages, key)
      case _ => ChannelData(channelType, messageId :: messages, privateKey)
    }
  }
}

object ChannelData extends Parsable {
  def apply(
             channelType: ObjectType.ObjectType,
             messages: List[Hash],
             privateKey: PrivateKey
           ): ChannelData = {
    new ChannelData(channelType, messages, privateKey)
  }

  override def buildFromJson(payload: String): ChannelData = payload.parseJson.asJsObject.convertTo[ChannelData] // doesn't decode data

  def getName: String = "ChannelData"
}
