package com.github.dedis.popstellar.model.objects

import com.github.dedis.popstellar.model.Immutable
import com.google.gson.annotations.SerializedName
import java.util.Objects

/** Represents an peer address  */
@Immutable
class PeerAddress(
        // Purpose of this class is that, in the future, the content of the peers field will contain
        // additional fields: peers: "type": "array", "items": {"type": "object", [...] }
        // ex: peers: {address: "x", type:"organizer"}, {address: "x", type:"witness"}]*/
        @JvmField @field:SerializedName("address") val address: String) {

    override fun toString(): String {
        return "{address:='$address'}"
    }

    override fun hashCode(): Int {
        return Objects.hash(address)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other == null || javaClass != other.javaClass) {
            return false
        }
        val that = other as PeerAddress
        return that.address == address
    }
}