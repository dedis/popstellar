package ch.epfl.pop.model.network.method.message.data.rollCall

import ch.epfl.pop.model.network.method.message.data.{ActionType, MessageData, ObjectType, Parsable}
import ch.epfl.pop.model.objects.{Hash, Timestamp}

case class OpenRollCall(
                         update_id: Hash,
                         opens: Hash,
                         start: Timestamp
                       ) {
  private final val _object = ObjectType.ROLL_CALL
  private final val action = ActionType.OPEN
}

object OpenRollCall extends Parsable {
  def apply(
             update_id: Hash,
             opens: Hash,
             start: Timestamp
           ): OpenRollCall = {
    // FIXME add checks
    new OpenRollCall(update_id, opens, start)
  }

  override def buildFromJson(messageData: MessageData, payload: String): Any = ???
}
