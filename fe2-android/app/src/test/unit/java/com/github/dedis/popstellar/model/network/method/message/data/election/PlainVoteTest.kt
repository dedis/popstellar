package com.github.dedis.popstellar.model.network.method.message.data.election

import com.github.dedis.popstellar.model.objects.Election.Companion.generateElectionVoteId
import com.github.dedis.popstellar.utility.security.HashSHA256.hash
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.Assert
import org.junit.Test

class PlainVoteTest {
  private val electionId = "my election id"
  private val questionId = " my question id"

  // We vote for ballot option in position 2, the vote is now unique due to new specification
  private val vote = 2
  private val writeIn = "My write in ballot option"
  private val plainVote1 = PlainVote(questionId, vote, false, writeIn, electionId)
  private val plainVote2 = PlainVote(questionId, vote, true, writeIn, electionId)

  // Hash values util for testing
  private val expectedIdNoWriteIn =
    generateElectionVoteId(electionId, questionId, plainVote1.vote, writeIn, false)
  private val wrongFormatId = hash("Vote", electionId, plainVote2.questionId)
  private val expectedIdWithWriteIn =
    generateElectionVoteId(electionId, questionId, plainVote2.vote, writeIn, true)

  @Test
  fun electionVoteWriteInDisabledReturnsCorrectId() {
    // WriteIn enabled so id is Hash('Vote'||election_id||question_id||write_in)
    MatcherAssert.assertThat(plainVote1.id, CoreMatchers.`is`(expectedIdNoWriteIn))
  }

  @Test
  fun electionVoteWriteInEnabledReturnsCorrectId() {
    // WriteIn enabled so id is Hash('Vote'||election_id||question_id)
    // Hash code shouldn't change with new protocol specifications
    MatcherAssert.assertThat(plainVote2.id == wrongFormatId, CoreMatchers.`is`(false))
    MatcherAssert.assertThat(plainVote2.id == expectedIdWithWriteIn, CoreMatchers.`is`(true))
    Assert.assertNull(plainVote2.vote)
  }

  @Test
  fun electionVoteGetterReturnsCorrectQuestionId() {
    MatcherAssert.assertThat(plainVote1.questionId, CoreMatchers.`is`(questionId))
  }

  @Test
  fun attributesIsNull() {
    Assert.assertNull(plainVote2.vote)
    Assert.assertNotNull(plainVote1.vote)
  }

  @Test
  fun electionVoteGetterReturnsCorrectVotes() {
    MatcherAssert.assertThat(plainVote1.vote, CoreMatchers.`is`(vote))
  }

  @Test
  fun isEqual() {
    Assert.assertNotEquals(plainVote1, plainVote2)
    Assert.assertEquals(plainVote1, PlainVote(questionId, vote, false, writeIn, electionId))
    Assert.assertNotEquals(plainVote1, PlainVote("random", vote, false, writeIn, electionId))
    Assert.assertNotEquals(plainVote1, PlainVote(questionId, 0, false, writeIn, electionId))
    Assert.assertNotEquals(plainVote1, PlainVote(questionId, vote, false, writeIn, "random"))

    // Here because writeInEnabled is false it will be computed as null making both elections the
    // same even though we don't give them the same constructor
    Assert.assertEquals(plainVote1, PlainVote(questionId, vote, false, "random", electionId))

    // Here because writeInEnabled is true the list of votes should be computed as null making both
    // election the same
    Assert.assertEquals(plainVote2, PlainVote(questionId, 2, true, writeIn, electionId))
  }

  @Test
  fun toStringTest() {
    val format =
      String.format(
        "ElectionVote{id='%s', questionId='%s', vote=%s}",
        expectedIdNoWriteIn,
        questionId,
        vote
      )
    Assert.assertEquals(format, plainVote1.toString())
  }
}
