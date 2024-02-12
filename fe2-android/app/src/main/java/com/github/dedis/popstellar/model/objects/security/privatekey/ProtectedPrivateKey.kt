package com.github.dedis.popstellar.model.objects.security.privatekey

import com.github.dedis.popstellar.model.objects.security.Base64URLData
import com.github.dedis.popstellar.model.objects.security.PrivateKey
import com.github.dedis.popstellar.model.objects.security.Signature
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.PublicKeySign
import java.security.GeneralSecurityException

/**
 * A private key where the key is protected by android and thus inaccessible.
 *
 * We do not have access to the key, but we have access to the primitives derived from it like a
 * [PublicKeySign] that can be used to sign data.
 */
class ProtectedPrivateKey(handler: KeysetHandle) : PrivateKey {
  private val signer: PublicKeySign =
      try {
        handler.getPrimitive(PublicKeySign::class.java)
      } catch (e: GeneralSecurityException) {
        throw IllegalArgumentException("Could not create the private key from the keyset", e)
      }

  @Throws(GeneralSecurityException::class)
  override fun sign(data: Base64URLData): Signature {
    return Signature(signer.sign(data.data))
  }
}
