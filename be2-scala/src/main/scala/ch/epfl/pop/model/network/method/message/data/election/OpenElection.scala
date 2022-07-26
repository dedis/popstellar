package ch.epfl.pop.model.network.method.message.data.election

import ch.epfl.pop.json.MessageDataProtocol._
import ch.epfl.pop.model.network.Parsable
import ch.epfl.pop.model.network.method.message.data.ActionType.ActionType
import ch.epfl.pop.model.network.method.message.data.ObjectType.ObjectType
import ch.epfl.pop.model.network.method.message.data.{ActionType, MessageData, ObjectType}
import ch.epfl.pop.model.objects.{Hash, Timestamp}
import spray.json._

final case class OpenElection(
    lao: Hash,
    election: Hash,
    opened_at: Timestamp
) extends MessageData {
  override val _object: ObjectType = ObjectType.ELECTION
  override val action: ActionType = ActionType.OPEN
}

object OpenElection extends Parsable {
  def apply(
      lao: Hash,
      election: Hash,
      opened_at: Timestamp
  ): OpenElection = new OpenElection(lao, election, opened_at)

  override def buildFromJson(payload: String): OpenElection = payload.parseJson.asJsObject.convertTo[OpenElection]
}
