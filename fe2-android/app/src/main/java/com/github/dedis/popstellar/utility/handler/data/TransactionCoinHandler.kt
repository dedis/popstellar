package com.github.dedis.popstellar.utility.handler.data

import com.github.dedis.popstellar.model.network.method.message.data.digitalcash.Input
import com.github.dedis.popstellar.model.network.method.message.data.digitalcash.Output
import com.github.dedis.popstellar.model.network.method.message.data.digitalcash.PostTransactionCoin
import com.github.dedis.popstellar.model.objects.InputObject
import com.github.dedis.popstellar.model.objects.OutputObject
import com.github.dedis.popstellar.model.objects.digitalcash.ScriptInputObject
import com.github.dedis.popstellar.model.objects.digitalcash.ScriptOutputObject
import com.github.dedis.popstellar.model.objects.digitalcash.TransactionObjectBuilder
import com.github.dedis.popstellar.repository.DigitalCashRepository
import com.github.dedis.popstellar.utility.error.keys.NoRollCallException
import timber.log.Timber
import javax.inject.Inject

class TransactionCoinHandler @Inject constructor(private val digitalCashRepo: DigitalCashRepository) {
    /**
     * Process an PostTransactionCoin.
     *
     * @param context the HandlerContext of the message
     * @param postTransactionCoin the data of the message that was received
     */
    @Throws(NoRollCallException::class)
    fun handlePostTransactionCoin(
        context: HandlerContext, postTransactionCoin: PostTransactionCoin
    ) {
        val channel = context.channel
        Timber.tag(TAG)
            .d("handlePostTransactionCoin: channel: %s, msg: %s", channel, postTransactionCoin)

        // inputs and outputs for the creation
        val inputs: MutableList<InputObject> = ArrayList()
        val outputs: MutableList<OutputObject> = ArrayList()

        // Should always be at least one input and one output
        require(
            !(postTransactionCoin.transaction.inputs.isEmpty()
                    || postTransactionCoin.transaction.outputs.isEmpty())
        )

        // Iterate on the inputs and the outputs
        val iteratorInput: Iterator<Input> = postTransactionCoin.transaction.inputs.iterator()
        val iteratorOutput: Iterator<Output> = postTransactionCoin.transaction.outputs.iterator()
        while (iteratorInput.hasNext()) {
            val current = iteratorInput.next()
            // Normally if there is an input there is always a script
            requireNotNull(current.script)
            val scriptInputObject = ScriptInputObject(
                current.script.type,
                current.script.pubkey,
                current.script.sig
            )
            inputs.add(
                InputObject(current.txOutHash, current.txOutIndex, scriptInputObject)
            )
        }
        while (iteratorOutput.hasNext()) {
            // Normally if there is an output there is always a script
            val current = iteratorOutput.next()
            requireNotNull(current.script)
            val script = ScriptOutputObject(
                current.script.type, current.script.pubKeyHash
            )
            outputs.add(OutputObject(current.value, script))
        }
        val transactionObject = TransactionObjectBuilder()
            .setChannel(channel)
            .setLockTime(postTransactionCoin.transaction.lockTime)
            .setVersion(postTransactionCoin.transaction.version)
            .setTransactionId(postTransactionCoin.transactionId)
            .setInputs(inputs)
            .setOutputs(outputs)
            .build()
        digitalCashRepo.updateTransactions(channel.extractLaoId(), transactionObject)
    }

    companion object {
        private val TAG = TransactionCoinHandler::class.java.simpleName
    }
}