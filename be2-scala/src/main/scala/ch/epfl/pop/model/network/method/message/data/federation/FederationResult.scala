package ch.epfl.pop.model.network.method.message.data.federation

import ch.epfl.pop.json.MessageDataProtocol.FederationResultFormat
import ch.epfl.pop.model.network.Parsable
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.network.method.message.data.{ActionType, MessageData, ObjectType}
import ch.epfl.pop.model.objects.PublicKey
import spray.json.*

final case class FederationResult(
    status: String,
    reason: Option[String],
    organizer: Option[PublicKey],
    challenge_message: Message
) extends MessageData {
  require(status.matches("^success$") || status.matches("^failure$"), s"This is an invalid status")
  def this(status: String, organizer: PublicKey, challenge_message: Message) = this(status, None, Some(organizer), challenge_message)
  def this(status: String, reason: String, challenge_message: Message) = this(status, Some(reason), None, challenge_message)
  override val _object: ObjectType = ObjectType.federation
  override val action: ActionType = ActionType.federation_result
}

object FederationResult extends Parsable {
  def apply(status: String, reason: Option[String], organizer: Option[PublicKey], challenge_message: Message): FederationResult = {
    new FederationResult(status, reason, organizer, challenge_message)
  }
  def apply(status: String, organizer: PublicKey, challenge_message: Message): FederationResult = new FederationResult(status, None, Some(organizer), challenge_message)
  def apply(status: String, reason: String, challenge_message: Message): FederationResult = new FederationResult(status, Some(reason), None, challenge_message)

  override def buildFromJson(payload: String): FederationResult = payload.parseJson.asJsObject.convertTo[FederationResult]
}
