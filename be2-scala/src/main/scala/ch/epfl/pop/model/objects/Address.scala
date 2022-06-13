package ch.epfl.pop.model.objects

import java.nio.charset.StandardCharsets
import java.security.MessageDigest

/** A LAO address is a shortened, human-readable representation of a public key.
  *
  * Currently, it consists of the first 160 bits of the SHA2-256 digest of the public key.
  */
final case class Address(base64Data: Base64Data) {
  def decode(): Array[Byte] = base64Data.decode()
}

object Address {

  private val bytesCount = 20
  val sha2_256: MessageDigest = MessageDigest.getInstance("SHA-256")

  /** Compute the address of the given public key
    *
    * @param pk
    *   the public key
    * @return
    *   the address associated with the public key.
    */
  def of(pk: PublicKey): Address = Address(Base64Data.encode(
    sha2_256.digest(pk.base64Data.decode()).take(bytesCount)
  ))
}
