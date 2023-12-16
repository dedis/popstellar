package com.github.dedis.popstellar.model.objects.security

import java.security.GeneralSecurityException

/** A private key that can be used to sign data  */
interface PrivateKey {
    /**
     * Signs some data and returns the generated [Signature]
     *
     * @param data to sign
     * @return generated signature
     * @throws GeneralSecurityException if an error occurs while signing
     */
    @Throws(GeneralSecurityException::class)
    fun sign(data: Base64URLData?): Signature?
}