package com.github.dedis.popstellar.model.network.method.message.data.consensus

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.dedis.popstellar.model.network.JsonTestUtils
import com.github.dedis.popstellar.model.network.method.message.data.Action
import com.github.dedis.popstellar.model.network.method.message.data.Objects
import com.github.dedis.popstellar.model.objects.security.MessageID
import com.github.dedis.popstellar.testutils.Base64DataUtils
import com.google.gson.JsonParseException
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ConsensusProposeTest {

  private val instanceId = "aaa"
  private val messageId = MessageID("TVNHX0lE")
  private val timeInSeconds: Long = 1635277619
  private val proposedTry = 4
  private val proposedValue = true
  private val acceptorSignatures: List<String> = mutableListOf("h1", "h2")
  private val propose =
    ConsensusPropose(
      instanceId,
      messageId,
      timeInSeconds,
      proposedTry,
      proposedValue,
      acceptorSignatures
    )

  @Test
  fun objectTest() {
    Assert.assertEquals(Objects.CONSENSUS.`object`, propose.`object`)
  }

  @Test
  fun actionTest() {
    Assert.assertEquals(Action.PROPOSE.action, propose.action)
  }

  @Test
  fun instanceIdTest() {
    Assert.assertEquals(instanceId, propose.instanceId)
  }

  @Test
  fun messageIdTest() {
    Assert.assertEquals(messageId, propose.messageId)
  }

  @Test
  fun creationTest() {
    Assert.assertEquals(timeInSeconds, propose.creation)
  }

  @Test
  fun proposeValueTest() {
    val value = propose.proposeValue

    Assert.assertEquals(proposedTry.toLong(), value.proposedTry.toLong())
    Assert.assertEquals(proposedValue, value.isProposedValue)

    val value2 = ProposeValue(proposedTry, proposedValue)

    Assert.assertEquals(value, value2)
    Assert.assertEquals(value.hashCode().toLong(), value2.hashCode().toLong())

    Assert.assertNotEquals(value, null)
    Assert.assertNotEquals(value, ProposeValue(proposedTry + 1, proposedValue))
    Assert.assertNotEquals(value, ProposeValue(proposedTry, !proposedValue))
  }

  @Test
  fun acceptorSignaturesTest() {
    Assert.assertEquals(acceptorSignatures, propose.getAcceptorSignatures())
  }

  @Test
  fun equalsTest() {
    val propose2 =
      ConsensusPropose(
        instanceId,
        messageId,
        timeInSeconds,
        proposedTry,
        proposedValue,
        ArrayList(acceptorSignatures)
      )

    Assert.assertEquals(propose, propose2)
    Assert.assertEquals(propose.hashCode().toLong(), propose2.hashCode().toLong())

    val random = "random"

    Assert.assertNotEquals(propose, null)
    Assert.assertNotEquals(
      propose,
      ConsensusPropose(
        random,
        messageId,
        timeInSeconds,
        proposedTry,
        proposedValue,
        acceptorSignatures
      )
    )
    Assert.assertNotEquals(
      propose,
      ConsensusPropose(
        instanceId,
        Base64DataUtils.generateMessageIDOtherThan(messageId),
        timeInSeconds,
        proposedTry,
        proposedValue,
        acceptorSignatures
      )
    )
    Assert.assertNotEquals(
      propose,
      ConsensusPropose(
        instanceId,
        messageId,
        timeInSeconds + 1,
        proposedTry,
        proposedValue,
        acceptorSignatures
      )
    )
    Assert.assertNotEquals(
      propose,
      ConsensusPropose(
        instanceId,
        messageId,
        timeInSeconds,
        proposedTry + 1,
        proposedValue,
        acceptorSignatures
      )
    )
    Assert.assertNotEquals(
      propose,
      ConsensusPropose(
        instanceId,
        messageId,
        timeInSeconds,
        proposedTry,
        !proposedValue,
        acceptorSignatures
      )
    )
    Assert.assertNotEquals(
      propose,
      ConsensusPropose(
        instanceId,
        messageId,
        timeInSeconds,
        proposedTry,
        proposedValue,
        emptyList()
      )
    )
  }

  @Test
  fun toStringTest() {
    Assert.assertEquals(
      "ConsensusPropose{instance_id='aaa', message_id='TVNHX0lE', created_at=1635277619, value=ProposeValue{proposed_try=4, proposed_value=true}, acceptor-signatures=[h1, h2]}",
      propose.toString()
    )
  }

  @Test
  fun jsonValidationTest() {
    JsonTestUtils.testData(propose)

    val dir = "protocol/examples/messageData/consensus_propose/"
    val jsonInvalid1 = JsonTestUtils.loadFile(dir + "wrong_propose_negative_created_at.json")
    val jsonInvalid2 = JsonTestUtils.loadFile(dir + "wrong_propose_negative_proposed_try.json")

    Assert.assertThrows(JsonParseException::class.java) { JsonTestUtils.parse(jsonInvalid1) }
    Assert.assertThrows(JsonParseException::class.java) { JsonTestUtils.parse(jsonInvalid2) }
  }
}
