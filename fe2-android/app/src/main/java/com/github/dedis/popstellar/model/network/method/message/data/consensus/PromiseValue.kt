package com.github.dedis.popstellar.model.network.method.message.data.consensus

import com.github.dedis.popstellar.model.Immutable
import com.google.gson.annotations.SerializedName
import java.util.Objects

@Immutable
class PromiseValue(
    @field:SerializedName("accepted_try") val acceptedTry: Int,
    @field:SerializedName("accepted_value") val isAcceptedValue: Boolean,
    @field:SerializedName("promised_try") val promisedTry: Int
) {

  override fun equals(other: Any?): Boolean {
    if (this === other) {
      return true
    }
    if (other == null || javaClass != other.javaClass) {
      return false
    }
    val that = other as PromiseValue
    return acceptedTry == that.acceptedTry &&
        isAcceptedValue == that.isAcceptedValue &&
        promisedTry == that.promisedTry
  }

  override fun hashCode(): Int {
    return Objects.hash(acceptedTry, isAcceptedValue, promisedTry)
  }

  override fun toString(): String {
    return "PromiseValue{accepted_try=$acceptedTry, accepted_value=$isAcceptedValue, promised_try=$promisedTry}"
  }
}
