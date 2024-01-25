package com.github.dedis.popstellar.model.network.method

import com.github.dedis.popstellar.model.network.GenericMessage
import com.github.dedis.popstellar.model.objects.Channel
import java.util.Objects

/** An abstract low level message that is sent over a specific channel */
abstract class Message protected constructor(channel: Channel?) : GenericMessage {
  /** Returns the message channel */
  val channel: Channel

  /**
   * Constructor for a Message
   *
   * @param channel the channel over which the message is sent
   * @throws IllegalArgumentException if channel is null
   */
  init {
    requireNotNull(channel) { "Trying to create a message with a null channel" }
    this.channel = channel
  }

  /** Return the Message method */
  abstract val method: String

  override fun equals(other: Any?): Boolean {
    if (this === other) {
      return true
    }
    if (other == null || javaClass != other.javaClass) {
      return false
    }
    val that = other as Message
    return channel == that.channel
  }

  override fun hashCode(): Int {
    return Objects.hash(channel)
  }
}
