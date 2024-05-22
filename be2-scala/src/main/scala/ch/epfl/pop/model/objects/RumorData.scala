package ch.epfl.pop.model.objects

import ch.epfl.pop.model.network.Parsable
import ch.epfl.pop.json.ObjectProtocol.*
import ch.epfl.pop.model.network.method.Rumor
import spray.json.*

final case class RumorData(
    rumorIds: List[Int]
) {
  def toJsonString: String = {
    this.toJson.toString
  }

  def updateWith(rumorId: Int): RumorData = {
    new RumorData((rumorId :: rumorIds).sorted)
  }

  def lastRumorId(): Int = {
    if (rumorIds.isEmpty) {
      -1
    } else {
      rumorIds.max
    }
  }

}

object RumorData extends Parsable {
  def apply(
      rumorIds: List[Int]
  ): RumorData = {
    new RumorData(rumorIds)
  }

  override def buildFromJson(payload: String): RumorData = payload.parseJson.asJsObject.convertTo[RumorData]
}
