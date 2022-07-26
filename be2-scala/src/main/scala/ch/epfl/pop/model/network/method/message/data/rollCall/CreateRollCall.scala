package ch.epfl.pop.model.network.method.message.data.rollCall

import ch.epfl.pop.json.MessageDataProtocol._
import ch.epfl.pop.model.network.Parsable
import ch.epfl.pop.model.network.method.message.data.ActionType.ActionType
import ch.epfl.pop.model.network.method.message.data.ObjectType.ObjectType
import ch.epfl.pop.model.network.method.message.data.{ActionType, MessageData, ObjectType}
import ch.epfl.pop.model.objects.{Hash, Timestamp}
import spray.json._

final case class CreateRollCall(
    id: Hash,
    name: String,
    creation: Timestamp,
    proposed_start: Timestamp,
    proposed_end: Timestamp,
    location: String,
    roll_call_description: Option[String]
) extends MessageData {
  override val _object: ObjectType = ObjectType.ROLL_CALL
  override val action: ActionType = ActionType.CREATE
}

object CreateRollCall extends Parsable {
  def apply(
      id: Hash,
      name: String,
      creation: Timestamp,
      proposed_start: Timestamp,
      proposed_end: Timestamp,
      location: String,
      roll_call_description: Option[String]
  ): CreateRollCall = {
    new CreateRollCall(id, name, creation, proposed_start, proposed_end, location, roll_call_description)
  }

  override def buildFromJson(payload: String): CreateRollCall = payload.parseJson.asJsObject.convertTo[CreateRollCall]
}
