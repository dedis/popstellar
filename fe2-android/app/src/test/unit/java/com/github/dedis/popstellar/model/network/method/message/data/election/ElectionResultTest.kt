package com.github.dedis.popstellar.model.network.method.message.data.election

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.dedis.popstellar.model.network.JsonTestUtils.testData
import com.github.dedis.popstellar.model.network.method.message.data.Action
import com.github.dedis.popstellar.model.network.method.message.data.Objects
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ElectionResultTest {
  private val results = setOf(QuestionResult("Candidate1", 40))
  private val question = ElectionResultQuestion("question id", results)
  private val questions = listOf(question)
  private val electionResult = ElectionResult(questions)

  @Test(expected = IllegalArgumentException::class)
  fun questionsCantBeNull() {
    ElectionResult(null)
  }

  @Test(expected = IllegalArgumentException::class)
  fun questionsCantBeEmpty() {
    val emptyList: List<ElectionResultQuestion> = ArrayList()
    ElectionResult(emptyList)
  }

  @Test(expected = IllegalArgumentException::class)
  fun questionsCantHaveDuplicates() {
    val duplicate = ElectionResultQuestion("question id", results)
    val duplicatesList = listOf(question, duplicate)
    ElectionResult(duplicatesList)
  }

  @Test
  fun electionResultGetterReturnsCorrectQuestions() {
    MatcherAssert.assertThat(electionResult.questions, CoreMatchers.`is`(questions))
  }

  @Test
  fun electionResultGetterReturnsCorrectObject() {
    MatcherAssert.assertThat(electionResult.`object`, CoreMatchers.`is`(Objects.ELECTION.`object`))
  }

  @Test
  fun electionResultGetterReturnsCorrectAction() {
    MatcherAssert.assertThat(electionResult.action, CoreMatchers.`is`(Action.RESULT.action))
  }

  @Test
  fun jsonValidationTest() {
    testData(electionResult)
  }
}
