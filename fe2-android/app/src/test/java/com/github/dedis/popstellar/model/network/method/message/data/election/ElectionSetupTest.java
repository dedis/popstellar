package com.github.dedis.popstellar.model.network.method.message.data.election;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;

import com.github.dedis.popstellar.model.network.method.message.data.Action;
import com.github.dedis.popstellar.model.network.method.message.data.Objects;
import com.github.dedis.popstellar.model.objects.event.EventType;
import com.github.dedis.popstellar.utility.security.Hash;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public class ElectionSetupTest {

  private String electionSetupName = "new election setup";
  private long creation = 0;
  private long start = 0;
  private long end = 1;
  private List<String> votingMethod = Arrays.asList("Plurality", "Plurality");
  private List<Boolean> writeIn = Arrays.asList(false, false);
  private List<List<String>> ballotOptions = Arrays
      .asList(Arrays.asList("candidate1", "candidate2"), Arrays.asList("Option a", "Option b"));
  private List<String> question = Arrays.asList("which is the best ?", "who is best ?");
  private String laoId = "my lao id";
  private ElectionSetup electionSetup = new ElectionSetup(electionSetupName, creation, start,
      end, votingMethod, writeIn, ballotOptions, question, laoId);

  @Test
  public void electionSetupGetterReturnsCorrectId() {
    // Hash('Election'||lao_id||created_at||name)
    String expectedId = Hash.hash(EventType.ELECTION.getSuffix(), electionSetup.getLao(),
        Long.toString(electionSetup.getCreation()), electionSetup.getName());
    assertThat(electionSetup.getId(), is(expectedId));
  }

  @Test
  public void electionSetupGetterReturnsCorrectName() {
    assertThat(electionSetup.getName(), is(electionSetupName));
  }

  @Test
  public void electionSetupGetterReturnsCorrectEndTime() {
    assertThat(electionSetup.getEndTime(), is(end));
  }

  @Test
  public void electionSetupGetterReturnsCorrectLaoId() {
    assertThat(electionSetup.getLao(), is(laoId));
  }

  @Test
  public void electionSetupOnlyOneQuestion() {
    assertThat(electionSetup.getQuestions().size(), is(2));
  }

  @Test
  public void electionSetupGetterReturnsCorrectObject() {
    assertThat(electionSetup.getObject(), is(Objects.ELECTION.getObject()));
  }

  @Test
  public void electionSetupGetterReturnsCorrectAction() {
    assertThat(electionSetup.getAction(), is(Action.SETUP.getAction()));
  }

  @Test
  public void fieldsCantBeNull() {
    assertThrows(IllegalArgumentException.class,
        () -> new ElectionSetup(null, creation, start, end, votingMethod, writeIn, ballotOptions,
            question, laoId));
    assertThrows(IllegalArgumentException.class,
        () -> new ElectionSetup(electionSetupName, creation, start, end, null, writeIn,
            ballotOptions, question, laoId));
    assertThrows(IllegalArgumentException.class,
        () -> new ElectionSetup(electionSetupName, creation, start, end, votingMethod, writeIn,
            null, question, laoId));
    assertThrows(IllegalArgumentException.class,
        () -> new ElectionSetup(electionSetupName, creation, start, end, votingMethod, writeIn,
            ballotOptions, null, laoId));
    assertThrows(IllegalArgumentException.class,
        () -> new ElectionSetup(electionSetupName, creation, start, end, votingMethod, writeIn,
            ballotOptions, question, null));
  }

  @Test
  public void endCantHappenBeforeStart() {
    assertThrows(IllegalArgumentException.class,
        () -> new ElectionSetup(electionSetupName, creation, 2, 1, votingMethod, writeIn,
            ballotOptions, question, laoId));
  }

  @Test
  public void startTimeIsNotSetSmallerThanCreation() {
    long time = Instant.now().getEpochSecond();
    long gap = 200L;

    ElectionSetup election1 = new ElectionSetup(electionSetupName, creation, time - gap, time, votingMethod,
        writeIn,
        ballotOptions, question, laoId);
    assertFalse(election1.getStartTime() < election1.getCreation());
  }

  @Test
  public void timestampsCantBeNegative() {
    assertThrows(IllegalArgumentException.class,
        () -> new ElectionSetup(electionSetupName, creation, -1, end, votingMethod, writeIn,
            ballotOptions, question, laoId));
    assertThrows(IllegalArgumentException.class,
        () -> new ElectionSetup(electionSetupName, creation, start, -1, votingMethod, writeIn,
            ballotOptions, question, laoId));
  }

  @Test
  public void electionSetupGetterReturnsCorrectVersion() {
    assertThat(electionSetup.getVersion(), is("1.0.0"));
  }

  @Test
  public void electionSetupEqualsTrueForSameInstance() {
    assertThat(electionSetup.equals(electionSetup), is(true));
  }
}
