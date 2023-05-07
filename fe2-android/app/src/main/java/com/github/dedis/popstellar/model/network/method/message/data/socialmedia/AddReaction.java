package com.github.dedis.popstellar.model.network.method.message.data.socialmedia;

import com.github.dedis.popstellar.model.network.method.message.data.*;
import com.github.dedis.popstellar.model.objects.security.MessageID;
import com.github.dedis.popstellar.utility.MessageValidator;
import com.google.gson.annotations.SerializedName;

public class AddReaction extends Data {

  @SerializedName("reaction_codepoint")
  private final String codepoint;

  @SerializedName("chirp_id")
  private final MessageID chirpId;

  private final long timestamp;

  public AddReaction(String codepoint, MessageID chirpId, long timestamp) {
    MessageValidator.verify()
        .isBase64(chirpId.getEncoded(), "chirp id")
        .validPastTimes(timestamp)
        .stringNotEmpty(codepoint, "reaction codepoint");
    this.codepoint = codepoint;
    this.chirpId = chirpId;
    this.timestamp = timestamp;
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
  public String getObject() {
    return Objects.REACTION.getObject();
  }

  @Override
  public String getAction() {
    return Action.ADD.getAction();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AddReaction that = (AddReaction) o;
    return timestamp == that.timestamp
        && java.util.Objects.equals(codepoint, that.codepoint)
        && java.util.Objects.equals(chirpId, that.chirpId);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(codepoint, chirpId, timestamp);
  }

  @Override
  public String toString() {
    return "AddReaction{"
        + "codepoint='"
        + codepoint
        + '\''
        + ", chirpId='"
        + chirpId
        + '\''
        + ", timestamp="
        + timestamp
        + '}';
  }
}
