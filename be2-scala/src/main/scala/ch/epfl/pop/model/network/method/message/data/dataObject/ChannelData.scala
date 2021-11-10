package ch.epfl.pop.model.network.method.message.data.dataObject

import ch.epfl.pop.json.HighLevelProtocol._
import ch.epfl.pop.json.MessageDataProtocol._
import ch.epfl.pop.model.network.Parsable
import ch.epfl.pop.model.network.method.message.data.{ObjectType}
import ch.epfl.pop.model.objects._
import spray.json._


//the general ObjectType will be LAO (for all but election and chirp channels for now)
case class ChannelData(
    channel_type: ObjectType.ObjectType,
    messages: List[Hash]
){
    def toJsonString: String = {
        val that: ChannelData = this // tricks the compiler into inferring the right type
        that.toJson.toString
    }

    def addMessage(message: Hash): ChannelData = {
        ChannelData(channel_type, message :: messages)
    }
    
}

object ChannelData extends Parsable {
  def apply(
             channel_type: ObjectType.ObjectType,
             messages: List[Hash]
           ): ChannelData = {
    new ChannelData(channel_type, messages)
  }

  override def buildFromJson(payload: String): ChannelData = payload.parseJson.asJsObject.convertTo[ChannelData] // doesn't decode data
}
