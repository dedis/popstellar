package ch.epfl.pop.model.network.method.message.data.election

import ch.epfl.pop.json.MessageDataProtocol._
import ch.epfl.pop.model.network.Parsable
import ch.epfl.pop.model.objects.Hash
import spray.json._

final case class ElectionQuestionResult(
    id: Hash,
    result: List[ElectionBallotVotes]
)

object ElectionQuestionResult extends Parsable {
  def apply(
      id: Hash,
      result: List[ElectionBallotVotes]
  ): ElectionQuestionResult = new ElectionQuestionResult(id, result)

  override def buildFromJson(payload: String): ElectionQuestionResult = payload.parseJson.asJsObject.convertTo[ElectionQuestionResult]
}
