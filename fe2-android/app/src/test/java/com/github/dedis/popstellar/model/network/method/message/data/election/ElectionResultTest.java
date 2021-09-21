package com.github.dedis.popstellar.model.network.method.message.data.election;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;

import com.github.dedis.popstellar.model.network.method.message.data.Action;
import com.github.dedis.popstellar.model.network.method.message.data.ElectionResultQuestion;
import com.github.dedis.popstellar.model.network.method.message.data.Objects;
import com.github.dedis.popstellar.model.network.method.message.data.QuestionResult;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public class ElectionResultTest {

  private List<QuestionResult> results = Arrays.asList(new QuestionResult("Candidate1", 40));
  private List<ElectionResultQuestion> questions = Arrays
      .asList(new ElectionResultQuestion("question id", results));
  private ElectionResult electionResult = new ElectionResult(questions);

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
}
