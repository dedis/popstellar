package com.github.dedis.popstellar.model.network.method.message.data.socialmedia

import com.github.dedis.popstellar.model.network.method.message.data.Action
import com.github.dedis.popstellar.model.network.method.message.data.Data
import com.github.dedis.popstellar.model.network.method.message.data.Objects
import com.github.dedis.popstellar.model.objects.security.MessageID
import com.github.dedis.popstellar.utility.MessageValidator.verify
import com.google.gson.annotations.SerializedName

class AddReaction(codepoint: String, chirpId: MessageID, timestamp: Long) : Data {
  @SerializedName("reaction_codepoint") val codepoint: String
  @SerializedName("chirp_id") val chirpId: MessageID
  val timestamp: Long

  init {
    verify()
        .isNotEmptyBase64(chirpId.encoded, "chirp id")
        .isValidEmoji(codepoint, "reaction codepoint")
        .validPastTimes(timestamp)

    this.codepoint = codepoint
    this.chirpId = chirpId
    this.timestamp = timestamp
  }

  override val `object`: String
    get() = Objects.REACTION.`object`

  override val action: String
    get() = Action.ADD.action

  override fun equals(other: Any?): Boolean {
    if (this === other) {
      return true
    }
    if (other == null || javaClass != other.javaClass) {
      return false
    }
    val that = other as AddReaction
    return timestamp == that.timestamp && codepoint == that.codepoint && chirpId == that.chirpId
  }

  override fun hashCode(): Int {
    return java.util.Objects.hash(codepoint, chirpId, timestamp)
  }

  override fun toString(): String {
    return "AddReaction{codepoint='$codepoint', chirpId='$chirpId', timestamp=$timestamp}"
  }
}
