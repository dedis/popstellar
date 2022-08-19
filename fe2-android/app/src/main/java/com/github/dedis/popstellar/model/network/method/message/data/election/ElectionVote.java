package com.github.dedis.popstellar.model.network.method.message.data.election;

import androidx.annotation.NonNull;

import com.github.dedis.popstellar.model.Immutable;
import com.github.dedis.popstellar.model.objects.Election;
import com.google.gson.annotations.SerializedName;

import java.util.Objects;

@Immutable
public class ElectionVote {

  @SerializedName(value = "id")
  private final String id;

  // Id of the object ElectionVote : Hash(“Vote”||election_id|| ||
  // JsonTestUtils.testData(castOpenVote);
  @SerializedName(value = "question")
  private final String questionId; // id of the question

  // represents a boolean to know whether write_in is allowed or not
  // list of indexes for the votes
  @SerializedName(value = "vote")
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
  public ElectionVote(
      String questionId, Integer vote, boolean writeInEnabled, String writeIn, String electionId) {

    this.questionId = questionId;
    this.vote = writeInEnabled ? null : vote;
    this.id =
        Election.generateElectionVoteId(electionId, questionId, vote, writeIn, writeInEnabled);
  }

  public String getId() {
    return id;
  }

  public String getQuestionId() {
    return questionId;
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
    ElectionVote that = (ElectionVote) o;
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
