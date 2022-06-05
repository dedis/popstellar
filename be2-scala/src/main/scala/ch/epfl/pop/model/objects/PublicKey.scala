package ch.epfl.pop.model.objects

import ch.epfl.dedis.lib.crypto.{Ed25519, Ed25519Point, Ed25519Scalar}

import java.security.SecureRandom
import PublicKey._

import java.security.MessageDigest

final case class PublicKey(base64Data: Base64Data) {

  def encrypt(messageB64: Base64Data): Base64Data = {
    val asPoint = new Ed25519Point(base64Data.decode())
    val message = messageB64.decode()
    if (message.length > MAX_MESSAGE_LENGTH) throw new IllegalArgumentException(s"The message should contain at maximum $MAX_MESSAGE_LENGTH bytes")
    val M = Ed25519Point.embed(message)
    val seed = new Array[Byte](Ed25519.field.getb / 8)
    new SecureRandom().nextBytes(seed)
    val k = new Ed25519Scalar(seed)
    val K = Ed25519Point.base.mul(k)
    val S = asPoint.mul(k)
    val C = S.add(M)
    val result: Array[Byte] = K.toBytes.concat(C.toBytes)
    Base64Data.encode(result)
  }

  def equals(that: PublicKey): Boolean = base64Data == that.base64Data

  def hash: Address = Address.of(this)
}

object PublicKey {
  protected val MAX_MESSAGE_LENGTH = 29
}
