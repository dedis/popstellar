package ch.epfl.pop.model.objects

import ch.epfl.dedis.lib.crypto.{Ed25519, Ed25519Point, Ed25519Scalar}
import java.io.ByteArrayOutputStream
import java.util.{Objects, Random}

final case class PublicKey(base64Data: Base64Data) {
  val asPoint = new Ed25519Point(base64Data.decode())


  def encrypt(messageB64: Base64Data): Base64Data = {
    val message = messageB64.decode()
    if (message.length > 29) throw new IllegalArgumentException("The message should contain at maximum 29 bytes")
    val M = Ed25519Point.embed(message)
    val seed = new Array[Byte](Ed25519.field.getb / 8)
    new Random().nextBytes(seed)
    val k = new Ed25519Scalar(seed)
    val K = Ed25519Point.base.mul(k)
    val S = asPoint.mul(k)
    val C = S.add(M)
    var result: Array[Byte] = null
    try {
      val output = new ByteArrayOutputStream
      output.write(K.toBytes)
      output.write(C.toBytes)
      result = output.toByteArray
    } catch {
      case e: Exception =>
        System.out.println("Something happened during the encryption, could concatenate the final result into a byte array")
        result = null
    }
    if (Objects.isNull(result)) return null
    Base64Data.encode(result)
  }

  def equals(that: PublicKey): Boolean = base64Data == that.base64Data
}
