package com.github.dedis.popstellar.model.network.method.message.data.election;

import com.github.dedis.popstellar.model.network.method.message.data.Action;
import com.github.dedis.popstellar.model.network.method.message.data.Data;
import com.github.dedis.popstellar.model.network.method.message.data.Objects;

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
}
