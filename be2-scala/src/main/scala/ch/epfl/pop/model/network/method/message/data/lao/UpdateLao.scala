package ch.epfl.pop.model.network.method.message.data.lao

import ch.epfl.pop.model.network.method.message.data.{ActionType, MessageData, ObjectType, Parsable}
import ch.epfl.pop.model.objects.{Hash, PublicKey, Timestamp}

case class UpdateLao(id: Hash, name: String, last_modified: Timestamp, witnesses: List[PublicKey]) {
  private final val _object = ObjectType.LAO
  private final val action = ActionType.UPDATE_PROPERTIES
}

object UpdateLao extends Parsable {
  def apply(id: Hash, name: String, last_modified: Timestamp, witnesses: List[PublicKey]): UpdateLao = {
    // FIXME add checks
    new UpdateLao(id, name, last_modified, witnesses)
  }

  override def buildFromJson(messageData: MessageData, payload: String): Any = ???
}
