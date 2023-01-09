package com.github.dedis.popstellar.model.network.method.message.data.election;

import com.github.dedis.popstellar.model.objects.Election;
import com.github.dedis.popstellar.utility.security.Hash;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;

public class EncryptedVoteTest {

  private final String electionId = "my election id";
  private final String questionId = " my question id";

  // We vote for ballot option in position 2, vote is unique
  private final String votes = "2";

  private final String encryptedWriteIn = "My write in ballot option";
  private final EncryptedVote encryptedVote1 =
      new EncryptedVote(questionId, votes, false, encryptedWriteIn, electionId);
  // Hash values util for testing
  private final String expectedIdNoWriteIn =
      Election.generateEncryptedElectionVoteId(
          electionId, questionId, encryptedVote1.getVote(), encryptedWriteIn, false);
  private final EncryptedVote electionEncryptedVotes2 =
      new EncryptedVote(questionId, votes, true, encryptedWriteIn, electionId);
  private final String wrongFormatId =
      Hash.hash("Vote", electionId, electionEncryptedVotes2.getQuestionId());
  private final String expectedIdWithWriteIn =
      Election.generateEncryptedElectionVoteId(
          electionId, questionId, electionEncryptedVotes2.getVote(), encryptedWriteIn, true);

  @Test
  public void electionVoteWriteInDisabledReturnsCorrectId() {
    // WriteIn enabled so id is Hash('Vote'||election_id||question_id||write_in)
    assertThat(encryptedVote1.getId(), is(expectedIdNoWriteIn));
  }

  @Test
  public void electionVoteWriteInEnabledReturnsCorrectIdTest() {
    // hash = Hash('Vote'||election_id||question_id||encryptedWriteIn)
    assertThat(electionEncryptedVotes2.getId().equals(wrongFormatId), is(false));
    assertThat(electionEncryptedVotes2.getId().equals(expectedIdWithWriteIn), is(true));
    assertNull(electionEncryptedVotes2.getVote());
  }

  @Test
  public void getIdTest() {
    assertThat(encryptedVote1.getQuestionId(), is(questionId));
  }

  @Test
  public void attributesIsNullTest() {
    assertNull(electionEncryptedVotes2.getVote());
    assertNotNull(encryptedVote1.getVote());
  }

  @Test
  public void getVoteTest() {
    assertThat(encryptedVote1.getVote(), is(votes));
  }

  @Test
  public void isEqualTest() {
    assertNotEquals(encryptedVote1, electionEncryptedVotes2);
    assertEquals(
        encryptedVote1, new EncryptedVote(questionId, votes, false, encryptedWriteIn, electionId));
    assertNotEquals(
        encryptedVote1, new EncryptedVote(questionId, votes, false, encryptedWriteIn, "random"));
    assertNotEquals(
        encryptedVote1,
        new EncryptedVote(questionId, "shouldNotBeEqual", false, encryptedWriteIn, electionId));
    assertNotEquals(
        encryptedVote1, new EncryptedVote("random", votes, false, encryptedWriteIn, electionId));

    // Same equals, no write_in
    assertEquals(encryptedVote1, new EncryptedVote(questionId, votes, false, "random", electionId));

    // Same elections, write_in is the same
    assertEquals(
        electionEncryptedVotes2,
        new EncryptedVote(questionId, votes, true, encryptedWriteIn, electionId));
  }

  @Test
  public void toStringTest() {
    String format =
        String.format(
            "{" + "id='%s', " + "questionId='%s', " + "vote=%s}",
            expectedIdNoWriteIn, questionId, votes);
    assertEquals(format, encryptedVote1.toString());
  }
}
