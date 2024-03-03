package com.github.dedis.popstellar.model.network.method.message.data.election

import com.github.dedis.popstellar.model.objects.Election
import com.github.dedis.popstellar.model.objects.Election.Companion.generateElectionVoteId
import com.github.dedis.popstellar.model.objects.Lao
import com.github.dedis.popstellar.model.objects.security.Base64URLData
import com.github.dedis.popstellar.testutils.Base64DataUtils
import com.github.dedis.popstellar.utility.security.HashSHA256.hash
import java.time.Instant
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.Assert
import org.junit.Test

class PlainVoteTest {
  private val organizer = Base64DataUtils.generatePublicKey()
  private val creation = Instant.now().epochSecond
  private val laoId = Lao.generateLaoId(organizer, creation, "lao name")
  private val validElectionId = Election.generateElectionSetupId(laoId, creation, "electionName")
  private val validQuestionId = Election.generateElectionQuestionId(validElectionId, "Question")
  private val randomBase64 = Base64URLData("random").encoded
  private val invalidBase64 = "definitely not base64"

  // We vote for ballot option in position 2, the vote is now unique due to new specification
  private val validVoteIndex = 2
  private val validWriteIn = Base64URLData(("My write-in ballot option").toByteArray()).encoded
  private val plainVote1 =
    PlainVote(validQuestionId, validVoteIndex, false, validWriteIn, validElectionId)
  private val plainVote2 =
    PlainVote(validQuestionId, validVoteIndex, true, validWriteIn, validElectionId)

  // Hash values util for testing
  private val expectedIdNoWriteIn =
    generateElectionVoteId(validElectionId, validQuestionId, plainVote1.vote, validWriteIn, false)
  private val wrongFormatId = hash("Vote", validElectionId, plainVote2.questionId)
  private val expectedIdWithWriteIn =
    generateElectionVoteId(validElectionId, validQuestionId, plainVote2.vote, validWriteIn, true)

  @Test
  fun constructorSucceedsWithValidDataAndVoteIndex() {
    PlainVote(validQuestionId, validVoteIndex, false, null, validElectionId)
  }

  @Test
  fun constructorSucceedsWithValidDataAndWriteIn() {
    PlainVote(validQuestionId, null, true, validWriteIn, validElectionId)
  }

  @Test(expected = IllegalArgumentException::class)
  fun constructorFailsWhenQuestionIdNotBase64() {
    PlainVote(invalidBase64, validVoteIndex, false, null, validElectionId)
  }

  @Test(expected = IllegalArgumentException::class)
  fun constructorFailsWhenQuestionIdEmpty() {
    PlainVote("", validVoteIndex, false, null, validElectionId)
  }

  @Test(expected = IllegalArgumentException::class)
  fun constructorFailsWhenElectionIdNotBase64() {
    PlainVote(validQuestionId, validVoteIndex, false, null, invalidBase64)
  }

  @Test(expected = IllegalArgumentException::class)
  fun constructorFailsWhenElectionIdEmpty() {
    PlainVote(validQuestionId, validVoteIndex, false, null, "")
  }

  @Test(expected = IllegalArgumentException::class)
  fun constructorFailsWhenWriteInNotBase64AndWriteInEnabled() {
    PlainVote(validQuestionId, null, true, invalidBase64, validElectionId)
  }

  @Test(expected = IllegalArgumentException::class)
  fun constructorFailsWhenWriteInEmptyAndWriteInEnabled() {
    PlainVote(validQuestionId, null, true, "", validElectionId)
  }

  @Test(expected = IllegalArgumentException::class)
  fun constructorFailsWhenVoteIsNullAndWriteInDisabled() {
    PlainVote(validQuestionId, null, false, null, validElectionId)
  }

  @Test
  fun secondConstructorSucceedsWithValidIdQuestionAndVote() {
    PlainVote(validElectionId, validQuestionId, validVoteIndex)
  }

  @Test(expected = IllegalArgumentException::class)
  fun constructorFailsWithInvalidElectionIdF() {
    PlainVote(invalidBase64, validQuestionId, validVoteIndex)
  }

  @Test(expected = IllegalArgumentException::class)
  fun secondConstructorFailsWithInvalidQuestionId() {
    PlainVote(validElectionId, invalidBase64, validVoteIndex)
  }

  @Test(expected = IllegalArgumentException::class)
  fun secondConstructorFailsEmptyQuestionId() {
    PlainVote(validElectionId, "", validVoteIndex)
  }

  @Test(expected = IllegalArgumentException::class)
  fun secondConstructorFailsWithEmptyElectionId() {
    PlainVote("", validQuestionId, validVoteIndex)
  }

  @Test(expected = IllegalArgumentException::class)
  fun constructorFailsWithInvalidIdForSecondConstructor() {
    PlainVote(invalidBase64, validQuestionId, validVoteIndex)
  }

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
    MatcherAssert.assertThat(plainVote1.questionId, CoreMatchers.`is`(validQuestionId))
  }

  @Test
  fun attributesIsNull() {
    Assert.assertNull(plainVote2.vote)
    Assert.assertNotNull(plainVote1.vote)
  }

  @Test
  fun electionVoteGetterReturnsCorrectVotes() {
    MatcherAssert.assertThat(plainVote1.vote, CoreMatchers.`is`(validVoteIndex))
  }

  @Test
  fun isEqual() {
    Assert.assertNotEquals(plainVote1, plainVote2)
    Assert.assertEquals(
      plainVote1,
      PlainVote(validQuestionId, validVoteIndex, false, validWriteIn, validElectionId),
    )
    Assert.assertNotEquals(
      plainVote1,
      PlainVote(randomBase64, validVoteIndex, false, validWriteIn, validElectionId),
    )
    Assert.assertNotEquals(
      plainVote1,
      PlainVote(validQuestionId, 0, false, validWriteIn, validElectionId),
    )
    Assert.assertNotEquals(
      plainVote1,
      PlainVote(validQuestionId, validVoteIndex, false, validWriteIn, randomBase64),
    )

    // Here because writeInEnabled is false it will be computed as null making both elections the
    // same even though we don't give them the same constructor
    Assert.assertEquals(
      plainVote1,
      PlainVote(validQuestionId, validVoteIndex, false, randomBase64, validElectionId),
    )

    // Here because writeInEnabled is true the list of votes should be computed as null making both
    // election the same
    Assert.assertEquals(
      plainVote2,
      PlainVote(validQuestionId, 2, true, validWriteIn, validElectionId),
    )
  }

  @Test
  fun toStringTest() {
    val format =
      String.format(
        "ElectionVote{id='%s', questionId='%s', vote=%s}",
        expectedIdNoWriteIn,
        validQuestionId,
        validVoteIndex,
      )
    Assert.assertEquals(format, plainVote1.toString())
  }
}
