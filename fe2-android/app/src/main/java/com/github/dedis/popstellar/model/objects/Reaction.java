package com.github.dedis.popstellar.model.objects;

import androidx.annotation.NonNull;

import com.github.dedis.popstellar.model.Copyable;
import com.github.dedis.popstellar.model.objects.security.MessageID;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.utility.MessageValidator;

import java.util.Objects;

public class Reaction implements Copyable<Reaction> {

  // Supported Emoji
  public enum Emoji {
    UPVOTE("U+1F44D"),
    DOWNVOTE("U+1F44E"),
    LOVE("U+2764");

    private final String unicode;

    Emoji(String unicode) {
      this.unicode = unicode;
    }

    public String getUnicode() {
      return unicode;
    }
  }

  private final MessageID id;
  private final PublicKey sender;
  private final String codepoint;
  private final MessageID chirpId;
  private final long timestamp;

  public Reaction(
      @NonNull MessageID id,
      @NonNull PublicKey sender,
      @NonNull String codepoint,
      @NonNull MessageID chirpId,
      long timestamp) {
    MessageValidator.verify()
        .stringNotEmpty(codepoint, "codepoint")
        .validPastTimes(timestamp)
        .isBase64(id.getEncoded(), "reaction id")
        .isBase64(chirpId.getEncoded(), "chirp id");
    this.id = id;
    this.sender = sender;
    this.codepoint = codepoint;
    this.chirpId = chirpId;
    this.timestamp = timestamp;
  }

  public Reaction(Reaction reaction) {
    id = reaction.id;
    sender = reaction.sender;
    codepoint = reaction.codepoint;
    chirpId = reaction.chirpId;
    timestamp = reaction.timestamp;
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

  @Override
  public Reaction copy() {
    return new Reaction(this);
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
