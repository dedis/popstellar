package ch.epfl.pop.model.network.method.message.data.dataObject

import ch.epfl.pop.json.HighLevelProtocol._
import ch.epfl.pop.json.MessageDataProtocol._
import ch.epfl.pop.model.network.Parsable
//import ch.epfl.pop.model.network.method.message.data.MessageData
import ch.epfl.pop.model.objects._
import spray.json._

case class LaoData(
    owner: PublicKey,
    attendees: List[PublicKey]
){
    def toJsonString: String = {
      val that: LaoData = this // tricks the compiler into inferring the right type
      that.toJson.toString
    }
}

object LaoData extends Parsable {
  def apply(
             owner: PublicKey,
             attendees: List[PublicKey]
           ): LaoData = {
    new LaoData(owner, attendees)
  }

  override def buildFromJson(payload: String): LaoData = payload.parseJson.asJsObject.convertTo[LaoData] // doesn't decode data
}