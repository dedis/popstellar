package com.github.dedis.popstellar.model.objects.security.elGamal

import ch.epfl.dedis.lib.crypto.Ed25519Point
import ch.epfl.dedis.lib.crypto.Ed25519Scalar
import ch.epfl.dedis.lib.crypto.Scalar
import ch.epfl.dedis.lib.exception.CothorityCryptoException
import com.github.dedis.popstellar.model.objects.security.Base64URLData
import java.util.Objects

class ElectionPrivateKey(privateKey: Base64URLData) {
    // Scalar generated with the private key
    private val privateKey: Ed25519Scalar

    /**
     * Create an decryption scheme for 64 bytes message using a private key
     *
     * @param privateKey private key used to decrypt
     */
    init {
        this.privateKey = Ed25519Scalar(privateKey.getData())
    }

    override fun toString(): String {
        return privateKey.toString()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other == null || javaClass != other.javaClass) {
            return false
        }
        val that = other as ElectionPrivateKey
        return that.getPrivateKey().equals(getPrivateKey())
    }

    /** Added this hash behavior by default  */
    override fun hashCode(): Int {
        return Objects.hash(getPrivateKey().toString())
    }

    fun getPrivateKey(): Scalar {
        return privateKey
    }

    /**
     * Decypt the given string using ElGamal
     *
     * @param message message encrypted in Base64 to decrypt (64 byte length)
     * @return byte array containing the decrypted message
     * @throws CothorityCryptoException if problem while transforming final data into a byte array
     */
    @Throws(CothorityCryptoException::class)
    fun decrypt(message: String): ByteArray {
        val decoded = Base64URLData(message)
        // Follows this implementation:
        // https://github.com/dedis/cothority/blob/0299bcd78bab22bde6d6449b1594613987355535/evoting/lib/elgamal.go#L27-L31
        val byteMessage = decoded.getData()
        require(byteMessage.size == MESSAGE_BYTE_SIZE) { "Your message to decrypt should contain exactly 64 bytes" }

        // Decompose into two bytes array for recuperating both C and K
        val Kbytes = ByteArray(HALF_MESSAGE_BYTE_SIZE)
        val Cbytes = ByteArray(HALF_MESSAGE_BYTE_SIZE)
        for (i in 0 until MESSAGE_BYTE_SIZE) {
            if (i < HALF_MESSAGE_BYTE_SIZE) {
                Kbytes[i] = byteMessage[i]
            } else {
                Cbytes[i - HALF_MESSAGE_BYTE_SIZE] = byteMessage[i]
            }
        }
        val K: Ed25519Point
        val C: Ed25519Point
        try {
            K = Ed25519Point(Kbytes)
            C = Ed25519Point(Cbytes)
        } catch (e: CothorityCryptoException) {
            throw IllegalArgumentException("Could not create K Point while decrypting")
        }

        // Export data to get the original message
        val S = K.mul(getPrivateKey())
        return S.add(C.negate()).data()
    }

    companion object {
        private const val MESSAGE_BYTE_SIZE = 64
        private const val HALF_MESSAGE_BYTE_SIZE = 32
    }
}