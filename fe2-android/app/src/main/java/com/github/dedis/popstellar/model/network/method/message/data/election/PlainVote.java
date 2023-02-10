package com.github.dedis.popstellar.model.network.method.message.data.election;

import androidx.annotation.NonNull;

import com.github.dedis.popstellar.model.Immutable;
import com.google.gson.annotations.SerializedName;

import java.util.Objects;

import static com.github.dedis.popstellar.model.objects.Election.generateElectionVoteId;

@Immutable
public class PlainVote implements Vote {

  // Id of the vote
  // Hash('Vote'||election_id||question_id||(vote_index(es)|write_in))
  private final String id;

  // Id of the object ElectionVote
  @SerializedName(value = "question")
  private final String questionId;

  // index of the chosen vote
  private final Integer vote;

  /**
   * Constructor for a data Vote, for cast vote . It represents a Vote for one Question.
   *
   * @param questionId the Id of the question
   * @param vote the list of indexes for the ballot options chose by the voter
   * @param writeInEnabled parameter to know if write is enabled or not
   * @param writeIn string corresponding to the write_in
   * @param electionId Id of the election
   */
  public PlainVote(
      String questionId, Integer vote, boolean writeInEnabled, String writeIn, String electionId) {
    this.questionId = questionId;
    this.vote = writeInEnabled ? null : vote;
    this.id = generateElectionVoteId(electionId, questionId, vote, writeIn, writeInEnabled);
  }

  public PlainVote(String id, String question, int vote) {
    this.id = id;
    this.questionId = question;
    this.vote = vote;
  }

  @NonNull
  public String getQuestionId() {
    return questionId;
  }

  @NonNull
  public String getId() {
    return id;
  }

  @Override
  public boolean isEncrypted() {
    return false;
  }

  public Integer getVote() {
    return vote;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PlainVote that = (PlainVote) o;
    return Objects.equals(getQuestionId(), that.getQuestionId())
        && Objects.equals(getId(), that.getId())
        && Objects.equals(getVote(), that.getVote());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getId(), getVote(), getQuestionId());
  }

  @NonNull
  @Override
  public String toString() {
    return "ElectionVote{"
        + "id='"
        + id
        + '\''
        + ", questionId='"
        + questionId
        + '\''
        + ", vote="
        + vote
        + '}';
  }
}
