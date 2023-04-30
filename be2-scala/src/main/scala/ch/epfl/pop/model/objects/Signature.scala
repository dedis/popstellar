package ch.epfl.pop.model.objects

import com.google.crypto.tink.subtle.Ed25519Verify

import scala.util.{Success, Try}

/**
 * Represents a signature by a private key. It may be used to verify that a given publicKey is the pair of the private key originally used for the signature.
 * @param signature data resulting from an encryption using a private key
 */
final case class Signature(signature: Base64Data) {
  /**
   * Verifies that the given key indeed decodes back to the given message
   * @param key Key to use to decode the signature
   * @param message Message expected to be found after decoding the signature
   * @return True iif the decoded message is equal to the message expected
   */
  def verify(key: PublicKey, message: Base64Data): Boolean = {
    val ed = new Ed25519Verify(key.base64Data.decode())

    Try(ed.verify(this.signature.decode(), message.decode())) match {
      case Success(_) => true
      case _          => false
    }
  }

  override def toString: String = signature.toString
}
