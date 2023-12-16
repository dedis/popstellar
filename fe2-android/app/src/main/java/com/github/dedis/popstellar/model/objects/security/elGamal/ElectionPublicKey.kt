package com.github.dedis.popstellar.model.objects.security.elGamal

import ch.epfl.dedis.lib.crypto.Ed25519
import ch.epfl.dedis.lib.crypto.Ed25519Point
import ch.epfl.dedis.lib.crypto.Ed25519Scalar
import ch.epfl.dedis.lib.crypto.Point
import ch.epfl.dedis.lib.crypto.Scalar
import ch.epfl.dedis.lib.exception.CothorityCryptoException
import com.github.dedis.popstellar.model.objects.security.Base64URLData
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.security.SecureRandom
import java.util.Objects

/** Represents a private key meant for El-Gam  */
class ElectionPublicKey(base64publicKey: Base64URLData) {
    // Point is generate with given public key
    @JvmField
    var publicKey: Point? = null

    init {
        publicKey = try {
            Ed25519Point(base64publicKey.getData())
        } catch (e: CothorityCryptoException) {
            throw IllegalArgumentException(
                    "Could not create the point for elliptic curve, please provide another key")
        }
    }

    override fun toString(): String {
        return publicKey.toString()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other == null || javaClass != other.javaClass) {
            return false
        }
        val that = other as ElectionPublicKey
        return that.publicKey == publicKey
    }

    /**
     * @return the string encoded public key in Base64 format
     */
    fun encodeToBase64(): String {
        val encodedKey = Base64URLData(publicKey!!.toBytes())
        return encodedKey.encoded
    }

    /**
     * @return the public key in Base64 format (not encoded)
     */
    fun toBase64(): Base64URLData {
        return Base64URLData(publicKey!!.toBytes())
    }

    /** Added this hash behavior by default  */
    override fun hashCode(): Int {
        return Objects.hash(publicKey.toString())
    }

    /**
     * Encrypts with ElGamal under Ed25519 curve
     *
     * @param message message a 2 byte integer corresponding to the chosen vote
     * @return Returns 2 32 byte strings which should be appended together and base64 encoded. For
     * clarification, the first 32 bytes is point K and the second 32 bytes is point C.
     */
    fun encrypt(message: ByteArray): String? {
        require(message.size <= 29) { "The message should contain at maximum 29 bytes" }
        // Follows this implementation:
        // https://github.com/dedis/cothority/blob/0299bcd78bab22bde6d6449b1594613987355535/evoting/lib/elgamal.go#L15-L23
        // An example for embedding was found here:
        // https://github.com/dedis/cothority/blob/0299bcd78bab22bde6d6449b1594613987355535/external/js/kyber/spec/group/edwards25519/point.spec.ts#L203

        // Proper embedding with overflowing byte array (len > 32) will need to be changed later
        val M = Ed25519Point.embed(message)

        // ElGamal-encrypt the point to produce ciphertext (K,C).
        val seed = ByteArray(Ed25519.field.getb() / 8)
        SecureRandom().nextBytes(seed)
        val k: Scalar = Ed25519Scalar(seed)
        val K = Ed25519Point.base().mul(k)
        val S = publicKey!!.mul(k)
        val C = S.add(M)

        // Concat K and C and encodes it in Base64
        // IO exception no testable
        val result: ByteArray? = try {
            val output = ByteArrayOutputStream()
            output.write(K.toBytes())
            output.write(C.toBytes())
            output.toByteArray()
        } catch (e: IOException) {
            Timber.tag(TAG)
                    .d(
                            "Something happened during the encryption, could NOT concatenate the final result into a byte array")
            null
        }
        if (Objects.isNull(result)) {
            return null
        }
        val encodedResult = Base64URLData(result!!)
        return encodedResult.encoded
    }

    companion object {
        private val TAG = ElectionPublicKey::class.java.simpleName
    }
}