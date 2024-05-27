package ch.epfl.pop.model.network.method.message.data.federation

import ch.epfl.pop.json.MessageDataProtocol.FederationChallengeFormat
import ch.epfl.pop.model.network.Parsable
import ch.epfl.pop.model.network.method.message.data.{ActionType, MessageData, ObjectType}
import ch.epfl.pop.model.objects.{Base16Data, Timestamp}
import spray.json.*

final case class FederationChallenge(
    value: Base16Data,
    validUntil: Timestamp
) extends MessageData {
  override val _object: ObjectType = ObjectType.federation
  override val action: ActionType = ActionType.challenge
}

object FederationChallenge extends Parsable {
  def apply(value: Base16Data, validUntil: Timestamp): FederationChallenge = {
    new FederationChallenge(value, validUntil)
  }
  override def buildFromJson(payload: String): FederationChallenge = payload.parseJson.asJsObject.convertTo[FederationChallenge]
}
