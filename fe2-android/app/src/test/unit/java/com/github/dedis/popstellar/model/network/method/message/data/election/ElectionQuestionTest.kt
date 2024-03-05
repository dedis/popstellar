package com.github.dedis.popstellar.model.network.method.message.data.election

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.dedis.popstellar.model.network.JsonTestUtils.testData
import com.github.dedis.popstellar.model.network.method.message.data.election.ElectionQuestion.Question
import com.github.dedis.popstellar.model.objects.Election
import com.github.dedis.popstellar.model.objects.Lao
import com.github.dedis.popstellar.testutils.Base64DataUtils
import com.github.dedis.popstellar.ui.lao.event.election.fragments.ElectionSetupFragment
import com.github.dedis.popstellar.utility.security.HashSHA256.hash
import java.time.Instant
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ElectionQuestionTest {

  private val organizer = Base64DataUtils.generatePublicKey()
  private val creation = Instant.now().epochSecond
  private val laoId = Lao.generateLaoId(organizer, creation, "name")

  private val validElectionId = Election.generateElectionSetupId(laoId, creation, "election name")
  private val invalidElectionId = "this is not base64"
  private val validQuestionTitle = "Valid Question Title"
  private val validVotingMethod = ElectionSetupFragment.VotingMethods.values()[0].desc
  private val invalidVotingMethod = "Invalid Voting Method for sure"
  private val validBallotOptions = listOf("Option 1", "Option 2")
  private val insufficientBallotOptions = listOf("OnlyOneOption")
  private val emptyBallotOption = listOf("Option 1", "")
  private val duplicateBallotOptions = listOf("Option 1", "Option 1")
  private val writeIn = false

  private val validQuestion =
    Question(validQuestionTitle, validVotingMethod, validBallotOptions, writeIn)

  @Test
  fun constructorSucceedsWithValidData() {
    val electionQuestion = ElectionQuestion(validElectionId, validQuestion)
    Assert.assertNotNull(electionQuestion)
  }

  @Test(expected = IllegalArgumentException::class)
  fun constructorFailsWhenElectionIdNotBase64() {
    ElectionQuestion(invalidElectionId, validQuestion)
  }

  @Test(expected = IllegalArgumentException::class)
  fun questionConstructorFailsWithInvalidVotingMethod() {
    Question(validQuestionTitle, invalidVotingMethod, validBallotOptions, writeIn)
  }

  @Test(expected = IllegalArgumentException::class)
  fun questionConstructorFailsWithInsufficientBallotOptions() {
    Question(validQuestionTitle, validVotingMethod, insufficientBallotOptions, writeIn)
  }

  @Test(expected = IllegalArgumentException::class)
  fun questionConstructorFailsWithInvalidBallotOptions() {
    Question(validQuestionTitle, validVotingMethod, insufficientBallotOptions, writeIn)
  }

  @Test(expected = IllegalArgumentException::class)
  fun questionConstructorFailsWithDuplicateBallotOptions() {
    Question(validQuestionTitle, validVotingMethod, duplicateBallotOptions, writeIn)
  }

  @Test(expected = IllegalArgumentException::class)
  fun questionConstructorFailsWithEmptyBallotOption() {
    Question(validQuestionTitle, validVotingMethod, emptyBallotOption, writeIn)
  }

  @Test
  fun electionQuestionGetterReturnsCorrectId() {
    // Hash(“Question”||election_id||question)
    val expectedId = hash("Question", ELECTION_SETUP.id, QUESTION)
    MatcherAssert.assertThat(ELECTION_SETUP.questions[0].id, CoreMatchers.`is`(expectedId))
  }

  @Test
  fun electionQuestionGetterReturnsCorrectQuestion() {
    MatcherAssert.assertThat(ELECTION_QUESTION.question, CoreMatchers.`is`(QUESTION))
  }

  @Test
  fun electionQuestionGetterReturnsCorrectVotingMethod() {
    MatcherAssert.assertThat(ELECTION_QUESTION.votingMethod, CoreMatchers.`is`(VOTING_METHOD))
  }

  @Test
  fun electionQuestionGetterReturnsCorrectWriteIn() {
    MatcherAssert.assertThat(ELECTION_QUESTION.writeIn, CoreMatchers.`is`(false))
  }

  @Test
  fun electionQuestionGetterReturnsCorrectBallotOptions() {
    MatcherAssert.assertThat(ELECTION_QUESTION.ballotOptions, CoreMatchers.`is`(BALLOT_OPTIONS))
  }

  @Test
  fun testEquals() {
    Assert.assertNotEquals(QUESTION1, QUESTION2)
    Assert.assertEquals(QUESTION2, QUESTION3)
  }

  @Test
  fun jsonValidationTest() {
    testData(ELECTION_SETUP)
  }

  companion object {
    private val VERSION = ElectionVersion.OPEN_BALLOT
    private val LAO_ID = hash("laoId")
    private const val NAME = "name"
    private val NOW = Instant.now().epochSecond
    private val END = NOW + 30L
    private const val VOTING_METHOD = "Plurality"
    private const val QUESTION = "Question"
    private val BALLOT_OPTIONS: List<String> = mutableListOf("a", "b")
    private val QUESTION1 = Question("Question", VOTING_METHOD, BALLOT_OPTIONS, false)
    private val QUESTION2 = Question("Question2", VOTING_METHOD, BALLOT_OPTIONS, false)
    private val QUESTION3 = Question("Question2", VOTING_METHOD, BALLOT_OPTIONS, false)
    private val QUESTIONS = listOf(QUESTION1, QUESTION2)
    private val ELECTION_SETUP = ElectionSetup(NAME, NOW, NOW, END, LAO_ID, VERSION, QUESTIONS)
    private val ELECTION_QUESTION = ELECTION_SETUP.questions[0]
  }
}
