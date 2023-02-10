package com.github.dedis.popstellar.model.network.method.message;

import com.github.dedis.popstellar.model.network.method.message.data.election.ElectionResultQuestion;
import com.github.dedis.popstellar.model.network.method.message.data.election.QuestionResult;

import org.junit.Test;

import java.util.Collections;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;

public class ElectionQuestionResultTest {

  private final String questionId = "questionId";
  private final Set<QuestionResult> results =
      Collections.singleton(new QuestionResult("Candidate1", 30));
  private final ElectionResultQuestion electionQuestionResult =
      new ElectionResultQuestion(questionId, results);

  @Test
  public void electionQuestionResultGetterReturnsCorrectQuestionId() {
    assertThat(electionQuestionResult.getId(), is(questionId));
  }

  @Test
  public void electionQuestionResultGetterReturnsCorrectResults() {
    assertThat(electionQuestionResult.getResult(), is(results));
  }

  @Test
  public void resultsCantBeEmpty() {
    Set<QuestionResult> emptySet = Collections.emptySet();
    assertThrows(
        IllegalArgumentException.class, () -> new ElectionResultQuestion(questionId, emptySet));
  }
}
