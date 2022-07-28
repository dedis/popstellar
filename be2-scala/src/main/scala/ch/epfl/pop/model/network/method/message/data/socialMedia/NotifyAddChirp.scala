package ch.epfl.pop.model.network.method.message.data.socialMedia

import ch.epfl.pop.json.MessageDataProtocol._
import ch.epfl.pop.model.network.Parsable
import ch.epfl.pop.model.network.method.message.data.ActionType.ActionType
import ch.epfl.pop.model.network.method.message.data.ObjectType.ObjectType
import ch.epfl.pop.model.network.method.message.data.{ActionType, MessageData, ObjectType}
import ch.epfl.pop.model.objects.{Channel, Hash, Timestamp}
import spray.json._

final case class NotifyAddChirp(
    chirp_id: Hash,
    channel: Channel,
    timestamp: Timestamp
) extends MessageData {
  override val _object: ObjectType = ObjectType.CHIRP
  override val action: ActionType = ActionType.NOTIFY_ADD
}

object NotifyAddChirp extends Parsable {
  def apply(
      chirp_id: Hash,
      channel: Channel,
      timestamp: Timestamp
  ): NotifyAddChirp = new NotifyAddChirp(chirp_id, channel, timestamp)

  override def buildFromJson(payload: String): NotifyAddChirp = payload.parseJson.asJsObject.convertTo[NotifyAddChirp]
}
