package ch.epfl.pop.model.network.method.message.data.federation

import ch.epfl.pop.json.MessageDataProtocol.FederationChallengeRequestFormat
import ch.epfl.pop.model.network.Parsable
import ch.epfl.pop.model.network.method.message.data.{ActionType, MessageData, ObjectType}
import ch.epfl.pop.model.objects.Timestamp
import spray.json.*

final case class FederationChallengeRequest(
    timestamp: Timestamp
) extends MessageData {
  override val _object: ObjectType = ObjectType.federation
  override val action: ActionType = ActionType.challenge_request
}

object FederationChallengeRequest extends Parsable {
  def apply(timestamp: Timestamp): FederationChallengeRequest = {
    new FederationChallengeRequest(timestamp)
  }

  override def buildFromJson(payload: String): FederationChallengeRequest = payload.parseJson.asJsObject.convertTo[FederationChallengeRequest]
}
