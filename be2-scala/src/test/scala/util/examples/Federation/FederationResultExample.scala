package util.examples.Federation

import ch.epfl.pop.json.MessageDataProtocol.*
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.network.method.message.data.federation.{FederationChallenge, FederationResult}
import ch.epfl.pop.model.objects.{Base64Data, Hash, PublicKey, Signature}
import util.examples.Federation.FederationExpectExample.{CHALLENGE, CHALLENGE_WRONG_DATA, CHALLENGE_WRONG_SENDER}
import spray.json.*

object FederationResultExample {
  final val STATUS_1: String = "success"
  final val STATUS_2: String = "failure"
  final val WRONG_STATUS: String = "wrongStatus"

  final val REASON: String = "Unexpected behavior"
  final val PUBLIC_KEY: PublicKey = PublicKey(Base64Data("VHfxTlbM3nTnLQuKnKfs1fGP2cwVT8KJkc-sRGs_2KM="))
  final val WRONG_PUBLIC_KEY: PublicKey = PublicKey(Base64Data.encode("wrongPublicKey"))

  final val SENDER: PublicKey = PublicKey(Base64Data("zXgzQaa_NpUe-v0Zk_4q8k184ohQ5nTQhBDKgncHzq4="))
  final val SIGNATURE: Signature = Signature(Base64Data("BILYwYkT5tOBL4rCD7yvhBkhAYqRXOI3ajQ2uJ1gAk-g6nRc38vMMnlHShuNCQ3dQFXYZPn37cCFelhWGjY8Bg=="))
  final val MESSAGE_ID: Hash = Hash(Base64Data("sD_PdryBuOr14_65h8L-e1lzdQpDWxUAngtu1uwqgEI="))

  final val RESULT_1: FederationResult = FederationResult(STATUS_1, PUBLIC_KEY, CHALLENGE)
  final val RESULT_2: FederationResult = FederationResult(STATUS_2, REASON, CHALLENGE)
  final val RESULT_WRONG_STATUS: FederationResult = FederationResult(WRONG_STATUS, PUBLIC_KEY, CHALLENGE)
  final val RESULT_WRONG_CHALLENGE: FederationResult = FederationResult(STATUS_1, PUBLIC_KEY, CHALLENGE_WRONG_DATA)
  final val RESULT_WRONG_CHALLENGE_SENDER: FederationResult = FederationResult(STATUS_1, PUBLIC_KEY, CHALLENGE_WRONG_SENDER)
  final val RESULT_WRONG_PUBLIC_KEY: FederationResult = FederationResult(STATUS_1, WRONG_PUBLIC_KEY, CHALLENGE)

  final val DATA_RESULT_1_MESSAGE: Hash = Hash(Base64Data.encode(RESULT_1.toJson.toString))
  final val DATA_RESULT_2_MESSAGE: Hash = Hash(Base64Data.encode(RESULT_2.toJson.toString))
  final val DATA_RESULT_WRONG_STATUS_MESSAGE: Hash = Hash(Base64Data.encode(RESULT_WRONG_STATUS.toJson.toString))
  final val DATA_RESULT_WRONG_CHALLENGE_MESSAGE: Hash = Hash(Base64Data.encode(RESULT_WRONG_CHALLENGE.toJson.toString))
  final val DATA_RESULT_WRONG_CHALLENGE_SENDER_MESSAGE: Hash = Hash(Base64Data.encode(RESULT_WRONG_CHALLENGE_SENDER.toJson.toString))
  final val DATA_RESULT_WRONG_PUBLIC_KEY_MESSAGE: Hash = Hash(Base64Data.encode(RESULT_WRONG_PUBLIC_KEY.toJson.toString))

  final val RESULT_1_MESSAGE: Message = Message(
    DATA_RESULT_1_MESSAGE.base64Data,
    SENDER,
    SIGNATURE,
    MESSAGE_ID,
    List.empty,
    Some(RESULT_1)
  )

  final val RESULT_2_MESSAGE: Message = Message(
    DATA_RESULT_2_MESSAGE.base64Data,
    SENDER,
    SIGNATURE,
    MESSAGE_ID,
    List.empty,
    Some(RESULT_2)
  )

  final val RESULT_WRONG_STATUS_MESSAGE: Message = Message(
    DATA_RESULT_WRONG_STATUS_MESSAGE.base64Data,
    SENDER,
    SIGNATURE,
    MESSAGE_ID,
    List.empty,
    Some(RESULT_WRONG_STATUS)
  )

  final val RESULT_WRONG_CHALLENGE_MESSAGE: Message = Message(
    DATA_RESULT_WRONG_CHALLENGE_MESSAGE.base64Data,
    SENDER,
    SIGNATURE,
    MESSAGE_ID,
    List.empty,
    Some(RESULT_WRONG_CHALLENGE)
  )

  final val RESULT_WRONG_CHALLENGE_SENDER_MESSAGE: Message = Message(
    DATA_RESULT_WRONG_CHALLENGE_SENDER_MESSAGE.base64Data,
    SENDER,
    SIGNATURE,
    MESSAGE_ID,
    List.empty,
    Some(RESULT_WRONG_CHALLENGE_SENDER)
  )

  final val RESULT_WRONG_PUBLIC_KEY_MESSAGE: Message = Message(
    DATA_RESULT_WRONG_PUBLIC_KEY_MESSAGE.base64Data,
    SENDER,
    SIGNATURE,
    MESSAGE_ID,
    List.empty,
    Some(RESULT_WRONG_PUBLIC_KEY)
  )

}
