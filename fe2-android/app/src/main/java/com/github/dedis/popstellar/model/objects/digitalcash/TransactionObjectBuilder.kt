package com.github.dedis.popstellar.model.objects.digitalcash

import com.github.dedis.popstellar.model.objects.Channel
import com.github.dedis.popstellar.model.objects.InputObject
import com.github.dedis.popstellar.model.objects.OutputObject

class TransactionObjectBuilder {
    private var channel: Channel? = null

    // version
    private var version = 0

    // inputs
    private var inputs: List<InputObject>? = null

    // outputs
    private var outputs: List<OutputObject>? = null

    // lock_time
    private var lockTime: Long = 0
    private var transactionId: String? = null
    fun setChannel(channel: Channel?): TransactionObjectBuilder {
        this.channel = channel
        return this
    }

    fun setVersion(version: Int): TransactionObjectBuilder {
        this.version = version
        return this
    }

    fun setInputs(inputs: List<InputObject>?): TransactionObjectBuilder {
        this.inputs = inputs
        return this
    }

    fun setOutputs(outputs: List<OutputObject>?): TransactionObjectBuilder {
        this.outputs = outputs
        return this
    }

    fun setLockTime(lockTime: Long): TransactionObjectBuilder {
        this.lockTime = lockTime
        return this
    }

    fun setTransactionId(transactionId: String?): TransactionObjectBuilder {
        this.transactionId = transactionId
        return this
    }

    fun build(): TransactionObject {
        checkNotNull(channel) { "channel is null" }
        checkNotNull(inputs) { "inputs is null" }
        checkNotNull(outputs) { "outputs is null" }
        checkNotNull(transactionId) { "transactionId is null" }
        return TransactionObject(channel!!, version, inputs, outputs, lockTime, transactionId!!)
    }
}