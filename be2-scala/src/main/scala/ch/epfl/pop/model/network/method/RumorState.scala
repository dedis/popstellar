package ch.epfl.pop.model.network.method

import ch.epfl.pop.model.network.Parsable
import ch.epfl.pop.json.HighLevelProtocol.RumorStateFormat
import ch.epfl.pop.model.objects.PublicKey
import spray.json.*

final case class RumorState(state: Map[PublicKey, Int]) extends Params {

  override def hasChannel: Boolean = false

  override def hasMessage: Boolean = false

  def toJsonString: String = {
    this.toJson.toString
  }
}

object RumorState extends Parsable {

  def apply(state: Map[PublicKey, Int]): RumorState = {
    new RumorState(state)
  }

  override def buildFromJson(payload: String): RumorState = payload.parseJson.asJsObject.convertTo[RumorState]

}
