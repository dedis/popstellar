package com.github.dedis.popstellar.model.objects;

import androidx.annotation.NonNull;

import com.github.dedis.popstellar.model.Copyable;
import com.github.dedis.popstellar.model.objects.security.MessageID;
import com.github.dedis.popstellar.model.objects.security.PublicKey;

import java.util.Objects;

/** Class modeling a Chirp */
public class Chirp implements Copyable<Chirp> {

  private static final int MAX_CHIRP_CHARS = 300;

  private final MessageID id;
  private final PublicKey sender;
  private final String text;
  private final long timestamp;
  private final boolean isDeleted;
  private final MessageID parentId;

  public Chirp(
      @NonNull MessageID id,
      @NonNull PublicKey sender,
      @NonNull String text,
      long timestamp,
      @NonNull MessageID parentId) {
    if (id.getEncoded().isEmpty()) {
      throw new IllegalArgumentException("The id of the Chirp is empty");
    }

    if (timestamp < 0) {
      throw new IllegalArgumentException("The timestamp of the Chirp is negative");
    }

    if (text.length() > MAX_CHIRP_CHARS) {
      throw new IllegalArgumentException("the text exceed the maximum numbers of characters");
    }

    this.id = id;
    this.sender = sender;
    this.text = text;
    this.timestamp = timestamp;
    this.parentId = parentId;
    this.isDeleted = false;
  }

  public Chirp(Chirp chirp, boolean deleted) {
    this.id = chirp.id;
    this.sender = chirp.sender;
    this.text = "";
    this.timestamp = chirp.timestamp;
    this.parentId = chirp.parentId;
    this.isDeleted = deleted;
  }

  public Chirp(Chirp chirp) {
    this.id = chirp.id;
    this.sender = chirp.sender;
    this.text = chirp.text;
    this.timestamp = chirp.timestamp;
    this.isDeleted = chirp.isDeleted;
    this.parentId = chirp.parentId;
  }

  public MessageID getId() {
    return id;
  }

  public PublicKey getSender() {
    return sender;
  }

  public String getText() {
    return text;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public boolean isDeleted() {
    return isDeleted;
  }

  public MessageID getParentId() {
    return parentId;
  }

  @Override
  public Chirp copy() {
    return new Chirp(this);
  }

  /**
   * @return a new deleted chirp
   */
  public Chirp deleted() {
    return new Chirp(this, true);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Chirp chirp = (Chirp) o;
    return timestamp == chirp.timestamp
        && isDeleted == chirp.isDeleted
        && Objects.equals(id, chirp.id)
        && Objects.equals(sender, chirp.sender)
        && Objects.equals(text, chirp.text)
        && Objects.equals(parentId, chirp.parentId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, sender, text, timestamp, isDeleted, parentId);
  }

  @NonNull
  @Override
  public String toString() {
    return String.format(
        "Chirp{id='%s', sender='%s', text='%s', timestamp='%s', isDeleted='%s', parentId='%s'",
        id.getEncoded(), sender, text, timestamp, isDeleted, parentId.getEncoded());
  }
}
