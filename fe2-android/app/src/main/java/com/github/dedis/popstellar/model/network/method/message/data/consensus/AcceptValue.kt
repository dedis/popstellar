package com.github.dedis.popstellar.model.network.method.message.data.consensus

import com.github.dedis.popstellar.model.Immutable
import com.google.gson.annotations.SerializedName
import java.util.Objects

@Immutable
class AcceptValue(
    @field:SerializedName("accepted_try") val acceptedTry: Int,
    @field:SerializedName("accepted_value") val isAcceptedValue: Boolean
) {

  override fun equals(other: Any?): Boolean {
    if (this === other) {
      return true
    }
    if (other == null || javaClass != other.javaClass) {
      return false
    }
    val that = other as AcceptValue
    return acceptedTry == that.acceptedTry && isAcceptedValue == that.isAcceptedValue
  }

  override fun hashCode(): Int {
    return Objects.hash(acceptedTry, isAcceptedValue)
  }

  override fun toString(): String {
    return "AcceptValue{accepted_try=$acceptedTry, accepted_value=$isAcceptedValue}"
  }
}
