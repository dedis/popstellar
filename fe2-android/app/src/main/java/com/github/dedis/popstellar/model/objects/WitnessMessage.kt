package com.github.dedis.popstellar.model.objects

import com.github.dedis.popstellar.model.Copyable
import com.github.dedis.popstellar.model.objects.security.MessageID
import com.github.dedis.popstellar.model.objects.security.PublicKey
import java.time.Instant
import java.util.Objects

/** Class to model a message that needs to be signed by witnesses  */
class WitnessMessage : Copyable<WitnessMessage?> {
    /** Base 64 URL encoded ID of the message that we want to sign  */
    @JvmField
    val messageId: MessageID

    /** Set of witnesses that have signed the message  */
    private val witnesses: MutableSet<PublicKey>

    /** Title that will be displayed for the message  */
    @JvmField
    var title = ""

    /** Description that will be displayed for the message  */
    @JvmField
    var description = ""

    /** Timestamp of the creation of the witnessing message used to sort messages by most recent  */
    @JvmField
    val timestamp: Long

    /**
     * Constructor for a Witness Message
     *
     * @param messageId ID of the message to sign
     */
    constructor(messageId: MessageID) {
        witnesses = HashSet()
        this.messageId = messageId
        timestamp = Instant.now().epochSecond
    }

    constructor(witnessMessage: WitnessMessage) {
        messageId = witnessMessage.messageId
        witnesses = HashSet(witnessMessage.witnesses)
        title = witnessMessage.title
        description = witnessMessage.description
        timestamp = witnessMessage.timestamp
    }

    /**
     * Method to add a new witness that have signed the message
     *
     * @param pk public key of the witness that have signed the message
     */
    @Synchronized
    fun addWitness(pk: PublicKey) {
        witnesses.add(pk)
    }

    @Synchronized
    fun getWitnesses(): Set<PublicKey> {
        return witnesses
    }

    override fun copy(): WitnessMessage {
        return WitnessMessage(this)
    }

    override fun toString(): String {
        return ("WitnessMessage{"
                + "messageId='"
                + messageId
                + '\''
                + ", witnesses="
                + witnesses
                + ", title='"
                + title
                + '\''
                + ", description='"
                + description
                + '\''
                + '}')
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is WitnessMessage) {
            return false
        }
        val that = other
        return messageId == that.messageId && witnesses == that.witnesses
    }

    override fun hashCode(): Int {
        return Objects.hash(messageId, witnesses)
    }
}