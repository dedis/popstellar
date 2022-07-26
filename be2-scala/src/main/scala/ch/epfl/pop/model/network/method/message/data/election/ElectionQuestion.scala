package ch.epfl.pop.model.network.method.message.data.election

import ch.epfl.pop.json.MessageDataProtocol._
import ch.epfl.pop.model.network.Parsable
import ch.epfl.pop.model.objects.Hash
import spray.json._

final case class ElectionQuestion(
    id: Hash,
    question: String,
    voting_method: String,
    ballot_options: List[String],
    write_in: Boolean
)

object ElectionQuestion extends Parsable {
  def apply(
      id: Hash,
      question: String,
      voting_method: String,
      ballot_options: List[String],
      write_in: Boolean
  ): ElectionQuestion = new ElectionQuestion(id, question, voting_method, ballot_options, write_in)

  override def buildFromJson(payload: String): ElectionQuestion = payload.parseJson.asJsObject.convertTo[ElectionQuestion]
}
