package com.github.dedis.popstellar.model.network.method.message.data.consensus;

import com.github.dedis.popstellar.model.network.method.message.data.Action;
import com.github.dedis.popstellar.model.network.method.message.data.Data;
import com.github.dedis.popstellar.model.network.method.message.data.Objects;
import com.google.gson.annotations.SerializedName;

public final class ConsensusElectAccept extends Data {

  @SerializedName("message_id")
  private final String messageId;

  private final boolean accept;

  public ConsensusElectAccept(String messageId, boolean accept) {
    this.messageId = messageId;
    this.accept = accept;
  }

  public String getMessageId() {
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
    return java.util.Objects.hash(messageId, accept);
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

    return messageId.equals(that.messageId) && accept == that.accept;
  }

  @Override
  public String toString() {
    return String.format("ConsensusElectAccept{message_id='%s', accept=%b}", messageId, accept);
  }
}
