package com.github.dedis.popstellar.model.network.method.message.data.election;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CastVoteTest {

  private final String questionId1 = " myQuestion1";
  private final String questionId2 = " myQuestion2";
  private final String laoId = "myLao";
  private final String electionId = " myElection";
  private final boolean writeInEnabled = false;
  private final String write_in = "My write in ballot option";
  private final ElectionVote electionVote1 =
      new ElectionVote(
          questionId1,
          new ArrayList<>(Arrays.asList(2, 1, 0)),
          writeInEnabled,
          write_in,
          electionId);
  private final ElectionVote electionVote2 =
      new ElectionVote(
          questionId2,
          new ArrayList<>(Arrays.asList(0, 1, 2)),
          writeInEnabled,
          write_in,
          electionId);
  private final List<ElectionVote> electionVotes =
      new ArrayList<>(Arrays.asList(electionVote1, electionVote2));

  private final CastVote castVote = new CastVote(electionVotes, electionId, laoId);

  @Test
  public void castVoteGetterReturnsCorrectLaoId() {
    assertThat(castVote.getLaoId(), is(laoId));
  }

  @Test
  public void castVoteGetterReturnsElectionId() {
    assertThat(castVote.getElectionId(), is(electionId));
  }

  @Test
  public void castVoteGetterReturnsVotes() {
    assertThat(castVote.getVotes(), is(electionVotes));
  }

  @Test
  public void isEqual() {
    assertEquals(castVote, new CastVote(electionVotes, electionId, laoId));
    assertEquals(castVote, castVote);
    assertNotEquals(
        castVote, new CastVote(new ArrayList<>(Arrays.asList(electionVote1)), electionId, laoId));
    assertNotEquals(
        castVote, new CastVote(new ArrayList<>(Arrays.asList(electionVote1)), "random", laoId));
    assertNotEquals(
        castVote,
        new CastVote(new ArrayList<>(Arrays.asList(electionVote1)), electionId, "random"));
  }
}
