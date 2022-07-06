package ch.epfl.pop.model.network.method.message.data.socialMedia

import ch.epfl.pop.json.MessageDataProtocol._
import ch.epfl.pop.model.network.Parsable
import ch.epfl.pop.model.network.method.message.data.ActionType.ActionType
import ch.epfl.pop.model.network.method.message.data.ObjectType.ObjectType
import ch.epfl.pop.model.network.method.message.data.{ActionType, MessageData, ObjectType}
import ch.epfl.pop.model.objects.{Hash, Timestamp}
import spray.json._

final case class AddReaction(
    reaction_codepoint: String,
    chirp_id: Hash,
    timestamp: Timestamp
) extends MessageData {
  override val _object: ObjectType = ObjectType.REACTION
  override val action: ActionType = ActionType.ADD
}

object AddReaction extends Parsable {
  def apply(
      reaction_codepoint: String,
      chirp_id: Hash,
      timestamp: Timestamp
  ): AddReaction = new AddReaction(reaction_codepoint, chirp_id, timestamp)

  override def buildFromJson(payload: String): AddReaction = payload.parseJson.asJsObject.convertTo[AddReaction]
}
