package com.github.dedis.popstellar.model.objects

import com.github.dedis.popstellar.model.Copyable
import com.github.dedis.popstellar.model.objects.security.MessageID
import com.github.dedis.popstellar.model.objects.security.PublicKey
import com.github.dedis.popstellar.utility.MessageValidator.verify
import java.util.Arrays
import java.util.Objects

class Reaction : Copyable<Reaction?> {
  /** Enum representing the supported emoji for the reaction */
  enum class ReactionEmoji(val code: String) {
    UPVOTE("\uD83D\uDC4D"),
    DOWNVOTE("\uD83D\uDC4E"),
    HEART("❤️");

    companion object {
      /**
       * Method to validate whether a certain emoji is supported for reactions.
       *
       * @param emoji unicode string of the emoji to test
       * @return true if it's supported, false otherwise
       */
      fun isSupported(emoji: String): Boolean {
        return Arrays.stream(values()).anyMatch { reactionEmoji: ReactionEmoji ->
          reactionEmoji.code == emoji
        }
      }
    }
  }

  val id: MessageID
  val sender: PublicKey
  val codepoint: String
  val chirpId: MessageID
  val timestamp: Long
  val isDeleted: Boolean

  @JvmOverloads
  constructor(
      id: MessageID,
      sender: PublicKey,
      codepoint: String,
      chirpId: MessageID,
      timestamp: Long,
      isDeleted: Boolean = false
  ) {
    verify()
        .isNotEmptyBase64(id.encoded, "reaction id")
        .isNotEmptyBase64(chirpId.encoded, "chirp id")
        .isValidEmoji(codepoint, "codepoint")
        .validPastTimes(timestamp)

    this.id = id
    this.sender = sender
    this.codepoint = codepoint
    this.chirpId = chirpId
    this.timestamp = timestamp
    this.isDeleted = isDeleted
  }

  constructor(reaction: Reaction) {
    id = reaction.id
    sender = reaction.sender
    codepoint = reaction.codepoint
    chirpId = reaction.chirpId
    timestamp = reaction.timestamp
    isDeleted = reaction.isDeleted
  }

  constructor(reaction: Reaction, deleted: Boolean) {
    id = reaction.id
    sender = reaction.sender
    codepoint = reaction.codepoint
    chirpId = reaction.chirpId
    timestamp = reaction.timestamp
    isDeleted = deleted
  }

  override fun copy(): Reaction {
    return Reaction(this)
  }

  fun deleted(): Reaction {
    return Reaction(this, true)
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) {
      return true
    }
    if (other == null || javaClass != other.javaClass) {
      return false
    }
    val reaction = other as Reaction
    return timestamp == reaction.timestamp &&
        id == reaction.id &&
        sender == reaction.sender &&
        codepoint == reaction.codepoint &&
        chirpId == reaction.chirpId
  }

  override fun hashCode(): Int {
    return Objects.hash(id, sender, codepoint, chirpId, timestamp)
  }

  override fun toString(): String {
    return "Reaction{id=$id, sender=$sender, codepoint='$codepoint', chirpId=$chirpId, timestamp=$timestamp}"
  }
}
