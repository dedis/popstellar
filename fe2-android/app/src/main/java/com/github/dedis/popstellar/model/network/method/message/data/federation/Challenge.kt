package com.github.dedis.popstellar.model.network.method.message.data.federation

import com.github.dedis.popstellar.model.network.method.message.data.Action
import com.github.dedis.popstellar.model.network.method.message.data.Data
import com.github.dedis.popstellar.model.network.method.message.data.Objects
import com.google.gson.annotations.SerializedName

/** Challenge sent by the server */
class Challenge : Data {
  val value: String
  @SerializedName("valid_until") val validUntil: Long

  /**
   * Constructor for a data Challenge
   *
   * @param value value of the Challenge (A 32 bytes array encoded in hexadecimal)
   * @param validUntil expiration time of the Challenge
   */
  constructor(value: String, validUntil: Long) {
    this.value = value
    if (validUntil < 0L) {
      this.validUntil = 0L
    } else {
      this.validUntil = validUntil
    }
  }

  override val `object`: String
    get() = Objects.FEDERATION.`object`

  override val action: String
    get() = Action.CHALLENGE.action

  override fun equals(other: Any?): Boolean {
    if (this === other) {
      return true
    }
    if (other == null || javaClass != other.javaClass) {
      return false
    }
    val that = other as Challenge
    return value == that.value && validUntil == that.validUntil
  }

  override fun hashCode(): Int {
    return java.util.Objects.hash(value, validUntil)
  }

  override fun toString(): String {
    return "Challenge{value='$value', valid_until='$validUntil'}"
  }
}
