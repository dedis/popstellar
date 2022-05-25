package util.examples.Witness

import ch.epfl.pop.json.MessageDataProtocol._
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.network.method.message.data.witness.WitnessMessage
import ch.epfl.pop.model.objects._
import com.google.crypto.tink.subtle.Ed25519Sign
import spray.json._


object WitnessMessageExamples {

  val keyPair: Ed25519Sign.KeyPair = Ed25519Sign.KeyPair.newKeyPair
  val privateKey: PrivateKey = PrivateKey(Base64Data.encode(keyPair.getPrivateKey))
  val SENDER: PublicKey = PublicKey(Base64Data.encode(keyPair.getPublicKey))

  final val MESSAGE_ID: Hash = Hash(Base64Data.encode("messageId"))
  final val WITNESS_SIGNATURE: Signature = privateKey.signData(MESSAGE_ID.base64Data)

  val invalidSender: PublicKey = PublicKey(Base64Data.encode("wrong"))
  val invalidSignature: Signature = Signature(Base64Data.encode("wrongSignature"))

  val workingWitnessMessage: WitnessMessage = WitnessMessage(MESSAGE_ID, WITNESS_SIGNATURE)
  val dataWitnessMessage: Base64Data = Base64Data.encode(workingWitnessMessage.toJson.toString)
  final val MESSAGE_WITNESS_MESSAGE_WORKING: Message = Message(
    dataWitnessMessage,
    SENDER,
    privateKey.signData(dataWitnessMessage),
    MESSAGE_ID,
    List.empty,
    Some(workingWitnessMessage)
  )

  val wrongSignatureWitnessMessage: WitnessMessage = WitnessMessage(MESSAGE_ID, invalidSignature)
  val dataWrongSignature: Base64Data = Base64Data.encode(wrongSignatureWitnessMessage.toJson.toString)
  final val MESSAGE_WITNESS_MESSAGE_WRONG_SIGNATURE: Message = new Message(
    dataWrongSignature,
    SENDER,
    privateKey.signData(dataWrongSignature),
    MESSAGE_ID,
    List.empty,
    Some(wrongSignatureWitnessMessage)
  )

  final val MESSAGE_WITNESS_MESSAGE_WRONG_OWNER: Message = new Message(
    dataWitnessMessage,
    invalidSender,
    privateKey.signData(dataWitnessMessage),
    MESSAGE_ID,
    List.empty,
    Some(workingWitnessMessage)
  )
}
