package ch.epfl.pop.model.network.method

import ch.epfl.pop.model.network.Parsable
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.objects.{Channel, PublicKey}
import spray.json.*

final case class RumorState(state: Map[PublicKey, Integer]) extends Params {

  override def hasChannel: Boolean = false

  override def hasMessage: Boolean = false

  def toJsonString: String = {
    "" // this.toJson.toString
  }
}

object RumorState extends Parsable {

  def apply(state: Map[PublicKey, Integer]): RumorState = {
    new RumorState(state)
  }

  override def buildFromJson(payload: String): RumorState = RumorState(Map.empty) // payload.parseJson.asJsObject.convertTo[RumorState]

}
