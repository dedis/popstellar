package com.github.dedis.popstellar.model.objects

import com.github.dedis.popstellar.model.Copyable
import com.github.dedis.popstellar.model.objects.security.MessageID
import com.github.dedis.popstellar.model.objects.security.PublicKey
import java.util.Objects

/** Class modeling a Chirp */
class Chirp : Copyable<Chirp> {
  val id: MessageID
  val sender: PublicKey
  val text: String
  val timestamp: Long
  val isDeleted: Boolean
  val parentId: MessageID

  constructor(
      id: MessageID,
      sender: PublicKey,
      text: String,
      timestamp: Long,
      isDeleted: Boolean,
      parentId: MessageID
  ) {
    require(id.encoded.isNotEmpty()) { "The id of the Chirp is empty" }
    require(timestamp >= 0) { "The timestamp of the Chirp is negative" }
    require(text.length <= MAX_CHIRP_CHARS) { "the text exceed the maximum numbers of characters" }

    this.id = id
    this.sender = sender
    this.text = text
    this.timestamp = timestamp
    this.parentId = parentId
    this.isDeleted = isDeleted
  }

  constructor(
      id: MessageID,
      sender: PublicKey,
      text: String,
      timestamp: Long,
      parentId: MessageID
  ) : this(id, sender, text, timestamp, false, parentId)

  constructor(chirp: Chirp, deleted: Boolean) {
    id = chirp.id
    sender = chirp.sender
    text = ""
    timestamp = chirp.timestamp
    parentId = chirp.parentId
    isDeleted = deleted
  }

  constructor(chirp: Chirp) {
    id = chirp.id
    sender = chirp.sender
    text = chirp.text
    timestamp = chirp.timestamp
    isDeleted = chirp.isDeleted
    parentId = chirp.parentId
  }

  override fun copy(): Chirp {
    return Chirp(this)
  }

  /** @return a new deleted chirp */
  fun deleted(): Chirp {
    return Chirp(this, true)
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) {
      return true
    }
    if (other == null || javaClass != other.javaClass) {
      return false
    }
    val chirp = other as Chirp
    return timestamp == chirp.timestamp &&
        isDeleted == chirp.isDeleted &&
        id == chirp.id &&
        sender == chirp.sender &&
        text == chirp.text &&
        parentId == chirp.parentId
  }

  override fun hashCode(): Int {
    return Objects.hash(id, sender, text, timestamp, isDeleted, parentId)
  }

  override fun toString(): String {
    return "Chirp{id='${id.encoded}', sender='$sender', text='$text', timestamp='$timestamp', isDeleted='$isDeleted', parentId='${parentId.encoded}'"
  }

  companion object {
    private const val MAX_CHIRP_CHARS = 300
  }
}
