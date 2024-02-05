package com.github.dedis.popstellar.utility

import com.github.dedis.popstellar.model.network.method.message.data.election.ElectionQuestion.Question
import com.github.dedis.popstellar.model.network.method.message.data.election.PlainVote
import com.github.dedis.popstellar.model.network.method.message.data.election.Vote
import com.github.dedis.popstellar.model.objects.Election.Companion.generateElectionQuestionId
import com.github.dedis.popstellar.model.objects.Election.Companion.generateElectionSetupId
import com.github.dedis.popstellar.model.objects.Lao.Companion.generateLaoId
import com.github.dedis.popstellar.testutils.Base64DataUtils
import com.github.dedis.popstellar.utility.MessageValidator.MessageValidatorBuilder
import com.github.dedis.popstellar.utility.MessageValidator.verify
import java.time.Instant
import java.util.Base64
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MessageValidatorTest {

  private val QUESTION_ID1 = generateElectionQuestionId(ELECTION_ID, "Question 1")
  private val QUESTION_ID2 = generateElectionQuestionId(ELECTION_ID, "Question 2")

  @Test
  fun testValidLaoId() {
    val validator = verify()

    val invalid1 = "invalidID"
    val invalid2 = generateLaoId(Base64DataUtils.generatePublicKeyOtherThan(ORGANIZER), 0, "name")

    validator.validLaoId(LAO_ID, ORGANIZER, CREATION, NAME)

    Assert.assertThrows(IllegalArgumentException::class.java) {
      validator.validLaoId(invalid1, ORGANIZER, CREATION, NAME)
    }
    Assert.assertThrows(IllegalArgumentException::class.java) {
      validator.validLaoId(invalid2, ORGANIZER, CREATION, NAME)
    }
  }

  @Test
  fun testValidPastTimes() {
    val validator = verify()
    val currentTime = Instant.now().epochSecond
    // time that is too far in the past to be considered valid
    val pastTime = currentTime - MessageValidatorBuilder.VALID_PAST_DELAY - 1
    val futureTime = currentTime + DELTA_TIME
    validator.validPastTimes(currentTime)
    Assert.assertThrows(IllegalArgumentException::class.java) {
      validator.validPastTimes(futureTime)
    }
    Assert.assertThrows(IllegalArgumentException::class.java) { validator.validPastTimes(pastTime) }
  }

  @Test
  fun testOrderedTimes() {
    val validator = verify()

    val currentTime = Instant.now().epochSecond
    val futureTime = currentTime + DELTA_TIME
    val pastTime = currentTime - DELTA_TIME

    validator.orderedTimes(pastTime, currentTime, futureTime)

    Assert.assertThrows(IllegalArgumentException::class.java) {
      validator.orderedTimes(pastTime, futureTime, currentTime)
    }
  }

  @Test
  fun testIsBase64() {
    val validator = verify()

    val validBase64 = Base64.getEncoder().encodeToString("test data".toByteArray())
    val invalidBase64 = "This is not a valid Base64 string!"
    val field = "testField"

    validator.isBase64(validBase64, field)

    Assert.assertThrows(IllegalArgumentException::class.java) {
      validator.isBase64(invalidBase64, field)
    }
    Assert.assertThrows(IllegalArgumentException::class.java) { validator.isBase64(null, field) }
  }

  @Test
  fun testStringNotEmpty() {
    val validator = verify()

    val validString = "test string"
    val emptyString = ""
    val field = "testField"

    validator.stringNotEmpty(validString, field)

    Assert.assertThrows(IllegalArgumentException::class.java) {
      validator.stringNotEmpty(emptyString, field)
    }
    Assert.assertThrows(IllegalArgumentException::class.java) {
      validator.stringNotEmpty(null, field)
    }
  }

  @Test
  fun testListNotEmpty() {
    val validator = verify()

    val valid1: List<Int?> = mutableListOf<Int?>(1, 2, 3)
    val valid2: List<String?> = mutableListOf<String?>("a", "b")
    val invalid: List<String?> = ArrayList()

    validator.listNotEmpty(valid1)
    validator.listNotEmpty(valid2)

    Assert.assertThrows(IllegalArgumentException::class.java) { validator.listNotEmpty(invalid) }
    Assert.assertThrows(IllegalArgumentException::class.java) { validator.listNotEmpty(null) }
  }

  @Test
  fun testNoListDuplicates() {
    val validator = verify()

    val q1 =
      Question("Which is the best ?", "Plurality", mutableListOf("Option a", "Option b"), false)
    val q2 =
      Question("Which is the best ?", "Plurality", mutableListOf("Option a", "Option b"), false)
    val q3 =
      Question("Not the same question ?", "Plurality", mutableListOf("Option c", "Option d"), true)
    val valid1: List<Int?> = mutableListOf<Int?>(1, 2, 3)
    val valid2 = listOf(q1, q3)
    val valid3: List<String?> = ArrayList()
    val invalid1: List<Int?> = mutableListOf<Int?>(1, 2, 2)
    val invalid2 = listOf(q1, q2)

    validator.noListDuplicates(valid1)
    validator.noListDuplicates(valid2)
    validator.noListDuplicates(valid3)

    Assert.assertThrows(IllegalArgumentException::class.java) {
      validator.noListDuplicates(invalid1)
    }
    Assert.assertThrows(IllegalArgumentException::class.java) {
      validator.noListDuplicates(invalid2)
    }
  }

  @Test
  fun testValidVotesWithPlainVotes() {
    val validator = verify()

    val plainVote1 = PlainVote(QUESTION_ID1, 1, false, "something", ELECTION_ID)
    val plainVote2 = PlainVote(QUESTION_ID2, 2, false, "something else", ELECTION_ID)
    val validPlainVotes = listOf<Vote>(plainVote1, plainVote2)

    validator.validVotes(validPlainVotes)

    val plainVote3 = PlainVote("not base 64", 1, false, "something", ELECTION_ID)
    val invalidPlainVotes = listOf<Vote>(plainVote1, plainVote3)

    Assert.assertThrows(IllegalArgumentException::class.java) {
      validator.validVotes(invalidPlainVotes)
    }
  }

  @Test
  fun testValidUrl() {
    val validator = verify()

    validator.validUrl("http://example.com")
    validator.validUrl("https://example.com")
    validator.validUrl("ws://example.com")
    validator.validUrl("wss://10.0.2.2:8000/path")
    validator.validUrl("https://example.com/path/to/file.html")
    validator.validUrl("wss://example.com/path/to/file")

    Assert.assertThrows(IllegalArgumentException::class.java) {
      validator.validUrl("Random String")
    }
    Assert.assertThrows(IllegalArgumentException::class.java) { validator.validUrl("example.com") }
    Assert.assertThrows(IllegalArgumentException::class.java) {
      validator.validUrl("http:example.com")
    }
    Assert.assertThrows(IllegalArgumentException::class.java) {
      validator.validUrl("://example.com")
    }
    Assert.assertThrows(IllegalArgumentException::class.java) {
      validator.validUrl("http://example.")
    }
  }

  @Test
  fun testValidEmoji() {
    val validator = verify()

    val field = "testField"

    validator.isValidEmoji("\uD83D\uDC4D", field)
    validator.isValidEmoji("\uD83D\uDC4E", field)
    validator.isValidEmoji("❤️", field)

    Assert.assertThrows(IllegalArgumentException::class.java) {
      validator.isValidEmoji("\uD83D\uDE00", field)
    }
    Assert.assertThrows(IllegalArgumentException::class.java) {
      validator.isValidEmoji("U+1F600", field)
    }
    Assert.assertThrows(IllegalArgumentException::class.java) {
      validator.isValidEmoji("random string", field)
    }
  }

  @Test
  fun testValidLocalPopCHAUrl() {
    val validator = verify()

    val laoId = "6IQ-Q3S4ISaxZ-hLDelTszYxhJkNQ1gC4JjMxr4jy6Y="
    val valid =
      "http://localhost:9100/authorize?response_mode=query&response_type=id_token&client_id=WAsabGuEe5m1KpqOZQKgmO7UShX84Jmd_eaenOZ32wU&redirect_uri=http%3A%2F%2Flocalhost%3A8000%2Fcb&scope=openid+profile&login_hint=6IQ-Q3S4ISaxZ-hLDelTszYxhJkNQ1gC4JjMxr4jy6Y=&nonce=frXgNl-IxJPzsNia07f_3yV0ECYlWOb2RXG_SGvATKcJ7-s0LthmboTrnMqlQS1RnzmV9hW0iumu_5NwAqXwGA&state=m_9r5sPUD8NoRIdVVYFMyYCOb-8xh1d2q8l-pKDXO0sn9TWnR_2nmC8MfVj1COHZsh1rElqimOTLAp3CbhbYJQ"

    validator.isValidPoPCHAUrl(valid, laoId)
  }

  @Test
  fun testValidExternalPopCHAUrl() {
    val validator = verify()

    val laoId = "6IQ-Q3S4ISaxZ-hLDelTszYxhJkNQ1gC4JjMxr4jy6Y="
    val valid =
      "https://be1.personhood.online/authorize?response_mode=fragment&response_type=id_token&client_id=WAsabGuEe5m1KpqOZQKgmO7UShX84Jmd_eaenOZ32wU&redirect_uri=http%3A%2F%2Flocalhost%3A8000%2Fcb&scope=openid+profile+random&login_hint=6IQ-Q3S4ISaxZ-hLDelTszYxhJkNQ1gC4JjMxr4jy6Y=&nonce=frXgNl-IxJPzsNia07f_3yV0ECYlWOb2RXG_SGvATKcJ7-s0LthmboTrnMqlQS1RnzmV9hW0iumu_5NwAqXwGA"

    validator.isValidPoPCHAUrl(valid, laoId)
  }

  @Test
  fun testMissingClientIdPopCHAUrl() {
    val validator = verify()

    val laoId = "6IQ-Q3S4ISaxZ-hLDelTszYxhJkNQ1gC4JjMxr4jy6Y="
    val missingClientId =
      "http://localhost:9100/authorize?response_mode=query&response_type=id_token&redirect_uri=http%3A%2F%2Flocalhost%3A8000%2Fcb&scope=openid+profile&login_hint=6IQ-Q3S4ISaxZ-hLDelTszYxhJkNQ1gC4JjMxr4jy6Y=&nonce=frXgNl-IxJPzsNia07f_3yV0ECYlWOb2RXG_SGvATKcJ7-s0LthmboTrnMqlQS1RnzmV9hW0iumu_5NwAqXwGA&state=m_9r5sPUD8NoRIdVVYFMyYCOb-8xh1d2q8l-pKDXO0sn9TWnR_2nmC8MfVj1COHZsh1rElqimOTLAp3CbhbYJQ"

    Assert.assertThrows(IllegalArgumentException::class.java) {
      validator.isValidPoPCHAUrl(missingClientId, laoId)
    }
  }

  @Test
  fun testInvalidResponseTypePopCHAUrl() {
    val validator = verify()

    val laoId = "6IQ-Q3S4ISaxZ-hLDelTszYxhJkNQ1gC4JjMxr4jy6Y="
    val invalidResponseType =
      "http://localhost:9100/authorize?response_mode=query&response_type=token&client_id=WAsabGuEe5m1KpqOZQKgmO7UShX84Jmd_eaenOZ32wU&redirect_uri=http%3A%2F%2Flocalhost%3A8000%2Fcb&scope=openid+profile&login_hint=6IQ-Q3S4ISaxZ-hLDelTszYxhJkNQ1gC4JjMxr4jy6Y=&nonce=frXgNl-IxJPzsNia07f_3yV0ECYlWOb2RXG_SGvATKcJ7-s0LthmboTrnMqlQS1RnzmV9hW0iumu_5NwAqXwGA&state=m_9r5sPUD8NoRIdVVYFMyYCOb-8xh1d2q8l-pKDXO0sn9TWnR_2nmC8MfVj1COHZsh1rElqimOTLAp3CbhbYJQ"

    Assert.assertThrows(IllegalArgumentException::class.java) {
      validator.isValidPoPCHAUrl(invalidResponseType, laoId)
    }
  }

  @Test
  fun testMissingRequiredScopePopCHAUrl() {
    val validator = verify()

    val laoId = "6IQ-Q3S4ISaxZ-hLDelTszYxhJkNQ1gC4JjMxr4jy6Y="
    val missingRequiredScope =
      "http://localhost:9100/authorize?response_mode=query&response_type=id_token&client_id=WAsabGuEe5m1KpqOZQKgmO7UShX84Jmd_eaenOZ32wU&redirect_uri=http%3A%2F%2Flocalhost%3A8000%2Fcb&scope=openid+random&login_hint=6IQ-Q3S4ISaxZ-hLDelTszYxhJkNQ1gC4JjMxr4jy6Y=&nonce=frXgNl-IxJPzsNia07f_3yV0ECYlWOb2RXG_SGvATKcJ7-s0LthmboTrnMqlQS1RnzmV9hW0iumu_5NwAqXwGA&state=m_9r5sPUD8NoRIdVVYFMyYCOb-8xh1d2q8l-pKDXO0sn9TWnR_2nmC8MfVj1COHZsh1rElqimOTLAp3CbhbYJQ"

    Assert.assertThrows(IllegalArgumentException::class.java) {
      validator.isValidPoPCHAUrl(missingRequiredScope, laoId)
    }
  }

  @Test
  fun testInvalidResponseModePopCHAUrl() {
    val validator = verify()

    val laoId = "6IQ-Q3S4ISaxZ-hLDelTszYxhJkNQ1gC4JjMxr4jy6Y="
    val invalidResponseMode =
      "http://localhost:9100/authorize?response_mode=random&response_type=id_token&client_id=WAsabGuEe5m1KpqOZQKgmO7UShX84Jmd_eaenOZ32wU&redirect_uri=http%3A%2F%2Flocalhost%3A8000%2Fcb&scope=openid+profile&login_hint=6IQ-Q3S4ISaxZ-hLDelTszYxhJkNQ1gC4JjMxr4jy6Y=&nonce=frXgNl-IxJPzsNia07f_3yV0ECYlWOb2RXG_SGvATKcJ7-s0LthmboTrnMqlQS1RnzmV9hW0iumu_5NwAqXwGA&state=m_9r5sPUD8NoRIdVVYFMyYCOb-8xh1d2q8l-pKDXO0sn9TWnR_2nmC8MfVj1COHZsh1rElqimOTLAp3CbhbYJQ"

    Assert.assertThrows(IllegalArgumentException::class.java) {
      validator.isValidPoPCHAUrl(invalidResponseMode, laoId)
    }
  }

  @Test
  fun testInvalidLaoPopCHAUrl() {
    val validator = verify()

    val laoId = "6IQ-Q3S4ISaxZ-hLDelTszYxhJkNQ1gC4JjMxr4jy6Y="
    val invalidLao =
      "http://localhost:9100/authorize?response_mode=random&response_type=id_token&client_id=WAsabGuEe5m1KpqOZQKgmO7UShX84Jmd_eaenOZ32wU&redirect_uri=http%3A%2F%2Flocalhost%3A8000%2Fcb&scope=openid+profile&login_hint=6IQ-Q3S4ISaxZ-hLTylTszYxhJkNQ1gC4JjMxr4jy6Y=&nonce=frXgNl-IxJPzsNia07f_3yV0ECYlWOb2RXG_SGvATKcJ7-s0LthmboTrnMqlQS1RnzmV9hW0iumu_5NwAqXwGA&state=m_9r5sPUD8NoRIdVVYFMyYCOb-8xh1d2q8l-pKDXO0sn9TWnR_2nmC8MfVj1COHZsh1rElqimOTLAp3CbhbYJQ"

    Assert.assertThrows(IllegalArgumentException::class.java) {
      validator.isValidPoPCHAUrl(invalidLao, laoId)
    }
  }

  companion object {
    private const val DELTA_TIME = MessageValidatorBuilder.VALID_FUTURE_DELAY + 100

    // LAO constants
    private val ORGANIZER = Base64DataUtils.generatePublicKey()
    private const val NAME = "lao name"
    private val CREATION = Instant.now().epochSecond
    private val LAO_ID = generateLaoId(ORGANIZER, CREATION, NAME)

    // Election constants
    private val ELECTION_ID = generateElectionSetupId(LAO_ID, CREATION, "election name")
  }
}
