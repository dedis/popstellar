package util.examples.Federation

import ch.epfl.pop.json.MessageDataProtocol.*
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.network.method.message.data.federation.FederationInit
import ch.epfl.pop.model.objects.{Base64Data, Hash, PublicKey, Signature, WitnessSignaturePair}
import spray.json.*

object FederationInitExample {
  final val LAO_ID: Hash = Hash(Base64Data("fzJSZjKf-2cbXH7kds9H8NORuuFIRLkevJlN7qQemjo="))
  final val SERVER_ADDRESS: String = "wss://epfl.ch:9000/server"
  final val PUBLIC_KEY: PublicKey = PublicKey(Base64Data("J9fBzJV70Jk5c-i3277Uq4CmeL4t53WDfUghaK0HpeM="))

  final val DATA: Base64Data = Base64Data("eyJvYmplY3QiOiJmZWRlcmF0aW9uIiwiYWN0aW9uIjoiY2hhbGxlbmdlIiwidmFsdWUiOiJlYmEzZTI0ZWZjZDBiNTNmYTY5OTA4YmFkNWQxY2I2OTlkNzk4MGQ5MzEwOWRhMGIyYmZkNTAzN2MyYzg5ZWUwIiwidGltZXN0YW1wIjoxNzEzMzg1NTY4fQ==")
  final val SENDER: PublicKey = PublicKey(Base64Data("zXgzQaa_NpUe-v0Zk_4q8k184ohQ5nTQhBDKgncHzq4="))
  final val SIGNATURE: Signature = Signature(Base64Data("BILYwYkT5tOBL4rCD7yvhBkhAYqRXOI3ajQ2uJ1gAk-g6nRc38vMMnlHShuNCQ3dQFXYZPn37cCFelhWGjY8Bg=="))
  final val MESSAGE_ID: Hash = Hash(Base64Data("sD_PdryBuOr14_65h8L-e1lzdQpDWxUAngtu1uwqgEI="))
  final val WITNESS_SIGNATURES: List[WitnessSignaturePair] = Nil
  final val CHALLENGE: Message = Message(DATA, SENDER, SIGNATURE, MESSAGE_ID, WITNESS_SIGNATURES)
  final val INIT: FederationInit = FederationInit(LAO_ID, SERVER_ADDRESS, PUBLIC_KEY, CHALLENGE)

  final val DATA_INIT_MESSAGE: Hash = Hash(Base64Data.encode(INIT.toJson.toString))
  final val INIT_MESSAGE: Message = Message(
    DATA_INIT_MESSAGE.base64Data,
    SENDER,
    SIGNATURE,
    MESSAGE_ID,
    WITNESS_SIGNATURES,
    Some(INIT)
  )
}
