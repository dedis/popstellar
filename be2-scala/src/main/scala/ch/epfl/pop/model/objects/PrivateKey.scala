package ch.epfl.pop.model.objects

import ch.epfl.dedis.lib.crypto.{Ed25519Point, Ed25519Scalar, Point, Scalar}
import ch.epfl.dedis.lib.exception.CothorityCryptoException
import com.google.crypto.tink.subtle.Ed25519Sign
import java.nio.charset.StandardCharsets

final case class PrivateKey(base64Data: Base64Data) {
  private val asScalar: Scalar = new Ed25519Scalar(base64Data.decode())

  private val MESSAGE_BYTE_SIZE: Int = 64
  private val HALF_MESSAGE_BYTE_SIZE: Int = 32

  @throws[CothorityCryptoException]
  def decrypt(message: Base64Data): Base64Data = {
    val in_byte_message = message.decode()
    if (in_byte_message.length != MESSAGE_BYTE_SIZE) throw new IllegalArgumentException("Your message to decrypt should contain exactly 64 bytes")
    val Kbytes = new Array[Byte](HALF_MESSAGE_BYTE_SIZE)
    val Cbytes = new Array[Byte](HALF_MESSAGE_BYTE_SIZE)
    for (i <- 0 until MESSAGE_BYTE_SIZE -1 )
    {
      if (i < HALF_MESSAGE_BYTE_SIZE) Kbytes(i) = in_byte_message(i)
      else Cbytes(i-HALF_MESSAGE_BYTE_SIZE) = in_byte_message(i)
    }
    var K: Ed25519Point = null
    var C: Ed25519Point = null
    try {
      K = new Ed25519Point(Kbytes)
      C = new Ed25519Point(Cbytes)
    } catch {
      case _: CothorityCryptoException =>
        throw new CothorityCryptoException("Could not create K Point while decrypting")
    }
    val S = K.mul(asScalar)
    val encryptedBytes = S.add(C.negate).data
    Base64Data.encode(encryptedBytes)
  }


  def signData(data: Base64Data): Signature = {
    val ed: Ed25519Sign = new Ed25519Sign(base64Data.decode())
    Signature(Base64Data.encode(ed.sign(data.decode())))
  }
}
