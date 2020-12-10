package ch.epfl.pop.crypto

import java.nio.charset.StandardCharsets

import ch.epfl.pop.json.{Base64String, Key, Signature}
import scorex.crypto.signatures
import scorex.crypto.signatures.Curve25519
import scorex.util.encode.Base16

object Signature {

  /**
   * Verify if a signature is correct.
   *
   * @param msg       the message to verify, encoded in base64
   * @param signature the signature corresponding to the message
   * @param key       the public key used for verification
   * @return whether the signature is correct
   */
  def verify(msg: Base64String, signature: Signature, key: Key): Boolean = {
    val sigTagged = signatures.Signature @@ signature
    val keyTagged = signatures.PublicKey @@ key

    Curve25519.verify(sigTagged, msg.getBytes(StandardCharsets.UTF_8), keyTagged)
  }
}
