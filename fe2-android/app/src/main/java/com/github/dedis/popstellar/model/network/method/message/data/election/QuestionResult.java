package com.github.dedis.popstellar.model.network.method.message.data.election;

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

  public String getBallot() {
    return ballotOption;
  }

  public Integer getCount() {
    return count;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    QuestionResult that = (QuestionResult) o;
    return getCount().equals(that.getCount()) && Objects.equals(ballotOption, that.ballotOption);
  }

  @Override
  public int hashCode() {
    return Objects.hash(ballotOption, getCount());
  }

  @Override
  public String toString() {
    return "QuestionResult{" + "ballotOption='" + ballotOption + '\'' + ", count=" + count + '}';
  }
}
