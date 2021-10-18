package com.github.dedis.popstellar.model.network.method.message.data.election;

import com.github.dedis.popstellar.model.network.method.message.data.Action;
import com.github.dedis.popstellar.model.network.method.message.data.Data;
import com.github.dedis.popstellar.model.network.method.message.data.Objects;
import com.google.gson.annotations.SerializedName;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class CastVote extends Data {

  @SerializedName(value = "created_at")
  private long createdAt; // time the votes were submitted

  @SerializedName(value = "lao")
  private String laoId; // Id of the lao

  @SerializedName(value = "election")
  private String electionId; // Id of the election

  private List<ElectionVote> votes;

  /**
   * @param votes list of the Election Vote where an ElectionVote Object represents the
   *     corresponding votes for one question
   * @param electionId Id of the election for which to votee
   * @param laoId id of the LAO
   */
  public CastVote(List<ElectionVote> votes, String electionId, String laoId) {
    this.createdAt = Instant.now().getEpochSecond();
    this.votes = votes;
    this.electionId = electionId;
    this.laoId = laoId;
  }

  public String getLaoId() {
    return laoId;
  }

  public String getElectionId() {
    return electionId;
  }

  public long getCreation() {
    return createdAt;
  }

  public List<ElectionVote> getVotes() {
    return Collections.unmodifiableList(votes);
  }

  @Override
  public String getObject() {
    return Objects.ELECTION.getObject();
  }

  @Override
  public String getAction() {
    return Action.CAST_VOTE.getAction();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CastVote that = (CastVote) o;
    return java.util.Objects.equals(getLaoId(), that.getLaoId())
        && createdAt == that.getCreation()
        && java.util.Objects.equals(electionId, that.getElectionId())
        && java.util.Objects.equals(laoId, that.getLaoId())
        && java.util.Objects.equals(votes, that.getVotes());
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(getLaoId(), getElectionId(), getCreation(), getVotes());
  }

  @Override
  public String toString() {
    return "CastVote{"
        + "createdAt="
        + createdAt
        + ", laoId='"
        + laoId
        + '\''
        + ", electionId='"
        + electionId
        + '\''
        + ", votes="
        + Arrays.toString(votes.toArray())
        + '}';
  }
}
