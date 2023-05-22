package com.github.dedis.popstellar.model.network.method.message.data.election;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.github.dedis.popstellar.model.network.method.message.data.Objects;
import com.github.dedis.popstellar.model.network.method.message.data.*;
import com.github.dedis.popstellar.utility.MessageValidator;
import com.google.gson.annotations.SerializedName;

import java.time.Instant;
import java.util.*;

public class CastVote extends Data {

  @SerializedName(value = "created_at")
  private final long createdAt; // time the votes were submitted

  @SerializedName(value = "lao")
  private final String laoId; // Id of the lao

  @SerializedName(value = "election")
  private final String electionId; // Id of the election

  // Votes, either votes is null or encrypted votes is null depending on the value of the election
  // Type must be specified upon creation of the cast vote (either ElectionVote or
  // ElectionEncryptedVote)
  private final List<? extends Vote> votes;

  /**
   * @param votes list of the votes to cast (null if this is an OPEN_BALLOT election)
   * @param electionId election id
   * @param laoId lao id
   */
  public CastVote(
      @Nullable List<? extends Vote> votes, @NonNull String electionId, @NonNull String laoId) {
    // Lao id and election id are checked to match existing ones in the cast vote handler
    MessageValidator.verify()
        .isNotEmptyBase64(electionId, "election id")
        .isNotEmptyBase64(laoId, "lao id")
        .validVotes(votes);

    this.createdAt = Instant.now().getEpochSecond();
    this.electionId = electionId;
    this.laoId = laoId;
    this.votes = votes;
  }

  /**
   * Constructor used while receiving a CastVote message
   *
   * @param votes list of the votes to cast (null if this is an OPEN_BALLOT election)
   * @param electionId election id
   * @param laoId lao id
   * @param createdAt timestamp for creation
   */
  public CastVote(List<? extends Vote> votes, String electionId, String laoId, Long createdAt) {
    // Lao id and election id are checked to match existing ones in the cast vote handler
    MessageValidator.verify()
        .isNotEmptyBase64(electionId, "election id")
        .isNotEmptyBase64(laoId, "lao id")
        .validVotes(votes)
        .validPastTimes(createdAt);

    this.createdAt = createdAt;
    this.electionId = electionId;
    this.laoId = laoId;
    this.votes = votes;
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

  public List<Vote> getVotes() {
    return new ArrayList<>(votes);
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

  @NonNull
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
