package com.github.dedis.popstellar.model.network.method.message.data.consensus

import com.github.dedis.popstellar.model.Immutable
import com.github.dedis.popstellar.model.network.method.message.data.Action
import com.github.dedis.popstellar.model.network.method.message.data.Data
import com.github.dedis.popstellar.model.network.method.message.data.Objects
import com.github.dedis.popstellar.model.objects.security.MessageID
import com.google.gson.annotations.SerializedName
import java.util.Collections

@Immutable
class ConsensusLearn(
        @JvmField @field:SerializedName("instance_id") val instanceId: String,
        @JvmField @field:SerializedName("message_id") val messageId: MessageID,
        @JvmField @field:SerializedName("created_at") val creation: Long,
        decision: Boolean,
        acceptorSignatures: List<String>?) : Data() {

    @JvmField
    @SerializedName("value")
    val learnValue: LearnValue

    @JvmField
    @SerializedName("acceptor-signatures")
    val acceptorSignatures: List<String>

    /**
     * Constructor for a data Learn
     *
     * @param instanceId unique id of the consensus instance
     * @param messageId message id of the Elect message
     * @param creation UNIX timestamp in UTC
     * @param decision true if the consensus was successful
     * @param acceptorSignatures signatures of all the received Accept messages
     */
    init {
        learnValue = LearnValue(decision)
        this.acceptorSignatures = acceptorSignatures?.let { Collections.unmodifiableList(it) }!!
    }

    override val `object`: String
        get() = Objects.CONSENSUS.`object`
    override val action: String
        get() = Action.LEARN.action

    override fun hashCode(): Int {
        return java.util.Objects.hash(instanceId, messageId, creation, learnValue, acceptorSignatures)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other == null || javaClass != other.javaClass) {
            return false
        }
        val that = other as ConsensusLearn
        return creation == that.creation && instanceId == that.instanceId && messageId == that.messageId && learnValue == that.learnValue && acceptorSignatures == that.acceptorSignatures
    }

    override fun toString(): String {
        return String.format(
                "ConsensusLearn{instance_id='%s', message_id='%s', acceptor-signatures=%s}",
                instanceId, messageId.encoded, acceptorSignatures)
    }
}