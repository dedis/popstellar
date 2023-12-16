package com.github.dedis.popstellar.model.network.method.message.data.election

import com.github.dedis.popstellar.model.network.method.message.data.Action
import com.github.dedis.popstellar.model.network.method.message.data.Data
import com.github.dedis.popstellar.model.network.method.message.data.Objects
import com.github.dedis.popstellar.utility.MessageValidator
import com.google.gson.annotations.SerializedName
import java.time.Instant

class CastVote : Data {
    @SerializedName(value = "created_at")
    val creation // time the votes were submitted
            : Long

    @JvmField
    @SerializedName(value = "lao")
    val laoId // Id of the lao
            : String

    @JvmField
    @SerializedName(value = "election")
    val electionId // Id of the election
            : String

    // Votes, either votes is null or encrypted votes is null depending on the value of the election
    // Type must be specified upon creation of the cast vote (either ElectionVote or
    // ElectionEncryptedVote)
    private val votes: List<Vote>?

    /**
     * @param votes list of the votes to cast (null if this is an OPEN_BALLOT election)
     * @param electionId election id
     * @param laoId lao id
     */
    constructor(
            votes: List<Vote>?, electionId: String, laoId: String) {
        // Lao id and election id are checked to match existing ones in the cast vote handler
        MessageValidator.verify()
                .isNotEmptyBase64(electionId, "election id")
                .isNotEmptyBase64(laoId, "lao id")
                .validVotes(votes)
        creation = Instant.now().epochSecond
        this.electionId = electionId
        this.laoId = laoId
        this.votes = votes
    }

    /**
     * Constructor used while receiving a CastVote message
     *
     * @param votes list of the votes to cast (null if this is an OPEN_BALLOT election)
     * @param electionId election id
     * @param laoId lao id
     * @param createdAt timestamp for creation
     */
    constructor(votes: List<Vote>?, electionId: String, laoId: String, createdAt: Long) {
        // Lao id and election id are checked to match existing ones in the cast vote handler
        MessageValidator.verify()
                .isNotEmptyBase64(electionId, "election id")
                .isNotEmptyBase64(laoId, "lao id")
                .validVotes(votes)
                .validPastTimes(createdAt)
        creation = createdAt
        this.electionId = electionId
        this.laoId = laoId
        this.votes = votes
    }

    fun getVotes(): List<Vote>? {
        return votes?.let { ArrayList(it) }
    }

    override val `object`: String
        get() = Objects.ELECTION.`object`
    override val action: String
        get() = Action.CAST_VOTE.action

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other == null || javaClass != other.javaClass) {
            return false
        }
        val that = other as CastVote
        return laoId == that.laoId && creation == that.creation && electionId == that.electionId
                && laoId == that.laoId && votes == that.getVotes()
    }

    override fun hashCode(): Int {
        return java.util.Objects.hash(laoId, electionId, creation, getVotes())
    }

    override fun toString(): String {
        return ("CastVote{"
                + "createdAt="
                + creation
                + ", laoId='"
                + laoId
                + '\''
                + ", electionId='"
                + electionId
                + '\''
                + ", votes="
                + votes!!.toTypedArray().contentToString()
                + '}')
    }
}