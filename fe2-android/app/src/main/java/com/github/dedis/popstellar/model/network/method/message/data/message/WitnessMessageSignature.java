package com.github.dedis.popstellar.model.network.method.message.data.message;

import androidx.annotation.NonNull;

import com.github.dedis.popstellar.model.Immutable;
import com.github.dedis.popstellar.model.network.method.message.data.*;
import com.github.dedis.popstellar.model.objects.security.MessageID;
import com.github.dedis.popstellar.model.objects.security.Signature;
import com.google.gson.annotations.SerializedName;

/** Data sent to attest the message as a witness */
@Immutable
public class WitnessMessageSignature extends Data {

  @SerializedName("message_id")
  private final MessageID messageId;

  private final Signature signature;

  /**
   * Constructor for a data Witness Message Signature
   *
   * @param messageId ID of the message
   * @param signature signature by the witness over the message ID
   */
  public WitnessMessageSignature(MessageID messageId, Signature signature) {
    this.messageId = messageId;
    this.signature = signature;
  }

  public MessageID getMessageId() {
    return messageId;
  }

  public Signature getSignature() {
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
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    WitnessMessageSignature that = (WitnessMessageSignature) o;
    return java.util.Objects.equals(getMessageId(), that.getMessageId())
        && java.util.Objects.equals(getSignature(), that.getSignature());
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(getMessageId(), getSignature());
  }

  @NonNull
  @Override
  public String toString() {
    return "WitnessMessageSignature{"
        + "messageId='"
        + messageId
        + '\''
        + ", signature='"
        + signature
        + '\''
        + '}';
  }
}
