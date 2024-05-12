package util.examples.Federation

import ch.epfl.pop.json.MessageDataProtocol.*
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.network.method.message.data.federation.FederationRequestChallenge
import ch.epfl.pop.model.objects.{Base64Data, Hash, PublicKey, Signature, Timestamp}
import spray.json.*

object FederationRequestChallengeExample {
  final val TIMESTAMP: Timestamp = Timestamp(356383235)
  final val REQUEST_CHALLENGE: FederationRequestChallenge = FederationRequestChallenge(TIMESTAMP)

  final val SENDER: PublicKey = PublicKey(Base64Data("zXgzQaa_NpUe-v0Zk_4q8k184ohQ5nTQhBDKgncHzq4="))
  final val SIGNATURE: Signature = Signature(Base64Data("BILYwYkT5tOBL4rCD7yvhBkhAYqRXOI3ajQ2uJ1gAk-g6nRc38vMMnlHShuNCQ3dQFXYZPn37cCFelhWGjY8Bg=="))
  final val MESSAGE_ID: Hash = Hash(Base64Data("sD_PdryBuOr14_65h8L-e1lzdQpDWxUAngtu1uwqgEI="))

  final val DATA_REQUEST_CHALLENGE_MESSAGE: Hash = Hash(Base64Data.encode(REQUEST_CHALLENGE.toJson.toString))
  final val REQUEST_CHALLENGE_MESSAGE: Message = Message(
    DATA_REQUEST_CHALLENGE_MESSAGE.base64Data,
    SENDER,
    SIGNATURE,
    MESSAGE_ID,
    List.empty,
    Some(REQUEST_CHALLENGE)
  )
}
