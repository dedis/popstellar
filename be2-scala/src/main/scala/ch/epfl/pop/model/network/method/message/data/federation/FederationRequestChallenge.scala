package ch.epfl.pop.model.network.method.message.data.federation

import ch.epfl.pop.json.MessageDataProtocol.FederationRequestChallengeFormat
import ch.epfl.pop.model.network.Parsable
import ch.epfl.pop.model.network.method.message.data.{ActionType, MessageData, ObjectType}
import ch.epfl.pop.model.objects.Timestamp
import spray.json.*

final case class FederationRequestChallenge(
    timestamp: Timestamp
) extends MessageData {
  override val _object: ObjectType = ObjectType.federation
  override val action: ActionType = ActionType.challenge_request
}

object FederationRequestChallenge extends Parsable {
  def apply(timestamp: Timestamp): FederationRequestChallenge = {
    new FederationRequestChallenge(timestamp)
  }

  override def buildFromJson(payload: String): FederationRequestChallenge = payload.parseJson.asJsObject.convertTo[FederationRequestChallenge]
}
