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

  def isMissingRumorsFrom(otherRumorState: RumorState): Map[PublicKey, List[Int]] = {
    this.state.flatMap { (publicKey, rumorId) =>
      otherRumorState.state.get(publicKey) match
        case Some(otherRumorId) =>
          if (otherRumorId > rumorId)
            Some(publicKey -> List.range(rumorId + 1, otherRumorId + 1))
          else None
        case None => None
    } ++ {
      otherRumorState.state.filter((pk, _) => !this.state.contains(pk)).map((pk, rumorId) => pk -> List.range(0, rumorId + 1))
    }
  }
}

object RumorState extends Parsable {

  def apply(state: Map[PublicKey, Int]): RumorState = {
    new RumorState(state)
  }

  override def buildFromJson(payload: String): RumorState = payload.parseJson.asJsObject.convertTo[RumorState]

}
