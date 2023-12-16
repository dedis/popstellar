package com.github.dedis.popstellar.model.objects.security.elGamal

import ch.epfl.dedis.lib.crypto.Ed25519Pair
import ch.epfl.dedis.lib.exception.CothorityCryptoException
import com.github.dedis.popstellar.model.objects.security.Base64URLData

class ElectionKeyPair(
        @JvmField val encryptionScheme: ElectionPublicKey, // Set basic getters useful for encrypt/decrypt
        @JvmField val decryptionScheme: ElectionPrivateKey) {

    companion object {
        /**
         * Generate public / private keys for decryption / encryption
         *
         * @return ElectionKeyPair set
         * (ElectionPublicKey=encryptionScheme,ElectionPrivateKey=encryptionScheme)
         * @throws CothorityCryptoException
         */
        @JvmStatic
        fun generateKeyPair(): ElectionKeyPair {
            // Generate at random using Ed25519Pair class
            val keyPairScheme = Ed25519Pair()
            val encodedPublic = Base64URLData(keyPairScheme.point.toBytes())
            val privateKeyPrivate = Base64URLData(keyPairScheme.scalar.toBytes())
            val draftEncScheme = ElectionPublicKey(encodedPublic)
            val draftDecScheme = ElectionPrivateKey(privateKeyPrivate)
            return ElectionKeyPair(draftEncScheme, draftDecScheme)
        }
    }
}