package com.github.dedis.popstellar.model.network.method.message.data.election;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThrows;

import com.github.dedis.popstellar.model.network.JsonTestUtils;
import com.github.dedis.popstellar.model.network.method.message.data.Action;
import com.github.dedis.popstellar.model.network.method.message.data.Objects;
import com.github.dedis.popstellar.model.objects.Election;
import com.github.dedis.popstellar.model.objects.event.EventType;
import com.github.dedis.popstellar.utility.security.Hash;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public class ElectionSetupTest {

  private final Version version = Version.OPEN_BALLOT;
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
          version,
          electionSetupName,
          creation,
          start,
          end,
          votingMethod,
          writeIn,
          ballotOptions,
          question,
          laoId);
  private final ElectionSetup secretBallotSetup =
      new ElectionSetup(
          Version.SECRET_BALLOT,
          electionSetupName,
          creation,
          start,
          end,
          votingMethod,
          writeIn,
          ballotOptions,
          question,
          laoId);
  
  
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
  public void getVersionTest(){
    assertEquals(
        Version.OPEN_BALLOT.getStringBallotVersion(), openBallotSetup.getVersion().getStringBallotVersion());
    assertEquals(
        Version.SECRET_BALLOT.getStringBallotVersion(),
        secretBallotSetup.getVersion().getStringBallotVersion());
  }

  @Test
  public void fieldsCantBeNull() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new ElectionSetup(
                version, null, creation, start, end, votingMethod, writeIn, ballotOptions, question, laoId));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new ElectionSetup(
                version,
                electionSetupName,
                creation,
                start,
                end,
                null,
                writeIn,
                ballotOptions,
                question,
                laoId));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new ElectionSetup(
                version,
                electionSetupName,
                creation,
                start,
                end,
                votingMethod,
                writeIn,
                null,
                question,
                laoId));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new ElectionSetup(
                version,
                electionSetupName,
                creation,
                start,
                end,
                votingMethod,
                writeIn,
                ballotOptions,
                null,
                laoId));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new ElectionSetup(
                version,
                electionSetupName,
                creation,
                start,
                end,
                votingMethod,
                writeIn,
                ballotOptions,
                question,
                null));
  }

  @Test
  public void endCantHappenBeforeStart() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new ElectionSetup(
                version,
                electionSetupName,
                creation,
                2,
                1,
                votingMethod,
                writeIn,
                ballotOptions,
                question,
                laoId));
  }

  @Test
  public void startTimeIsNotSetSmallerThanCreation() {
    long time = Instant.now().getEpochSecond();
    long gap = 200L;

    ElectionSetup election1 =
        new ElectionSetup(
            version,
            electionSetupName,
            creation,
            time - gap,
            time,
            votingMethod,
            writeIn,
            ballotOptions,
            question,
            laoId);
    assertFalse(election1.getStartTime() < election1.getCreation());
  }

  @Test
  public void timestampsCantBeNegative() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new ElectionSetup(
                version,
                electionSetupName,
                creation,
                -1,
                end,
                votingMethod,
                writeIn,
                ballotOptions,
                question,
                laoId));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new ElectionSetup(
                version,
                electionSetupName,
                creation,
                start,
                -1,
                votingMethod,
                writeIn,
                ballotOptions,
                question,
                laoId));
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
  public void toStringTest(){
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
            version,
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
