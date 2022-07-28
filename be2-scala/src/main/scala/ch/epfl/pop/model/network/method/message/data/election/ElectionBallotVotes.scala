package ch.epfl.pop.model.network.method.message.data.election

import ch.epfl.pop.json.MessageDataProtocol._
import ch.epfl.pop.model.network.Parsable
import spray.json._

final case class ElectionBallotVotes(
    ballot_option: String,
    count: Int
)

object ElectionBallotVotes extends Parsable {
  def apply(
      ballot_option: String,
      count: Int
  ): ElectionBallotVotes = new ElectionBallotVotes(ballot_option, count)

  override def buildFromJson(payload: String): ElectionBallotVotes = payload.parseJson.asJsObject.convertTo[ElectionBallotVotes]
}
