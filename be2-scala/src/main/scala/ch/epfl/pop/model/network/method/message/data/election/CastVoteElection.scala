package ch.epfl.pop.model.network.method.message.data.election

import ch.epfl.pop.json.MessageDataProtocol._
import ch.epfl.pop.model.network.Parsable
import ch.epfl.pop.model.network.method.message.data.ActionType.ActionType
import ch.epfl.pop.model.network.method.message.data.ObjectType.ObjectType
import ch.epfl.pop.model.network.method.message.data.{ActionType, MessageData, ObjectType}
import ch.epfl.pop.model.objects.{Hash, Timestamp}
import spray.json._

final case class CastVoteElection(
    lao: Hash,
    election: Hash,
    created_at: Timestamp,
    votes: List[VoteElection]
) extends MessageData {
  override val _object: ObjectType = ObjectType.ELECTION
  override val action: ActionType = ActionType.CAST_VOTE
}

object CastVoteElection extends Parsable {
  def apply(
      lao: Hash,
      election: Hash,
      created_at: Timestamp,
      votes: List[VoteElection]
  ): CastVoteElection = new CastVoteElection(lao, election, created_at, votes)

  override def buildFromJson(payload: String): CastVoteElection = payload.parseJson.asJsObject.convertTo[CastVoteElection]
}
