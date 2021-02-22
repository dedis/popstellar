package com.github.dedis.student20_pop.model;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;

import org.junit.Test;

public class VoteTest {

  private final String person1 = new Keys().getPublicKey();
  private final String person2 = new Keys().getPublicKey();
  private final String election = new Keys().getPublicKey();
  private final String vote = "Encrypted Vote";
  private final Vote vote1 = new Vote(person1, election, vote);
  private final Vote vote2 = new Vote(person2, election, vote);

  @Test
  public void createVoteNullParametersTest() {
    assertThrows(IllegalArgumentException.class, () -> new Vote(null, election, vote));
    assertThrows(IllegalArgumentException.class, () -> new Vote(person1, null, vote));
    assertThrows(IllegalArgumentException.class, () -> new Vote(person1, election, null));
  }

  @Test
  public void getPersonTest() {
    assertThat(vote1.getPerson(), is(person1));
  }

  @Test
  public void getElectionTest() {
    assertThat(vote1.getElection(), is(election));
  }

  @Test
  public void getVoteTest() {
    assertThat(vote1.getVote(), is(vote));
  }

  @Test
  public void equalsTest() {
    assertEquals(vote1, vote1);
    assertNotEquals(vote1, vote2);
  }

  @Test
  public void hashCodeTest() {
    assertEquals(vote1.hashCode(), vote1.hashCode());
    assertNotEquals(vote1.hashCode(), vote2.hashCode());
  }
}
