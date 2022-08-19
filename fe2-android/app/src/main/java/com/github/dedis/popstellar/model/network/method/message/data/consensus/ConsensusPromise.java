package com.github.dedis.popstellar.model.network.method.message.data.consensus;

import com.github.dedis.popstellar.model.Immutable;
import com.github.dedis.popstellar.model.network.method.message.data.*;
import com.github.dedis.popstellar.model.objects.security.MessageID;
import com.google.gson.annotations.SerializedName;

@Immutable
public final class ConsensusPromise extends Data {

  @SerializedName("instance_id")
  private final String instanceId;

  @SerializedName("message_id")
  private final MessageID messageId;

  @SerializedName("created_at")
  private final long creation;

  @SerializedName("value")
  private final PromiseValue promiseValue;

  /**
   * Constructor for a data Promise
   *
   * @param instanceId unique id of the consensus instance
   * @param messageId message id of the Elect message
   * @param creation UNIX timestamp in UTC
   * @param acceptedTry previous accepted try number
   * @param acceptedValue previous accepted value
   * @param promisedTry promised try number
   */
  public ConsensusPromise(
      String instanceId,
      MessageID messageId,
      long creation,
      int acceptedTry,
      boolean acceptedValue,
      int promisedTry) {
    this.instanceId = instanceId;
    this.messageId = messageId;
    this.creation = creation;
    this.promiseValue = new PromiseValue(acceptedTry, acceptedValue, promisedTry);
  }

  @Override
  public String getObject() {
    return Objects.CONSENSUS.getObject();
  }

  @Override
  public String getAction() {
    return Action.PROMISE.getAction();
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

  public PromiseValue getPromiseValue() {
    return promiseValue;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ConsensusPromise that = (ConsensusPromise) o;

    return creation == that.creation
        && java.util.Objects.equals(instanceId, that.instanceId)
        && java.util.Objects.equals(messageId, that.messageId)
        && java.util.Objects.equals(promiseValue, that.promiseValue);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(instanceId, messageId, creation, promiseValue);
  }

  @Override
  public String toString() {
    return String.format(
        "ConsensusPromise{instance_id='%s', message_id='%s', created_at=%s, value=%s}",
        instanceId, messageId.getEncoded(), creation, promiseValue);
  }
}
