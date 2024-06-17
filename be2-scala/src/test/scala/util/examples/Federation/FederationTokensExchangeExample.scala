package util.examples.Federation

import ch.epfl.pop.model.network.method.message.data.federation.FederationTokensExchange
import ch.epfl.pop.model.objects.{Base64Data, Hash, PublicKey, Signature, Timestamp}
import ch.epfl.pop.pubsub.graph.validators.RollCallValidator.EVENT_HASH_PREFIX
import util.examples.RollCall.CreateRollCallExamples.{NAME, NOT_STALE_CREATION}
import spray.json.*
import ch.epfl.pop.json.MessageDataProtocol.*
import ch.epfl.pop.model.network.method.message.Message

object FederationTokensExchangeExample {

  final val SENDER: PublicKey = PublicKey(Base64Data("VHfxTlbM3nTnLQuKnKfs1fGP2cwVT8KJkc-sRGs_2KM="))
  final val SIGNATURE: Signature = Signature(Base64Data("BILYwYkT5tOBL4rCD7yvhBkhAYqRXOI3ajQ2uJ1gAk-g6nRc38vMMnlHShuNCQ3dQFXYZPn37cCFelhWGjY8Bg=="))
  final val MESSAGE_ID: Hash = Hash(Base64Data("sD_PdryBuOr14_65h8L-e1lzdQpDWxUAngtu1uwqgEI="))

  final val LAO_ID: Hash = Hash(Base64Data("lsWUv1bKBQ0t1DqWZTFwb0nhLsP_EtfGoXHny4hsrwA="))
  final val ROLL_CALL_ID: Hash = Hash.fromStrings(EVENT_HASH_PREFIX, LAO_ID.toString, NOT_STALE_CREATION.toString, NAME)
  final val ATTENDEE_1: PublicKey = PublicKey(Base64Data("zXgzQaa_NpUe-v0Zk_4q8k184ohQ5nTQhBDKgncHzq4="))
  final val ATTENDEE_2: PublicKey = PublicKey(Base64Data("VHfxTlbM3nTnLQuKnKfs1fGP2cwVT8KJkc-sRGs_2KM="))
  final val ATTENDEE_3: PublicKey = PublicKey(Base64Data("gUSKTlXcSHfQmHbKYsa0obpotjoc-wwtkeKods9WBcY="))
  final val TOKENS: List[PublicKey] = List(ATTENDEE_1, ATTENDEE_2, ATTENDEE_3)
  final val TIMESTAMP: Timestamp = Timestamp(1712854874)

  final val TOKENS_EXCHANGE: FederationTokensExchange = FederationTokensExchange(LAO_ID, ROLL_CALL_ID, TOKENS, TIMESTAMP)
  final val DATA_TOKENS_EXCHANGE_MESSAGE: Hash = Hash(Base64Data.encode(TOKENS_EXCHANGE.toJson.toString))

  final val TOKENS_EXCHANGE_MESSAGE: Message = Message(
    DATA_TOKENS_EXCHANGE_MESSAGE.base64Data,
    SENDER,
    SIGNATURE,
    MESSAGE_ID,
    List.empty,
    Some(TOKENS_EXCHANGE)
  )

}
