package com.github.dedis.popstellar.model.network.method.message.data.consensus;

import com.github.dedis.popstellar.model.network.method.message.data.Action;
import com.github.dedis.popstellar.model.network.method.message.data.Data;
import com.github.dedis.popstellar.model.network.method.message.data.Objects;
import com.google.gson.annotations.SerializedName;

import java.util.Collections;
import java.util.List;

public final class ConsensusLearn extends Data {

  @SerializedName("instance_id")
  private final String instanceId;

  @SerializedName("message_id")
  private final String messageId;

  @SerializedName("created_at")
  private final long creation;

  @SerializedName("value")
  private final LearnValue learnValue;

  @SerializedName("acceptor-signatures")
  private final List<String> acceptorSignatures;

  public ConsensusLearn(
      String instanceId,
      String messageId,
      long creation,
      boolean decision,
      List<String> acceptorSignatures) {
    this.instanceId = instanceId;
    this.messageId = messageId;
    this.creation = creation;
    this.learnValue = new LearnValue(decision);
    this.acceptorSignatures = Collections.unmodifiableList(acceptorSignatures);
  }

  public String getInstanceId() {
    return instanceId;
  }

  public String getMessageId() {
    return messageId;
  }

  public List<String> getAcceptorSignatures() {
    return acceptorSignatures;
  }

  @Override
  public String getObject() {
    return Objects.CONSENSUS.getObject();
  }

  @Override
  public String getAction() {
    return Action.LEARN.getAction();
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(instanceId, messageId, creation, learnValue, acceptorSignatures);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ConsensusLearn that = (ConsensusLearn) o;

    return creation == that.creation
        && java.util.Objects.equals(instanceId, that.instanceId)
        && java.util.Objects.equals(messageId, that.messageId)
        && java.util.Objects.equals(learnValue, that.learnValue)
        && java.util.Objects.equals(acceptorSignatures, that.acceptorSignatures);
  }

  @Override
  public String toString() {
    return String.format(
        "ConsensusLearn{instance_id='%s', message_id='%s', acceptor-signatures=%s}",
        instanceId, messageId, acceptorSignatures);
  }
}
