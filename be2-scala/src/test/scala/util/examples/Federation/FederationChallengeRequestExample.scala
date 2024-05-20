package util.examples.Federation

import ch.epfl.pop.json.MessageDataProtocol.*
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.network.method.message.data.federation.FederationChallengeRequest
import ch.epfl.pop.model.objects.{Base64Data, Hash, PublicKey, Signature, Timestamp}
import spray.json.*

object FederationChallengeRequestExample {
  final val TIMESTAMP: Timestamp = Timestamp(1712854874)
  final val CHALLENGE_REQUEST: FederationChallengeRequest = FederationChallengeRequest(TIMESTAMP)

  final val SENDER: PublicKey = PublicKey(Base64Data("VHfxTlbM3nTnLQuKnKfs1fGP2cwVT8KJkc-sRGs_2KM="))
  final val SIGNATURE: Signature = Signature(Base64Data("6c3jD8s4VwIeiv3-OrSkGxAG0SfPGwwX1YO-ioYcBVPZqsR_KQCEM6cb_M_XdSMag3Z6HVAXsWsu7vBtFKNtDA=="))
  final val MESSAGE_ID: Hash = Hash(Base64Data("CvPnQaEMpnJipXi21bNk8VpNubyvHYER6SRfY1C12ys="))

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
