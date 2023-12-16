package com.github.dedis.popstellar.model.objects.security

import com.github.dedis.popstellar.model.Immutable
import java.nio.charset.StandardCharsets
import java.util.Base64

/** Represents a data that can be encoded into a Base64 form  */
@Immutable
open class Base64URLData(data: ByteArray) {
    protected val data: ByteArray

    init {
        // Deep copy of byte array
        this.data = data.copyOf(data.size)
    }

    constructor(data: String) : this(decode(data))

    fun getData(): ByteArray {
        return data.copyOf(data.size)
    }

    val encoded: String
        /**
         * @return the Base64 - encoded string representation of the data
         */
        get() = encode(data)

    override fun toString(): String {
        return javaClass.simpleName + "(" + encoded + ")"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) {
            return false
        }
        val that = other as Base64URLData
        return data.contentEquals(that.data)
    }

    override fun hashCode(): Int {
        return data.contentHashCode()
    }

    companion object {
        private fun decode(data: String): ByteArray {
            return Base64.getUrlDecoder().decode(data)
        }

        @JvmStatic
        fun encode(data: String): String {
            return encode(data.toByteArray(StandardCharsets.UTF_8))
        }

        private fun encode(data: ByteArray): String {
            return Base64.getUrlEncoder().encodeToString(data)
        }
    }
}