package com.github.dedis.popstellar.model.network.answer

import com.github.dedis.popstellar.model.network.method.message.MessageGeneral
import java.util.Objects

/** A succeed query's answer with a list of MessageGeneral */
class ResultMessages
/**
 * Constructor of a ResultMessages
 *
 * @param id of the answer
 * @param messages of the answer
 */
(id: Int, val messages: List<MessageGeneral>) : Result(id) {

  override fun equals(other: Any?): Boolean {
    if (this === other) {
      return true
    }
    if (other == null || javaClass != other.javaClass) {
      return false
    }
    val that = other as ResultMessages
    return id == that.id && messages == that.messages
  }

  override fun hashCode(): Int {
    return Objects.hash(id, messages)
  }

  override fun toString(): String {
    return "ResultMessages{messages=${messages.toTypedArray().contentToString()}}"
  }
}
