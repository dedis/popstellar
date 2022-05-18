package ch.epfl.pop.model.objects

import ch.epfl.dedis.lib.crypto._
import com.google.crypto.tink.subtle.Ed25519Sign

final case class PrivateKey(base64Data: Base64Data) {
  val scalar: Scalar = new Ed25519Scalar(this.base64Data.data)

  def signData(data: Base64Data): Signature = {
    val ed: Ed25519Sign = new Ed25519Sign(base64Data.decode())
    Signature(Base64Data.encode(ed.sign(data.decode())))
  }
}
