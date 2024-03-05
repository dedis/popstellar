package com.github.dedis.popstellar.model.network.method.message

import com.github.dedis.popstellar.model.network.method.message.data.election.QuestionResult
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.Assert
import org.junit.Test

class QuestionResultTest {
  private val validCount = 30
  private val validBallotOption = "Valid Ballot Option"
  private val questionResult = QuestionResult(validBallotOption, validCount)
  private val invalidCount = -1

  @Test
  fun constructorSucceedsWithValidData() {
    QuestionResult(validBallotOption, validCount)
  }

  @Test(expected = IllegalArgumentException::class)
  fun constructorFailsWhenBallotOptionIsNull() {
    QuestionResult(null, validCount)
  }

  @Test(expected = IllegalArgumentException::class)
  fun constructorFailsWhenBallotOptionIsEmpty() {
    QuestionResult("", validCount)
  }

  @Test(expected = IllegalArgumentException::class)
  fun constructorFailsWhenCountIsLessThanZero() {
    QuestionResult(validBallotOption, invalidCount)
  }

  @Test
  fun fieldsCantBeNull() {
    Assert.assertThrows(IllegalArgumentException::class.java) { QuestionResult(null, 30) }
  }

  @Test
  fun questionResultGetterReturnsCorrectName() {
    MatcherAssert.assertThat(questionResult.ballot, CoreMatchers.`is`(validBallotOption))
  }

  @Test
  fun questionResultGetterReturnsCorrectCount() {
    MatcherAssert.assertThat(questionResult.count, CoreMatchers.`is`(validCount))
  }
}
