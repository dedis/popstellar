package com.github.dedis.popstellar.model.network.method.message

import com.github.dedis.popstellar.model.network.method.message.data.election.QuestionResult
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.Assert
import org.junit.Test

class QuestionResultTest {
  private val count = 30
  private val name = "Candidate1"
  private val questionResult = QuestionResult(name, count)
  @Test
  fun fieldsCantBeNull() {
    Assert.assertThrows(IllegalArgumentException::class.java) { QuestionResult(null, 30) }
  }

  @Test
  fun questionResultGetterReturnsCorrectName() {
    MatcherAssert.assertThat(questionResult.ballot, CoreMatchers.`is`(name))
  }

  @Test
  fun questionResultGetterReturnsCorrectCount() {
    MatcherAssert.assertThat(questionResult.count, CoreMatchers.`is`(count))
  }
}