package com.github.dedis.popstellar.model.objects.security.privatekey

import com.github.dedis.popstellar.model.objects.security.Base64URLData
import com.github.dedis.popstellar.model.objects.security.PrivateKey
import com.github.dedis.popstellar.model.objects.security.Signature
import com.google.crypto.tink.PublicKeySign
import com.google.crypto.tink.subtle.Ed25519Sign
import java.security.GeneralSecurityException

/**
 * A private key where we have direct access to the key itself.
 *
 *
 * This is used by [com.github.dedis.popstellar.model.objects.security.PoPToken] as the key
 * pair is generated independently from the phone.
 */
class PlainPrivateKey(key: ByteArray?) : Base64URLData(key!!), PrivateKey {
    private var signer: PublicKeySign? = null

    init {
        try {
            signer = Ed25519Sign(key)
        } catch (e: GeneralSecurityException) {
            throw IllegalArgumentException("Could not create the private key from its value", e)
        }
    }

    @Throws(GeneralSecurityException::class)
    override fun sign(data: Base64URLData?): Signature {
        return Signature(signer!!.sign(data!!.getData()))
    }

    override fun toString(): String {
        // The actual private key should never be printed
        // Prevent that by redefining the toString representation
        return "PlainPrivateKey@" + Integer.toHexString(hashCode())
    }
}