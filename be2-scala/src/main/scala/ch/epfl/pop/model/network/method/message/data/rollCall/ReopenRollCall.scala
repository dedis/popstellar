package ch.epfl.pop.model.network.method.message.data.rollCall

import ch.epfl.pop.json.MessageDataProtocol._
import ch.epfl.pop.model.network.Parsable
import ch.epfl.pop.model.network.method.message.data.ActionType.ActionType
import ch.epfl.pop.model.network.method.message.data.ObjectType.ObjectType
import ch.epfl.pop.model.network.method.message.data.{ActionType, MessageData, ObjectType}
import ch.epfl.pop.model.objects.{Hash, Timestamp}
import spray.json._

final case class ReopenRollCall(
    update_id: Hash,
    opens: Hash,
    opened_at: Timestamp
) extends MessageData with IOpenRollCall {
  override val _object: ObjectType = ObjectType.ROLL_CALL
  override val action: ActionType = ActionType.REOPEN
}

object ReopenRollCall extends Parsable {
  def apply(
      update_id: Hash,
      opens: Hash,
      opened_at: Timestamp
  ): ReopenRollCall = {
    new ReopenRollCall(update_id, opens, opened_at)
  }

  override def buildFromJson(payload: String): ReopenRollCall = payload.parseJson.asJsObject.convertTo[ReopenRollCall]
}
