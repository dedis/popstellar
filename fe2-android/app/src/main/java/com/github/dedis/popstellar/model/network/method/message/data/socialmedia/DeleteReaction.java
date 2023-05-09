package com.github.dedis.popstellar.model.network.method.message.data.socialmedia;

import com.github.dedis.popstellar.model.network.method.message.data.*;
import com.github.dedis.popstellar.model.objects.security.MessageID;
import com.github.dedis.popstellar.utility.MessageValidator;
import com.google.gson.annotations.SerializedName;

public class DeleteReaction extends Data {

  @SerializedName("reaction_id")
  private final MessageID reactionID;

  private final long timestamp;

  public DeleteReaction(MessageID reactionID, long timestamp) {
    MessageValidator.verify().isBase64(reactionID.getEncoded(), "reaction id");
    this.reactionID = reactionID;
    this.timestamp = timestamp;
  }

  @Override
  public String getObject() {
    return Objects.REACTION.getObject();
  }

  @Override
  public String getAction() {
    return Action.DELETE.getAction();
  }

  public MessageID getReactionID() {
    return reactionID;
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
    DeleteReaction that = (DeleteReaction) o;
    return timestamp == that.timestamp && java.util.Objects.equals(reactionID, that.reactionID);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(reactionID, timestamp);
  }

  @Override
  public String toString() {
    return "DeleteReaction{"
        + "reactionID='"
        + reactionID
        + '\''
        + ", timestamp="
        + timestamp
        + '}';
  }
}
