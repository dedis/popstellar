package com.github.dedis.popstellar.model.network.method.message.data.socialmedia;

import androidx.annotation.NonNull;

import com.github.dedis.popstellar.model.network.method.message.data.*;
import com.github.dedis.popstellar.model.objects.security.MessageID;
import com.google.gson.annotations.SerializedName;

public class DeleteChirp extends Data {

  @SerializedName("chirp_id")
  private final MessageID chirpId;

  private final long timestamp;

  /**
   * Constructor for a data Delete Chirp
   *
   * @param chirpId the id of the chirp to delete
   * @param timestamp UNIX timestamp in UTC
   */
  public DeleteChirp(MessageID chirpId, long timestamp) {
    this.chirpId = chirpId;
    this.timestamp = timestamp;
  }

  @Override
  public String getObject() {
    return Objects.CHIRP.getObject();
  }

  @Override
  public String getAction() {
    return Action.DELETE.getAction();
  }

  public MessageID getChirpId() {
    return chirpId;
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
    DeleteChirp that = (DeleteChirp) o;
    return java.util.Objects.equals(getChirpId(), that.getChirpId())
        && java.util.Objects.equals(getTimestamp(), that.getTimestamp());
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(getChirpId(), getTimestamp());
  }

  @NonNull
  @Override
  public String toString() {
    return "DeleteChirp{"
        + "chirpId='"
        + chirpId.getEncoded()
        + '\''
        + ", timestamp='"
        + timestamp
        + '\''
        + '}';
  }
}
