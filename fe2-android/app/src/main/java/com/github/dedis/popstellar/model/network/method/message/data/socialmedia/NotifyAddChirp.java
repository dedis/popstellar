package com.github.dedis.popstellar.model.network.method.message.data.socialmedia;

import com.github.dedis.popstellar.model.network.method.message.data.Action;
import com.github.dedis.popstellar.model.network.method.message.data.Data;
import com.github.dedis.popstellar.model.network.method.message.data.Objects;
import com.google.gson.annotations.SerializedName;

/** Data sent to broadcast AddChirp to the general channel */
public class NotifyAddChirp extends Data {

  @SerializedName("post_id")
  private final String postId;

  private final String channel;
  private final long timestamp;

  /**
   * @param postId message ID of the post
   * @param channel channel where the post is located
   * @param timestamp UNIX timestamp in UTC
   */
  public NotifyAddChirp(String postId, String channel, long timestamp) {
    this.postId = postId;
    this.channel = channel;
    this.timestamp = timestamp;
  }

  @Override
  public String getObject() {
    return Objects.CHIRP.getObject();
  }

  @Override
  public String getAction() {
    return Action.NOTIFY_ADD.getAction();
  }

  public String getPostId() {
    return postId;
  }

  public String getChannel() {
    return channel;
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
    NotifyAddChirp that = (NotifyAddChirp) o;
    return java.util.Objects.equals(getPostId(), that.getPostId())
        && java.util.Objects.equals(getChannel(), that.getChannel())
        && java.util.Objects.equals(getTimestamp(), that.getTimestamp());
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(getPostId(), getChannel(), getTimestamp());
  }

  @Override
  public String toString() {
    return "AddChirpBroadcast{"
        + "postId='"
        + postId
        + '\''
        + ", channel='"
        + channel
        + '\''
        + ", timestamp='"
        + timestamp
        + '\''
        + '}';
  }
}
