package ch.epfl.pop.crypto

import java.nio.charset.StandardCharsets
import java.security.MessageDigest

object Hash {

  /**
   * Compute the ID of the message, which is the hash of the message and the signature.
   * @param msg the message
   * @param signature the signature of the message
   * @return the ID in hex format
   */
  def computeID(msg: String, signature: String): String = {
    val digest = MessageDigest.getInstance("SHA-256")
    digest.update(msg.getBytes(StandardCharsets.UTF_8))
    digest.update(signature.getBytes(StandardCharsets.UTF_8))
    val id = digest.digest().map("%02x".format(_)).mkString
    id
  }

}
