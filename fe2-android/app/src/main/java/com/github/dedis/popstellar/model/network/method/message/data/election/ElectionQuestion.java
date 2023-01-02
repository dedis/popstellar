package com.github.dedis.popstellar.model.network.method.message.data.election;

import androidx.annotation.NonNull;

import com.github.dedis.popstellar.model.Immutable;
import com.github.dedis.popstellar.model.objects.Election;
import com.google.gson.annotations.SerializedName;

import java.util.*;

@Immutable
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
  public ElectionQuestion(String electionId, Question question) {
    this.question = question.question;
    this.ballotOptions = Collections.unmodifiableList(question.ballotOptions);
    this.writeIn = question.writeIn;
    this.votingMethod = question.votingMethod;

    this.id = Election.generateElectionQuestionId(electionId, this.question);
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
    return ballotOptions;
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

  @NonNull
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

  /**
   * This data class holds the information of an ElectionQuestion except its id.
   *
   * <p>This is used to pack the question data when the election id is not available yet
   */
  @Immutable
  public static class Question {

    private final String question;
    private final String votingMethod;
    private final List<String> ballotOptions;
    private final boolean writeIn;

    public Question(
        String question, String votingMethod, List<String> ballotOptions, boolean writeIn) {
      this.question = question;
      this.votingMethod = votingMethod;
      this.ballotOptions = ballotOptions;
      this.writeIn = writeIn;
    }

    @NonNull
    @Override
    public String toString() {
      return "Question{"
          + "question='"
          + question
          + '\''
          + ", votingMethod='"
          + votingMethod
          + '\''
          + ", ballotOptions="
          + ballotOptions
          + ", writeIn="
          + writeIn
          + '}';
    }
  }
}
