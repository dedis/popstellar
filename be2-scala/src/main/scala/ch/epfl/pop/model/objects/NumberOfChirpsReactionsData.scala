package ch.epfl.pop.model.objects

import ch.epfl.pop.json.MessageDataProtocol.NumberOfChirpsReactionsDataFormat
import ch.epfl.pop.model.network.Parsable
import spray.json.*

final case class NumberOfChirpsReactionsData(numberOfChirpsReactions: Int) {
  def toJsonString: String = {
    val that: NumberOfChirpsReactionsData = this // tricks the compiler into inferring the right type
    that.toJson.toString
  }
}

object NumberOfChirpsReactionsData extends Parsable {
  def apply(numberOfChirpsReactions: Int): NumberOfChirpsReactionsData =
    new NumberOfChirpsReactionsData(numberOfChirpsReactions)

  override def buildFromJson(payload: String): NumberOfChirpsReactionsData = payload.parseJson.asJsObject.convertTo[NumberOfChirpsReactionsData]

  def getName: String = "numberOfChirpsReactionsData"
}
