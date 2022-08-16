package com.github.dedis.popstellar.model.network.method.message.data.consensus;

import com.github.dedis.popstellar.model.Immutable;
import com.github.dedis.popstellar.model.network.method.message.data.*;
import com.github.dedis.popstellar.model.objects.security.MessageID;
import com.google.gson.annotations.SerializedName;

@Immutable
public final class ConsensusElectAccept extends Data {

  @SerializedName("instance_id")
  private final String instanceId;

  @SerializedName("message_id")
  private final MessageID messageId;

  private final boolean accept;

  /**
   * Constructor for a data Elect_Accept
   *
   * @param instanceId unique id of the consensus instance
   * @param messageId message id of the Elect message
   * @param accept true if the node agrees with the proposal
   */
  public ConsensusElectAccept(String instanceId, MessageID messageId, boolean accept) {
    this.instanceId = instanceId;
    this.messageId = messageId;
    this.accept = accept;
  }

  public String getInstanceId() {
    return instanceId;
  }

  public MessageID getMessageId() {
    return messageId;
  }

  public boolean isAccept() {
    return accept;
  }

  @Override
  public String getObject() {
    return Objects.CONSENSUS.getObject();
  }

  @Override
  public String getAction() {
    return Action.ELECT_ACCEPT.getAction();
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(instanceId, messageId, accept);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ConsensusElectAccept that = (ConsensusElectAccept) o;

    return accept == that.accept
        && java.util.Objects.equals(instanceId, that.instanceId)
        && java.util.Objects.equals(messageId, that.messageId);
  }

  @Override
  public String toString() {
    return String.format(
        "ConsensusElectAccept{instance_id='%s', message_id='%s', accept=%b}",
        instanceId, messageId.getEncoded(), accept);
  }
}
