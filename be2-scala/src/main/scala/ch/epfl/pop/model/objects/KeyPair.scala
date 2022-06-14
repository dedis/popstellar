package ch.epfl.pop.model.objects

import ch.epfl.dedis.lib.crypto.Ed25519Pair

case class KeyPair(privateKey: PrivateKey, publicKey: PublicKey) {
  def decrypt(messageB64: Base64Data): Base64Data = privateKey.decrypt(messageB64)
  def encrypt(messageB64: Base64Data): Base64Data = publicKey.encrypt(messageB64)
}

object KeyPair {
  def apply(): KeyPair = {
    val libKeyPair = new Ed25519Pair()
    val privateKey = PrivateKey(Base64Data.encode(libKeyPair.scalar.toBytes))
    val publicKey = PublicKey(Base64Data.encode(libKeyPair.point.toBytes))
    KeyPair(privateKey, publicKey)
  }
}
