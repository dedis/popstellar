package com.github.dedis.popstellar.model.network.method.message.data.election;

import com.github.dedis.popstellar.model.objects.Election;
import com.google.gson.annotations.SerializedName;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

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
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ElectionQuestion that = (ElectionQuestion) o;
    return getWriteIn() == that.getWriteIn()
        && Objects.equals(getId(), that.getId())
        && Objects.equals(getQuestion(), that.getQuestion())
        && Objects.equals(getVotingMethod(), that.getVotingMethod())
        && Objects.equals(getBallotOptions(), that.getBallotOptions());
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        getId(), getQuestion(), getVotingMethod(), getBallotOptions(), getWriteIn());
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
