package com.github.dedis.popstellar.model.network.method.message.data.election;

import com.github.dedis.popstellar.model.network.method.message.data.Action;
import com.github.dedis.popstellar.model.network.method.message.data.Data;
import com.github.dedis.popstellar.model.network.method.message.data.Objects;

import java.util.Arrays;
import java.util.List;

public class ElectionResult extends Data {

  private List<ElectionResultQuestion> questions;

  public ElectionResult(List<ElectionResultQuestion> questions) {
    if (questions == null || questions.isEmpty()) {
      throw new IllegalArgumentException();
    }
    this.questions = questions;
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
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
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
