package com.github.dedis.popstellar.model.network.method.message.data.consensus

import com.github.dedis.popstellar.model.Immutable
import com.google.gson.annotations.SerializedName
import java.util.Objects

@Immutable
class ProposeValue(
    @field:SerializedName("proposed_try") val proposedTry: Int,
    @field:SerializedName("proposed_value") val isProposedValue: Boolean
) {

  override fun equals(other: Any?): Boolean {
    if (this === other) {
      return true
    }
    if (other == null || javaClass != other.javaClass) {
      return false
    }
    val that = other as ProposeValue
    return proposedTry == that.proposedTry && isProposedValue == that.isProposedValue
  }

  override fun hashCode(): Int {
    return Objects.hash(proposedTry, isProposedValue)
  }

  override fun toString(): String {
    return "ProposeValue{proposed_try=$proposedTry, proposed_value=$isProposedValue}"
  }
}
