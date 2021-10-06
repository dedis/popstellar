package com.github.dedis.popstellar.model.network.method.message;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;

import com.github.dedis.popstellar.model.network.method.message.data.election.QuestionResult;

import org.junit.Test;

public class QuestionResultTest {

  private final Integer count = 30;
  private final String name = "Candidate1";
  private final QuestionResult questionResult = new QuestionResult(name, count);

  @Test
  public void fieldsCantBeNull() {
    //noinspection ConstantConditions
    assertThrows(IllegalArgumentException.class, () -> new QuestionResult(null, 30));
  }

  @Test
  public void questionResultGetterReturnsCorrectName() {
    assertThat(questionResult.getBallot(), is(name));
  }

  @Test
  public void questionResultGetterReturnsCorrectCount() {
    assertThat(questionResult.getCount(), is(count));
  }
}
