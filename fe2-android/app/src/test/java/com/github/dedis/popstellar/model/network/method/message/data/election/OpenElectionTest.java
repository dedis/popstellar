package com.github.dedis.popstellar.model.network.method.message.data.election;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import com.github.dedis.popstellar.model.network.JsonTestUtils;
import org.junit.Test;

public class OpenElectionTest {

  // Hardcoded strings for election
  private static final String laoId = "laoId";
  private static final String electionId = "electionId";
  private static final long openedAt = 1633099883;

  private static final OpenElection openElection = new OpenElection(laoId, electionId, openedAt);

  @Test
  public void getLaoIdTest() {
    assertEquals(laoId, openElection.getLaoId());
  }

  @Test
  public void getElectionIdTest() {
    assertEquals(electionId, openElection.getElectionId());
  }

  @Test
  public void getOpenedAtTest() {
    assertEquals(openedAt, openElection.getOpenedAt());
  }

  @Test
  public void equalsTest() {
    OpenElection openElection2 = new OpenElection(laoId, electionId, openedAt);
    assertEquals(openElection, openElection2);
    assertEquals(openElection.hashCode(), openElection2.hashCode());

    String random = "random";
    assertNotEquals(openElection, new OpenElection(random, electionId, openedAt));
    assertNotEquals(openElection, new OpenElection(laoId, random, openedAt));
    assertNotEquals(openElection, new OpenElection(laoId, electionId, 0));
    assertEquals(false, openElection.equals(null));
    assertEquals(openElection, openElection);

  }

  @Test
  public void toStringTest() {
    assertEquals("OpenElection{lao='laoId', election='electionId', opened_at=1633099883}", openElection.toString());
  }

  @Test
  public void jsonValidationTest() {
    JsonTestUtils.testData(openElection);
  }
}
