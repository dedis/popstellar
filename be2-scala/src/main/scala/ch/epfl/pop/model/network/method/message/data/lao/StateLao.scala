package ch.epfl.pop.model.network.method.message.data.lao

import ch.epfl.pop.json.MessageDataProtocol._
import ch.epfl.pop.model.network.Parsable
import ch.epfl.pop.model.network.method.message.data.{ActionType, MessageData, ObjectType}
import ch.epfl.pop.model.objects.{Hash, PublicKey, Timestamp, WitnessSignaturePair}
import spray.json._

final case class StateLao(
    id: Hash,
    name: String,
    creation: Timestamp,
    last_modified: Timestamp,
    organizer: PublicKey,
    witnesses: List[PublicKey],
    modification_id: Hash,
    modification_signatures: List[WitnessSignaturePair]
) extends MessageData {
  override val _object: ObjectType = ObjectType.lao
  override val action: ActionType = ActionType.state
}

object StateLao extends Parsable {
  def apply(
      id: Hash,
      name: String,
      creation: Timestamp,
      last_modified: Timestamp,
      organizer: PublicKey,
      witnesses: List[PublicKey],
      modification_id: Hash,
      modification_signatures: List[WitnessSignaturePair]
  ): StateLao = {
    new StateLao(id, name, creation, last_modified, organizer, witnesses, modification_id, modification_signatures)
  }

  override def buildFromJson(payload: String): StateLao = payload.parseJson.asJsObject.convertTo[StateLao]
}
