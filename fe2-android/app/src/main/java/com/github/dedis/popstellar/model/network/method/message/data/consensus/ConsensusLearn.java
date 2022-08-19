package com.github.dedis.popstellar.model.network.method.message.data.consensus;

import com.github.dedis.popstellar.model.Immutable;
import com.github.dedis.popstellar.model.network.method.message.data.*;
import com.github.dedis.popstellar.model.objects.security.MessageID;
import com.google.gson.annotations.SerializedName;

import java.util.Collections;
import java.util.List;

@Immutable
public final class ConsensusLearn extends Data {

  @SerializedName("instance_id")
  private final String instanceId;

  @SerializedName("message_id")
  private final MessageID messageId;

  @SerializedName("created_at")
  private final long creation;

  @SerializedName("value")
  private final LearnValue learnValue;

  @SerializedName("acceptor-signatures")
  private final List<String> acceptorSignatures;

  /**
   * Constructor for a data Learn
   *
   * @param instanceId unique id of the consensus instance
   * @param messageId message id of the Elect message
   * @param creation UNIX timestamp in UTC
   * @param decision true if the consensus was successful
   * @param acceptorSignatures signatures of all the received Accept messages
   */
  public ConsensusLearn(
      String instanceId,
      MessageID messageId,
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

  public MessageID getMessageId() {
    return messageId;
  }

  public List<String> getAcceptorSignatures() {
    return acceptorSignatures;
  }

  public long getCreation() {
    return creation;
  }

  public LearnValue getLearnValue() {
    return learnValue;
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
        instanceId, messageId.getEncoded(), acceptorSignatures);
  }
}
