package com.github.dedis.popstellar.model.network.method.message.data.election;

import androidx.annotation.NonNull;

import com.github.dedis.popstellar.model.objects.Election;
import com.google.gson.annotations.SerializedName;

import java.util.Arrays;
import java.util.List;

public class ElectionEncryptedVote {

  // Id of the object ElectionVote :
  // Hash(“Vote”||election_id||question_id||(encrypted_vote_index(es)|encrypted_write_in))
  private final String id;

  // Id of the question
  @SerializedName(value = "question")
  private final String questionId;

  // Vote array containing index corresponding to ballot options
  private final List<String> vote;

  /**
   * @param questionId       id of the question
   * @param encryptedVotes   list of encrypted votes
   * @param writeInEnabled   indicates if write in is enabled
   * @param encryptedWriteIn
   * @param electionId
   */
  public ElectionEncryptedVote(
          @NonNull String questionId, @NonNull List<String> encryptedVotes, @NonNull Boolean writeInEnabled, @NonNull String encryptedWriteIn, @NonNull String electionId) {

    this.questionId = questionId;
    this.id =
            Election.generateEncryptedElectionVoteId(electionId, questionId, encryptedVotes, encryptedWriteIn, writeInEnabled);
    this.vote = writeInEnabled ? null : encryptedVotes;
  }

  @NonNull
  public List<String> getVote(){
    return vote;
  }

  @NonNull
  public String getId(){
    return id;
  }

  @NonNull
  public String getQuestionId(){
    return questionId;
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(getId(), getVote(), getQuestionId());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ElectionEncryptedVote that = (ElectionEncryptedVote) o;
    return java.util.Objects.equals(getQuestionId(), that.getQuestionId())
            && java.util.Objects.equals(getId(), that.getId())
            && java.util.Objects.equals(getVote(), that.getVote());
  }

  @Override
  public String toString() {
    return "{"
            + "id='"
            + id
            + '\''
            + ", questionId='"
            + questionId
            + '\''
            + ", vote="
            + Arrays.toString(vote.toArray())
            + '\''
        + '}';
  }

}
