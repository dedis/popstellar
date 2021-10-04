package com.github.dedis.popstellar.model.network.method.message.data.consensus;

import com.github.dedis.popstellar.model.network.method.message.data.Action;
import com.github.dedis.popstellar.model.network.method.message.data.Data;
import com.github.dedis.popstellar.model.network.method.message.data.Objects;
import com.google.gson.annotations.SerializedName;
import java.util.Set;

public class LearnConsensus extends Data {

  @SerializedName("message_id")
  private final String messageId;
  private final Set<String> acceptors;

  public LearnConsensus(String messageId, Set<String> acceptors) {
    this.messageId = messageId;
    this.acceptors = acceptors;
  }


  public String getMessageId() {
    return messageId;
  }

  public Set<String> getAcceptors() {
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

}
