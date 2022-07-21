package com.github.dedis.popstellar.model.network.method.message.data.election;

import com.github.dedis.popstellar.model.objects.Election;
import com.github.dedis.popstellar.utility.security.Hash;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class ElectionVoteTest {

  private final String electionId = "my election id";
  private final String questionId = " my question id";
  // We vote for ballot option in position 2, the vote is now unique due to new specification
  private final Integer vote = 2;
  private final String writeIn = "My write in ballot option";
  private final ElectionVote electionVote1 =
      new ElectionVote(questionId, vote, false, writeIn, electionId);
  private final ElectionVote electionVote2 =
      new ElectionVote(questionId, vote, true, writeIn, electionId);

  // Hash values util for testing
  private final String expectedIdNoWriteIn =
      Election.generateElectionVoteId(
          electionId, questionId, electionVote1.getVote(), writeIn, false);
  private final String wrongFormatId = Hash.hash("Vote", electionId, electionVote2.getQuestionId());
  private final String expectedIdWithWriteIn =
      Election.generateElectionVoteId(
          electionId, questionId, electionVote2.getVote(), writeIn, true);

  @Test
  public void electionVoteWriteInDisabledReturnsCorrectId() {
    // WriteIn enabled so id is Hash('Vote'||election_id||question_id||write_in)
    assertThat(electionVote1.getId(), is(expectedIdNoWriteIn));
  }

  @Test
  public void electionVoteWriteInEnabledReturnsCorrectId() {
    // WriteIn enabled so id is Hash('Vote'||election_id||question_id)
    // Hash code shouldn't change with new protocol specifications
    assertThat(electionVote2.getId().equals(wrongFormatId), is(false));
    assertThat(electionVote2.getId().equals(expectedIdWithWriteIn), is(true));
    assertNull(electionVote2.getVote());
  }

  @Test
  public void electionVoteGetterReturnsCorrectQuestionId() {
    assertThat(electionVote1.getQuestionId(), is(questionId));
  }

  @Test
  public void attributesIsNull() {
    assertNull(electionVote2.getVote());
    assertNotNull(electionVote1.getVote());
  }

  @Test
  public void electionVoteGetterReturnsCorrectVotes() {
    assertThat(electionVote1.getVote(), is(vote));
  }

  @Test
  public void isEqual() {
    assertNotEquals(electionVote1, electionVote2);
    assertEquals(electionVote1, new ElectionVote(questionId, vote, false, writeIn, electionId));
    assertNotEquals(electionVote1, new ElectionVote("random", vote, false, writeIn, electionId));
    assertNotEquals(electionVote1, new ElectionVote(questionId, 0, false, writeIn, electionId));
    assertNotEquals(electionVote1, new ElectionVote(questionId, vote, false, writeIn, "random"));

    // Here because writeInEnabled is false it will be computed as null making both elections the
    // same even though we don't give them the same constructor
    assertEquals(electionVote1, new ElectionVote(questionId, vote, false, "random", electionId));

    // Here because writeInEnabled is true the list of votes should be computed as null making both
    // election the same
    assertEquals(electionVote2, new ElectionVote(questionId, 2, true, writeIn, electionId));
  }

  @Test
  public void toStringTest() {
    String format =
        String.format(
            "ElectionVote{" + "id='%s', " + "questionId='%s', " + "vote=%s}",
            expectedIdNoWriteIn, questionId, vote);
    assertEquals(format, electionVote1.toString());
  }
}
