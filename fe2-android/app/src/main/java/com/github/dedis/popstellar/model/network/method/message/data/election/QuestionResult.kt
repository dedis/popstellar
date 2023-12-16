package com.github.dedis.popstellar.model.network.method.message.data.election

import com.github.dedis.popstellar.model.Immutable
import com.google.gson.annotations.SerializedName
import java.util.Objects

@Immutable
class QuestionResult(ballotOption: String?, count: Int) {
    @SerializedName(value = "ballot_option")
    val ballot: String

    @JvmField
    val count: Int

    init {
        requireNotNull(ballotOption)
        ballot = ballotOption
        this.count = count
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other == null || javaClass != other.javaClass) {
            return false
        }
        val that = other as QuestionResult
        return count == that.count && ballot == that.ballot
    }

    override fun hashCode(): Int {
        return Objects.hash(ballot, count)
    }

    override fun toString(): String {
        return "QuestionResult{ballotOption='$ballot', count=$count}"
    }
}