package com.github.dedis.popstellar.model.network.method

import com.github.dedis.popstellar.model.Immutable
import com.github.dedis.popstellar.model.network.method.message.MessageGeneral
import com.github.dedis.popstellar.model.objects.Channel
import java.util.Objects

/** Publish a message on a channel */
@Immutable
class Publish(channel: Channel?, id: Int, message: MessageGeneral?) : Query(channel, id) {
  /** Returns the message to publish. */
  val message: MessageGeneral

  /**
   * Constructor for a Publish
   *
   * @param channel name of the channel
   * @param id request ID
   * @param message message to publish
   * @throws IllegalArgumentException if any parameter is null
   */
  init {
    requireNotNull(message) { "Trying to publish a null message" }

    this.message = message
  }

  override val method: String
    get() = Method.PUBLISH.method

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
    val publish = other as Publish
    return message == publish.message
  }

  override fun hashCode(): Int {
    return Objects.hash(super.hashCode(), message)
  }

  override fun toString(): String {
    return "Publish{id=$requestId, channel='$channel', message=$message}"
  }
}
