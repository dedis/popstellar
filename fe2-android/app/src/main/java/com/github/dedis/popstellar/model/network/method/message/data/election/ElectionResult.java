package com.github.dedis.popstellar.model.network.method.message.data.election;

import com.github.dedis.popstellar.model.network.method.message.data.Objects;
import com.github.dedis.popstellar.model.network.method.message.data.*;

import java.util.*;

public class ElectionResult extends Data {

  private final List<ElectionResultQuestion> questions;

  public ElectionResult(List<ElectionResultQuestion> questions) {
    if (questions == null || questions.isEmpty()) {
      throw new IllegalArgumentException();
    }
    this.questions = Collections.unmodifiableList(questions);
  }

  @Override
  public String getObject() {
    return Objects.ELECTION.getObject();
  }

  @Override
  public String getAction() {
    return Action.RESULT.getAction();
  }

  public List<ElectionResultQuestion> getElectionQuestionResults() {
    return questions;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ElectionResult that = (ElectionResult) o;

    return java.util.Objects.equals(questions, that.questions);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(questions);
  }

  @Override
  public String toString() {
    return "ElectionResult{questions=" + Arrays.toString(questions.toArray()) + '}';
  }
}
