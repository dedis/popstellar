package com.github.dedis.popstellar.model.objects.security

import com.github.dedis.popstellar.model.Immutable
import java.security.GeneralSecurityException
import java.util.Objects

/**
 * Represent a private/public key pair used for signing data
 *
 * This object does not actually store the private key as is it safely stored by another module.
 */
@Immutable
open class KeyPair(open val privateKey: PrivateKey, val publicKey: PublicKey) {

  /**
   * Signs the given data with the private key of the pair [PublicKeySign.sign]
   *
   * @param data to sign
   * @return the signature
   * @throws GeneralSecurityException if an error occurs
   */
  @Throws(GeneralSecurityException::class)
  fun sign(data: Base64URLData): Signature {
    return privateKey.sign(data)
  }

  fun verify(signature: Signature, data: Base64URLData): Boolean {
    return publicKey.verify(signature, data)
  }

  override fun toString(): String {
    return "${javaClass.simpleName}{privateKey=$privateKey, publicKey=$publicKey}"
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) {
      return true
    }
    if (other == null || javaClass != other.javaClass) {
      return false
    }
    val keyPair = other as KeyPair
    return privateKey == keyPair.privateKey && publicKey == keyPair.publicKey
  }

  override fun hashCode(): Int {
    return Objects.hash(privateKey, publicKey)
  }
}
