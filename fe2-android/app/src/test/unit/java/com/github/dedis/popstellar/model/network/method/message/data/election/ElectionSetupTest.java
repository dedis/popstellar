package com.github.dedis.popstellar.model.network.method.message.data.election;

import com.github.dedis.popstellar.model.network.JsonTestUtils;
import com.github.dedis.popstellar.model.network.method.message.data.Action;
import com.github.dedis.popstellar.model.network.method.message.data.Objects;
import com.github.dedis.popstellar.model.network.method.message.data.election.ElectionQuestion.Question;
import com.github.dedis.popstellar.model.objects.Election;
import com.github.dedis.popstellar.model.objects.event.EventType;
import com.github.dedis.popstellar.utility.security.Hash;

import org.junit.Test;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;

public class ElectionSetupTest {

  private static final String ELECTION_NAME = "New election";
  private static final long CREATION = 0;
  private static final long START = 0;
  private static final long END = 1;
  private static final List<Question> questions =
      Arrays.asList(
          new Question(
              "Which is the best ?", "Plurality", Arrays.asList("Option a", "Option b"), false),
          new Question(
              "Who is the best ?", "Plurality", Arrays.asList("candidate1", "candidate2"), false));

  private final String laoId = "my lao id";
  private final ElectionSetup openBallotSetup =
      new ElectionSetup(
          ELECTION_NAME, CREATION, START, END, laoId, ElectionVersion.OPEN_BALLOT, questions);
  private final ElectionSetup secretBallotSetup =
      new ElectionSetup(
          ELECTION_NAME, CREATION, START, END, laoId, ElectionVersion.SECRET_BALLOT, questions);

  @Test
  public void electionSetupGetterReturnsCorrectId() {
    // Hash('Election'||lao_id||created_at||name)
    String expectedId =
        Hash.hash(
            EventType.ELECTION.getSuffix(),
            openBallotSetup.getLao(),
            Long.toString(openBallotSetup.getCreation()),
            openBallotSetup.getName());
    assertThat(openBallotSetup.getId(), is(expectedId));
  }

  @Test
  public void getNameTest() {
    assertThat(openBallotSetup.getName(), is(ELECTION_NAME));
  }

  @Test
  public void getEndTimeTest() {
    assertThat(openBallotSetup.getEndTime(), is(END));
  }

  @Test
  public void getLaoTest() {
    assertThat(openBallotSetup.getLao(), is(laoId));
  }

  @Test
  public void electionSetupOnlyOneQuestion() {
    assertThat(openBallotSetup.getQuestions().size(), is(2));
  }

  @Test
  public void getObjectTest() {
    assertThat(openBallotSetup.getObject(), is(Objects.ELECTION.getObject()));
  }

  @Test
  public void getActionTest() {
    assertThat(openBallotSetup.getAction(), is(Action.SETUP.getAction()));
  }

  @Test
  public void getVersionTest() {
    assertEquals(
        ElectionVersion.OPEN_BALLOT.getStringBallotVersion(),
        openBallotSetup.getElectionVersion().getStringBallotVersion());
    assertEquals(
        ElectionVersion.SECRET_BALLOT.getStringBallotVersion(),
        secretBallotSetup.getElectionVersion().getStringBallotVersion());
  }

  @Test
  public void endCantHappenBeforeStart() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new ElectionSetup(
                ELECTION_NAME, CREATION, 2, 1, laoId, ElectionVersion.OPEN_BALLOT, questions));
  }

  @Test
  public void startTimeIsNotSetSmallerThanCreation() {
    long time = Instant.now().getEpochSecond();
    long gap = 200L;

    ElectionSetup election1 =
        new ElectionSetup(
            ELECTION_NAME,
            CREATION,
            time - gap,
            time,
            laoId,
            ElectionVersion.OPEN_BALLOT,
            questions);
    assertFalse(election1.getStartTime() < election1.getCreation());
  }

  @Test
  public void timestampsCantBeNegative() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new ElectionSetup(
                ELECTION_NAME, CREATION, -1, END, laoId, ElectionVersion.OPEN_BALLOT, questions));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new ElectionSetup(
                ELECTION_NAME, CREATION, START, -1, laoId, ElectionVersion.OPEN_BALLOT, questions));
  }

  @Test
  public void electionSetupEqualsTrueForSameInstance() {
    //noinspection EqualsWithItself
    assertThat(openBallotSetup.equals(openBallotSetup), is(true));
    assertNotEquals(openBallotSetup, secretBallotSetup);
  }

  @Test
  public void jsonValidationTest() {
    JsonTestUtils.testData(openBallotSetup);
  }

  @Test
  public void toStringTest() {
    String setupElectionStringTest =
        String.format(
            "ElectionSetup={"
                + "version='%s', "
                + "id='%s', "
                + "lao='%s', "
                + "name='%s', "
                + "createdAt=%d, "
                + "startTime=%d, "
                + "endTime=%d, "
                + "questions=%s}",
            ElectionVersion.OPEN_BALLOT,
            Election.generateElectionSetupId(laoId, CREATION, ELECTION_NAME),
            laoId,
            ELECTION_NAME,
            CREATION,
            START,
            END,
            Arrays.toString(openBallotSetup.getQuestions().toArray()));
    assertEquals(setupElectionStringTest, openBallotSetup.toString());
  }
}
