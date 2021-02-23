package com.github.dedis.student20_pop.model.network.method.message.data.message;

import com.github.dedis.student20_pop.model.network.method.message.data.Action;
import com.github.dedis.student20_pop.model.network.method.message.data.Data;
import com.github.dedis.student20_pop.model.network.method.message.data.Objects;
import com.google.gson.annotations.SerializedName;

/** Data sent to attest the message as a witness */
public class WitnessMessage extends Data {

  @SerializedName("message_id")
  private final String messageId;

  private final String signature;

  /**
   * Constructor for a data Witness Message
   *
   * @param messageId ID of the message
   * @param signature signature by the witness over the message ID
   */
  public WitnessMessage(String messageId, String signature) {
    this.messageId = messageId;
    this.signature = signature;
  }

  public String getMessageId() {
    return messageId;
  }

  public String getSignature() {
    return signature;
  }

  @Override
  public String getObject() {
    return Objects.MESSAGE.getObject();
  }

  @Override
  public String getAction() {
    return Action.WITNESS.getAction();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    WitnessMessage that = (WitnessMessage) o;
    return java.util.Objects.equals(getMessageId(), that.getMessageId())
        && java.util.Objects.equals(getSignature(), that.getSignature());
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(getMessageId(), getSignature());
  }

  @Override
  public String toString() {
    return "WitnessMessage{"
        + "message_id='"
        + messageId
        + '\''
        + ", signature='"
        + signature
        + '\''
        + '}';
  }
}
