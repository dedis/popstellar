package com.github.dedis.popstellar.model.network.method.message.data.digitalcash

import com.github.dedis.popstellar.model.Immutable
import com.github.dedis.popstellar.utility.security.Hash
import com.google.gson.annotations.SerializedName
import java.util.Collections
import java.util.Objects

/** The transaction object  */
@Immutable
class Transaction(// The version of the transaction inputs
        @JvmField @field:SerializedName(value = "version") val version: Int,
        inputs: List<Input>?,
        outputs: List<Output>?, // LockTime
        @JvmField @field:SerializedName("lock_time") val lockTime: Long) {

    @SerializedName(value = "inputs")
    private val inputs // [Array[Objects]] array of output transactions to use as inputs
            : List<Input>

    @SerializedName(value = "outputs")
    private val outputs // [Array[Objects]] array of outputs from this transactions
            : List<Output>

    /**
     * Transaction constructor
     *
     * @param version The version of the transaction inputs
     * @param inputs [Array[Objects]] array of output transactions to use as inputs
     * @param outputs [Array[Objects]] array of outputs from this transactions
     * @param lockTime TimeStamp
     */
    init {
        this.inputs = inputs?.let { Collections.unmodifiableList(it) }!!
        this.outputs = outputs?.let { Collections.unmodifiableList(it) }!!
        // change the sig for all the inputs
    }

    fun getInputs(): List<Input> {
        return ArrayList(inputs)
    }

    fun getOutputs(): List<Output> {
        return ArrayList(outputs)
    }

    fun computeId(): String {
        // Make a list all the string in the transaction
        val collectTransaction: MutableList<String?> = ArrayList()
        // Add them in lexicographic order

        // Inputs
        for (currentTxin in inputs) {
            // Script
            // PubKey
            collectTransaction.add(currentTxin.script.pubkey.encoded)
            // Sig
            collectTransaction.add(currentTxin.script.sig.encoded)
            // Type
            collectTransaction.add(currentTxin.script.type)
            // TxOutHash
            collectTransaction.add(currentTxin.txOutHash)
            // TxOutIndex
            collectTransaction.add(currentTxin.txOutIndex.toString())
        }

        // lock_time
        collectTransaction.add(lockTime.toString())
        // Outputs
        for (currentTxout in outputs) {
            // Script
            // PubKeyHash
            collectTransaction.add(currentTxout.script.pubKeyHash)
            // Type
            collectTransaction.add(currentTxout.script.type)
            // Value
            collectTransaction.add(currentTxout.value.toString())
        }
        // Version
        collectTransaction.add(version.toString())

        // Use already implemented hash function
        return Hash.hash(*collectTransaction.toTypedArray())
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other == null || javaClass != other.javaClass) {
            return false
        }
        val that = other as Transaction
        return version == that.version && lockTime == that.lockTime && inputs == that.inputs && outputs == that.outputs
    }

    override fun hashCode(): Int {
        return Objects.hash(version, getInputs(), getOutputs(), lockTime)
    }

    override fun toString(): String {
        return ("Transaction{"
                + "version="
                + version
                + ", inputs="
                + inputs.toTypedArray().contentToString()
                + ", outputs="
                + outputs.toTypedArray().contentToString()
                + ", lock_time="
                + lockTime
                + '}')
    }

    companion object {
        /**
         * Function that given a key pair change the sig of an input considering all the outputs
         *
         * @return sig other all the outputs and inputs with the public key
         */
        fun computeSigOutputsPairTxOutHashAndIndex(
                outputs: List<Output>, inputsPairs: Map<String?, Int>): String {
            // input #1: tx_out_hash Value //input #1: tx_out_index Value
            // input #2: tx_out_hash Value //input #2: tx_out_index Value ...
            // TxOut #1: LaoCoin Value
            // TxOut #1: script.type
            // TxOut #1: script.pubkey_hash Value
            // TxOut #2: LaoCoin Value
            // TxOut #2: script.type Value
            // TxOut #2: script.pubkey_hash
            // Value...
            val sig: MutableList<String?> = ArrayList()
            for ((key, value) in inputsPairs) {
                sig.add(key)
                sig.add(value.toString())
            }
            for (current in outputs) {
                sig.add(current.value.toString())
                sig.add(current.script.type)
                sig.add(current.script.pubKeyHash)
            }
            return java.lang.String.join("", *sig.toTypedArray())
        }
    }
}