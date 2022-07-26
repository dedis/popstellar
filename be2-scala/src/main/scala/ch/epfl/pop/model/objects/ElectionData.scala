package ch.epfl.pop.model.objects

import ch.epfl.pop.json.MessageDataProtocol._
import ch.epfl.pop.model.network.Parsable
import spray.json._

final case class ElectionData(electionId: Hash, keyPair: KeyPair) {
  def toJsonString: String = {
    val that: ElectionData = this // tricks the compiler into inferring the right type
    that.toJson.toString
  }
}

object ElectionData extends Parsable {
  def apply(
      id: Hash
  ): ElectionData =
    ElectionData(id, KeyPair())

  override def buildFromJson(payload: String): ElectionData = payload.parseJson.asJsObject.convertTo[ElectionData]

  def getName: String = "electionData"
}
