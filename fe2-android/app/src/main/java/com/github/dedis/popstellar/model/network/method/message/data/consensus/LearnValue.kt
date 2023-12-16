package com.github.dedis.popstellar.model.network.method.message.data.consensus

import com.github.dedis.popstellar.model.Immutable
import java.util.Objects

@Immutable
class LearnValue(val isDecision: Boolean) {

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other == null || javaClass != other.javaClass) {
            return false
        }
        val that = other as LearnValue
        return isDecision == that.isDecision
    }

    override fun hashCode(): Int {
        return Objects.hash(isDecision)
    }

    override fun toString(): String {
        return String.format("LearnValue{decision=%b}", isDecision)
    }
}