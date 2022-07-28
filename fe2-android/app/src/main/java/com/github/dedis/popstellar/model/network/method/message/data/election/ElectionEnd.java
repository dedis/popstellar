package com.github.dedis.popstellar.model.network.method.message.data.election;

import com.github.dedis.popstellar.model.network.method.message.data.*;
import com.google.gson.annotations.SerializedName;

import java.time.Instant;

public class ElectionEnd extends Data {

  @SerializedName(value = "election")
  private final String electionId;

  @SerializedName(value = "created_at")
  private final long createdAt;

  @SerializedName(value = "lao")
  private final String laoId;

  @SerializedName(value = "registered_votes")
  private final String registeredVotes; // hashed

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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ElectionEnd that = (ElectionEnd) o;

    return createdAt == that.createdAt
        && java.util.Objects.equals(electionId, that.electionId)
        && java.util.Objects.equals(laoId, that.laoId)
        && java.util.Objects.equals(registeredVotes, that.registeredVotes);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(electionId, createdAt, laoId, registeredVotes);
  }

  @Override
  public String toString() {
    return "ElectionEnd{"
        + "electionId='"
        + electionId
        + '\''
        + ", createdAt="
        + createdAt
        + ", laoId='"
        + laoId
        + '\''
        + ", registeredVotes='"
        + registeredVotes
        + '\''
        + '}';
  }
}
