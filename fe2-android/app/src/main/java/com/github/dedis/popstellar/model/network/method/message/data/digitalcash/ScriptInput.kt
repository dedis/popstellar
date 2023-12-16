package com.github.dedis.popstellar.model.network.method.message.data.digitalcash

import com.github.dedis.popstellar.model.Immutable
import com.github.dedis.popstellar.model.objects.security.PublicKey
import com.github.dedis.popstellar.model.objects.security.Signature
import com.google.gson.annotations.SerializedName
import java.util.Objects

// The script describing the unlock mechanism
@Immutable
class ScriptInput
/**
 * @param type The script describing the unlock mechanism
 * @param pubKeyRecipient The recipient’s public key
 * @param sig Signature on all txins and txouts using the recipient's private key
 */(// The script describing the unlock mechanism
        @JvmField @field:SerializedName("type") val type: String, // The recipient’s public key
        @field:SerializedName("pubkey") val pubkey: PublicKey, // Signature on all txins and txouts using the recipient's private key
        @JvmField @field:SerializedName("sig") val sig: Signature) {

    // Transaction //with all txin txout
    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other == null || javaClass != other.javaClass) {
            return false
        }
        val that = other as ScriptInput
        return type == that.type && pubkey == that.pubkey && sig == that.sig
    }

    override fun hashCode(): Int {
        return Objects.hash(type, pubkey, sig)
    }

    override fun toString(): String {
        return ("script{"
                + "type='"
                + type
                + '\''
                + ", pubkey='"
                + pubkey
                + '\''
                + ", sig='"
                + sig
                + '\''
                + '}')
    }
}