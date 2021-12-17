package ch.epfl.pop.model.network.method.message.data.socialMedia

import ch.epfl.pop.json.MessageDataProtocol._
import ch.epfl.pop.model.network.Parsable
import ch.epfl.pop.model.network.method.message.data.ActionType.ActionType
import ch.epfl.pop.model.network.method.message.data.ObjectType.ObjectType
import ch.epfl.pop.model.network.method.message.data.{ActionType, MessageData, ObjectType}
import ch.epfl.pop.model.objects.{Channel, Hash, Timestamp}
import spray.json._

case class DeleteBroadcastChirp(
                          chirp_id: Hash,
                          channel: Channel,
                          timestamp: Timestamp
                        ) extends MessageData {
  override val _object: ObjectType = ObjectType.CHIRP
  override val action: ActionType = ActionType.DELETE_BROADCAST
}

object DeleteBroadcastChirp extends Parsable {
  def apply(
            chirp_id: Hash,
            channel: Channel,
            timestamp: Timestamp
      ): DeleteBroadcastChirp = new DeleteBroadcastChirp(chirp_id, channel, timestamp)

  override def buildFromJson(payload: String): DeleteBroadcastChirp = payload.parseJson.asJsObject.convertTo[DeleteBroadcastChirp]
}
