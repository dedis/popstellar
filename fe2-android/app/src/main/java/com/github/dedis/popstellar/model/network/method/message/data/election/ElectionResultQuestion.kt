package com.github.dedis.popstellar.model.network.method.message.data.election

import com.github.dedis.popstellar.model.Immutable
import java.util.Objects

@Immutable
class ElectionResultQuestion(id: String, result: Set<QuestionResult?>) {
    @JvmField
    val id: String

    @JvmField
    val result: Set<QuestionResult?>

    init {
        require(result.isNotEmpty())
        this.id = id
        this.result = HashSet(result)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other == null || javaClass != other.javaClass) {
            return false
        }
        val that = other as ElectionResultQuestion
        return id == that.id && result == that.result
    }

    override fun hashCode(): Int {
        return Objects.hash(id, result)
    }

    override fun toString(): String {
        return ("ElectionResultQuestion{"
                + "id='"
                + id
                + '\''
                + ", result="
                + result.toTypedArray().contentToString()
                + '}')
    }
}