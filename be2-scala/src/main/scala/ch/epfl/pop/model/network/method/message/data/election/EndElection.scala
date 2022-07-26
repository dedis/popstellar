package ch.epfl.pop.model.network.method.message.data.election

import ch.epfl.pop.json.MessageDataProtocol._
import ch.epfl.pop.model.network.Parsable
import ch.epfl.pop.model.network.method.message.data.ActionType.ActionType
import ch.epfl.pop.model.network.method.message.data.ObjectType.ObjectType
import ch.epfl.pop.model.network.method.message.data.{ActionType, MessageData, ObjectType}
import ch.epfl.pop.model.objects.{Hash, Timestamp}
import spray.json._

final case class EndElection(
    lao: Hash,
    election: Hash,
    created_at: Timestamp,
    registered_votes: Hash
) extends MessageData {
  override val _object: ObjectType = ObjectType.ELECTION
  override val action: ActionType = ActionType.END
}

object EndElection extends Parsable {
  def apply(
      lao: Hash,
      election: Hash,
      created_at: Timestamp,
      registered_votes: Hash
  ): EndElection = new EndElection(lao, election, created_at, registered_votes)

  override def buildFromJson(payload: String): EndElection = payload.parseJson.asJsObject.convertTo[EndElection]
}
