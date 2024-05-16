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
    publicKey: Option[PublicKey],
    challenge: Message
) extends MessageData {
  def this(status: String, publicKey: PublicKey, challenge: Message) = this(status, None, Some(publicKey), challenge)
  def this(status: String, reason: String, challenge: Message) = this(status, Some(reason), None, challenge)
  override val _object: ObjectType = ObjectType.federation
  override val action: ActionType = ActionType.result
}

object FederationResult extends Parsable {
  def apply(status: String, reason: Option[String], public_key: Option[PublicKey], challenge: Message): FederationResult = {
    new FederationResult(status, reason, public_key, challenge)
  }
  def apply(status: String, public_key: PublicKey, challenge: Message): FederationResult = new FederationResult(status, None, Some(public_key), challenge)
  def apply(status: String, reason: String, challenge: Message): FederationResult = new FederationResult(status, Some(reason), None, challenge)

  override def buildFromJson(payload: String): FederationResult = payload.parseJson.asJsObject.convertTo[FederationResult]
}
