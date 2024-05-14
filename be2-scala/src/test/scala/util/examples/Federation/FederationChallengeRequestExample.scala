package util.examples.Federation

import ch.epfl.pop.json.MessageDataProtocol.*
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.network.method.message.data.federation.FederationChallengeRequest
import ch.epfl.pop.model.objects.{Base64Data, Hash, PublicKey, Signature, Timestamp}
import spray.json.*

object FederationChallengeRequestExample {
  final val TIMESTAMP: Timestamp = Timestamp(356383235)
  final val CHALLENGE_REQUEST: FederationChallengeRequest = FederationChallengeRequest(TIMESTAMP)

  final val SENDER: PublicKey = PublicKey(Base64Data("zXgzQaa_NpUe-v0Zk_4q8k184ohQ5nTQhBDKgncHzq4="))
  final val SIGNATURE: Signature = Signature(Base64Data("BILYwYkT5tOBL4rCD7yvhBkhAYqRXOI3ajQ2uJ1gAk-g6nRc38vMMnlHShuNCQ3dQFXYZPn37cCFelhWGjY8Bg=="))
  final val MESSAGE_ID: Hash = Hash(Base64Data("sD_PdryBuOr14_65h8L-e1lzdQpDWxUAngtu1uwqgEI="))

  final val DATA_CHALLENGE_REQUEST_MESSAGE: Hash = Hash(Base64Data.encode(CHALLENGE_REQUEST.toJson.toString))
  final val CHALLENGE_REQUEST_MESSAGE: Message = Message(
    DATA_CHALLENGE_REQUEST_MESSAGE.base64Data,
    SENDER,
    SIGNATURE,
    MESSAGE_ID,
    List.empty,
    Some(CHALLENGE_REQUEST)
  )
}
