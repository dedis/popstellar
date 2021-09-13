package com.github.dedis.student20_pop.model.network.method.message.data.election;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import com.github.dedis.student20_pop.model.network.method.message.data.ElectionQuestion;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public class ElectionQuestionTest {

  private String electionId = "my election id";
  private String votingMethod = "Plurality";
  private boolean writeIn = false;
  private List<String> ballotOptions = Arrays.asList("candidate1", "candidate2");
  private String question = "which is the best ?";

  ElectionQuestion electionQuestion = new ElectionQuestion(question, votingMethod, writeIn,
      ballotOptions, electionId);

  @Test
  public void electionQuestionGetterReturnsCorrectQuestion() {
    assertThat(electionQuestion.getQuestion(), is(question));
  }

  @Test
  public void electionQuestionGetterReturnsCorrectVotingMethod() {
    assertThat(electionQuestion.getVotingMethod(), is(votingMethod));
  }

  @Test
  public void electionQuestionGetterReturnsCorrectWriteIn() {
    assertThat(electionQuestion.getWriteIn(), is(writeIn));
  }

  @Test
  public void electionQuestionGetterReturnsCorrectBallotOptions() {
    assertThat(electionQuestion.getBallotOptions(), is(ballotOptions));
  }

}
