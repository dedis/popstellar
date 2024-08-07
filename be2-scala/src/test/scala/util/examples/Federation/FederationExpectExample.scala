package util.examples.Federation

import ch.epfl.pop.json.MessageDataProtocol.*
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.network.method.message.data.federation.FederationExpect
import ch.epfl.pop.model.objects.{Base64Data, Hash, PublicKey, Signature, WitnessSignaturePair}
import spray.json.*

object FederationExpectExample {
  final val LAO_ID: Hash = Hash(Base64Data("lsWUv1bKBQ0t1DqWZTFwb0nhLsP_EtfGoXHny4hsrwA="))
  final val SERVER_ADDRESS: String = "wss://ethz.ch:9000/server"
  final val WRONG_SERVER_ADDRESS: String = "w:/ethz.ch:9000/server"

  final val PUBLIC_KEY: PublicKey = PublicKey(Base64Data("zXgzQaa_NpUe-v0Zk_4q8k184ohQ5nTQhBDKgncHzq4="))
  final val PUBLIC_KEY_1: PublicKey = PublicKey(Base64Data("UvViTxoKsB3XVP_ctkmOKCJpMWb7fCzrcb1XDmhNe7Q="))

  final val DATA: Base64Data = Base64Data("eyJvYmplY3QiOiJmZWRlcmF0aW9uIiwiYWN0aW9uIjoiY2hhbGxlbmdlIiwidmFsdWUiOiJlYmEzZTI0ZWZjZDBiNTNmYTY5OTA4YmFkNWQxY2I2OTlkNzk4MGQ5MzEwOWRhMGIyYmZkNTAzN2MyYzg5ZWUwIiwidmFsaWRfdW50aWwiOjE3MTMzODU1Njh9")
  final val DATA_1: Base64Data = Base64Data("eyJvYmplY3QiOiJmZWRlcmF0aW9uIiwiYWN0aW9uIjoiY2hhbGxlbmdlIiwidmFsdWUiOiJlYmEzZTI0ZWZjZDBiNTNmYTY5OTA4YmFkNWQxY2I2OTlkNzk4MGQ5MzEwOWRhMGIyYmZkNTAzN2MyYzg5ZWUwIiwidGltZXN0YW1wIjoxNzEzMzg1NTY4fQ==")
  final val WRONG_DATA: Base64Data = Base64Data("eyJvYmplY3QiOiJmZWRlcmF0aW9uIiwiYWN0aW9uIjoiY2hhbGxlbmdlIiwidmFsdWUiOiJlYmEzZTI0ZWZjZDBiNTNmYTY5OTA4YmFkNWQxY2I2OTlkNzk4MGQ5MzEwOWRhMGIyYmZkNTAzN2MyYzg5ZWUwIiwidmFsaWRfdW50aWwiOi02NTc0OH0=")

  final val SENDER: PublicKey = PublicKey(Base64Data("VHfxTlbM3nTnLQuKnKfs1fGP2cwVT8KJkc-sRGs_2KM="))
  final val SENDER_1: PublicKey = PublicKey(Base64Data("zXgzQaa_NpUe-v0Zk_4q8k184ohQ5nTQhBDKgncHzq4="))
  final val WRONG_SENDER: PublicKey = PublicKey(Base64Data.encode("wrongSender"))

  final val SIGNATURE: Signature = Signature(Base64Data("BILYwYkT5tOBL4rCD7yvhBkhAYqRXOI3ajQ2uJ1gAk-g6nRc38vMMnlHShuNCQ3dQFXYZPn37cCFelhWGjY8Bg=="))
  final val MESSAGE_ID: Hash = Hash(Base64Data("sD_PdryBuOr14_65h8L-e1lzdQpDWxUAngtu1uwqgEI="))
  final val WITNESS_SIGNATURES: List[WitnessSignaturePair] = Nil

  final val CHALLENGE: Message = Message(DATA, SENDER, SIGNATURE, MESSAGE_ID, WITNESS_SIGNATURES)
  final val CHALLENGE_1: Message = Message(DATA_1, SENDER_1, SIGNATURE, MESSAGE_ID, WITNESS_SIGNATURES)
  final val CHALLENGE_WRONG_SENDER: Message = Message(DATA, WRONG_SENDER, SIGNATURE, MESSAGE_ID, WITNESS_SIGNATURES)
  final val CHALLENGE_WRONG_DATA: Message = Message(WRONG_DATA, SENDER, SIGNATURE, MESSAGE_ID, WITNESS_SIGNATURES)

  final val EXPECT: FederationExpect = FederationExpect(LAO_ID, SERVER_ADDRESS, PUBLIC_KEY, CHALLENGE)
  final val EXPECT_1: FederationExpect = FederationExpect(LAO_ID, SERVER_ADDRESS, PUBLIC_KEY_1, CHALLENGE_1)
  final val EXPECT_WRONG_SERVER_ADDRESS: FederationExpect = FederationExpect(LAO_ID, WRONG_SERVER_ADDRESS, PUBLIC_KEY, CHALLENGE)
  final val EXPECT_WRONG_CHALLENGE_SENDER: FederationExpect = FederationExpect(LAO_ID, SERVER_ADDRESS, PUBLIC_KEY, CHALLENGE_WRONG_SENDER)
  final val EXPECT_WRONG_CHALLENGE: FederationExpect = FederationExpect(LAO_ID, SERVER_ADDRESS, PUBLIC_KEY, CHALLENGE_WRONG_DATA)

  final val DATA_EXPECT_MESSAGE: Hash = Hash(Base64Data.encode(EXPECT.toJson.toString))
  final val DATA_EXPECT_WRONG_SERVER_ADDRESS_MESSAGE: Hash = Hash(Base64Data.encode(EXPECT_WRONG_SERVER_ADDRESS.toJson.toString))
  final val DATA_EXPECT_WRONG_CHALLENGE_SENDER_MESSAGE: Hash = Hash(Base64Data.encode(EXPECT_WRONG_CHALLENGE_SENDER.toJson.toString))
  final val DATA_EXPECT_WRONG_CHALLENGE_MESSAGE: Hash = Hash(Base64Data.encode(EXPECT_WRONG_CHALLENGE.toJson.toString))

  final val EXPECT_MESSAGE: Message = Message(
    DATA_EXPECT_MESSAGE.base64Data,
    SENDER,
    SIGNATURE,
    MESSAGE_ID,
    WITNESS_SIGNATURES,
    Some(EXPECT)
  )

  final val EXPECT_WRONG_SERVER_ADDRESS_MESSAGE: Message = Message(
    DATA_EXPECT_WRONG_SERVER_ADDRESS_MESSAGE.base64Data,
    SENDER,
    SIGNATURE,
    MESSAGE_ID,
    WITNESS_SIGNATURES,
    Some(EXPECT_WRONG_SERVER_ADDRESS)
  )

  final val EXPECT_WRONG_SENDER_MESSAGE: Message = Message(
    DATA_EXPECT_MESSAGE.base64Data,
    WRONG_SENDER,
    SIGNATURE,
    MESSAGE_ID,
    WITNESS_SIGNATURES,
    Some(EXPECT)
  )

  final val EXPECT_WRONG_CHALLENGE_SENDER_MESSAGE: Message = Message(
    DATA_EXPECT_WRONG_CHALLENGE_SENDER_MESSAGE.base64Data,
    SENDER,
    SIGNATURE,
    MESSAGE_ID,
    WITNESS_SIGNATURES,
    Some(EXPECT_WRONG_CHALLENGE_SENDER)
  )

  final val EXPECT_WRONG_CHALLENGE_MESSAGE: Message = Message(
    DATA_EXPECT_WRONG_CHALLENGE_MESSAGE.base64Data,
    SENDER,
    SIGNATURE,
    MESSAGE_ID,
    WITNESS_SIGNATURES,
    Some(EXPECT_WRONG_CHALLENGE)
  )

}
