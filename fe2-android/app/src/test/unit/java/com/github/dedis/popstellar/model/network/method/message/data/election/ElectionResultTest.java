package com.github.dedis.popstellar.model.network.method.message.data.election;

import com.github.dedis.popstellar.model.network.JsonTestUtils;
import com.github.dedis.popstellar.model.network.method.message.data.Action;
import com.github.dedis.popstellar.model.network.method.message.data.Objects;

import org.junit.Test;

import java.util.*;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;

public class ElectionResultTest {

  private final Set<QuestionResult> results =
      Collections.singleton(new QuestionResult("Candidate1", 40));
  private final List<ElectionResultQuestion> questions =
      Collections.singletonList(new ElectionResultQuestion("question id", results));
  private final ElectionResult electionResult = new ElectionResult(questions);

  @Test
  public void questionsCantBeNull() {
    assertThrows(IllegalArgumentException.class, () -> new ElectionResult(null));
  }

  @Test
  public void questionsCantBeEmpty() {
    List<ElectionResultQuestion> emptyList = new ArrayList<>();
    assertThrows(IllegalArgumentException.class, () -> new ElectionResult(emptyList));
  }

  @Test
  public void electionResultGetterReturnsCorrectQuestions() {
    assertThat(electionResult.getElectionQuestionResults(), is(questions));
  }

  @Test
  public void electionResultGetterReturnsCorrectObject() {
    assertThat(electionResult.getObject(), is(Objects.ELECTION.getObject()));
  }

  @Test
  public void electionResultGetterReturnsCorrectAction() {
    assertThat(electionResult.getAction(), is(Action.RESULT.getAction()));
  }

  @Test
  public void jsonValidationTest() {
    JsonTestUtils.testData(electionResult);
  }
}
