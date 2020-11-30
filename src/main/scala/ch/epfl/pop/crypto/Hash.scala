package ch.epfl.pop.crypto

import java.nio.charset.StandardCharsets
import java.security.MessageDigest

import ch.epfl.pop.json.{Base64String, Hash, Signature}

object Hash {

  /**
   * Compute the ID of the message, which is the hash of the message and the signature.
   * @param msg the message in base64
   * @param signature the signature of the message
   * @return the ID
   */
  def computeID(msg: Base64String, signature: Signature): Hash = {
    val digest = MessageDigest.getInstance("SHA-256")
    digest.update(msg.getBytes(StandardCharsets.UTF_8))
    digest.update(signature)
    val id = digest.digest()
    id
  }

}
