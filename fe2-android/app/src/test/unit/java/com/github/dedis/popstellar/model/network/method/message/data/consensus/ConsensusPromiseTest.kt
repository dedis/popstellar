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
class ConsensusPromiseTest {

  private val instanceId = "aaa"
  private val messageId = MessageID("TVNHX0lE")
  private val timeInSeconds: Long = 1635277619
  private val acceptedTry = 4
  private val acceptedValue = true
  private val promisedTry = 4
  private val promise =
    ConsensusPromise(instanceId, messageId, timeInSeconds, acceptedTry, acceptedValue, promisedTry)

  @Test
  fun objectTest() {
    Assert.assertEquals(Objects.CONSENSUS.`object`, promise.`object`)
  }

  @Test
  fun actionTest() {
    Assert.assertEquals(Action.PROMISE.action, promise.action)
  }

  @Test
  fun instanceIdTest() {
    Assert.assertEquals(instanceId, promise.instanceId)
  }

  @Test
  fun messageIdTest() {
    Assert.assertEquals(messageId, promise.messageId)
  }

  @Test
  fun creationTest() {
    Assert.assertEquals(timeInSeconds, promise.creation)
  }

  @Test
  fun promiseValueTest() {
    val value = promise.promiseValue

    Assert.assertEquals(acceptedTry.toLong(), value.acceptedTry.toLong())
    Assert.assertEquals(acceptedValue, value.isAcceptedValue)
    Assert.assertEquals(promisedTry.toLong(), value.promisedTry.toLong())

    val value2 = PromiseValue(acceptedTry, acceptedValue, promisedTry)

    Assert.assertEquals(value, value2)
    Assert.assertEquals(value.hashCode().toLong(), value2.hashCode().toLong())

    Assert.assertNotEquals(value, null)
    Assert.assertNotEquals(value, PromiseValue(acceptedTry + 1, acceptedValue, promisedTry))
    Assert.assertNotEquals(value, PromiseValue(acceptedTry, !acceptedValue, promisedTry))
    Assert.assertNotEquals(value, PromiseValue(acceptedTry, acceptedValue, promisedTry + 1))
  }

  @Test
  fun equalsTest() {
    val promise2 =
      ConsensusPromise(
        instanceId,
        messageId,
        timeInSeconds,
        acceptedTry,
        acceptedValue,
        promisedTry
      )

    Assert.assertEquals(promise, promise2)
    Assert.assertEquals(promise.hashCode().toLong(), promise2.hashCode().toLong())

    val random = "random"

    Assert.assertNotEquals(promise, null)
    Assert.assertNotEquals(
      promise,
      ConsensusPromise(random, messageId, timeInSeconds, acceptedTry, acceptedValue, promisedTry)
    )
    Assert.assertNotEquals(
      promise,
      ConsensusPromise(
        instanceId,
        Base64DataUtils.generateMessageIDOtherThan(messageId),
        timeInSeconds,
        acceptedTry,
        acceptedValue,
        promisedTry
      )
    )
    Assert.assertNotEquals(
      promise,
      ConsensusPromise(
        instanceId,
        messageId,
        timeInSeconds + 1,
        acceptedTry,
        acceptedValue,
        promisedTry
      )
    )
    Assert.assertNotEquals(
      promise,
      ConsensusPromise(
        instanceId,
        messageId,
        timeInSeconds,
        acceptedTry + 1,
        acceptedValue,
        promisedTry
      )
    )
    Assert.assertNotEquals(
      promise,
      ConsensusPromise(
        instanceId,
        messageId,
        timeInSeconds,
        acceptedTry,
        !acceptedValue,
        promisedTry
      )
    )
    Assert.assertNotEquals(
      promise,
      ConsensusPromise(
        instanceId,
        messageId,
        timeInSeconds,
        acceptedTry,
        acceptedValue,
        promisedTry + 1
      )
    )
  }

  @Test
  fun toStringTest() {
    Assert.assertEquals(
      "ConsensusPromise{instance_id='aaa', message_id='TVNHX0lE', created_at=1635277619, value=PromiseValue{accepted_try=4, accepted_value=true, promised_try=4}}",
      promise.toString()
    )
  }

  @Test
  fun jsonValidationTest() {
    JsonTestUtils.testData(promise)

    val dir = "protocol/examples/messageData/consensus_promise/"
    val jsonInvalid1 = JsonTestUtils.loadFile(dir + "wrong_promise_negative_created_at.json")
    val jsonInvalid2 = JsonTestUtils.loadFile(dir + "wrong_promise_negative_accepted_try.json")
    val jsonInvalid3 = JsonTestUtils.loadFile(dir + "wrong_promise_negative_promised_try.json")

    Assert.assertThrows(JsonParseException::class.java) { JsonTestUtils.parse(jsonInvalid1) }
    Assert.assertThrows(JsonParseException::class.java) { JsonTestUtils.parse(jsonInvalid2) }
    Assert.assertThrows(JsonParseException::class.java) { JsonTestUtils.parse(jsonInvalid3) }
  }
}
