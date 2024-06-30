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
  final val WRONG_SENDER: PublicKey = PublicKey(Base64Data.encode("wrongSender"))

  final val SIGNATURE: Signature = Signature(Base64Data("BILYwYkT5tOBL4rCD7yvhBkhAYqRXOI3ajQ2uJ1gAk-g6nRc38vMMnlHShuNCQ3dQFXYZPn37cCFelhWGjY8Bg=="))
  final val MESSAGE_ID: Hash = Hash(Base64Data("sD_PdryBuOr14_65h8L-e1lzdQpDWxUAngtu1uwqgEI="))

  final val LAO_ID: Hash = Hash(Base64Data("fzJSZjKf-2cbXH7kds9H8NORuuFIRLkevJlN7qQemjo="))
  final val ROLL_CALL_ID: Hash = Hash(Base64Data("fEvAfdtNrykd9NPYl9ReHLX-6IP6SFLKTZJLeGUHZ_U="))
  final val ATTENDEE_1: PublicKey = PublicKey(Base64Data("M5ZychEi5rwm22FjwjNuljL1qMJWD2sE7oX9fcHNMDU="))
  final val TOKENS: List[PublicKey] = List(ATTENDEE_1)
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

  final val TOKENS_EXCHANGE_WRONG_SENDER_MESSAGE: Message = Message(
    DATA_TOKENS_EXCHANGE_MESSAGE.base64Data,
    WRONG_SENDER,
    SIGNATURE,
    MESSAGE_ID,
    List.empty,
    Some(TOKENS_EXCHANGE)
  )

}
