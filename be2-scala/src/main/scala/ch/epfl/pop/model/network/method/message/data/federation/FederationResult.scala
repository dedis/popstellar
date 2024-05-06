package ch.epfl.pop.model.network.method.message.data.federation

import ch.epfl.pop.json.MessageDataProtocol.FederationResultFormat
import ch.epfl.pop.model.network.Parsable
import ch.epfl.pop.model.network.method.message.data.{ActionType, MessageData, ObjectType}
import ch.epfl.pop.model.objects.PublicKey
import spray.json.*

final case class FederationResult(
     status : String, //it should matches the pattern
     reason : String,
     organizer : PublicKey,
     challenge_message : FederationChallenge // to change 
) extends MessageData {
  override val _object: ObjectType = ObjectType.federation
  override val action: ActionType = ActionType.result
}

object FederationResult extends Parsable {
  def apply(status: String, reason: String, organizer: PublicKey, challenge_message : ): FederationResult = {
    new FederationResult(status, reason, organizer, challenge_message)
  }

  override def buildFromJson(payload: String): FederationResult = payload.parseJson.asJsObject.convertTo[FederationResult]
}
