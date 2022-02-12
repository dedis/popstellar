package ch.epfl.pop.model.objects

import com.google.crypto.tink.subtle.Ed25519Sign

case class PrivateKey(base64Data: Base64Data) {

  def signData(data: Base64Data): Signature = {
    val ed: Ed25519Sign = new Ed25519Sign(base64Data.decode())
    Signature(Base64Data.encode(ed.sign(data.decode())))
  }
}
