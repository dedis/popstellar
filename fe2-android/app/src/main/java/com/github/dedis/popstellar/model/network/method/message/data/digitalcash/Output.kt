package com.github.dedis.popstellar.model.network.method.message.data.digitalcash

import com.github.dedis.popstellar.model.Immutable
import com.google.gson.annotations.SerializedName
import java.util.Objects

// Object representing an output of this transaction
@Immutable
class Output
/**
 * @param value the value of the output transaction, expressed in miniLAOs
 * @param script The script describing the TxOut unlock mechanism
 */(// the value of the output transaction, expressed in miniLAOs
        @JvmField @field:SerializedName("value") val value: Long, // The script describing the TxOut unlock mechanism
        @JvmField @field:SerializedName("script") val script: ScriptOutput) {

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other == null || javaClass != other.javaClass) {
            return false
        }
        val txOut = other as Output
        return value == txOut.value && script == txOut.script
    }

    override fun hashCode(): Int {
        return Objects.hash(value, script)
    }

    override fun toString(): String {
        return "output{value=$value, script=$script}"
    }
}