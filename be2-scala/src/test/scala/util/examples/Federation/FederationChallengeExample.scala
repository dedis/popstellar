package util.examples.Federation

import ch.epfl.pop.json.MessageDataProtocol.*
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.network.method.message.data.federation.FederationChallenge
import ch.epfl.pop.model.objects.{Base16Data, Base64Data, Hash, PublicKey, Signature, Timestamp}
import spray.json.*

object FederationChallengeExample {

  final val VALUE: Base16Data = Base16Data("82eadde2a4ba832518b90bb93c8480ee1ae16a91d5efe9281e91e2ec11da03e4")
  final val VALID_UNTIL: Timestamp = Timestamp(1712854874)
  final val CHALLENGE: FederationChallenge = FederationChallenge(VALUE, VALID_UNTIL)

  final val SENDER: PublicKey = PublicKey(Base64Data("zXgzQaa_NpUe-v0Zk_4q8k184ohQ5nTQhBDKgncHzq4="))
  final val SIGNATURE: Signature = Signature(Base64Data("BILYwYkT5tOBL4rCD7yvhBkhAYqRXOI3ajQ2uJ1gAk-g6nRc38vMMnlHShuNCQ3dQFXYZPn37cCFelhWGjY8Bg=="))
  final val MESSAGE_ID: Hash = Hash(Base64Data("sD_PdryBuOr14_65h8L-e1lzdQpDWxUAngtu1uwqgEI="))

  final val DATA_CHALLENGE_MESSAGE: Hash = Hash(Base64Data.encode(CHALLENGE.toJson.toString))
  final val CHALLENGE_MESSAGE: Message = Message(
    DATA_CHALLENGE_MESSAGE.base64Data,
    SENDER,
    SIGNATURE,
    MESSAGE_ID,
    List.empty,
    Some(CHALLENGE)
  )

}
