package com.github.dedis.popstellar.model.network.method.message.data.consensus

import com.github.dedis.popstellar.model.Immutable
import com.google.gson.annotations.SerializedName
import java.util.Objects

@Immutable
class PrepareValue(@JvmField @field:SerializedName("proposed_try") val proposedTry: Int) {

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other == null || javaClass != other.javaClass) {
            return false
        }
        val that = other as PrepareValue
        return proposedTry == that.proposedTry
    }

    override fun hashCode(): Int {
        return Objects.hash(proposedTry)
    }

    override fun toString(): String {
        return String.format("PrepareValue{proposed_try=%s}", proposedTry)
    }
}