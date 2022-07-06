package ch.epfl.pop.model.network.method.message.data.socialMedia

import ch.epfl.pop.json.MessageDataProtocol._
import ch.epfl.pop.model.network.Parsable
import ch.epfl.pop.model.network.method.message.data.ActionType.ActionType
import ch.epfl.pop.model.network.method.message.data.ObjectType.ObjectType
import ch.epfl.pop.model.network.method.message.data.{ActionType, MessageData, ObjectType}
import ch.epfl.pop.model.objects.{Hash, Timestamp}
import spray.json._

final case class DeleteChirp(
    chirp_id: Hash,
    timestamp: Timestamp
) extends MessageData {
  override val _object: ObjectType = ObjectType.CHIRP
  override val action: ActionType = ActionType.DELETE
}

object DeleteChirp extends Parsable {
  def apply(
      chirp_id: Hash,
      timestamp: Timestamp
  ): DeleteChirp = new DeleteChirp(chirp_id, timestamp)

  override def buildFromJson(payload: String): DeleteChirp = payload.parseJson.asJsObject.convertTo[DeleteChirp]
}
