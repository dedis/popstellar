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
internal class ConsensusAcceptTest {

  private val instanceId = "aaa"
  private val messageId = MessageID("TVNHX0lE")
  private val timeInSeconds: Long = 1635277619
  private val acceptedTry = 4
  private val acceptedValue = true
  private val accept =
    ConsensusAccept(instanceId, messageId, timeInSeconds, acceptedTry, acceptedValue)

  @Test
  fun objectTest() {
    Assert.assertEquals(Objects.CONSENSUS.getObject(), accept.getObject())
  }

  @Test
  fun actionTest() {
    Assert.assertEquals(Action.ACCEPT.action, accept.action)
  }

  @Test
  fun instanceIdTest() {
    Assert.assertEquals(instanceId, accept.instanceId)
  }

  @Test
  fun messageIdTest() {
    Assert.assertEquals(messageId, accept.messageId)
  }

  @Test
  fun creationTest() {
    Assert.assertEquals(timeInSeconds, accept.creation)
  }

  @Test
  fun acceptValueTest() {
    val value = accept.acceptValue

    Assert.assertEquals(acceptedTry.toLong(), value.acceptedTry.toLong())
    Assert.assertEquals(acceptedValue, value.isAcceptedValue)

    val value2 = AcceptValue(acceptedTry, acceptedValue)

    Assert.assertEquals(value, value2)
    Assert.assertEquals(value.hashCode().toLong(), value2.hashCode().toLong())

    Assert.assertNotEquals(value, null)
    Assert.assertNotEquals(value, AcceptValue(acceptedTry + 1, acceptedValue))
    Assert.assertNotEquals(value, AcceptValue(acceptedTry, !acceptedValue))
  }

  @Test
  fun equalsTest() {
    val accept2 = ConsensusAccept(instanceId, messageId, timeInSeconds, acceptedTry, acceptedValue)

    Assert.assertEquals(accept, accept2)
    Assert.assertEquals(accept.hashCode().toLong(), accept2.hashCode().toLong())

    val random = "random"

    Assert.assertNotEquals(accept, null)
    Assert.assertNotEquals(
      accept,
      ConsensusAccept(random, messageId, timeInSeconds, acceptedTry, acceptedValue)
    )
    Assert.assertNotEquals(
      accept,
      ConsensusAccept(
        instanceId,
        Base64DataUtils.generateMessageIDOtherThan(messageId),
        timeInSeconds,
        acceptedTry,
        acceptedValue
      )
    )
    Assert.assertNotEquals(
      accept,
      ConsensusAccept(instanceId, messageId, timeInSeconds + 1, acceptedTry, acceptedValue)
    )
    Assert.assertNotEquals(
      accept,
      ConsensusAccept(instanceId, messageId, timeInSeconds, acceptedTry + 1, acceptedValue)
    )
    Assert.assertNotEquals(
      accept,
      ConsensusAccept(instanceId, messageId, timeInSeconds, acceptedTry, !acceptedValue)
    )
  }

  @Test
  fun toStringTest() {
    Assert.assertEquals(
      "ConsensusAccept{instance_id='aaa', message_id='TVNHX0lE', created_at=1635277619, value=AcceptValue{accepted_try=4, accepted_value=true}}",
      accept.toString()
    )
  }

  @Test
  fun jsonValidationTest() {
    JsonTestUtils.testData(accept)

    val pathDir = "protocol/examples/messageData/consensus_accept/"
    val jsonInvalid1 = JsonTestUtils.loadFile(pathDir + "wrong_accept_negative_created_at.json")
    val jsonInvalid2 = JsonTestUtils.loadFile(pathDir + "wrong_accept_negative_accepted_try.json")

    Assert.assertThrows(JsonParseException::class.java) { JsonTestUtils.parse(jsonInvalid1) }
    Assert.assertThrows(JsonParseException::class.java) { JsonTestUtils.parse(jsonInvalid2) }
  }
}
