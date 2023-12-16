package com.github.dedis.popstellar.model.network.method.message.data.digitalcash

import com.github.dedis.popstellar.model.Immutable
import com.google.gson.annotations.SerializedName
import java.util.Objects

// The script describing the TxOut unlock mechanism
@Immutable
class ScriptOutput
/**
 * @param type Type of script
 * @param pubKeyHash Hash of the recipient’s public key
 */(// Type of script
        @JvmField @field:SerializedName("type") val type: String, // Hash of the recipient’s public key
        @JvmField @field:SerializedName("pubkey_hash") val pubKeyHash: String) {

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other == null || javaClass != other.javaClass) {
            return false
        }
        val that = other as ScriptOutput
        return type == that.type && pubKeyHash == that.pubKeyHash
    }

    override fun hashCode(): Int {
        return Objects.hash(type, pubKeyHash)
    }

    override fun toString(): String {
        return "script{type='$type', pubkey_hash='$pubKeyHash'}"
    }
}