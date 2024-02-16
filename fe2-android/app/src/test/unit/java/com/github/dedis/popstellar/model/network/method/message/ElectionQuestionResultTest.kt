package com.github.dedis.popstellar.model.network.method.message

import com.github.dedis.popstellar.model.network.method.message.data.election.ElectionResultQuestion
import com.github.dedis.popstellar.model.network.method.message.data.election.QuestionResult
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.Assert
import org.junit.Test

class ElectionQuestionResultTest {
  private val questionId = "questionId"
  private val results = setOf(QuestionResult("Candidate1", 30))
  private val electionQuestionResult = ElectionResultQuestion(questionId, results)
  @Test
  fun electionQuestionResultGetterReturnsCorrectQuestionId() {
    MatcherAssert.assertThat(electionQuestionResult.id, CoreMatchers.`is`(questionId))
  }

  @Test
  fun electionQuestionResultGetterReturnsCorrectResults() {
    MatcherAssert.assertThat(electionQuestionResult.result, CoreMatchers.`is`(results))
  }

  @Test
  fun resultsCantBeEmpty() {
    val emptySet = emptySet<QuestionResult>()
    Assert.assertThrows(
      IllegalArgumentException::class.java
    ) { ElectionResultQuestion(questionId, emptySet) }
  }
}