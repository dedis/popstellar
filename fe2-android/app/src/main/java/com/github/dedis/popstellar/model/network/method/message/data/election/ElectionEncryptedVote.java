package com.github.dedis.popstellar.model.network.method.message.data.election;

import androidx.annotation.NonNull;

import com.github.dedis.popstellar.model.Immutable;
import com.github.dedis.popstellar.model.objects.Election;
import com.google.gson.annotations.SerializedName;

import java.util.Objects;

@Immutable
public class ElectionEncryptedVote {

  // Id of the object ElectionVote :
  // Hash(“Vote”||election_id||question_id||(encrypted_vote_index(es)|encrypted_write_in))
  private final String id;

  // Id of the question
  @SerializedName(value = "question")
  private final String questionId;

  // Vote array containing index corresponding to ballot options
  private final String vote;

  /**
   * @param questionId id of the question
   * @param encryptedVote encrypted unique indices of the chosen vote
   * @param writeInEnabled indicates if write in is enabled
   * @param encryptedWriteIn whether the election allows write-in
   * @param electionId the election id
   */
  public ElectionEncryptedVote(
      @NonNull String questionId,
      @NonNull String encryptedVote,
      @NonNull Boolean writeInEnabled,
      String encryptedWriteIn,
      @NonNull String electionId) {

    this.questionId = questionId;
    this.id =
        Election.generateEncryptedElectionVoteId(
            electionId, questionId, encryptedVote, encryptedWriteIn, writeInEnabled);
    if (Boolean.TRUE.equals(writeInEnabled)) {
      this.vote = null;
    } else {
      this.vote = encryptedVote;
    }
  }

  @NonNull
  public String getVote() {
    return vote;
  }

  @NonNull
  public String getId() {
    return id;
  }

  @NonNull
  public String getQuestionId() {
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
    return Objects.equals(getQuestionId(), that.getQuestionId())
        && Objects.equals(getId(), that.getId())
        && Objects.equals(getVote(), that.getVote());
  }

  @NonNull
  @Override
  public String toString() {
    return "{" + "id='" + id + '\'' + ", questionId='" + questionId + '\'' + ", vote=" + vote + '}';
  }
}
