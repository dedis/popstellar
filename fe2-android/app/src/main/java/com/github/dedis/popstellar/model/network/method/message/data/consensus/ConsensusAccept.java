package com.github.dedis.popstellar.model.network.method.message.data.consensus;

import com.github.dedis.popstellar.model.network.method.message.data.Action;
import com.github.dedis.popstellar.model.network.method.message.data.Data;
import com.github.dedis.popstellar.model.network.method.message.data.Objects;
import com.google.gson.annotations.SerializedName;

public final class ConsensusAccept extends Data {

  @SerializedName("instance_id")
  private final String instanceId;

  @SerializedName("message_id")
  private final String messageId;

  @SerializedName("created_at")
  private final long creation;

  @SerializedName("value")
  private final AcceptValue acceptValue;

  public ConsensusAccept(
      String instanceId, String messageId, long creation, int acceptedTry, boolean acceptedValue) {
    this.instanceId = instanceId;
    this.messageId = messageId;
    this.creation = creation;
    this.acceptValue = new AcceptValue(acceptedTry, acceptedValue);
  }

  @Override
  public String getObject() {
    return Objects.CONSENSUS.getObject();
  }

  @Override
  public String getAction() {
    return Action.ACCEPT.getAction();
  }

  public String getInstanceId() {
    return instanceId;
  }

  public String getMessageId() {
    return messageId;
  }

  public long getCreation() {
    return creation;
  }

  public AcceptValue getAcceptValue() {
    return acceptValue;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ConsensusAccept that = (ConsensusAccept) o;

    return creation == that.creation
        && java.util.Objects.equals(instanceId, that.instanceId)
        && java.util.Objects.equals(messageId, that.messageId)
        && java.util.Objects.equals(acceptValue, that.acceptValue);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(instanceId, messageId, creation, acceptValue);
  }

  @Override
  public String toString() {
    return String.format(
        "ConsensusAccept{instance_id='%s', message_id='%s', created_at=%s, value=%s}",
        instanceId, messageId, creation, acceptValue);
  }
}
