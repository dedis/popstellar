package com.github.dedis.popstellar.model.network.method.message.data.election

import com.github.dedis.popstellar.model.Immutable
import com.github.dedis.popstellar.model.network.method.message.data.Action
import com.github.dedis.popstellar.model.network.method.message.data.Data
import com.github.dedis.popstellar.model.network.method.message.data.Objects
import com.github.dedis.popstellar.utility.MessageValidator
import com.google.gson.annotations.SerializedName

@Immutable
class ElectionOpen(laoId: String, electionId: String, openedAt: Long) : Data() {
    @JvmField
    @SerializedName("lao")
    val laoId: String

    @JvmField
    @SerializedName("election")
    val electionId: String

    @JvmField
    @SerializedName("opened_at")
    val openedAt: Long

    /**
     * @param laoId id of the LAO
     * @param electionId id of the election
     * @param openedAt timestamp of election opening
     */
    init {
        // The election open handler checks that lao and election id match with an existing lao
        MessageValidator.verify()
                .isNotEmptyBase64(laoId, "lao id")
                .isNotEmptyBase64(electionId, "election id")
                .validPastTimes(openedAt)
        this.laoId = laoId
        this.electionId = electionId
        this.openedAt = openedAt
    }

    override val `object`: String
        get() = Objects.ELECTION.`object`
    override val action: String
        get() = Action.OPEN.action

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other == null || javaClass != other.javaClass) {
            return false
        }
        val election = other as ElectionOpen
        return openedAt == election.openedAt && laoId == election.laoId && electionId == election.electionId
    }

    override fun hashCode(): Int {
        return java.util.Objects.hash(laoId, electionId, openedAt)
    }

    override fun toString(): String {
        return String.format(
                "ElectionOpen{lao='%s', election='%s', opened_at=%s}", laoId, electionId, openedAt)
    }
}