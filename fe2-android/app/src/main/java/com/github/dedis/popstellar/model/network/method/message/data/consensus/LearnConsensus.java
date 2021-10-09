package com.github.dedis.popstellar.model.network.method.message.data.consensus;

import com.github.dedis.popstellar.model.network.method.message.data.Action;
import com.github.dedis.popstellar.model.network.method.message.data.Data;
import com.github.dedis.popstellar.model.network.method.message.data.Objects;
import com.google.gson.annotations.SerializedName;
import java.util.List;

public final class LearnConsensus extends Data {

  @SerializedName("message_id")
  private final String messageId;

  private final List<String> acceptors;

  public LearnConsensus(String messageId, List<String> acceptors) {
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
    return Action.PHASE_1_LEARN.getAction();
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
    LearnConsensus that = (LearnConsensus) o;

    return messageId.equals(that.messageId) && acceptors.equals(that.acceptors);
  }

  @Override
  public String toString() {
    return String.format("LearnConsensus{message_id='%s', acceptors=%s}", messageId, acceptors);
  }
}
