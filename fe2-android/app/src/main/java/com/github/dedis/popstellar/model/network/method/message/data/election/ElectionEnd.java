package com.github.dedis.popstellar.model.network.method.message.data.election;

import com.github.dedis.popstellar.model.network.method.message.data.Action;
import com.github.dedis.popstellar.model.network.method.message.data.Data;
import com.github.dedis.popstellar.model.network.method.message.data.Objects;
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
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ElectionEnd that = (ElectionEnd) o;
    return getCreatedAt() == that.getCreatedAt()
        && java.util.Objects.equals(getElectionId(), that.getElectionId())
        && java.util.Objects.equals(getLaoId(), that.getLaoId())
        && java.util.Objects.equals(getRegisteredVotes(), that.getRegisteredVotes());
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(
        getElectionId(), getCreatedAt(), getLaoId(), getRegisteredVotes());
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
