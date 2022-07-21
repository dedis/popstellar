package com.github.dedis.popstellar.model.network.method.message.data.election;

import com.github.dedis.popstellar.model.network.JsonTestUtils;
import com.github.dedis.popstellar.model.network.method.message.data.Action;
import com.github.dedis.popstellar.model.network.method.message.data.Objects;
import com.github.dedis.popstellar.model.objects.Election;
import com.github.dedis.popstellar.model.objects.event.EventType;
import com.github.dedis.popstellar.utility.security.Hash;

import org.junit.Test;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThrows;

public class ElectionSetupTest {

  private final String electionSetupName = "new election setup";
  private final long creation = 0;
  private final long start = 0;
  private final long end = 1;
  private final List<String> votingMethod = Arrays.asList("Plurality", "Plurality");
  private final List<Boolean> writeIn = Arrays.asList(false, false);
  private final List<List<String>> ballotOptions =
      Arrays.asList(
          Arrays.asList("candidate1", "candidate2"), Arrays.asList("Option a", "Option b"));
  private final List<String> question = Arrays.asList("which is the best ?", "who is best ?");
  private final String laoId = "my lao id";
  private final ElectionSetup openBallotSetup =
      new ElectionSetup(
          writeIn,
          electionSetupName,
          creation,
          start,
          end,
          votingMethod,
          laoId,
          ballotOptions,
          question,
          ElectionVersion.OPEN_BALLOT);
  private final ElectionSetup secretBallotSetup =
      new ElectionSetup(
          writeIn,
          electionSetupName,
          creation,
          start,
          end,
          votingMethod,
          laoId,
          ballotOptions,
          question,
          ElectionVersion.SECRET_BALLOT);

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
    assertThat(openBallotSetup.getName(), is(electionSetupName));
  }

  @Test
  public void getEndTimeTest() {
    assertThat(openBallotSetup.getEndTime(), is(end));
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
  public void fieldsCantBeNull() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new ElectionSetup(
                writeIn,
                null,
                creation,
                start,
                end,
                votingMethod,
                laoId,
                ballotOptions,
                question,
                ElectionVersion.OPEN_BALLOT));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new ElectionSetup(
                writeIn,
                electionSetupName,
                creation,
                start,
                end,
                null,
                laoId,
                ballotOptions,
                question,
                ElectionVersion.OPEN_BALLOT));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new ElectionSetup(
                writeIn,
                electionSetupName,
                creation,
                start,
                end,
                votingMethod,
                laoId,
                null,
                question,
                ElectionVersion.OPEN_BALLOT));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new ElectionSetup(
                writeIn,
                electionSetupName,
                creation,
                start,
                end,
                votingMethod,
                laoId,
                ballotOptions,
                null,
                ElectionVersion.OPEN_BALLOT));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new ElectionSetup(
                writeIn,
                electionSetupName,
                creation,
                start,
                end,
                votingMethod,
                null,
                ballotOptions,
                question,
                ElectionVersion.OPEN_BALLOT));
  }

  @Test
  public void endCantHappenBeforeStart() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new ElectionSetup(
                writeIn,
                electionSetupName,
                creation,
                2,
                1,
                votingMethod,
                laoId,
                ballotOptions,
                question,
                ElectionVersion.OPEN_BALLOT));
  }

  @Test
  public void startTimeIsNotSetSmallerThanCreation() {
    long time = Instant.now().getEpochSecond();
    long gap = 200L;

    ElectionSetup election1 =
        new ElectionSetup(
            writeIn,
            electionSetupName,
            creation,
            time - gap,
            time,
            votingMethod,
            laoId,
            ballotOptions,
            question,
            ElectionVersion.OPEN_BALLOT);
    assertFalse(election1.getStartTime() < election1.getCreation());
  }

  @Test
  public void timestampsCantBeNegative() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new ElectionSetup(
                writeIn,
                electionSetupName,
                creation,
                -1,
                end,
                votingMethod,
                laoId,
                ballotOptions,
                question,
                ElectionVersion.OPEN_BALLOT));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new ElectionSetup(
                writeIn,
                electionSetupName,
                creation,
                start,
                -1,
                votingMethod,
                laoId,
                ballotOptions,
                question,
                ElectionVersion.OPEN_BALLOT));
  }

  @Test
  public void electionSetupEqualsTrueForSameInstance() {
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
            Election.generateElectionSetupId(laoId, creation, electionSetupName),
            laoId,
            electionSetupName,
            creation,
            start,
            end,
            Arrays.toString(openBallotSetup.getQuestions().toArray()));
    assertEquals(setupElectionStringTest, openBallotSetup.toString());
  }
}
