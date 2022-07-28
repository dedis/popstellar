package ch.epfl.pop.model.network.method.message.data.socialMedia

import ch.epfl.pop.json.MessageDataProtocol._
import ch.epfl.pop.model.network.Parsable
import ch.epfl.pop.model.network.method.message.data.ActionType.ActionType
import ch.epfl.pop.model.network.method.message.data.ObjectType.ObjectType
import ch.epfl.pop.model.network.method.message.data.{ActionType, MessageData, ObjectType}
import ch.epfl.pop.model.objects.Timestamp
import spray.json._

final case class AddChirp(
    text: String,
    parent_id: Option[String],
    timestamp: Timestamp
) extends MessageData {
  override val _object: ObjectType = ObjectType.CHIRP
  override val action: ActionType = ActionType.ADD
}

object AddChirp extends Parsable {
  def apply(
      text: String,
      parent_id: Option[String],
      timestamp: Timestamp
  ): AddChirp = new AddChirp(text, parent_id, timestamp)

  override def buildFromJson(payload: String): AddChirp = payload.parseJson.asJsObject.convertTo[AddChirp]
}
