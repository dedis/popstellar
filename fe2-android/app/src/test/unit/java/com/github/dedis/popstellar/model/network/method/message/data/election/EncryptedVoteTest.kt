package com.github.dedis.popstellar.model.network.method.message.data.election

import com.github.dedis.popstellar.model.objects.Election
import com.github.dedis.popstellar.model.objects.Election.Companion.generateEncryptedElectionVoteId
import com.github.dedis.popstellar.model.objects.Lao
import com.github.dedis.popstellar.model.objects.security.Base64URLData
import com.github.dedis.popstellar.testutils.Base64DataUtils
import com.github.dedis.popstellar.utility.security.HashSHA256.hash
import java.time.Instant
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.Assert
import org.junit.Test

class EncryptedVoteTest {
  private val organizer = Base64DataUtils.generatePublicKey()
  private val creation = Instant.now().epochSecond
  private val laoId = Lao.generateLaoId(organizer, creation, "lao name")
  private val electionId = Election.generateElectionSetupId(laoId, creation, "electionName")
  private val questionId = Election.generateElectionQuestionId(electionId, "Question")

  // We vote for ballot option in position 2, vote is unique
  private val votes = Base64URLData(("2").toByteArray()).encoded
  private val encryptedWriteIn = Base64URLData(("My write-in ballot option").toByteArray()).encoded
  private val encryptedVote1 = EncryptedVote(questionId, votes, false, encryptedWriteIn, electionId)

  // random base64 value
  private val randomBase64 = Base64URLData("random".toByteArray()).encoded

  // Hash values util for testing
  private val expectedIdNoWriteIn =
    generateEncryptedElectionVoteId(
      electionId,
      questionId,
      encryptedVote1.vote,
      encryptedWriteIn,
      false,
    )
  private val electionEncryptedVotes2 =
    EncryptedVote(questionId, votes, true, encryptedWriteIn, electionId)
  private val wrongFormatId = hash("Vote", electionId, electionEncryptedVotes2.questionId)
  private val expectedIdWithWriteIn =
    generateEncryptedElectionVoteId(
      electionId,
      questionId,
      electionEncryptedVotes2.vote,
      encryptedWriteIn,
      true,
    )

  @Test
  fun electionVoteWriteInDisabledReturnsCorrectId() {
    // WriteIn enabled so id is Hash('Vote'||election_id||question_id||write_in)
    MatcherAssert.assertThat(encryptedVote1.id, CoreMatchers.`is`(expectedIdNoWriteIn))
  }

  @Test
  fun electionVoteWriteInEnabledReturnsCorrectIdTest() {
    // hash = Hash('Vote'||election_id||question_id||encryptedWriteIn)
    MatcherAssert.assertThat(electionEncryptedVotes2.id == wrongFormatId, CoreMatchers.`is`(false))
    MatcherAssert.assertThat(
      electionEncryptedVotes2.id == expectedIdWithWriteIn,
      CoreMatchers.`is`(true),
    )
    Assert.assertNull(electionEncryptedVotes2.vote)
  }

  @Test
  fun idTest() {
    MatcherAssert.assertThat(encryptedVote1.questionId, CoreMatchers.`is`(questionId))
  }

  @Test
  fun attributesIsNullTest() {
    Assert.assertNull(electionEncryptedVotes2.vote)
    Assert.assertNotNull(encryptedVote1.vote)
  }

  @Test
  fun voteTest() {
    MatcherAssert.assertThat(encryptedVote1.vote, CoreMatchers.`is`(votes))
  }

  @Test
  fun isEqualTest() {
    Assert.assertNotEquals(encryptedVote1, electionEncryptedVotes2)
    Assert.assertEquals(
      encryptedVote1,
      EncryptedVote(questionId, votes, false, encryptedWriteIn, electionId),
    )
    Assert.assertNotEquals(
      encryptedVote1,
      EncryptedVote(questionId, votes, false, encryptedWriteIn, randomBase64),
    )
    Assert.assertNotEquals(
      encryptedVote1,
      EncryptedVote(questionId, "shouldNotBeEqual", false, encryptedWriteIn, electionId),
    )
    Assert.assertNotEquals(
      encryptedVote1,
      EncryptedVote(randomBase64, votes, false, encryptedWriteIn, electionId),
    )

    // Same equals, no write_in
    Assert.assertEquals(
      encryptedVote1,
      EncryptedVote(questionId, votes, false, randomBase64, electionId),
    )

    // Same elections, write_in is the same
    Assert.assertEquals(
      electionEncryptedVotes2,
      EncryptedVote(questionId, votes, true, encryptedWriteIn, electionId),
    )
  }

  @Test
  fun toStringTest() {
    val format =
      String.format("{id='%s', questionId='%s', vote=%s}", expectedIdNoWriteIn, questionId, votes)
    Assert.assertEquals(format, encryptedVote1.toString())
  }
}
