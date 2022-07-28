package com.github.dedis.popstellar.model.network.method.message.data.election;

import androidx.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

import java.util.Objects;

public class QuestionResult {

  @SerializedName(value = "ballot_option")
  private final String ballotOption;

  private final int count;

  public QuestionResult(String ballotOption, int count) {
    if (ballotOption == null) {
      throw new IllegalArgumentException();
    }
    this.ballotOption = ballotOption;
    this.count = count;
  }

  public QuestionResult(QuestionResult questionResult) {
    this(questionResult.ballotOption, questionResult.count);
  }

  public String getBallot() {
    return ballotOption;
  }

  public Integer getCount() {
    return count;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    QuestionResult that = (QuestionResult) o;

    return count == that.count && Objects.equals(ballotOption, that.ballotOption);
  }

  @Override
  public int hashCode() {
    return Objects.hash(ballotOption, count);
  }

  @NonNull
  @Override
  public String toString() {
    return "QuestionResult{" + "ballotOption='" + ballotOption + '\'' + ", count=" + count + '}';
  }
}
