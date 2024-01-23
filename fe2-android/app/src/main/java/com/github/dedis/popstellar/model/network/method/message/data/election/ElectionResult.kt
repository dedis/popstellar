package com.github.dedis.popstellar.model.network.method.message.data.election

import com.github.dedis.popstellar.model.Immutable
import com.github.dedis.popstellar.model.network.method.message.data.Action
import com.github.dedis.popstellar.model.network.method.message.data.Data
import com.github.dedis.popstellar.model.network.method.message.data.Objects
import com.github.dedis.popstellar.utility.MessageValidator.verify
import java.util.Collections

@Immutable
class ElectionResult(questions: List<ElectionResultQuestion>?) : Data() {
  val questions: List<ElectionResultQuestion>

  init {
    verify().listNotEmpty(questions).noListDuplicates(questions!!)

    this.questions = Collections.unmodifiableList(questions)
  }

  override val `object`: String
    get() = Objects.ELECTION.`object`

  override val action: String
    get() = Action.RESULT.action

  override fun equals(other: Any?): Boolean {
    if (this === other) {
      return true
    }
    if (other == null || javaClass != other.javaClass) {
      return false
    }
    val that = other as ElectionResult
    return questions == that.questions
  }

  override fun hashCode(): Int {
    return java.util.Objects.hash(questions)
  }

  override fun toString(): String {
    return "ElectionResult{questions=${questions.toTypedArray().contentToString()}}"
  }
}
