package com.github.dedis.popstellar.model.network.method.message

import com.github.dedis.popstellar.model.network.method.message.data.election.ElectionResultQuestion
import com.github.dedis.popstellar.model.network.method.message.data.election.QuestionResult
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.Test

class ElectionQuestionResultTest {
  private val validQuestionId = "questionId"
  private val validCount = 30
  private val validResults = setOf(QuestionResult("Candidate1", validCount))
  private val validElectionQuestionResult = ElectionResultQuestion(validQuestionId, validResults)
  private val emptyResultSet = emptySet<QuestionResult>()

  @Test
  fun constructorSucceedsWithValidData() {
    ElectionResultQuestion(validQuestionId, validResults)
  }

  @Test(expected = IllegalArgumentException::class)
  fun constructorFailsWhenIdIsEmpty() {
    ElectionResultQuestion("", validResults)
  }

  @Test(expected = IllegalArgumentException::class)
  fun constructorFailsWhenResultSetIsEmpty() {
    ElectionResultQuestion(validQuestionId, emptyResultSet)
  }

  @Test
  fun electionQuestionResultGetterReturnsCorrectQuestionId() {
    MatcherAssert.assertThat(validElectionQuestionResult.id, CoreMatchers.`is`(validQuestionId))
  }

  @Test
  fun electionQuestionResultGetterReturnsCorrectResults() {
    MatcherAssert.assertThat(validElectionQuestionResult.result, CoreMatchers.`is`(validResults))
  }

  @Test(expected = IllegalArgumentException::class)
  fun resultsCantBeEmpty() {
    val emptySet = emptySet<QuestionResult>()
    ElectionResultQuestion(validQuestionId, emptySet)
  }
}
