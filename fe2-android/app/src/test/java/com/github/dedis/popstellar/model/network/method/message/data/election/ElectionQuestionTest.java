package com.github.dedis.popstellar.model.network.method.message.data.election;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import com.github.dedis.popstellar.utility.security.Hash;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public class ElectionQuestionTest {

  private final String laoId = Hash.hash("laoId");
  private final String name = "name";
  private final long now = Instant.now().getEpochSecond();
  private final long end = now + 30L;
  private final String votingMethod = "Plurality";
  private final String question = "Question";
  private final List<String> allMethods = Arrays.asList("Plurality", "Plurality");
  private final List<String> allQuestions = Arrays.asList("Question", "Question2");
  private final List<Boolean> allWriteIns = Arrays.asList(false, false);
  private final ElectionSetup electionSetup = new ElectionSetup(name, now, now, end, allMethods, allWriteIns,
      Arrays.asList(new ArrayList<>(), new ArrayList<>()), allQuestions, laoId);
  private final ElectionQuestion electionQuestion = electionSetup.getQuestions().get(0);

  @Test
  public void electionQuestionGetterReturnsCorrectId() {
    // Hash(“Question”||election_id||question)
    String expectedId = Hash
        .hash("Question", electionSetup.getId(), question);
    assertThat(electionSetup.getQuestions().get(0).getId(), is(expectedId));
  }

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
    assertThat(electionQuestion.getWriteIn(), is(allWriteIns.get(0)));
  }

  @Test
  public void electionQuestionGetterReturnsCorrectBallotOptions() {
    assertThat(electionQuestion.getBallotOptions(), is(new ArrayList<>()));
  }

}
