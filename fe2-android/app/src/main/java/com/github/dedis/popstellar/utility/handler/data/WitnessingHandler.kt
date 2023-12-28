package com.github.dedis.popstellar.utility.handler.data

import com.github.dedis.popstellar.model.network.method.message.data.message.WitnessMessageSignature
import com.github.dedis.popstellar.repository.LAORepository
import com.github.dedis.popstellar.repository.WitnessingRepository
import com.github.dedis.popstellar.utility.error.DataHandlingException
import com.github.dedis.popstellar.utility.error.InvalidSignatureException
import com.github.dedis.popstellar.utility.error.InvalidWitnessingException
import com.github.dedis.popstellar.utility.error.UnknownLaoException
import timber.log.Timber
import javax.inject.Inject

class WitnessingHandler @Inject constructor(
    private val laoRepo: LAORepository,
    private val witnessingRepo: WitnessingRepository
) {
    /**
     * Process a WitnessMessageSignature message.
     *
     * @param context the HandlerContext of the message
     * @param witnessMessageSignature the message that was received
     */
    @Throws(UnknownLaoException::class, DataHandlingException::class)
    fun handleWitnessMessageSignature(
        context: HandlerContext, witnessMessageSignature: WitnessMessageSignature
    ) {
        Timber.tag(TAG)
            .d("Received a WitnessMessageSignature Broadcast msg: %s", witnessMessageSignature)
        val channel = context.channel
        val laoId = laoRepo.getLaoViewByChannel(channel).id
        val witnessPk = context.senderPk

        // Check that the sender of the message is a witness
        if (!witnessingRepo.isWitness(laoId, witnessPk)) {
            throw InvalidWitnessingException(witnessMessageSignature, witnessPk)
        }

        // Check that the signature of the message id is correct
        val messageID = witnessMessageSignature.messageId
        val signature = witnessMessageSignature.signature
        if (!witnessPk.verify(signature, messageID)) {
            throw InvalidSignatureException(witnessMessageSignature, signature)
        }

        // Add the witness to the witness message in the repository
        if (!witnessingRepo.addWitnessToMessage(laoId, messageID, witnessPk)) {
            throw InvalidWitnessingException(witnessMessageSignature, messageID)
        }
    }

    companion object {
        val TAG: String = WitnessingHandler::class.java.simpleName
    }
}