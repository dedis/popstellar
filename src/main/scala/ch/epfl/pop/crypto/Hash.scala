package ch.epfl.pop.crypto

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

import ch.epfl.pop.json.{Base64String, Hash, Key, Signature}

object Hash {

  /**
   * Compute the id of the message, which is the Hash(msg||signature)
   * @param msg the message in base64
   * @param signature the signature of the message
   * @return the id of the message
   */
  def computeMessageId(msg: Base64String, signature: Signature): Hash = {
    val md = getMessageDigest()
    md.update(msg.getBytes(StandardCharsets.UTF_8))
    md.update(signature)
    val id = md.digest()
    id
  }

  /**
   * Compute the LAO id, which is Hash(organizer||creation||name)
   * @param organizer the organizer of the LAO
   * @param creation the creation timestamp of the LAO
   * @param name the name of the LAO
   * @return the id of the LAO
   */
  def computeLAOId(organizer: Key, creation: Long, name: String): Hash = computeGenericId(organizer, creation, name)


  /**
   * Compute the meeting id, which is Hash(laoId||creation||name)
   * @param laoID the id of the LAO
   * @param creation the creation timestamp of the LAO
   * @param name the name of the meeting
   * @return the id of the meeting
   */
  def computeMeetingId(laoID: Hash, creation: Long, name: String): Hash = computeGenericId(laoID, creation, name)


  private def computeGenericId(a: Array[Byte], l: Long, s: String): Hash = {
    val md = getMessageDigest()
    md.update(a)
    md.update(l)
    md.update(s.getBytes)
    val id = md.digest()
    id
  }

  private def getMessageDigest() = MessageDigest.getInstance("SHA-256")

  implicit def longToByteArray(l: Long): ByteBuffer = ByteBuffer.allocate(8).putLong(l)
}
