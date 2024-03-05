package com.github.dedis.popstellar.model.network.method.message.data.election

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.dedis.popstellar.model.network.JsonTestUtils.loadFile
import com.github.dedis.popstellar.model.network.JsonTestUtils.parse
import com.github.dedis.popstellar.model.network.JsonTestUtils.testData
import com.github.dedis.popstellar.model.objects.Election.Companion.generateElectionQuestionId
import com.github.dedis.popstellar.model.objects.Election.Companion.generateElectionSetupId
import com.github.dedis.popstellar.model.objects.Lao.Companion.generateLaoId
import com.github.dedis.popstellar.model.objects.security.Base64URLData
import com.github.dedis.popstellar.testutils.Base64DataUtils
import com.google.gson.JsonParseException
import java.time.Instant
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CastVoteTest {
  private val organizer = Base64DataUtils.generatePublicKey()
  private val creation = Instant.now().epochSecond
  private val laoId = generateLaoId(organizer, creation, "lao name")
  private val electionId = generateElectionSetupId(laoId, creation, "electionName")
  private val questionId1 = generateElectionQuestionId(electionId, "Question 1")
  private val questionId2 = generateElectionQuestionId(electionId, "Question 2")
  private val writeInEnabled = false
  private val writeIn = "My write in ballot option"
  private val vote1 = 1
  private val vote2 = 2
  private val vote1Base64 = Base64URLData(vote1.toString().toByteArray()).encoded
  private val vote2Base64 = Base64URLData(vote2.toString().toByteArray()).encoded

  // Set up a open ballot election
  private val plainVote1 = PlainVote(questionId1, vote1, writeInEnabled, writeIn, electionId)
  private val plainVote2 = PlainVote(questionId2, vote2, writeInEnabled, writeIn, electionId)
  private val plainVotes = listOf<Vote>(plainVote1, plainVote2)

  // Set up a secret ballot election
  private val encryptedVote1 =
    EncryptedVote(questionId1, vote2Base64, writeInEnabled, writeIn, electionId)
  private val encryptedVote2 =
    EncryptedVote(questionId2, vote1Base64, writeInEnabled, writeIn, electionId)
  private val electionEncryptedVotes = listOf<Vote>(encryptedVote1, encryptedVote2)

  // Create the cast votes messages
  private val castOpenVote = CastVote(plainVotes, electionId, laoId)
  private val castVoteWithTimestamp = CastVote(plainVotes, electionId, laoId, creation)
  private val castEncryptedVote = CastVote(electionEncryptedVotes, electionId, laoId)

  @Test(expected = IllegalArgumentException::class)
  fun constructorFailsElectionIdNotBase64Test() {
    CastVote(plainVotes, "not base 64", laoId, creation)
  }

  @Test(expected = IllegalArgumentException::class)
  fun constructorFailsLaoIdNotBase64Test() {
    CastVote(plainVotes, electionId, "not base 64", creation)
  }

  @Test(expected = IllegalArgumentException::class)
  fun constructorFailsCreationTooOldTest() {
    CastVote(plainVotes, electionId, laoId, 1L)
  }

  @Test(expected = IllegalArgumentException::class)
  fun constructorFailsCreationInFutureTest() {
    CastVote(plainVotes, electionId, laoId, creation + 1000)
  }

  @Test(expected = IllegalArgumentException::class)
  fun constructorFailsWithDuplicateVotesTest() {
    val duplicate = PlainVote(questionId1, 1, writeInEnabled, writeIn, electionId)
    val duplicateVotes = listOf<Vote>(plainVote1, plainVote2, duplicate)
    CastVote(duplicateVotes, electionId, laoId, creation)
  }

  @Test(expected = IllegalArgumentException::class)
  fun constructorFailsWithVoteQuestionIdNotBase64Test() {
    val invalid = PlainVote("not base 64", vote1, writeInEnabled, writeIn, electionId)
    val invalidVotes = listOf<Vote>(plainVote1, plainVote2, invalid)
    CastVote(invalidVotes, electionId, laoId, creation)
  }

  @Test
  fun laoIdTest() {
    MatcherAssert.assertThat(castOpenVote.laoId, CoreMatchers.`is`(laoId))
    MatcherAssert.assertThat(castEncryptedVote.laoId, CoreMatchers.`is`(laoId))
  }

  @Test
  fun electionIdTest() {
    MatcherAssert.assertThat(castOpenVote.electionId, CoreMatchers.`is`(electionId))
    MatcherAssert.assertThat(castEncryptedVote.electionId, CoreMatchers.`is`(electionId))
  }

  @Test
  fun votesTest() {
    MatcherAssert.assertThat(plainVotes, CoreMatchers.`is`(castOpenVote.votes))
    MatcherAssert.assertThat(electionEncryptedVotes, CoreMatchers.`is`(castEncryptedVote.votes))
  }

  @Test
  fun isEqualTest() {
    // Test an OPEN_BALLOT cast vote
    Assert.assertEquals(castOpenVote, CastVote(plainVotes, electionId, laoId))
    Assert.assertEquals(castOpenVote, castOpenVote)
    val randomId = generateElectionSetupId(laoId, creation, "random")
    Assert.assertNotEquals(castOpenVote, CastVote(listOf(plainVote1), electionId, laoId))
    Assert.assertNotEquals(castOpenVote, CastVote(listOf(plainVote1), randomId, laoId))
    Assert.assertNotEquals(castOpenVote, CastVote(listOf(plainVote1), electionId, randomId))
    Assert.assertEquals(castVoteWithTimestamp, CastVote(plainVotes, electionId, laoId, creation))

    // Test a SECRET_BALLOT cast vote
    Assert.assertEquals(castEncryptedVote, CastVote(electionEncryptedVotes, electionId, laoId))
    Assert.assertEquals(castEncryptedVote, castEncryptedVote)
    Assert.assertNotEquals(castEncryptedVote, CastVote(listOf(encryptedVote1), electionId, laoId))
    Assert.assertNotEquals(castEncryptedVote, CastVote(listOf(encryptedVote1), randomId, laoId))
    Assert.assertNotEquals(
      castEncryptedVote,
      CastVote(listOf(encryptedVote1), electionId, randomId),
    )
  }

  /** Deserialization needs a specific generic type to match correctly the class */
  @Test
  fun jsonValidationTest() {
    // Schema should be valid with both vote lists
    // Should use the custom deserializer
    testData(castEncryptedVote)
    testData(castOpenVote)
    val pathDir = "protocol/examples/messageData/vote_cast_vote/"
    val jsonValid1 = loadFile(pathDir + "vote_cast_vote.json")
    val jsonValid2 = loadFile(pathDir + "vote_cast_vote_encrypted.json")

    parse(jsonValid1)
    parse(jsonValid2)

    val jsonInvalid1 = loadFile(pathDir + "wrong_vote_cast_vote_created_at_negative.json")
    Assert.assertThrows(JsonParseException::class.java) { parse(jsonInvalid1) }
  }
}
