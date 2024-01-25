package com.github.dedis.popstellar.model.network.method.message.data.socialmedia

import com.github.dedis.popstellar.model.network.method.message.data.Action
import com.github.dedis.popstellar.model.network.method.message.data.Data
import com.github.dedis.popstellar.model.network.method.message.data.Objects
import com.github.dedis.popstellar.model.objects.security.MessageID
import com.github.dedis.popstellar.utility.MessageValidator.verify
import com.google.gson.annotations.SerializedName

class DeleteReaction(reactionID: MessageID, timestamp: Long) : Data {
  @SerializedName("reaction_id") val reactionID: MessageID
  val timestamp: Long

  init {
    verify().isNotEmptyBase64(reactionID.encoded, "reaction id").validPastTimes(timestamp)

    this.reactionID = reactionID
    this.timestamp = timestamp
  }

  override val `object`: String
    get() = Objects.REACTION.`object`

  override val action: String
    get() = Action.DELETE.action

  override fun equals(other: Any?): Boolean {
    if (this === other) {
      return true
    }
    if (other == null || javaClass != other.javaClass) {
      return false
    }
    val that = other as DeleteReaction
    return timestamp == that.timestamp && reactionID == that.reactionID
  }

  override fun hashCode(): Int {
    return java.util.Objects.hash(reactionID, timestamp)
  }

  override fun toString(): String {
    return "DeleteReaction{reactionID='$reactionID', timestamp=$timestamp}"
  }
}
