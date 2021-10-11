package com.github.dedis.popstellar.model.network.method.message.data.election;

import com.github.dedis.popstellar.model.network.method.message.data.Action;
import com.github.dedis.popstellar.model.network.method.message.data.Data;
import com.github.dedis.popstellar.model.network.method.message.data.Objects;
import com.google.gson.annotations.SerializedName;

import java.time.Instant;

public class ElectionEnd extends Data {

  @SerializedName(value = "election")
  private String electionId;

  @SerializedName(value = "created_at")
  private long createdAt;

  @SerializedName(value = "lao")
  private String laoId;

  @SerializedName(value = "registered_votes")
  private String registeredVotes; // hashed

  public ElectionEnd(String electionId, String laoId, String registeredVotes) {
    if (electionId == null || laoId == null || registeredVotes == null) {
      throw new IllegalArgumentException();
    }
    this.createdAt = Instant.now().getEpochSecond();
    this.electionId = electionId;
    this.laoId = laoId;
    this.registeredVotes = registeredVotes;
  }

  @Override
  public String getObject() {
    return Objects.ELECTION.getObject();
  }

  @Override
  public String getAction() {
    return Action.END.getAction();
  }

  public String getLaoId() {
    return laoId;
  }

  public String getElectionId() {
    return electionId;
  }

  public String getRegisteredVotes() {
    return registeredVotes;
  }

  public long getCreatedAt() {
    return createdAt;
  }
}
