package com.github.dedis.popstellar.model.network.method.message.data.popcha

import com.github.dedis.popstellar.model.Immutable
import com.github.dedis.popstellar.model.network.method.message.data.Action
import com.github.dedis.popstellar.model.network.method.message.data.Data
import com.github.dedis.popstellar.model.network.method.message.data.Objects
import com.github.dedis.popstellar.model.objects.security.Base64URLData
import com.google.gson.annotations.SerializedName

/** Data sent to authenticate to a PoPCHA server  */
@Immutable
class PoPCHAAuthentication(
        @JvmField @field:SerializedName("client_id") val clientId: String,
        @JvmField val nonce: String,
        @JvmField val identifier: Base64URLData,
        @JvmField @field:SerializedName("identifier_proof") val identifierProof: Base64URLData,
        @JvmField @field:SerializedName("popcha_address") val popchaAddress: String,
        @JvmField val state: String?,
        @JvmField @field:SerializedName("response_mode") val responseMode: String?) : Data() {

    override val `object`: String
        get() = Objects.POPCHA.`object`
    override val action: String
        get() = Action.AUTH.action

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other == null || javaClass != other.javaClass) {
            return false
        }
        val that = other as PoPCHAAuthentication
        return clientId == that.clientId && nonce == that.nonce && identifier == that.identifier
                && identifierProof == that.identifierProof && state == that.state &&
                responseMode == that.responseMode && popchaAddress == that.popchaAddress
    }

    override fun hashCode(): Int {
        return java.util.Objects.hash(
                clientId, nonce, identifier, identifierProof, state, responseMode, popchaAddress)
    }

    override fun toString(): String {
        return ("PoPCHAAuthentication{"
                + "clientId='"
                + clientId
                + '\''
                + ", nonce='"
                + nonce
                + '\''
                + ", identifier='"
                + identifier
                + '\''
                + ", identifierProof='"
                + identifierProof
                + '\''
                + ", state='"
                + state
                + '\''
                + ", responseMode='"
                + responseMode
                + '\''
                + ", popchaAddress='"
                + popchaAddress
                + '\''
                + '}')
    }
}