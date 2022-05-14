package ch.epfl.pop.model.objects

import com.google.crypto.tink.subtle.Ed25519Sign
import com.swiftcryptollc.crypto.provider.KyberJCE
import java.security.{KeyPairGenerator, Security}

case class KeyPair(privateKey: PrivateKey, publicKey: PublicKey)

object KeyPair {
  def apply(): KeyPair = {
    val libKeyPair = Ed25519Sign.KeyPair.newKeyPair
    val privateKey = PrivateKey(Base64Data.encode(libKeyPair.getPrivateKey))
    val publicKey = PublicKey(Base64Data.encode(libKeyPair.getPublicKey))
    KeyPair(privateKey, publicKey)
  }

  /*
  To generate keys with Kyber :
  def apply(): KeyPair = {
    Security.setProperty("crypto.policy", "unlimited")
    Security.addProvider(new KyberJCE)
    // fixme : crash at the line bellow (NoClassDefFoundError)
    val keyGen = KeyPairGenerator.getInstance("Kyber1024")
    val aliceKeyPair = keyGen.generateKeyPair
    val privateKey = PrivateKey(Base64Data.encode(aliceKeyPair.getPrivate.getEncoded))
    val publicKey = PublicKey(Base64Data.encode(aliceKeyPair.getPublic.getEncoded))
    KeyPair(privateKey, publicKey)
  }*/
}
