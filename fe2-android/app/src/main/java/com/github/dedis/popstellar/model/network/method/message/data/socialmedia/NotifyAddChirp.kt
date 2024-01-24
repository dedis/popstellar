package com.github.dedis.popstellar.model.network.method.message.data.socialmedia

import com.github.dedis.popstellar.model.Immutable
import com.github.dedis.popstellar.model.network.method.message.data.Action
import com.github.dedis.popstellar.model.network.method.message.data.Data
import com.github.dedis.popstellar.model.network.method.message.data.Objects
import com.github.dedis.popstellar.model.objects.Channel
import com.github.dedis.popstellar.model.objects.security.MessageID
import com.google.gson.annotations.SerializedName

/** Data sent to broadcast AddChirp to the general channel */
@Immutable
class NotifyAddChirp
/**
 * @param chirpId message ID of the chirp
 * @param channel channel where the post is located
 * @param timestamp UNIX timestamp in UTC
 */
(
    @field:SerializedName("chirp_id") val chirpId: MessageID,
    val channel: Channel,
    val timestamp: Long
) : Data() {

  override val `object`: String
    get() = Objects.CHIRP.`object`

  override val action: String
    get() = Action.NOTIFY_ADD.action

  override fun equals(other: Any?): Boolean {
    if (this === other) {
      return true
    }
    if (other == null || javaClass != other.javaClass) {
      return false
    }
    val that = other as NotifyAddChirp
    return chirpId == that.chirpId && channel == that.channel && timestamp == that.timestamp
  }

  override fun hashCode(): Int {
    return java.util.Objects.hash(chirpId, channel, timestamp)
  }

  override fun toString(): String {
    return "NotifyAddChirp{chirpId='${chirpId.encoded}', channel='$channel', timestamp='$timestamp'}"
  }
}
