package ch.epfl.pop.model.network.method.message.data.rollCall

import ch.epfl.pop.model.network.method.message.data.ActionType.ActionType
import ch.epfl.pop.model.network.method.message.data.ObjectType.ObjectType
import ch.epfl.pop.model.network.method.message.data.{ActionType, MessageData, ObjectType, Parsable}
import ch.epfl.pop.model.objects.{Hash, Timestamp}

case class ReopenRollCall(
                           update_id: Hash,
                           opens: Hash,
                           start: Timestamp
                         ) extends MessageData {
  override val _object: ObjectType = ObjectType.ROLL_CALL
  override val action: ActionType = ActionType.REOPEN
}

object ReopenRollCall extends Parsable {
  def apply(
             update_id: Hash,
             opens: Hash,
             start: Timestamp
           ): ReopenRollCall = {
    // FIXME add checks
    new ReopenRollCall(update_id, opens, start)
  }

  override def buildFromJson(messageData: MessageData, payload: String): ReopenRollCall = ???
}

