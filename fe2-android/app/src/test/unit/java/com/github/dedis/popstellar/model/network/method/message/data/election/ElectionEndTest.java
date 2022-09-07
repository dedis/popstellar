package com.github.dedis.popstellar.model.network.method.message.data.election;

import com.github.dedis.popstellar.model.network.JsonTestUtils;
import com.github.dedis.popstellar.model.network.method.message.data.Action;
import com.github.dedis.popstellar.model.network.method.message.data.Objects;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;

public class ElectionEndTest {

  private final String electionId = "electionId";
  private final String laoId = "laoId";
  private final String registeredVotes = "hashed";
  private final ElectionEnd electionEnd = new ElectionEnd(electionId, laoId, registeredVotes);

  @Test
  public void electionEndGetterReturnsCorrectElectionId() {
    assertThat(electionEnd.getElectionId(), is(electionId));
  }

  @Test
  public void electionEndGetterReturnsCorrectLaoId() {
    assertThat(electionEnd.getLaoId(), is(laoId));
  }

  @Test
  public void electionEndGetterReturnsCorrectRegisteredVotes() {
    assertThat(electionEnd.getRegisteredVotes(), is(registeredVotes));
  }

  @Test
  public void electionEndGetterReturnsCorrectObject() {
    assertThat(electionEnd.getObject(), is(Objects.ELECTION.getObject()));
  }

  @Test
  public void electionEndGetterReturnsCorrectAction() {
    assertThat(electionEnd.getAction(), is(Action.END.getAction()));
  }

  @Test
  public void fieldsCantBeNull() {
    assertThrows(
        IllegalArgumentException.class, () -> new ElectionEnd(null, laoId, registeredVotes));
    assertThrows(
        IllegalArgumentException.class, () -> new ElectionEnd(electionId, null, registeredVotes));
    assertThrows(IllegalArgumentException.class, () -> new ElectionEnd(electionId, laoId, null));
  }

  @Test
  public void jsonValidationTest() {
    JsonTestUtils.testData(electionEnd);
  }
}
