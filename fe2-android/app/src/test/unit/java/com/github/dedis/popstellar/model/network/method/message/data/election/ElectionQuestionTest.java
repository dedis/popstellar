package com.github.dedis.popstellar.model.network.method.message.data.election;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.github.dedis.popstellar.model.network.JsonTestUtils;
import com.github.dedis.popstellar.utility.security.Hash;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static com.github.dedis.popstellar.model.network.method.message.data.election.ElectionVersion.OPEN_BALLOT;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

@RunWith(AndroidJUnit4.class)
public class ElectionQuestionTest {

  private static final ElectionVersion VERSION = OPEN_BALLOT;
  private static final String LAO_ID = Hash.hash("laoId");
  private static final String NAME = "name";
  private static final long NOW = Instant.now().getEpochSecond();
  private static final long END = NOW + 30L;
  private static final String VOTING_METHOD = "Plurality";
  private static final String QUESTION = "Question";
  private static final List<String> BALLOT_OPTIONS = Arrays.asList("a", "b");
  private static final ElectionQuestion.Question QUESTION1 =
      new ElectionQuestion.Question("Question", VOTING_METHOD, BALLOT_OPTIONS, false);
  private static final ElectionQuestion.Question QUESTION2 =
      new ElectionQuestion.Question("Question2", VOTING_METHOD, BALLOT_OPTIONS, false);
  private static final ElectionQuestion.Question QUESTION3 =
      new ElectionQuestion.Question("Question2", VOTING_METHOD, BALLOT_OPTIONS, false);

  private static final List<ElectionQuestion.Question> QUESTIONS =
      Arrays.asList(QUESTION1, QUESTION2);

  private static final ElectionSetup ELECTION_SETUP =
      new ElectionSetup(NAME, NOW, NOW, END, LAO_ID, VERSION, QUESTIONS);

  private static final ElectionQuestion ELECTION_QUESTION = ELECTION_SETUP.getQuestions().get(0);

  @Before
  public void setup() {
    JsonTestUtils.loadGSON(ApplicationProvider.getApplicationContext());
  }

  @Test
  public void electionQuestionGetterReturnsCorrectId() {
    // Hash(“Question”||election_id||question)
    String expectedId = Hash.hash("Question", ELECTION_SETUP.getElectionId(), QUESTION);
    assertThat(ELECTION_SETUP.getQuestions().get(0).getId(), is(expectedId));
  }

  @Test
  public void electionQuestionGetterReturnsCorrectQuestion() {
    assertThat(ELECTION_QUESTION.getQuestion(), is(QUESTION));
  }

  @Test
  public void electionQuestionGetterReturnsCorrectVotingMethod() {
    assertThat(ELECTION_QUESTION.getVotingMethod(), is(VOTING_METHOD));
  }

  @Test
  public void electionQuestionGetterReturnsCorrectWriteIn() {
    assertThat(ELECTION_QUESTION.getWriteIn(), is(false));
  }

  @Test
  public void electionQuestionGetterReturnsCorrectBallotOptions() {
    assertThat(ELECTION_QUESTION.getBallotOptions(), is(BALLOT_OPTIONS));
  }

  @Test
  public void testEquals() {
    assertNotEquals(QUESTION1, QUESTION2);
    assertEquals(QUESTION2, QUESTION3);
  }

  @Test
  public void jsonValidationTest() {
    JsonTestUtils.testData(ELECTION_SETUP);
  }
}
