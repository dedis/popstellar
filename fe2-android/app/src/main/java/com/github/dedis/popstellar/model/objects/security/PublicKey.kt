package com.github.dedis.popstellar.model.objects.security

import com.github.dedis.popstellar.model.Immutable
import com.google.crypto.tink.PublicKeyVerify
import com.google.crypto.tink.subtle.Ed25519Verify
import java.security.GeneralSecurityException
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.Arrays
import java.util.Base64
import timber.log.Timber

/** A public key that can be used to verify a signature */
@Immutable
class PublicKey : Base64URLData {
  private val verifier: PublicKeyVerify

  constructor(data: ByteArray) : super(data) {
    verifier = Ed25519Verify(data)
  }

  constructor(data: String) : super(data) {
    verifier = Ed25519Verify(this.data)
  }

  fun verify(signature: Signature, data: Base64URLData): Boolean {
    return try {
      verifier.verify(signature.data, data.data)
      true
    } catch (e: GeneralSecurityException) {
      Timber.tag(TAG).e("Failed to verify witness signature %s", e.message)
      false
    }
  }

  /**
   * Function that compute the hash of a public key
   *
   * @return String which correspond to the SHA256 Hash
   */
  fun computeHash(): String {
    try {
      val digest = MessageDigest.getInstance("SHA-256")
      val hash = digest.digest(data)

      return Base64.getUrlEncoder().encodeToString(Arrays.copyOf(hash, 20))
    } catch (e: NoSuchAlgorithmException) {
      Timber.tag(TAG).e(e, "Something is wrong by hashing the String element")
      throw IllegalArgumentException("Error in computing the hash in public key")
    }
  }

  companion object {
    private val TAG = PublicKey::class.java.simpleName
  }
}
