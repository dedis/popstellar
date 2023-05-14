package com.github.dedis.popstellar.model.network.method.message.data.election;

import androidx.annotation.NonNull;

import com.github.dedis.popstellar.model.Immutable;
import com.github.dedis.popstellar.model.network.method.message.data.*;
import com.github.dedis.popstellar.utility.MessageValidator;
import com.google.gson.annotations.SerializedName;

/**
 * ElectionKey message is sent by the backend after opening an election with SECRET_BALLOT option,
 * it should contain the public key to encrypt the votes.
 */
@Immutable
public class ElectionKey extends Data {

  // Id of the election
  @SerializedName("election")
  private final String electionId;

  // Public key of the election for casting encrypted votes
  @SerializedName("election_key")
  private final String electionVoteKey;

  public ElectionKey(@NonNull String electionId, @NonNull String electionVoteKey) {
    MessageValidator.verify()
        .isNotEmptyBase64(electionId, "election id")
        .isNotEmptyBase64(electionVoteKey, "election vote key");

    this.electionId = electionId;
    this.electionVoteKey = electionVoteKey;
  }

  @NonNull
  public String getElectionVoteKey() {
    return electionVoteKey;
  }

  @NonNull
  public String getElectionId() {
    return electionId;
  }

  @Override
  public String getAction() {
    return Action.KEY.getAction();
  }

  @Override
  public String getObject() {
    return Objects.ELECTION.getObject();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ElectionKey that = (ElectionKey) o;
    return electionVoteKey.equals(that.getElectionVoteKey())
        && electionId.equals(that.getElectionId());
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(electionId, electionVoteKey);
  }

  @NonNull
  @Override
  public String toString() {
    return String.format(
        "ElectionKey{election='%s', election_key='%s'}", electionId, electionVoteKey);
  }
}
