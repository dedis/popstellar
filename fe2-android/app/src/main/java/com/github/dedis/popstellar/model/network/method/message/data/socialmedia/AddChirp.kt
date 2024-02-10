package com.github.dedis.popstellar.model.network.method.message.data.socialmedia

import com.github.dedis.popstellar.model.Immutable
import com.github.dedis.popstellar.model.network.method.message.data.Action
import com.github.dedis.popstellar.model.network.method.message.data.Data
import com.github.dedis.popstellar.model.network.method.message.data.Objects
import com.github.dedis.popstellar.model.objects.security.MessageID
import com.google.gson.annotations.SerializedName
import java.util.Optional

/** Data sent to add a Chirp to the user channel */
@Immutable
class AddChirp(text: String, parentId: MessageID?, timestamp: Long) : Data {
  val text: String

  @SerializedName("parent_id") private val parentId: MessageID?

  val timestamp: Long

  /**
   * Constructor for a data Add Chirp
   *
   * @param text text of the chirp
   * @param parentId message ID of parent chirp, can be null
   * @param timestamp UNIX timestamp in UTC
   */
  init {
    require(text.length <= MAX_CHIRP_CHARS) { "the text exceed the maximum numbers of characters" }

    this.text = text
    this.parentId = parentId
    this.timestamp = timestamp
  }

  override val `object`: String
    get() = Objects.CHIRP.`object`

  override val action: String
    get() = Action.ADD.action

  fun getParentId(): Optional<MessageID> {
    return Optional.ofNullable(parentId)
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) {
      return true
    }
    if (other == null || javaClass != other.javaClass) {
      return false
    }
    val that = other as AddChirp
    return text == that.text && getParentId() == that.getParentId() && timestamp == that.timestamp
  }

  override fun hashCode(): Int {
    return java.util.Objects.hash(text, getParentId(), timestamp)
  }

  override fun toString(): String {
    return "AddChirp{text='$text', parentId='$parentId', timestamp='$timestamp'}"
  }

  companion object {
    private const val MAX_CHIRP_CHARS = 300
  }
}
