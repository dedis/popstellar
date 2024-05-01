package com.github.dedis.popstellar.model.network.method.message.data.federation

import com.github.dedis.popstellar.model.Immutable
import com.github.dedis.popstellar.model.network.method.message.data.Action
import com.github.dedis.popstellar.model.network.method.message.data.Data
import com.github.dedis.popstellar.model.network.method.message.data.Objects

/** Data sent to get a challenge */
@Immutable
class ChallengeRequest : Data {
  val timestamp: Long

  /**
   * Constructor for a data Challenge Request
   *
   * @param timestamp time of the Challenge Request
   */
  constructor(timestamp: Long) {
    this.timestamp = timestamp
  }

  override val `object`: String
    get() = Objects.FEDERATION.`object`

  override val action: String
    get() = Action.CHALLENGE_REQUEST.action

  override fun equals(other: Any?): Boolean {
    if (this === other) {
      return true
    }
    if (other == null || javaClass != other.javaClass) {
      return false
    }
    val that = other as ChallengeRequest
    return timestamp == that.timestamp
  }

  override fun hashCode(): Int {
    return java.util.Objects.hash(timestamp)
  }

  override fun toString(): String {
    return "ChallengeRequest{timestamp='$timestamp'}"
  }
}
