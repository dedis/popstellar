package com.github.dedis.popstellar.model.network.method.message.data.consensus

import com.github.dedis.popstellar.model.Immutable
import java.util.Objects

@Immutable
class ConsensusKey(val type: String, val id: String, val property: String) {

  override fun hashCode(): Int {
    return Objects.hash(type, id, property)
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) {
      return true
    }
    if (other == null || javaClass != other.javaClass) {
      return false
    }
    val that = other as ConsensusKey
    return type == that.type && id == that.id && property == that.property
  }

  override fun toString(): String {
    return "ConsensusKey{id='$id', type='$type', property='$property'}"
  }
}
