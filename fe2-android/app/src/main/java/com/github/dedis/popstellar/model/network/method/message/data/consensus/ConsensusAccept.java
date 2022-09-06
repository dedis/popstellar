package com.github.dedis.popstellar.model.network.method.message.data.consensus;

import com.github.dedis.popstellar.model.Immutable;
import com.github.dedis.popstellar.model.network.method.message.data.*;
import com.github.dedis.popstellar.model.objects.security.MessageID;
import com.google.gson.annotations.SerializedName;

@Immutable
public final class ConsensusAccept extends Data {

  @SerializedName("instance_id")
  private final String instanceId;

  @SerializedName("message_id")
  private final MessageID messageId;

  @SerializedName("created_at")
  private final long creation;

  @SerializedName("value")
  private final AcceptValue acceptValue;

  /**
   * Constructor for a data Accept
   *
   * @param instanceId unique id of the consensus instance
   * @param messageId message id of the Elect message
   * @param creation UNIX timestamp in UTC
   * @param acceptedTry the accepted try number
   * @param acceptedValue the value accepted
   */
  public ConsensusAccept(
      String instanceId,
      MessageID messageId,
      long creation,
      int acceptedTry,
      boolean acceptedValue) {
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

  public MessageID getMessageId() {
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
        instanceId, messageId.getEncoded(), creation, acceptValue);
  }
}
