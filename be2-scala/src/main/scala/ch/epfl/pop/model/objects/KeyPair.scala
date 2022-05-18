package ch.epfl.pop.model.objects

import ch.epfl.dedis.lib.crypto.{Ed25519Pair, Point, Scalar}

case class KeyPair(privateKey: PrivateKey, publicKey: PublicKey) {
  /*
   func ElGamalDecrypt(group kyber.Group, prikey kyber.Scalar, K, C kyber.Point) (
     message []byte, err error) {

     // ElGamal-decrypt the ciphertext (K,C) to reproduce the message.
     S := group.Point().Mul(prikey, K) // regenerate shared secret
     M := group.Point().Sub(C, S)      // use to un-blind the message
     message, err = M.Data()           // extract the embedded data
     return
   }
 */

  def decrypt(base64Data: Base64Data): Base64Data = {
    val K: Point = ???
    val C: Point = ???
    val S = K.mul(privateKey.scalar)
    val M = C.add(S.negate())
    Base64Data(new String(M.data()))
  }
}

object KeyPair {

  def apply(): KeyPair = {
    val libKeyPair = new Ed25519Pair()
    val privateKey = PrivateKey(Base64Data.encode(libKeyPair.scalar.toBytes))
    val publicKey = PublicKey(Base64Data.encode(libKeyPair.point.toBytes))
    KeyPair(privateKey, publicKey)
  }
}
