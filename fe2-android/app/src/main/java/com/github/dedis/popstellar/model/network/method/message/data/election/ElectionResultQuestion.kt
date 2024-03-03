package com.github.dedis.popstellar.model.network.method.message.data.election

import com.github.dedis.popstellar.model.Immutable
import com.github.dedis.popstellar.utility.MessageValidator.verify
import java.util.Objects

@Immutable
class ElectionResultQuestion(id: String, result: Set<QuestionResult>) {
  val id: String
  val result: Set<QuestionResult>

  init {
    // Set can't have duplicates, so no need to check for duplicates
    // QuestionResult are already validated when constructed
    verify().stringNotEmpty(id, "election id").listNotEmpty(result.toList())

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
    return "ElectionResultQuestion{id='$id', result=${result.toTypedArray().contentToString()}}"
  }
}
