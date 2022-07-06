package ch.epfl.pop.model.objects

import ch.epfl.pop.json.MessageDataProtocol._
import ch.epfl.pop.model.network.Parsable
import ch.epfl.pop.model.network.method.message.data.ObjectType
import spray.json._

//the general ObjectType will be LAO (for all but election and chirp channels for now)
final case class ChannelData(
    channelType: ObjectType.ObjectType,
    messages: List[Hash]
) {
  def toJsonString: String = {
    val that: ChannelData = this // tricks the compiler into inferring the right type
    that.toJson.toString
  }

  def addMessage(messageId: Hash): ChannelData = {
    ChannelData(channelType, messageId :: messages)
  }

}

object ChannelData extends Parsable {
  def apply(
      channelType: ObjectType.ObjectType,
      messages: List[Hash]
  ): ChannelData = {
    new ChannelData(channelType, messages)
  }

  override def buildFromJson(payload: String): ChannelData = payload.parseJson.asJsObject.convertTo[ChannelData] // doesn't decode data

  def getName: String = "ChannelData"
}
