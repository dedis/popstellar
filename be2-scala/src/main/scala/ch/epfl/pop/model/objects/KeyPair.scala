package ch.epfl.pop.model.objects

import ch.epfl.dedis.lib.crypto.Ed25519Pair

/** Pair of public/private keys for encrypt/decrypt purpose
  * @param privateKey
  *   key to use for encryption
  * @param publicKey
  *   key to use for decryption
  *
  * @note
  *   Warning: this is not intended to be used for signing purpose !! (see Signature.scala for such usage)
  */
case class KeyPair(privateKey: PrivateKey, publicKey: PublicKey) {
  def decrypt(messageB64: Base64Data): Base64Data = privateKey.decrypt(messageB64)
  def encrypt(messageB64: Base64Data): Base64Data = publicKey.encrypt(messageB64)
}

object KeyPair {

  /** Generates a secure pair of keys to use for encryption/decryption
    * @return
    *   a KeyPair holding both keys generated
    *
    * @note
    *   Warning: this is not intended to be used for signing purpose !! (see Signature.scala for such usage)
    */
  def apply(): KeyPair = {
    val libKeyPair = new Ed25519Pair()
    val privateKey = PrivateKey(Base64Data.encode(libKeyPair.scalar.toBytes))
    val publicKey = PublicKey(Base64Data.encode(libKeyPair.point.toBytes))
    KeyPair(privateKey, publicKey)
  }
}
