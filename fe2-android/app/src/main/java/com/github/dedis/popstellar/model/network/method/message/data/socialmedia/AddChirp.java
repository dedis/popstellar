package com.github.dedis.popstellar.model.network.method.message.data.socialmedia;

import androidx.annotation.Nullable;

import com.github.dedis.popstellar.model.network.method.message.data.*;
import com.github.dedis.popstellar.model.objects.security.MessageID;
import com.google.gson.annotations.SerializedName;

import java.util.Optional;

/** Data sent to add a Chirp to the user channel */
public class AddChirp extends Data {

  private static final int MAX_CHIRP_CHARS = 300;
  private final String text;

  @SerializedName("parent_id")
  @Nullable
  private final MessageID parentId;

  private final long timestamp;

  /**
   * Constructor for a data Add Chirp
   *
   * @param text text of the chirp
   * @param parentId message ID of parent chirp, can be null
   * @param timestamp UNIX timestamp in UTC
   */
  public AddChirp(String text, @Nullable MessageID parentId, long timestamp) {
    if (text.length() > MAX_CHIRP_CHARS) {
      throw new IllegalArgumentException("the text exceed the maximum numbers of characters");
    }
    this.text = text;
    this.parentId = parentId;
    this.timestamp = timestamp;
  }

  @Override
  public String getObject() {
    return Objects.CHIRP.getObject();
  }

  @Override
  public String getAction() {
    return Action.ADD.getAction();
  }

  public String getText() {
    return text;
  }

  public Optional<MessageID> getParentId() {
    return Optional.ofNullable(parentId);
  }

  public long getTimestamp() {
    return timestamp;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AddChirp that = (AddChirp) o;
    return java.util.Objects.equals(getText(), that.getText())
        && java.util.Objects.equals(getParentId(), that.getParentId())
        && java.util.Objects.equals(getTimestamp(), that.getTimestamp());
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(getText(), getParentId(), getTimestamp());
  }

  @Override
  public String toString() {
    return "AddChirp{"
        + "text='"
        + text
        + '\''
        + ", parentId='"
        + parentId
        + '\''
        + ", timestamp='"
        + timestamp
        + '\''
        + '}';
  }
}
