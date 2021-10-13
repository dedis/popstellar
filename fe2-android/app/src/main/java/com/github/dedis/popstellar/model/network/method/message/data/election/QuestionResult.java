package com.github.dedis.popstellar.model.network.method.message.data.election;

import com.google.gson.annotations.SerializedName;

public class QuestionResult {

  @SerializedName(value = "ballot_option")
  private String ballotOption;
  private int count;

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
  public String toString() {
    return "QuestionResult{" + "ballotOption='" + ballotOption + '\'' + ", count=" + count + '}';
  }
}
