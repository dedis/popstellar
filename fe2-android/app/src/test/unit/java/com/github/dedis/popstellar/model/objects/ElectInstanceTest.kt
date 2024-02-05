package com.github.dedis.popstellar.model.objects

import com.github.dedis.popstellar.model.network.method.message.data.consensus.ConsensusElect
import com.github.dedis.popstellar.model.network.method.message.data.consensus.ConsensusElectAccept
import com.github.dedis.popstellar.model.network.method.message.data.consensus.ConsensusKey
import com.github.dedis.popstellar.model.objects.Channel.Companion.fromString
import com.github.dedis.popstellar.model.objects.ElectInstance.Companion.generateConsensusId
import com.github.dedis.popstellar.testutils.Base64DataUtils
import com.github.dedis.popstellar.utility.security.HashSHA256.hash
import org.junit.Assert
import org.junit.Test
import org.mockito.internal.util.collections.Sets

class ElectInstanceTest {
  @Test
  fun messageIdTest() {
    Assert.assertEquals(electMessageId, electInstance.messageId)
  }

  @Test
  fun channelTest() {
    Assert.assertEquals(channel, electInstance.channel)
  }

  @Test
  fun instanceIdTest() {
    // Hash("consensus"||key:type||key:id||key:property)
    val expectedId = hash("consensus", type, objId, property)
    Assert.assertEquals(expectedId, electInstance.instanceId)
  }

  @Test
  fun keyTest() {
    Assert.assertEquals(key, electInstance.key)
  }

  @Test
  fun valueTest() {
    Assert.assertEquals(value, electInstance.value)
  }

  @Test
  fun creationTest() {
    Assert.assertEquals(creationInSeconds, electInstance.creation)
  }

  @Test
  fun proposerTest() {
    Assert.assertEquals(proposer, electInstance.proposer)
  }

  @Test
  fun nodesTest() {
    Assert.assertEquals(nodes, electInstance.nodes)
  }

  @Test
  fun acceptorsResponsesTest() {
    val messageId1 = Base64DataUtils.generateMessageID()
    val messageId2 = Base64DataUtils.generateMessageID()
    val instanceId = electInstance.instanceId
    val electAccept = ConsensusElectAccept(instanceId, electMessageId, true)
    val electReject = ConsensusElectAccept(instanceId, electMessageId, false)
    electInstance.addElectAccept(node2, messageId1, electAccept)
    electInstance.addElectAccept(node2, messageId2, electReject)
    val messageIds = electInstance.acceptorsToMessageId
    Assert.assertEquals(1, messageIds.size.toLong())
    Assert.assertEquals(messageId1, messageIds[node2])
  }

  @Test
  fun setAndGetStateTest() {
    // default state should be STARTING
    Assert.assertEquals(ElectInstance.State.STARTING, electInstance.state)
    for (state in ElectInstance.State.values()) {
      electInstance.state = state
      Assert.assertEquals(state, electInstance.state)
    }
  }

  @Test
  fun generateConsensusIdTest() {
    // Hash(“consensus”||key:type||key:id||key:property)
    val expectedId = hash("consensus", type, objId, property)
    Assert.assertEquals(expectedId, generateConsensusId(type, objId, property))
  }

  companion object {
    private const val creationInSeconds: Long = 1635277619
    private const val type = "TestType"
    private val objId = hash("TestId")
    private const val property = "TestProperty"
    private val key = ConsensusKey(type, objId, property)
    private val value: Any = "TestValue"
    private val electMessageId = Base64DataUtils.generateMessageID()
    private val channel = fromString("/root/aaa/consensus")
    private val proposer = Base64DataUtils.generatePublicKey()
    private val node2 = Base64DataUtils.generatePublicKey()
    private val node3 = Base64DataUtils.generatePublicKey()
    private val nodes = Sets.newSet(proposer, node2, node3)
    private val elect = ConsensusElect(creationInSeconds, objId, type, property, value)
    private val electInstance = ElectInstance(electMessageId, channel, proposer, nodes, elect)
  }
}
