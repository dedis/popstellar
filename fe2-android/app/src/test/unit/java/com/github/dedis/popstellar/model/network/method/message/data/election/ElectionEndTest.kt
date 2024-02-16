package com.github.dedis.popstellar.model.network.method.message.data.election

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.dedis.popstellar.model.network.JsonTestUtils.testData
import com.github.dedis.popstellar.model.network.method.message.data.Action
import com.github.dedis.popstellar.model.network.method.message.data.Objects
import com.github.dedis.popstellar.model.objects.Election.Companion.generateElectionSetupId
import com.github.dedis.popstellar.model.objects.Lao.Companion.generateLaoId
import com.github.dedis.popstellar.testutils.Base64DataUtils
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class ElectionEndTest {
  private val organizer = Base64DataUtils.generatePublicKey()
  private val creation = Instant.now().epochSecond
  private val laoId = generateLaoId(organizer, creation, "name")
  private val electionId = generateElectionSetupId(laoId, creation, "election name")
  private val registeredVotes = Base64DataUtils.generateRandomBase64String()
  private val electionEnd = ElectionEnd(electionId, laoId, registeredVotes)
  @Test(expected = IllegalArgumentException::class)
  fun constructorFailsElectionIdNotBase64Test() {
    ElectionEnd("not base 64", laoId, registeredVotes)
  }

  @Test(expected = IllegalArgumentException::class)
  fun constructorFailsLaoIdNotBase64Test() {
    ElectionEnd(electionId, "not base 64", registeredVotes)
  }

  @Test(expected = IllegalArgumentException::class)
  fun constructorFailsRegisteredVotesNotBase64Test() {
    ElectionEnd(electionId, laoId, "not base 64")
  }

  @Test
  fun electionEndGetterReturnsCorrectElectionId() {
    MatcherAssert.assertThat(electionEnd.electionId, CoreMatchers.`is`(electionId))
  }

  @Test
  fun electionEndGetterReturnsCorrectLaoId() {
    MatcherAssert.assertThat(electionEnd.laoId, CoreMatchers.`is`(laoId))
  }

  @Test
  fun electionEndGetterReturnsCorrectRegisteredVotes() {
    MatcherAssert.assertThat(electionEnd.registeredVotes, CoreMatchers.`is`(registeredVotes))
  }

  @Test
  fun electionEndGetterReturnsCorrectObject() {
    MatcherAssert.assertThat(electionEnd.`object`, CoreMatchers.`is`(Objects.ELECTION.`object`))
  }

  @Test
  fun electionEndGetterReturnsCorrectAction() {
    MatcherAssert.assertThat(electionEnd.action, CoreMatchers.`is`(Action.END.action))
  }

  @Test
  fun fieldsCantBeNull() {
    Assert.assertThrows(
      IllegalArgumentException::class.java
    ) { ElectionEnd(null, laoId, registeredVotes) }
    Assert.assertThrows(
      IllegalArgumentException::class.java
    ) { ElectionEnd(electionId, null, registeredVotes) }
    Assert.assertThrows(IllegalArgumentException::class.java) {
      ElectionEnd(
        electionId,
        laoId,
        null
      )
    }
  }

  @Test
  fun jsonValidationTest() {
    testData(electionEnd)
  }
}