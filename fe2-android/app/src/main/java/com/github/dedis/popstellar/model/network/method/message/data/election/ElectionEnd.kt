package com.github.dedis.popstellar.model.network.method.message.data.election

import com.github.dedis.popstellar.model.Immutable
import com.github.dedis.popstellar.model.network.method.message.data.Action
import com.github.dedis.popstellar.model.network.method.message.data.Data
import com.github.dedis.popstellar.model.network.method.message.data.Objects
import com.github.dedis.popstellar.utility.MessageValidator
import com.google.gson.annotations.SerializedName
import java.time.Instant

@Immutable
class ElectionEnd(
        electionId: String, laoId: String, registeredVotes: String) : Data() {
    @JvmField
    @SerializedName(value = "election")
    val electionId: String

    @SerializedName(value = "created_at")
    val createdAt: Long

    @JvmField
    @SerializedName(value = "lao")
    val laoId: String

    @JvmField
    @SerializedName(value = "registered_votes")
    val registeredVotes // hashed
            : String

    init {
        MessageValidator.verify()
                .isNotEmptyBase64(electionId, "election id")
                .isNotEmptyBase64(laoId, "laoId")
                .isBase64(registeredVotes, "registered votes")
        createdAt = Instant.now().epochSecond
        this.electionId = electionId
        this.laoId = laoId
        this.registeredVotes = registeredVotes
    }

    override val `object`: String
        get() = Objects.ELECTION.`object`
    override val action: String
        get() = Action.END.action

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other == null || javaClass != other.javaClass) {
            return false
        }
        val that = other as ElectionEnd
        return createdAt == that.createdAt && electionId == that.electionId && laoId == that.laoId && registeredVotes == that.registeredVotes
    }

    override fun hashCode(): Int {
        return java.util.Objects.hash(electionId, createdAt, laoId, registeredVotes)
    }

    override fun toString(): String {
        return ("ElectionEnd{"
                + "electionId='"
                + electionId
                + '\''
                + ", createdAt="
                + createdAt
                + ", laoId='"
                + laoId
                + '\''
                + ", registeredVotes='"
                + registeredVotes
                + '\''
                + '}')
    }
}