package ch.epfl.pop.model.objects

import com.google.crypto.tink.subtle.Ed25519Verify

import scala.util.{Success, Try}

final case class Signature(signature: Base64Data) {
  def verify(key: PublicKey, message: Base64Data): Boolean = {
    val ed = new Ed25519Verify(key.base64Data.decode())

    Try(ed.verify(this.signature.decode(), message.decode())) match {
      case Success(_) => true
      case _          => false
    }
  }

  override def toString: String = signature.toString
}
