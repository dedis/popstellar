package ch.epfl.pop.model.objects

import java.security.MessageDigest

final case class PublicKey(base64Data: Base64Data) {
  def equals(that: PublicKey): Boolean = base64Data == that.base64Data

  def hash: Base64Data = {
      val messageDigest = MessageDigest.getInstance("SHA-256")
      val data = base64Data.decode()
      Base64Data.encode(messageDigest.digest(data).take(20))
  } 
}
