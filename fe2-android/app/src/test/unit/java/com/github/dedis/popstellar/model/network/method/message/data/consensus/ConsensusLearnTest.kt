package com.github.dedis.popstellar.model.network.method.message.data.consensus

import com.github.dedis.popstellar.model.network.method.message.data.Action
import com.github.dedis.popstellar.model.network.method.message.data.Objects
import com.github.dedis.popstellar.testutils.Base64DataUtils
import com.github.dedis.popstellar.utility.security.HashSHA256.hash
import org.junit.Assert
import org.junit.Test

class ConsensusLearnTest {

  private val messageId = Base64DataUtils.generateMessageID()
  private val timeInSeconds: Long = 1635277619
  private val decision = true
  private val acceptorSignatures: List<String> = mutableListOf("aaa", "bbb")
  private val instanceId = hash("ccc")
  private val consensusLearn =
    ConsensusLearn(instanceId, messageId, timeInSeconds, decision, acceptorSignatures)

  @Test
  fun instanceIdTest() {
    Assert.assertEquals(instanceId, consensusLearn.instanceId)
  }

  @Test
  fun messageIdTest() {
    Assert.assertEquals(messageId, consensusLearn.messageId)
  }

  @Test
  fun acceptorSignaturesTest() {
    Assert.assertEquals(acceptorSignatures, consensusLearn.acceptorSignatures)
  }

  @Test
  fun objectTest() {
    Assert.assertEquals(Objects.CONSENSUS.`object`, consensusLearn.`object`)
  }

  @Test
  fun actionTest() {
    Assert.assertEquals(Action.LEARN.action, consensusLearn.action)
  }

  @Test
  fun creationTest() {
    Assert.assertEquals(timeInSeconds, consensusLearn.creation)
  }

  @Test
  fun learnValueTest() {
    val value = consensusLearn.learnValue

    Assert.assertEquals(decision, value.isDecision)

    val value2 = LearnValue(decision)

    Assert.assertEquals(value, value2)
    Assert.assertEquals(value.hashCode().toLong(), value2.hashCode().toLong())

    Assert.assertNotEquals(value, null)
    Assert.assertNotEquals(value, LearnValue(!decision))
  }

  @Test
  fun equalsTest() {
    val learn2 =
      ConsensusLearn(instanceId, messageId, timeInSeconds, decision, ArrayList(acceptorSignatures))

    Assert.assertEquals(consensusLearn, learn2)
    Assert.assertEquals(consensusLearn.hashCode().toLong(), learn2.hashCode().toLong())

    val random = "random"

    Assert.assertNotEquals(consensusLearn, null)
    Assert.assertNotEquals(
      consensusLearn,
      ConsensusLearn(random, messageId, timeInSeconds, decision, acceptorSignatures)
    )
    Assert.assertNotEquals(
      consensusLearn,
      ConsensusLearn(
        instanceId,
        Base64DataUtils.generateMessageIDOtherThan(messageId),
        timeInSeconds,
        decision,
        acceptorSignatures
      )
    )
    Assert.assertNotEquals(
      consensusLearn,
      ConsensusLearn(instanceId, messageId, 0, decision, acceptorSignatures)
    )
    Assert.assertNotEquals(
      consensusLearn,
      ConsensusLearn(instanceId, messageId, timeInSeconds, !decision, acceptorSignatures)
    )
    Assert.assertNotEquals(
      consensusLearn,
      ConsensusLearn(instanceId, messageId, timeInSeconds, decision, emptyList())
    )
  }
}
