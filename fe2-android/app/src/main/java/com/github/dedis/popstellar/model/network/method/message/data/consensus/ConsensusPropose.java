package com.github.dedis.popstellar.model.network.method.message.data.consensus;

import com.github.dedis.popstellar.model.network.method.message.data.Action;
import com.github.dedis.popstellar.model.network.method.message.data.Data;
import com.github.dedis.popstellar.model.network.method.message.data.Objects;
import com.google.gson.annotations.SerializedName;

import java.util.Arrays;
import java.util.List;

public final class ConsensusPropose extends Data {

  @SerializedName("instance_id")
  private final String instanceId;

  @SerializedName("message_id")
  private final String messageId;

  @SerializedName("created_at")
  private final long creation;

  @SerializedName("value")
  private final ProposeValue proposeValue;

  @SerializedName("acceptor-signatures")
  private final List<String> acceptorSignatures;

  public ConsensusPropose(
      String instanceId,
      String messageId,
      long creation,
      int proposedTry,
      boolean proposedValue,
      List<String> acceptorSignatures) {
    this.instanceId = instanceId;
    this.messageId = messageId;
    this.creation = creation;
    this.proposeValue = new ProposeValue(proposedTry, proposedValue);
    this.acceptorSignatures = acceptorSignatures;
  }

  @Override
  public String getObject() {
    return Objects.CONSENSUS.getObject();
  }

  @Override
  public String getAction() {
    return Action.PROPOSE.getAction();
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

  public ProposeValue getProposeValue() {
    return proposeValue;
  }

  public List<String> getAcceptorSignatures() {
    return acceptorSignatures;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ConsensusPropose that = (ConsensusPropose) o;

    return creation == that.creation
        && java.util.Objects.equals(instanceId, that.instanceId)
        && java.util.Objects.equals(messageId, that.messageId)
        && java.util.Objects.equals(proposeValue, that.proposeValue)
        && java.util.Objects.equals(acceptorSignatures, that.acceptorSignatures);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(
        instanceId, messageId, creation, proposeValue, acceptorSignatures);
  }

  @Override
  public String toString() {
    return String.format(
        "ConsensusPropose{instance_id='%s', message_id='%s', created_at=%s, value=%s, acceptor-signatures=%s}",
        instanceId,
        messageId,
        creation,
        proposeValue,
        Arrays.toString(acceptorSignatures.toArray()));
  }
}
