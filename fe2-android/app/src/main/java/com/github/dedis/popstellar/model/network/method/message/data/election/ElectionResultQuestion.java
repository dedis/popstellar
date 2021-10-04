package com.github.dedis.popstellar.model.network.method.message.data.election;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

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
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ElectionResultQuestion that = (ElectionResultQuestion) o;
    return Objects.equals(getId(), that.getId()) && Objects.equals(getResult(), that.getResult());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getId(), getResult());
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
