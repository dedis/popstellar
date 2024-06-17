package ch.epfl.pop.model.network.method.message.data.federation

import ch.epfl.pop.json.MessageDataProtocol.*
import ch.epfl.pop.model.network.Parsable
import ch.epfl.pop.model.network.method.message.data.{ActionType, MessageData, ObjectType}
import ch.epfl.pop.model.objects.{Hash, PublicKey, Timestamp}
import spray.json.*

final case class FederationTokensExchange(
    laoId: Hash,
    rollCallId: Hash,
    tokens: List[PublicKey],
    timestamp: Timestamp
) extends MessageData {

  override val _object: ObjectType = ObjectType.federation
  override val action: ActionType = ActionType.tokens_exchange

}

object FederationTokensExchange extends Parsable {
  def apply(laoId: Hash, rollCallId: Hash, tokens: List[PublicKey], timestamp: Timestamp): FederationTokensExchange = {
    new FederationTokensExchange(laoId, rollCallId, tokens, timestamp)
  }

  override def buildFromJson(payload: String): FederationTokensExchange = payload.parseJson.asJsObject.convertTo[FederationTokensExchange]
}
