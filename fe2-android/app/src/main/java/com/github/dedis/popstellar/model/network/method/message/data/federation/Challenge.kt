package com.github.dedis.popstellar.model.network.method.message.data.federation

import com.github.dedis.popstellar.model.network.method.message.data.Action
import com.github.dedis.popstellar.model.network.method.message.data.Data
import com.github.dedis.popstellar.model.network.method.message.data.Objects

/** Challenge sent by the server */
class Challenge : Data {
  val value: Int
  val valid_until: Long

  /**
   * Constructor for a data Challenge
   *
   * @param value value of the Challenge
   * @param valid_until date until the Challenge is valid
   */
  constructor(value: Int, valid_until: Long) {
    this.value = value
    this.valid_until = valid_until // TODO add check on valid date
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
    return value == that.value && valid_until == that.valid_until
  }

  override fun hashCode(): Int {
    return java.util.Objects.hash(value, valid_until)
  }

  override fun toString(): String {
    return "Challenge{value='$value', valid_until='$valid_until'}"
  }
}
