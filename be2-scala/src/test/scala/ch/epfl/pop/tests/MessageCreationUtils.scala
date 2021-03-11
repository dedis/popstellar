package ch.epfl.pop.tests


import ch.epfl.pop.crypto.Hash
import ch.epfl.pop.json._
import spray.json.enrichAny
import com.google.crypto.tink.subtle.Ed25519Sign.KeyPair
import com.google.crypto.tink.subtle.Ed25519Sign
import ch.epfl.pop.json.JsonCommunicationProtocol.MessageContentDataFormat
import java.nio.charset.StandardCharsets.UTF_8

import java.util.Base64

object MessageCreationUtils {

  def b64Encode(b: Array[Byte]): Array[Byte] = Base64.getEncoder.encode(b)

  def b64EncodeToString(b: Array[Byte]): String = Base64.getEncoder.encodeToString(b)

  def b64Decode(s: Base64String): Array[Byte] = Base64.getDecoder.decode(s.getBytes(UTF_8))

  def generateKeyPair(): KeyPair = {
     val keyPair = KeyPair.newKeyPair()
     keyPair
  }

  def sign(kp: KeyPair, data: Array[Byte]): Array[Byte] = sign(kp.getPrivateKey, data)

  def sign(sk: Array[Byte], data: Array[Byte]): Array[Byte] = {
    new Ed25519Sign(sk).sign(data)
  }

  def getMessageParams(data: MessageContentData, kp: KeyPair, channel: ChannelName): MessageParameters =
    getMessageParams(data, kp.getPublicKey, kp.getPrivateKey, channel)

  def getMessageParams(data: MessageContentData, sender: Array[Byte], sk: Array[Byte], channel: ChannelName): MessageParameters = {
    val dataJson = data.toJson.compactPrint.getBytes
    val encodedData = b64EncodeToString(dataJson)
    val signature = sign(sk, dataJson)
    val messageId = Hash.computeMessageId(encodedData, signature)
    val witnessSignature: List[KeySignPair] = Nil

    val content = MessageContent(encodedData, data, sender, signature, messageId, witnessSignature)
    val params = MessageParameters(channel, Some(content))
    params
  }
}
