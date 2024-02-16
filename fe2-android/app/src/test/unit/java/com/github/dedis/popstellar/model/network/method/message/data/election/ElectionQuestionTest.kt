package com.github.dedis.popstellar.model.network.method.message.data.election

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.dedis.popstellar.model.network.JsonTestUtils.testData
import com.github.dedis.popstellar.model.network.method.message.data.election.ElectionQuestion.Question
import com.github.dedis.popstellar.utility.security.HashSHA256.hash
import java.time.Instant
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ElectionQuestionTest {
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
