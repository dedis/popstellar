package ch.epfl.pop.model.objects

import ch.epfl.pop.json.HighLevelProtocol._
import ch.epfl.pop.model.network.Parsable
//import ch.epfl.pop.model.network.method.message.data.MessageData
import ch.epfl.pop.model.objects._
import spray.json._


//the general ObjectType will be LAO (for all but election and chirp channels for now)
case class ChannelData(
    channelType: ObjectType,
    messages: List[Hash]
){
    def toJsonString: String = {
        val that: LaoData = this // tricks the compiler into inferring the right type
        that.toJson.toString
    }

    def addMessage(message: Hash): ChannelData = {
        ChannelData(channelType, message :: messages)
    }
    
}

object ChannelData extends Parsable {
  def apply(
             channelType: ObjectType,
             messages: List[Hash]
           ): ChannelData = {
    new ChannelData(channelType, messages)
  }

  override def buildFromJson(payload: String): ChannelData = payload.parseJson.asJsObject.convertTo[ChannelData] // doesn't decode data
}
