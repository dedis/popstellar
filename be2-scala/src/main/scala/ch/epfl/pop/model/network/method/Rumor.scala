package ch.epfl.pop.model.network.method

import ch.epfl.pop.json.HighLevelProtocol.RumorFormat
import ch.epfl.pop.model.network.Parsable
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.objects.{Channel, PublicKey}
import spray.json._

final case class Rumor(senderPk: PublicKey, rumorId: Int, messages: Map[Channel, Array[Message]]) extends Params {

  override def hasChannel: Boolean = true

  override def hasMessage: Boolean = true
  
  def toJsonString: String = {
    this.toJson.toString
  }

}

object Rumor extends Parsable {

  def apply(senderPk: PublicKey, rumorId: Int, messages: Map[Channel, Array[Message]]): Rumor = {
    new Rumor(senderPk, rumorId, messages)
  }

  override def buildFromJson(payload: String): Rumor = payload.parseJson.asJsObject.convertTo[Rumor]

}
