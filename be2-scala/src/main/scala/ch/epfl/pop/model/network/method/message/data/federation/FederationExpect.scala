package ch.epfl.pop.model.network.method.message.data.federation

import ch.epfl.pop.json.MessageDataProtocol.FederationExpectFormat
import ch.epfl.pop.model.network.Parsable
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.network.method.message.data.{ActionType, MessageData, ObjectType}
import ch.epfl.pop.model.objects.{Hash, PublicKey}
import spray.json.*

final case class FederationExpect(
    laoId: Hash,
    serverAddress: String,
    publicKey: PublicKey,
    challenge: Message
) extends MessageData {
  override val _object: ObjectType = ObjectType.federation
  override val action: ActionType = ActionType.expect
}

object FederationExpect extends Parsable {
  def apply(laoId: Hash, serverAddress: String, publicKey: PublicKey, challenge: Message): FederationExpect = {
    new FederationExpect(laoId, serverAddress, publicKey, challenge)
  }

  override def buildFromJson(payload: String): FederationExpect = payload.parseJson.asJsObject.convertTo[FederationExpect]
}
