package ch.epfl.pop.model.network.method.message.data.lao

import ch.epfl.pop.json.MessageDataProtocol._
import ch.epfl.pop.model.network.Parsable
import ch.epfl.pop.model.network.method.message.data.ActionType.ActionType
import ch.epfl.pop.model.network.method.message.data.ObjectType.ObjectType
import ch.epfl.pop.model.network.method.message.data.{ActionType, MessageData, ObjectType}
import ch.epfl.pop.model.objects.{Hash, PublicKey, Timestamp}
import spray.json._

final case class UpdateLao(id: Hash, name: String, last_modified: Timestamp, witnesses: List[PublicKey]) extends MessageData {
  override val _object: ObjectType = ObjectType.LAO
  override val action: ActionType = ActionType.UPDATE_PROPERTIES
}

object UpdateLao extends Parsable {
  def apply(id: Hash, name: String, last_modified: Timestamp, witnesses: List[PublicKey]): UpdateLao = {
    new UpdateLao(id, name, last_modified, witnesses)
  }

  override def buildFromJson(payload: String): UpdateLao = payload.parseJson.asJsObject.convertTo[UpdateLao]
}
