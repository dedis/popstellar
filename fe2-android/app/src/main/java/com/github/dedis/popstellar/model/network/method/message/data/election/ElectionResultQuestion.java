package com.github.dedis.popstellar.model.network.method.message.data.election;

import com.github.dedis.popstellar.model.Immutable;

import java.util.*;

@Immutable
public class ElectionResultQuestion {

  private final String id;
  private final List<QuestionResult> result;

  public ElectionResultQuestion(String id, List<QuestionResult> result) {
    if (id == null || result == null || result.isEmpty()) {
      throw new IllegalArgumentException();
    }
    this.id = id;
    this.result = Collections.unmodifiableList(result);
  }

  public String getId() {
    return id;
  }

  public List<QuestionResult> getResult() {
    return result;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ElectionResultQuestion that = (ElectionResultQuestion) o;

    return Objects.equals(id, that.id) && Objects.equals(result, that.result);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, result);
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
