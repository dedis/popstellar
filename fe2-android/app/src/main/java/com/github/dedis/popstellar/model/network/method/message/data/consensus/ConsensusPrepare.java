package com.github.dedis.popstellar.model.network.method.message.data.consensus;

import com.github.dedis.popstellar.model.network.method.message.data.*;
import com.github.dedis.popstellar.model.objects.security.MessageID;
import com.google.gson.annotations.SerializedName;

public final class ConsensusPrepare extends Data {

  @SerializedName("instance_id")
  private final String instanceId;

  @SerializedName("message_id")
  private final MessageID messageId;

  @SerializedName("created_at")
  private final long creation;

  @SerializedName("value")
  private final PrepareValue prepareValue;

  /**
   * Constructor for a data Prepare
   *
   * @param instanceId unique id of the consensus instance
   * @param messageId message id of the Elect message
   * @param creation UNIX timestamp in UTC
   * @param proposedTry proposed try number
   */
  public ConsensusPrepare(String instanceId, MessageID messageId, long creation, int proposedTry) {
    this.instanceId = instanceId;
    this.messageId = messageId;
    this.creation = creation;
    this.prepareValue = new PrepareValue(proposedTry);
  }

  @Override
  public String getObject() {
    return Objects.CONSENSUS.getObject();
  }

  @Override
  public String getAction() {
    return Action.PREPARE.getAction();
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

  public PrepareValue getPrepareValue() {
    return prepareValue;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ConsensusPrepare that = (ConsensusPrepare) o;

    return creation == that.creation
        && java.util.Objects.equals(instanceId, that.instanceId)
        && java.util.Objects.equals(messageId, that.messageId)
        && java.util.Objects.equals(prepareValue, that.prepareValue);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(instanceId, messageId, creation, prepareValue);
  }

  @Override
  public String toString() {
    return String.format(
        "ConsensusPrepare{instance_id='%s', message_id='%s', created_at=%s, value=%s}",
        instanceId, messageId.getEncoded(), creation, prepareValue);
  }
}
