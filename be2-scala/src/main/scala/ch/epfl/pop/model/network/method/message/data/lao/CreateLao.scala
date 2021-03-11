package ch.epfl.pop.model.network.method.message.data.lao

import ch.epfl.pop.model.network.method.message.data.{ActionType, MessageData, ObjectType, Parsable}
import ch.epfl.pop.model.objects.{Hash, PublicKey, Timestamp}

case class CreateLao(id: Hash, name: String, creation: Timestamp, organizer: PublicKey, witnesses: List[PublicKey]) {
  private final val _object = ObjectType.LAO
  private final val action = ActionType.CREATE
}

object CreateLao extends Parsable {
  def apply(id: Hash, name: String, creation: Timestamp, organizer: PublicKey, witnesses: List[PublicKey]): CreateLao = {
    // FIXME add checks
    new CreateLao(id, name, creation, organizer, witnesses)
  }

  override def buildFromJson(messageData: MessageData, payload: String): Any = ???
}
