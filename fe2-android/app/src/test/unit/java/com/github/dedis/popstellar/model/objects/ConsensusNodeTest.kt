package com.github.dedis.popstellar.model.objects

import com.github.dedis.popstellar.model.network.method.message.data.consensus.ConsensusElect
import com.github.dedis.popstellar.model.network.method.message.data.consensus.ConsensusKey
import com.github.dedis.popstellar.model.objects.Channel.Companion.fromString
import com.github.dedis.popstellar.testutils.Base64DataUtils
import java.util.Optional
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class ConsensusNodeTest {
  @Before
  fun setup() {
    electInstance1.state = ElectInstance.State.FAILED
    electInstance3.state = ElectInstance.State.ACCEPTED
  }

  @Test
  fun publicKeyTest() {
    val node = ConsensusNode(publicKey)
    Assert.assertEquals(publicKey, node.publicKey)
  }

  @Test
  fun lastElectInstanceTest() {
    val node = ConsensusNode(publicKey)
    Assert.assertEquals(Optional.empty<Any>(), node.getLastElectInstance(instanceId))

    node.addElectInstance(electInstance1)
    val opt = node.getLastElectInstance(instanceId)

    Assert.assertTrue(opt.isPresent)
    Assert.assertEquals(electInstance1, opt.get())

    node.addElectInstance(electInstance2)
    val opt2 = node.getLastElectInstance(instanceId)

    Assert.assertTrue(opt2.isPresent)
    Assert.assertEquals(electInstance2, opt2.get())
    Assert.assertEquals(Optional.empty<Any>(), node.getLastElectInstance(random))
  }

  @Test
  fun stateTest() {
    val node = ConsensusNode(publicKey)
    Assert.assertEquals(ElectInstance.State.WAITING, node.getState(instanceId))

    node.addElectInstance(electInstance1)
    Assert.assertEquals(ElectInstance.State.FAILED, node.getState(instanceId))

    node.addElectInstance(electInstance2)
    Assert.assertEquals(ElectInstance.State.STARTING, node.getState(instanceId))

    node.addElectInstance(electInstance3)
    Assert.assertEquals(ElectInstance.State.ACCEPTED, node.getState(instanceId))
  }

  @Test
  fun addMessageIdOfAnAcceptedConsensusTest() {
    val node = ConsensusNode(publicKey)
    Assert.assertTrue(node.getAcceptedMessageIds().isEmpty())
    node.addMessageIdOfAnAcceptedElect(messageId1)
    node.addMessageIdOfAnAcceptedElect(messageId2)

    val messageIds = node.getAcceptedMessageIds()
    Assert.assertEquals(2, messageIds.size.toLong())
    Assert.assertTrue(messageIds.contains(messageId1))
    Assert.assertTrue(messageIds.contains(messageId2))
  }

  companion object {
    private val key = ConsensusKey("type", "id", "property")
    private val elect1 = ConsensusElect(1000, key.id, key.type, key.property, "value_1")
    private val elect2 = ConsensusElect(2000, key.id, key.type, key.property, "value_2")
    private val elect3 = ConsensusElect(3000, key.id, key.type, key.property, "value_3")
    private val instanceId = elect1.instanceId
    private val channel = fromString("/root/laoChannel/consensus")
    private val publicKey = Base64DataUtils.generatePublicKey()
    private val messageId1 = Base64DataUtils.generateMessageID()
    private val messageId2 = Base64DataUtils.generateMessageID()
    private val messageId3 = Base64DataUtils.generateMessageID()
    private val electInstance1 = ElectInstance(messageId1, channel, publicKey, emptySet(), elect1)
    private val electInstance2 = ElectInstance(messageId2, channel, publicKey, emptySet(), elect2)
    private val electInstance3 = ElectInstance(messageId3, channel, publicKey, emptySet(), elect3)
    private const val random = "random"
  }
}
