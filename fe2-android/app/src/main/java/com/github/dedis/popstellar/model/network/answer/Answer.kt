package com.github.dedis.popstellar.model.network.answer

import com.github.dedis.popstellar.model.network.GenericMessage
import java.util.Objects

/**
 * An abstract result from a request
 *
 * Is linked to an earlier request with a unique id
 */
abstract class Answer
/**
 * Constructor of an Answer
 *
 * @param id of the answer
 */
(
    /** Returns the ID of the answer */
    val id: Int
) : GenericMessage() {

  override fun equals(other: Any?): Boolean {
    if (this === other) {
      return true
    }
    if (other == null || javaClass != other.javaClass) {
      return false
    }
    val answer = other as Answer
    return id == answer.id
  }

  override fun hashCode(): Int {
    return Objects.hash(id)
  }
}
