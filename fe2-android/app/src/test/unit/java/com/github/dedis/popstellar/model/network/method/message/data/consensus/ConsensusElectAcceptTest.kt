package com.github.dedis.popstellar.model.network.method.message.data.consensus

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.dedis.popstellar.model.network.JsonTestUtils
import com.github.dedis.popstellar.model.network.method.message.data.Action
import com.github.dedis.popstellar.model.network.method.message.data.Objects
import com.github.dedis.popstellar.testutils.Base64DataUtils
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class ConsensusElectAcceptTest {

  private val messageId = Base64DataUtils.generateMessageID()
  private val instanceId = "bbb"
  private val consensusElectAcceptAccept = ConsensusElectAccept(instanceId, messageId, true)
  private val consensusElectAcceptReject = ConsensusElectAccept(instanceId, messageId, false)

  @Test
  fun instanceId() {
    Assert.assertEquals(instanceId, consensusElectAcceptAccept.instanceId)
  }

  @Test
  fun messageIdTest() {
    Assert.assertEquals(messageId, consensusElectAcceptAccept.messageId)
  }

  @Test
  fun isAcceptTest() {
    Assert.assertTrue(consensusElectAcceptAccept.isAccept)
    Assert.assertFalse(consensusElectAcceptReject.isAccept)
  }

  @Test
  fun objectTest() {
    Assert.assertEquals(Objects.CONSENSUS.`object`, consensusElectAcceptAccept.`object`)
  }

  @Test
  fun actionTest() {
    Assert.assertEquals(Action.ELECT_ACCEPT.action, consensusElectAcceptAccept.action)
  }

  @Test
  fun equalsTest() {
    Assert.assertEquals(
      consensusElectAcceptAccept,
      ConsensusElectAccept(instanceId, messageId, true)
    )
    Assert.assertEquals(
      consensusElectAcceptReject,
      ConsensusElectAccept(instanceId, messageId, false)
    )
    Assert.assertNotEquals(
      consensusElectAcceptAccept,
      ConsensusElectAccept("random", messageId, true)
    )
    Assert.assertNotEquals(
      consensusElectAcceptAccept,
      ConsensusElectAccept(instanceId, Base64DataUtils.generateMessageIDOtherThan(messageId), true)
    )
    Assert.assertNotEquals(
      consensusElectAcceptAccept,
      ConsensusElectAccept(instanceId, messageId, false)
    )
    Assert.assertNotEquals(
      consensusElectAcceptReject,
      ConsensusElectAccept(instanceId, messageId, true)
    )
  }

  @Test
  fun jsonValidationTest() {
    JsonTestUtils.testData(consensusElectAcceptAccept)
  }
}
