package com.github.dedis.popstellar.model.network.method

import com.github.dedis.popstellar.model.Immutable
import com.github.dedis.popstellar.model.network.method.message.MessageGeneral
import com.github.dedis.popstellar.model.objects.Channel
import java.util.Objects

/**
 * Broadcast a high level message inside a container.
 *
 * Does not expect any answer
 */
@Immutable
class Broadcast(channel: Channel?, message: MessageGeneral?) : Message(channel) {
  /** Returns the message of the Broadcast. */
  val message: MessageGeneral

  /**
   * Constructor for a Broadcast
   *
   * @param channel name of the channel
   * @param message the message to broadcast
   * @throws IllegalArgumentException if any parameter is null
   */
  init {
    requireNotNull(message) { "Trying to broadcast a null message" }

    this.message = message
  }

  override val method: String
    get() = Method.MESSAGE.method

  override fun equals(other: Any?): Boolean {
    if (this === other) {
      return true
    }
    if (other == null || javaClass != other.javaClass) {
      return false
    }
    if (!super.equals(other)) {
      return false
    }
    val that = other as Broadcast
    return message == that.message
  }

  override fun hashCode(): Int {
    return Objects.hash(super.hashCode(), message)
  }

  override fun toString(): String {
    return "Broadcast{channel='$channel', method='$method', message=$message}"
  }
}
