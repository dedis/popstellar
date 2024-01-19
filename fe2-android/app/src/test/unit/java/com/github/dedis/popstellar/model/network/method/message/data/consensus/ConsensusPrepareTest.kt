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
class ConsensusPrepareTest {

  private val instanceId = "aaa"
  private val messageId = MessageID("TVNHX0lE")
  private val timeInSeconds: Long = 1635277619
  private val proposedTry = 4
  private val prepare = ConsensusPrepare(instanceId, messageId, timeInSeconds, proposedTry)

  @Test
  fun objectTest() {
    Assert.assertEquals(Objects.CONSENSUS.getObject(), prepare.getObject())
  }

  @Test
  fun actionTest() {
    Assert.assertEquals(Action.PREPARE.action, prepare.action)
  }

  @Test
  fun instanceIdTest() {
    Assert.assertEquals(instanceId, prepare.instanceId)
  }

  @Test
  fun messageIdTest() {
    Assert.assertEquals(messageId, prepare.messageId)
  }

  @Test
  fun creationTest() {
    Assert.assertEquals(timeInSeconds, prepare.creation)
  }

  @Test
  fun prepareValueTest() {
    val value = prepare.prepareValue

    Assert.assertEquals(proposedTry.toLong(), value.proposedTry.toLong())

    val value2 = PrepareValue(proposedTry)

    Assert.assertEquals(value, value2)
    Assert.assertEquals(value.hashCode().toLong(), value2.hashCode().toLong())

    Assert.assertNotEquals(value, null)
    Assert.assertNotEquals(value, PrepareValue(proposedTry + 1))
  }

  @Test
  fun equalsTest() {
    val prepare2 = ConsensusPrepare(instanceId, messageId, timeInSeconds, proposedTry)

    Assert.assertEquals(prepare, prepare2)
    Assert.assertEquals(prepare.hashCode().toLong(), prepare2.hashCode().toLong())

    val random = "random"

    Assert.assertNotEquals(prepare, null)
    Assert.assertNotEquals(prepare, ConsensusPrepare(random, messageId, timeInSeconds, proposedTry))
    Assert.assertNotEquals(
      prepare,
      ConsensusPrepare(
        instanceId,
        Base64DataUtils.generateMessageIDOtherThan(messageId),
        timeInSeconds,
        proposedTry
      )
    )
    Assert.assertNotEquals(
      prepare,
      ConsensusPrepare(instanceId, messageId, timeInSeconds + 1, proposedTry)
    )
    Assert.assertNotEquals(
      prepare,
      ConsensusPrepare(instanceId, messageId, timeInSeconds, proposedTry + 1)
    )
  }

  @Test
  fun toStringTest() {
    Assert.assertEquals(
      "ConsensusPrepare{instance_id='aaa', message_id='TVNHX0lE', created_at=1635277619, value=PrepareValue{proposed_try=4}}",
      prepare.toString()
    )
  }

  @Test
  fun jsonValidationTest() {
    JsonTestUtils.testData(prepare)

    val dir = "protocol/examples/messageData/consensus_prepare/"
    val jsonInvalid1 = JsonTestUtils.loadFile(dir + "wrong_prepare_negative_created_at.json")
    val jsonInvalid2 = JsonTestUtils.loadFile(dir + "wrong_prepare_negative_proposed_try.json")

    Assert.assertThrows(JsonParseException::class.java) { JsonTestUtils.parse(jsonInvalid1) }
    Assert.assertThrows(JsonParseException::class.java) { JsonTestUtils.parse(jsonInvalid2) }
  }
}
