package ch.epfl.pop.model.objects

import ch.epfl.dedis.lib.crypto.{Ed25519Point, Ed25519Scalar, Scalar}
import ch.epfl.pop.model.objects.PrivateKey._
import com.google.crypto.tink.subtle.Ed25519Sign

final case class PrivateKey(base64Data: Base64Data) {
  private val asScalar: Scalar = new Ed25519Scalar(base64Data.decode())

  def decrypt(messageB64: Base64Data): Base64Data = {
    val message = messageB64.decode()
    if (message.length != MESSAGE_BYTE_SIZE)
      throw new IllegalArgumentException(s"Your message to decrypt should contain exactly $MESSAGE_BYTE_SIZE bytes")
    val Kbytes = message.slice(0, HALF_MESSAGE_BYTE_SIZE)
    val Cbytes = message.slice(HALF_MESSAGE_BYTE_SIZE, MESSAGE_BYTE_SIZE)
    val K: Ed25519Point = new Ed25519Point(Kbytes)
    val C: Ed25519Point = new Ed25519Point(Cbytes)
    val S = K.mul(asScalar)
    val encrypted = S.add(C.negate).data
    Base64Data.encode(encrypted)
  }


  def signData(data: Base64Data): Signature = {
    val ed: Ed25519Sign = new Ed25519Sign(base64Data.decode())
    Signature(Base64Data.encode(ed.sign(data.decode())))
  }
}

object PrivateKey {
  protected val MESSAGE_BYTE_SIZE: Int = 64
  protected val HALF_MESSAGE_BYTE_SIZE: Int = MESSAGE_BYTE_SIZE / 2
}
