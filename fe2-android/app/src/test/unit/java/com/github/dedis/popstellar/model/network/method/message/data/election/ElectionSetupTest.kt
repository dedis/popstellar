package com.github.dedis.popstellar.model.network.method.message.data.election

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.dedis.popstellar.model.network.JsonTestUtils.loadFile
import com.github.dedis.popstellar.model.network.JsonTestUtils.parse
import com.github.dedis.popstellar.model.network.JsonTestUtils.testData
import com.github.dedis.popstellar.model.network.method.message.data.Action
import com.github.dedis.popstellar.model.network.method.message.data.Objects
import com.github.dedis.popstellar.model.network.method.message.data.election.ElectionQuestion.Question
import com.github.dedis.popstellar.model.objects.Election.Companion.generateElectionSetupId
import com.github.dedis.popstellar.model.objects.Lao.Companion.generateLaoId
import com.github.dedis.popstellar.model.objects.event.EventType
import com.github.dedis.popstellar.testutils.Base64DataUtils
import com.github.dedis.popstellar.utility.security.HashSHA256.hash
import com.google.gson.JsonParseException
import java.time.Instant
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ElectionSetupTest {
  private val LAO_ID = generateLaoId(ORGANIZER, CREATION, ELECTION_NAME)
  private val openBallotSetup =
    ElectionSetup(
      ELECTION_NAME,
      CREATION,
      START,
      END,
      LAO_ID,
      ElectionVersion.OPEN_BALLOT,
      QUESTIONS
    )
  private val secretBallotSetup =
    ElectionSetup(
      ELECTION_NAME,
      CREATION,
      START,
      END,
      LAO_ID,
      ElectionVersion.SECRET_BALLOT,
      QUESTIONS
    )

  @Test
  fun electionSetupGetterReturnsCorrectId() {
    // Hash('Election'||lao_id||created_at||name)
    val expectedId =
      hash(
        EventType.ELECTION.suffix,
        openBallotSetup.lao,
        openBallotSetup.creation.toString(),
        openBallotSetup.name
      )
    MatcherAssert.assertThat(openBallotSetup.id, CoreMatchers.`is`(expectedId))
  }

  @Test
  fun nameTest() {
    MatcherAssert.assertThat(openBallotSetup.name, CoreMatchers.`is`(ELECTION_NAME))
  }

  @Test
  fun endTimeTest() {
    MatcherAssert.assertThat(openBallotSetup.endTime, CoreMatchers.`is`(END))
  }

  @Test
  fun laoTest() {
    MatcherAssert.assertThat(openBallotSetup.lao, CoreMatchers.`is`(LAO_ID))
  }

  @Test
  fun electionSetupOnlyOneQuestion() {
    MatcherAssert.assertThat(openBallotSetup.questions.size, CoreMatchers.`is`(2))
  }

  @Test
  fun objectTest() {
    MatcherAssert.assertThat(openBallotSetup.`object`, CoreMatchers.`is`(Objects.ELECTION.`object`))
  }

  @Test
  fun actionTest() {
    MatcherAssert.assertThat(openBallotSetup.action, CoreMatchers.`is`(Action.SETUP.action))
  }

  @Test
  fun versionTest() {
    Assert.assertEquals(
      ElectionVersion.OPEN_BALLOT.stringBallotVersion,
      openBallotSetup.electionVersion.stringBallotVersion
    )
    Assert.assertEquals(
      ElectionVersion.SECRET_BALLOT.stringBallotVersion,
      secretBallotSetup.electionVersion.stringBallotVersion
    )
  }

  @Test
  fun constructorFailsWithLaoIdNotBase64() {
    Assert.assertThrows(IllegalArgumentException::class.java) {
      ElectionSetup(
        ELECTION_NAME,
        CREATION,
        START,
        END,
        "invalid id",
        ElectionVersion.OPEN_BALLOT,
        QUESTIONS_DUPLICATES
      )
    }
  }

  @Test
  fun constructorFailsWithEmptyElectionName() {
    Assert.assertThrows(IllegalArgumentException::class.java) {
      ElectionSetup(
        "",
        CREATION,
        START,
        END,
        LAO_ID,
        ElectionVersion.OPEN_BALLOT,
        QUESTIONS_DUPLICATES
      )
    }
  }

  @Test
  fun constructorFailsWithDuplicateQuestions() {
    Assert.assertThrows(IllegalArgumentException::class.java) {
      ElectionSetup(
        ELECTION_NAME,
        CREATION,
        START,
        END,
        LAO_ID,
        ElectionVersion.OPEN_BALLOT,
        QUESTIONS_DUPLICATES
      )
    }
  }

  @Test
  fun constructorFailsWithUnorderedTimes() {
    Assert.assertThrows(IllegalArgumentException::class.java) {
      ElectionSetup(
        ELECTION_NAME,
        CREATION,
        START,
        START - 1,
        LAO_ID,
        ElectionVersion.OPEN_BALLOT,
        QUESTIONS
      )
    }
    Assert.assertThrows(IllegalArgumentException::class.java) {
      ElectionSetup(
        ELECTION_NAME,
        CREATION,
        CREATION - 1,
        END,
        LAO_ID,
        ElectionVersion.OPEN_BALLOT,
        QUESTIONS
      )
    }
  }

  @Test
  fun constructorFailsWithNegativeTimes() {
    Assert.assertThrows(IllegalArgumentException::class.java) {
      ElectionSetup(
        ELECTION_NAME,
        CREATION,
        -1,
        END,
        LAO_ID,
        ElectionVersion.OPEN_BALLOT,
        QUESTIONS
      )
    }
    Assert.assertThrows(IllegalArgumentException::class.java) {
      ElectionSetup(
        ELECTION_NAME,
        CREATION,
        START,
        -1,
        LAO_ID,
        ElectionVersion.OPEN_BALLOT,
        QUESTIONS
      )
    }
    Assert.assertThrows(IllegalArgumentException::class.java) {
      ElectionSetup(
        ELECTION_NAME,
        CREATION,
        START,
        -1,
        LAO_ID,
        ElectionVersion.OPEN_BALLOT,
        QUESTIONS
      )
    }
  }

  @Test
  fun electionSetupEqualsTest() {
    val openBallotSetup2 =
      ElectionSetup(
        ELECTION_NAME,
        CREATION,
        START,
        END,
        LAO_ID,
        ElectionVersion.OPEN_BALLOT,
        QUESTIONS
      )
    MatcherAssert.assertThat(openBallotSetup == openBallotSetup2, CoreMatchers.`is`(true))
    Assert.assertNotEquals(openBallotSetup, secretBallotSetup)
  }

  @Test
  fun jsonValidationTest() {

    // Check that valid data is successfully parses
    testData(openBallotSetup)
    testData(secretBallotSetup)
    val pathDir = "protocol/examples/messageData/election_setup/"
    val jsonValid1 = loadFile(pathDir + "election_setup.json")
    val jsonValid2 = loadFile(pathDir + "election_setup_secret_ballot.json")
    parse(jsonValid1)
    parse(jsonValid2)

    // Check that invalid data is rejected
    val jsonInvalid1 = loadFile(pathDir + "bad_election_setup_created_at_negative.json")
    val jsonInvalid2 = loadFile(pathDir + "bad_election_setup_end_time_before_created_at.json")
    val jsonInvalid3 = loadFile(pathDir + "bad_election_setup_end_time_negative.json")
    val jsonInvalid4 = loadFile(pathDir + "bad_election_setup_id_invalid_hash.json")
    val jsonInvalid5 = loadFile(pathDir + "bad_election_setup_lao_id_not_base64.json")
    val jsonInvalid6 = loadFile(pathDir + "bad_election_setup_lao_id_invalid_hash.json")
    val jsonInvalid7 = loadFile(pathDir + "bad_election_setup_lao_id_not_base64.json")
    val jsonInvalid8 = loadFile(pathDir + "bad_election_setup_missing_name.json")
    val jsonInvalid9 = loadFile(pathDir + "bad_election_setup_name_empty.json")
    val jsonInvalid10 = loadFile(pathDir + "bad_election_setup_question_empty.json")
    val jsonInvalid11 = loadFile(pathDir + "bad_election_setup_question_id_invalid_hash.json")
    val jsonInvalid12 = loadFile(pathDir + "bad_election_setup_question_id_not_base64.json")
    val jsonInvalid13 = loadFile(pathDir + "bad_election_setup_question_voting_method_invalid.json")
    val jsonInvalid14 = loadFile(pathDir + "bad_election_setup_start_time_before_created_at.json")
    val jsonInvalid15 = loadFile(pathDir + "bad_election_setup_start_time_negative.json")

    Assert.assertThrows(JsonParseException::class.java) { parse(jsonInvalid1) }
    Assert.assertThrows(JsonParseException::class.java) { parse(jsonInvalid2) }
    Assert.assertThrows(JsonParseException::class.java) { parse(jsonInvalid3) }
    Assert.assertThrows(JsonParseException::class.java) { parse(jsonInvalid4) }
    Assert.assertThrows(JsonParseException::class.java) { parse(jsonInvalid5) }
    Assert.assertThrows(JsonParseException::class.java) { parse(jsonInvalid6) }
    Assert.assertThrows(JsonParseException::class.java) { parse(jsonInvalid7) }
    Assert.assertThrows(JsonParseException::class.java) { parse(jsonInvalid8) }
    Assert.assertThrows(JsonParseException::class.java) { parse(jsonInvalid9) }
    Assert.assertThrows(JsonParseException::class.java) { parse(jsonInvalid10) }
    Assert.assertThrows(JsonParseException::class.java) { parse(jsonInvalid11) }
    Assert.assertThrows(JsonParseException::class.java) { parse(jsonInvalid12) }
    Assert.assertThrows(JsonParseException::class.java) { parse(jsonInvalid13) }
    Assert.assertThrows(JsonParseException::class.java) { parse(jsonInvalid14) }
    Assert.assertThrows(JsonParseException::class.java) { parse(jsonInvalid15) }
  }

  @Test
  fun toStringTest() {
    val setupElectionStringTest =
      String.format(
        "ElectionSetup={version='%s', id='%s', lao='%s', name='%s', createdAt=%d, startTime=%d, endTime=%d, questions=%s}",
        ElectionVersion.OPEN_BALLOT,
        generateElectionSetupId(LAO_ID, CREATION, ELECTION_NAME),
        LAO_ID,
        ELECTION_NAME,
        CREATION,
        START,
        END,
        openBallotSetup.questions.toTypedArray().contentToString()
      )
    Assert.assertEquals(setupElectionStringTest, openBallotSetup.toString())
  }

  companion object {
    private const val ELECTION_NAME = "New election"
    private val CREATION = Instant.now().epochSecond
    private val START = CREATION
    private val END = START + 1
    private val ORGANIZER = Base64DataUtils.generatePublicKey()
    private val QUESTION1 =
      Question("Which is the best ?", "Plurality", mutableListOf("Option a", "Option b"), false)
    private val QUESTION2 =
      Question("Who is the best ?", "Plurality", mutableListOf("candidate1", "candidate2"), false)
    private val QUESTION3 =
      Question("Who is the best ?", "Plurality", mutableListOf("candidate1", "candidate2"), false)
    private val QUESTIONS = listOf(QUESTION1, QUESTION2)
    private val QUESTIONS_DUPLICATES = listOf(QUESTION1, QUESTION2, QUESTION3)
  }
}
