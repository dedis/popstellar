package com.github.dedis.popstellar.model.network.method.message.data.election;

import com.github.dedis.popstellar.model.network.JsonTestUtils;

import org.junit.Test;

import java.util.*;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class CastVoteTest {

  private final String questionId1 = " myQuestion1";
  private final String questionId2 = " myQuestion2";
  private final String laoId = "myLao";
  private final String electionId = " myElection";
  private final boolean writeInEnabled = false;
  private final long timestamp = 10;
  private final String write_in = "My write in ballot option";

  // Set up a open ballot election
  private final PlainVote plainVote1 =
      new PlainVote(questionId1, 1, writeInEnabled, write_in, electionId);
  private final PlainVote plainVote2 =
      new PlainVote(questionId2, 2, writeInEnabled, write_in, electionId);
  private final List<Vote> plainVotes = Arrays.asList(plainVote1, plainVote2);

  // Set up a secret ballot election
  private final EncryptedVote encryptedVote1 =
      new EncryptedVote(questionId1, "2", writeInEnabled, write_in, electionId);
  private final EncryptedVote encryptedVote2 =
      new EncryptedVote(questionId2, "1", writeInEnabled, write_in, electionId);
  private final List<Vote> electionEncryptedVotes = Arrays.asList(encryptedVote1, encryptedVote2);

  // Create the cast votes messages
  private final CastVote castOpenVote = new CastVote(plainVotes, electionId, laoId);
  private final CastVote castVoteWithTimestamp =
      new CastVote(plainVotes, electionId, laoId, timestamp);
  private final CastVote castEncryptedVote =
      new CastVote(electionEncryptedVotes, electionId, laoId);

  @Test
  public void getLaoIdTest() {
    assertThat(castOpenVote.getLaoId(), is(laoId));
    assertThat(castEncryptedVote.getLaoId(), is(laoId));
  }

  @Test
  public void getElectionIdTest() {
    assertThat(castOpenVote.getElectionId(), is(electionId));
    assertThat(castEncryptedVote.getElectionId(), is(electionId));
  }

  @Test
  public void getVotesTest() {
    assertThat(plainVotes, is(castOpenVote.getVotes()));
    assertThat(electionEncryptedVotes, is(castEncryptedVote.getVotes()));
  }

  @Test
  public void isEqualTest() {
    // Test an OPEN_BALLOT cast vote
    assertEquals(castOpenVote, new CastVote(plainVotes, electionId, laoId));
    assertEquals(castOpenVote, castOpenVote);
    assertNotEquals(
        castOpenVote, new CastVote(Collections.singletonList(plainVote1), electionId, laoId));
    assertNotEquals(
        castOpenVote, new CastVote(Collections.singletonList(plainVote1), "random", laoId));
    assertNotEquals(
        castOpenVote, new CastVote(Collections.singletonList(plainVote1), electionId, "random"));
    assertEquals(castVoteWithTimestamp, new CastVote(plainVotes, electionId, laoId, timestamp));

    // Test a SECRET_BALLOT cast vote
    assertEquals(castEncryptedVote, new CastVote(electionEncryptedVotes, electionId, laoId));
    assertEquals(castEncryptedVote, castEncryptedVote);
    assertNotEquals(
        castEncryptedVote,
        new CastVote(Collections.singletonList(encryptedVote1), electionId, laoId));
    assertNotEquals(
        castEncryptedVote,
        new CastVote(Collections.singletonList(encryptedVote1), "random", laoId));
    assertNotEquals(
        castEncryptedVote,
        new CastVote(Collections.singletonList(encryptedVote1), electionId, "random"));
  }

  /** Deserialization needs a specific generic type to match correctly the class */
  @Test
  public void jsonValidationTest() {
    // Schema should be valid with both vote lists
    // Should use the custom deserializer
    JsonTestUtils.testData(castEncryptedVote);
    JsonTestUtils.testData(castOpenVote);
  }
}
