package com.github.dedis.popstellar.model.network.method.message.data.consensus;

import com.github.dedis.popstellar.model.network.method.message.data.*;
import com.github.dedis.popstellar.model.objects.security.MessageID;
import com.google.gson.annotations.SerializedName;

public final class ConsensusFailure extends Data {

  @SerializedName("instance_id")
  private final String instanceId;

  @SerializedName("message_id")
  private final MessageID messageId;

  @SerializedName("created_at")
  private final long creation;

  /**
   * Constructor for a data Failure
   *
   * @param instanceId unique id of the consensus instance
   * @param messageId message id of the Elect message
   * @param creation UNIX timestamp in UTC
   */
  public ConsensusFailure(String instanceId, MessageID messageId, long creation) {
    this.instanceId = instanceId;
    this.messageId = messageId;
    this.creation = creation;
  }

  @Override
  public String getObject() {
    return Objects.CONSENSUS.getObject();
  }

  @Override
  public String getAction() {
    return Action.FAILURE.getAction();
  }

  public String getInstanceId() {
    return instanceId;
  }

  public MessageID getMessageId() {
    return messageId;
  }

  public long getCreation() {
    return creation;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ConsensusFailure that = (ConsensusFailure) o;

    return creation == that.creation
        && java.util.Objects.equals(instanceId, that.instanceId)
        && java.util.Objects.equals(messageId, that.messageId);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(instanceId, messageId, creation);
  }

  @Override
  public String toString() {
    return String.format(
        "ConsensusFailure{instance_id='%s', message_id='%s', created_at=%s}",
        instanceId, messageId.getEncoded(), creation);
  }
}
