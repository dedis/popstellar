package com.github.dedis.popstellar.model.network.method.message.data.consensus

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.dedis.popstellar.model.network.JsonTestUtils
import com.github.dedis.popstellar.model.network.method.message.data.Action
import com.github.dedis.popstellar.model.network.method.message.data.Objects
import com.github.dedis.popstellar.testutils.Base64DataUtils
import com.google.gson.JsonParseException
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ConsensusFailureTest {

  private val messageId = Base64DataUtils.generateMessageID()
  private val instanceId = "bbb"
  private val timeInSeconds: Long = 1635277619
  private val failure = ConsensusFailure(instanceId, messageId, timeInSeconds)

  @Test
  fun instanceId() {
    Assert.assertEquals(instanceId, failure.instanceId)
  }

  @Test
  fun messageIdTest() {
    Assert.assertEquals(messageId, failure.messageId)
  }

  @Test
  fun objectTest() {
    Assert.assertEquals(Objects.CONSENSUS.`object`, failure.`object`)
  }

  @Test
  fun actionTest() {
    Assert.assertEquals(Action.FAILURE.action, failure.action)
  }

  @Test
  fun equalsTest() {
    val failure2 = ConsensusFailure(instanceId, messageId, timeInSeconds)

    Assert.assertEquals(failure, failure2)
    Assert.assertEquals(failure.hashCode().toLong(), failure2.hashCode().toLong())

    Assert.assertNotEquals(failure, ConsensusFailure("random", messageId, timeInSeconds))
    Assert.assertNotEquals(
      failure,
      ConsensusFailure(
        instanceId,
        Base64DataUtils.generateMessageIDOtherThan(messageId),
        timeInSeconds
      )
    )
    Assert.assertNotEquals(failure, ConsensusFailure(instanceId, messageId, 0))
  }

  @Test
  fun jsonValidationTest() {
    JsonTestUtils.testData(failure)

    val pathDir = "protocol/examples/messageData/consensus_failure/"
    val jsonInvalid = JsonTestUtils.loadFile(pathDir + "wrong_failure_negative_created_at.json")

    Assert.assertThrows(JsonParseException::class.java) { JsonTestUtils.parse(jsonInvalid) }
  }
}
