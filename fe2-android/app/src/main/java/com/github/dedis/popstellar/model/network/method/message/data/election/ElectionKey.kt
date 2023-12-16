package com.github.dedis.popstellar.model.network.method.message.data.election

import com.github.dedis.popstellar.model.Immutable
import com.github.dedis.popstellar.model.network.method.message.data.Action
import com.github.dedis.popstellar.model.network.method.message.data.Data
import com.github.dedis.popstellar.model.network.method.message.data.Objects
import com.github.dedis.popstellar.utility.MessageValidator
import com.google.gson.annotations.SerializedName

/**
 * ElectionKey message is sent by the backend after opening an election with SECRET_BALLOT option,
 * it should contain the public key to encrypt the votes.
 */
@Immutable
class ElectionKey(electionId: String, electionVoteKey: String) : Data() {
    // Id of the election
    @JvmField
    @SerializedName("election")
    val electionId: String

    // Public key of the election for casting encrypted votes
    @JvmField
    @SerializedName("election_key")
    val electionVoteKey: String

    init {
        MessageValidator.verify()
                .isNotEmptyBase64(electionId, "election id")
                .isNotEmptyBase64(electionVoteKey, "election vote key")
        this.electionId = electionId
        this.electionVoteKey = electionVoteKey
    }

    override val action: String
        get() = Action.KEY.action
    override val `object`: String
        get() = Objects.ELECTION.`object`

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other == null || javaClass != other.javaClass) {
            return false
        }
        val that = other as ElectionKey
        return electionVoteKey == that.electionVoteKey && electionId == that.electionId
    }

    override fun hashCode(): Int {
        return java.util.Objects.hash(electionId, electionVoteKey)
    }

    override fun toString(): String {
        return String.format(
                "ElectionKey{election='%s', election_key='%s'}", electionId, electionVoteKey)
    }
}