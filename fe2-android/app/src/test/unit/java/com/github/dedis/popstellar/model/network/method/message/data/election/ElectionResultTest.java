package com.github.dedis.popstellar.model.network.method.message.data.election;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.github.dedis.popstellar.model.network.JsonTestUtils;
import com.github.dedis.popstellar.model.network.method.message.data.Action;
import com.github.dedis.popstellar.model.network.method.message.data.Objects;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.*;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(AndroidJUnit4.class)
public class ElectionResultTest {

  private final Set<QuestionResult> results =
      Collections.singleton(new QuestionResult("Candidate1", 40));

  private final ElectionResultQuestion question =
      new ElectionResultQuestion("question id", results);
  private final List<ElectionResultQuestion> questions = Collections.singletonList(question);
  private final ElectionResult electionResult = new ElectionResult(questions);

  @Test(expected = IllegalArgumentException.class)
  public void questionsCantBeNull() {
    new ElectionResult(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void questionsCantBeEmpty() {
    List<ElectionResultQuestion> emptyList = new ArrayList<>();
    new ElectionResult(emptyList);
  }

  @Test(expected = IllegalArgumentException.class)
  public void questionsCantHaveDuplicates() {
    ElectionResultQuestion duplicate = new ElectionResultQuestion("question id", results);
    List<ElectionResultQuestion> duplicatesList = Arrays.asList(question, duplicate);
    new ElectionResult(duplicatesList);
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
