package com.github.dedis.popstellar.model.network.method.message.data.consensus;

import com.github.dedis.popstellar.model.network.method.message.data.Action;
import com.github.dedis.popstellar.model.network.method.message.data.Data;
import com.github.dedis.popstellar.model.network.method.message.data.Objects;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public final class ConsensusLearn extends Data {

  @SerializedName("message_id")
  private final String messageId;

  private final List<String> acceptors;

  public ConsensusLearn(String messageId, List<String> acceptors) {
    this.messageId = messageId;
    this.acceptors = acceptors;
  }

  public String getMessageId() {
    return messageId;
  }

  public List<String> getAcceptors() {
    return acceptors;
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
    return java.util.Objects.hash(messageId, acceptors);
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

    return java.util.Objects.equals(messageId, that.messageId)
        && java.util.Objects.equals(acceptors, that.acceptors);
  }

  @Override
  public String toString() {
    return String.format("ConsensusLearn{message_id='%s', acceptors=%s}", messageId, acceptors);
  }
}
