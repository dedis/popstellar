package ch.epfl.pop.model.network.method.message.data.lao

import ch.epfl.pop.model.network.{JsonRpcRequest, Parsable}
import ch.epfl.pop.model.network.method.message.data.ActionType.ActionType
import ch.epfl.pop.model.network.method.message.data.ObjectType.ObjectType
import ch.epfl.pop.model.network.method.message.data.{ActionType, MessageData, ObjectType}
import ch.epfl.pop.model.objects.{Hash, PublicKey, Timestamp}

import spray.json._
import ch.epfl.pop.jsonNew.MessageDataProtocol._

case class CreateLao(
                      id: Hash,
                      name: String,
                      creation: Timestamp,
                      organizer: PublicKey,
                      witnesses: List[PublicKey]
                    ) extends MessageData {
  override val _object: ObjectType = ObjectType.LAO
  override val action: ActionType = ActionType.CREATE
}

object CreateLao extends Parsable {
  def apply(id: Hash, name: String, creation: Timestamp, organizer: PublicKey, witnesses: List[PublicKey]): CreateLao = {
    new CreateLao(id, name, creation, organizer, witnesses)
  }

  override def buildFromJson(payload: String): CreateLao = payload.parseJson.asJsObject.convertTo[CreateLao]

  def buildFromPartial(messageData: MessageData, payload: JsonRpcRequest): CreateLao = ???
}
