package com.github.dedis.popstellar.model.network.method.message.data.election;

import java.util.Arrays;
import java.util.List;

public class ElectionResultQuestion {

  private String id;
  private List<QuestionResult> result;

  public ElectionResultQuestion(String id, List<QuestionResult> result) {
    if (id == null || result == null || result.isEmpty()) {
      throw new IllegalArgumentException();
    }
    this.id = id;
    this.result = result;
  }

  public String getId() {
    return id;
  }

  public List<QuestionResult> getResult() {
    return result;
  }

  @Override
  public String toString() {
    return "ElectionResultQuestion{"
        + "id='"
        + id
        + '\''
        + ", result="
        + Arrays.toString(result.toArray())
        + '}';
  }
}
