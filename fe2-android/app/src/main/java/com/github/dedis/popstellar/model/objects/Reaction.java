package com.github.dedis.popstellar.model.objects;

import androidx.annotation.NonNull;

import com.github.dedis.popstellar.model.Copyable;
import com.github.dedis.popstellar.model.objects.security.MessageID;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.utility.MessageValidator;

import java.util.Arrays;
import java.util.Objects;

public class Reaction implements Copyable<Reaction> {

  /** Enum representing the supported emoji for the reaction */
  public enum ReactionEmoji {
    UPVOTE("\uD83D\uDC4D"),
    DOWNVOTE("\uD83D\uDC4E"),
    HEART("❤");

    private final String code;

    ReactionEmoji(String code) {
      this.code = code;
    }

    public String getCode() {
      return code;
    }

    /**
     * Method to validate whether a certain emoji is supported for reactions.
     *
     * @param emoji unicode string of the emoji to test
     * @return true if it's supported, false otherwise
     */
    public static boolean isSupported(String emoji) {
      return Arrays.stream(ReactionEmoji.values())
          .anyMatch(reactionEmoji -> reactionEmoji.code.equals(emoji));
    }
  }

  private final MessageID id;
  private final PublicKey sender;
  private final String codepoint;
  private final MessageID chirpId;
  private final long timestamp;
  private final boolean isDeleted;

  public Reaction(
      @NonNull MessageID id,
      @NonNull PublicKey sender,
      @NonNull String codepoint,
      @NonNull MessageID chirpId,
      long timestamp) {
    MessageValidator.verify()
        .isBase64(id.getEncoded(), "reaction id")
        .isBase64(chirpId.getEncoded(), "chirp id")
        .isValidEmoji(codepoint, "codepoint");
    this.id = id;
    this.sender = sender;
    this.codepoint = codepoint;
    this.chirpId = chirpId;
    this.timestamp = timestamp;
    isDeleted = false;
  }

  public Reaction(Reaction reaction) {
    id = reaction.id;
    sender = reaction.sender;
    codepoint = reaction.codepoint;
    chirpId = reaction.chirpId;
    timestamp = reaction.timestamp;
    isDeleted = reaction.isDeleted;
  }

  public Reaction(Reaction reaction, boolean deleted) {
    id = reaction.id;
    sender = reaction.sender;
    codepoint = reaction.codepoint;
    chirpId = reaction.chirpId;
    timestamp = reaction.timestamp;
    isDeleted = deleted;
  }

  public MessageID getId() {
    return id;
  }

  public PublicKey getSender() {
    return sender;
  }

  public String getCodepoint() {
    return codepoint;
  }

  public MessageID getChirpId() {
    return chirpId;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public boolean isDeleted() {
    return isDeleted;
  }

  @Override
  public Reaction copy() {
    return new Reaction(this);
  }

  public Reaction deleted() {
    return new Reaction(this, true);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Reaction reaction = (Reaction) o;
    return timestamp == reaction.timestamp
        && Objects.equals(id, reaction.id)
        && Objects.equals(sender, reaction.sender)
        && Objects.equals(codepoint, reaction.codepoint)
        && Objects.equals(chirpId, reaction.chirpId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, sender, codepoint, chirpId, timestamp);
  }

  @Override
  public String toString() {
    return "Reaction{"
        + "id="
        + id
        + ", sender="
        + sender
        + ", codepoint='"
        + codepoint
        + '\''
        + ", chirpId="
        + chirpId
        + ", timestamp="
        + timestamp
        + '}';
  }
}
