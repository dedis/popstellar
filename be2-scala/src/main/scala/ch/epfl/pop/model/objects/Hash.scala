package ch.epfl.pop.model.objects

import java.nio.charset.StandardCharsets
import java.security.MessageDigest

final case class Hash(base64Data: Base64Data) {
  def getBytes: Array[Byte] = base64Data.getBytes

  override def toString: String = base64Data.toString
}

object Hash {

  val messageDigest: MessageDigest = MessageDigest.getInstance("SHA-256")

  /**
   * Create a base64 encoded hash of an array of strings according to the communication protocol
   *
   * @param data values to be hashed
   * @return resulting hash
   */
  def fromStrings(data: String*): Hash = Hash.sha256Hash(data.foldLeft("")((acc, s) => acc + s.length + s))

  /**
   * Create a hash of a string
   *
   * @param data value to be hashed
   * @return resulting hash
   */
  def sha256Hash(data: String): Hash = Hash(Base64Data.encode(
    messageDigest.digest(data.getBytes(StandardCharsets.UTF_8))
  ))
}
