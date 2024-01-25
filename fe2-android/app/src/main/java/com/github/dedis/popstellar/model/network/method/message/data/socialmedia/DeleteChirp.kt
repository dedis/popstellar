package com.github.dedis.popstellar.model.network.method.message.data.socialmedia

import com.github.dedis.popstellar.model.Immutable
import com.github.dedis.popstellar.model.network.method.message.data.Action
import com.github.dedis.popstellar.model.network.method.message.data.Data
import com.github.dedis.popstellar.model.network.method.message.data.Objects
import com.github.dedis.popstellar.model.objects.security.MessageID
import com.google.gson.annotations.SerializedName

@Immutable
class DeleteChirp
/**
 * Constructor for a data Delete Chirp
 *
 * @param chirpId the id of the chirp to delete
 * @param timestamp UNIX timestamp in UTC
 */
(@field:SerializedName("chirp_id") val chirpId: MessageID, val timestamp: Long) : Data {

  override val `object`: String
    get() = Objects.CHIRP.`object`

  override val action: String
    get() = Action.DELETE.action

  override fun equals(other: Any?): Boolean {
    if (this === other) {
      return true
    }
    if (other == null || javaClass != other.javaClass) {
      return false
    }
    val that = other as DeleteChirp
    return chirpId == that.chirpId && timestamp == that.timestamp
  }

  override fun hashCode(): Int {
    return java.util.Objects.hash(chirpId, timestamp)
  }

  override fun toString(): String {
    return "DeleteChirp{chirpId='${chirpId.encoded}', timestamp='$timestamp'}"
  }
}
