package com.github.dedis.popstellar.model.network.method.message.data.election;

import com.github.dedis.popstellar.model.objects.Election;
import com.google.gson.annotations.SerializedName;

import java.util.*;

public class ElectionQuestion {

  private final String id;
  private final String question;

  @SerializedName(value = "voting_method")
  private final String votingMethod;

  @SerializedName(value = "ballot_options")
  private final List<String> ballotOptions;

  @SerializedName(value = "write_in")
  private final boolean writeIn;

  /** Constructor for a data Question, for the election setup */
  public ElectionQuestion(
      String question,
      String votingMethod,
      boolean writeIn,
      List<String> ballotOptions,
      String electionId) {

    this.question = question;
    this.ballotOptions = ballotOptions;
    this.writeIn = writeIn;
    this.votingMethod = votingMethod;
    this.id = Election.generateElectionQuestionId(electionId, question);
  }

  public String getId() {
    return id;
  }

  public String getQuestion() {
    return question;
  }

  public boolean getWriteIn() {
    return writeIn;
  }

  public List<String> getBallotOptions() {
    return Collections.unmodifiableList(ballotOptions);
  }

  public String getVotingMethod() {
    return votingMethod;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ElectionQuestion that = (ElectionQuestion) o;
    return java.util.Objects.equals(getQuestion(), that.getQuestion())
        && getWriteIn() == that.getWriteIn()
        && java.util.Objects.equals(getId(), that.getId())
        && java.util.Objects.equals(getBallotOptions(), that.getBallotOptions())
        && java.util.Objects.equals(getVotingMethod(), that.getVotingMethod());
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(
        getId(), getVotingMethod(), getWriteIn(), getBallotOptions(), getQuestion());
  }

  @Override
  public String toString() {
    return "ElectionQuestion{"
        + "id='"
        + id
        + '\''
        + ", question='"
        + question
        + '\''
        + ", votingMethod='"
        + votingMethod
        + '\''
        + ", ballotOptions="
        + Arrays.toString(ballotOptions.toArray())
        + ", writeIn="
        + writeIn
        + '}';
  }
}
