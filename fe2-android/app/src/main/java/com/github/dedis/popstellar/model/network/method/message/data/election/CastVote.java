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
  private final long createdAt; // time the votes were submitted

  @SerializedName(value = "lao")
  private final String laoId; // Id of the lao

  @SerializedName(value = "election")
  private final String electionId; // Id of the election

  // Votes, either votes is null or encrypted votes is null depending on the value of the election
  // either SECRET_BALLOT or OPEN_BALLOT
  private final List<ElectionVote> votes;
  private final List<ElectionEncryptedVote> encryptedVotes;

  /**
   * @param electionVotes list of the votes to cast (null if this is an OPEN_BALLOT election)
   * @param encryptedVotes list of the encrypted votes to cast (null if this is an OPEN_BALLOT election)
   * @param electionId election id
   * @param laoId lao id
   */
  public CastVote(List<ElectionVote> electionVotes,
                  List<ElectionEncryptedVote> encryptedVotes,
                  String electionId,
                  String laoId) {
    this.createdAt = Instant.now().getEpochSecond();
    this.votes = electionVotes;
    this.electionId = electionId;
    this.laoId = laoId;
    this.encryptedVotes=encryptedVotes;
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

  /**
   * @return null if the election is encrypted else the votes
   */
  public List<ElectionVote> getOpenBallotVotes() {
    return !java.util.Objects.isNull(votes) ? Collections.unmodifiableList(votes) : null;
  }

  /**
   * @return null if the election is with open ballots else the encrypted votes
   */
  public List<ElectionEncryptedVote> getEncryptedVotes() {
    return !java.util.Objects.isNull(encryptedVotes) ? Collections.unmodifiableList(encryptedVotes) : null;
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

    // Compare votes values given the version of the election
    boolean compareVotes =
            !java.util.Objects.isNull(votes)
                    ? java.util.Objects.equals(votes, that.getOpenBallotVotes())
                    : java.util.Objects.equals(encryptedVotes, that.getEncryptedVotes());

    return java.util.Objects.equals(getLaoId(), that.getLaoId())
        && createdAt == that.getCreation()
        && java.util.Objects.equals(electionId, that.getElectionId())
        && java.util.Objects.equals(laoId, that.getLaoId())
        && compareVotes;
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(getLaoId(), getElectionId(), getCreation(), getOpenBallotVotes());
  }

  @Override
  public String toString() {
    // If one is null take the other
    String votesFormat =
            !java.util.Objects.isNull(votes)
              ? Arrays.toString(votes.toArray())
              : Arrays.toString(encryptedVotes.toArray());
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
        + votesFormat
        + '}';
  }
}
