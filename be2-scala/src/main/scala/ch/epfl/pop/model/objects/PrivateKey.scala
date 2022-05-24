package ch.epfl.pop.model.objects

import ch.epfl.dedis.lib.crypto.{Ed25519Point, Ed25519Scalar}
import com.google.crypto.tink.subtle.Ed25519Sign

final case class PrivateKey(base64Data: Base64Data) {
  private val asScalar = new Ed25519Scalar(base64Data.decode())

  def elGamalDecrypt(messageB64: Base64Data, K: EphemeralPublicKey): Base64Data = {
    // todo : complete with
    //  https://github.com/dedis/kyber/blob/master/examples/enc_test.go
    val C = new Ed25519Point(messageB64.decode())
    val S = K.asPoint.mul(this.asScalar)
    val M = S.add(C.negate())
    val message = M.data()
    Base64Data.encode(message)
  }

  def signData(data: Base64Data): Signature = {
    val ed: Ed25519Sign = new Ed25519Sign(base64Data.decode())
    Signature(Base64Data.encode(ed.sign(data.decode())))
  }
}
