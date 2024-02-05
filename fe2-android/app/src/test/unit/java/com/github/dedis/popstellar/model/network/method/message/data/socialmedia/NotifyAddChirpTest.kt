package com.github.dedis.popstellar.model.network.method.message.data.socialmedia

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.dedis.popstellar.model.network.JsonTestUtils.loadFile
import com.github.dedis.popstellar.model.network.JsonTestUtils.parse
import com.github.dedis.popstellar.model.network.JsonTestUtils.testData
import com.github.dedis.popstellar.model.network.method.message.data.Action
import com.github.dedis.popstellar.model.network.method.message.data.Objects
import com.github.dedis.popstellar.model.objects.Channel
import com.github.dedis.popstellar.model.objects.Channel.Companion.fromString
import com.github.dedis.popstellar.testutils.Base64DataUtils
import com.google.gson.JsonParseException
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NotifyAddChirpTest {
  @Test
  fun objectTest() {
    Assert.assertEquals(Objects.CHIRP.`object`, NOTIFY_ADD_CHIRP.`object`)
  }

  @Test
  fun actionTest() {
    Assert.assertEquals(Action.NOTIFY_ADD.action, NOTIFY_ADD_CHIRP.action)
  }

  @Test
  fun chirpIdTest() {
    Assert.assertEquals(CHIRP_ID, NOTIFY_ADD_CHIRP.chirpId)
  }

  @Test
  fun channelTest() {
    Assert.assertEquals(CHANNEL, NOTIFY_ADD_CHIRP.channel)
  }

  @Test
  fun timestampTest() {
    Assert.assertEquals(TIMESTAMP, NOTIFY_ADD_CHIRP.timestamp)
  }

  @Test
  fun equalsTest() {
    Assert.assertEquals(NOTIFY_ADD_CHIRP, NotifyAddChirp(CHIRP_ID, CHANNEL, TIMESTAMP))
    val random = "random"
    Assert.assertNotEquals(
      NOTIFY_ADD_CHIRP,
      NotifyAddChirp(Base64DataUtils.generateMessageIDOtherThan(CHIRP_ID), CHANNEL, TIMESTAMP)
    )
    Assert.assertNotEquals(
      NOTIFY_ADD_CHIRP,
      NotifyAddChirp(CHIRP_ID, Channel.ROOT.subChannel(random), TIMESTAMP)
    )
    Assert.assertNotEquals(NOTIFY_ADD_CHIRP, NotifyAddChirp(CHIRP_ID, CHANNEL, TIMESTAMP + 1))
  }

  @Test
  fun jsonValidationTest() {
    testData(NOTIFY_ADD_CHIRP)
    val pathDir = "protocol/examples/messageData/chirp_notify_add/"
    val jsonInvalid1 = loadFile(pathDir + "wrong_chirp_notify_add_negative_time.json")
    val jsonInvalid2 = loadFile(pathDir + "wrong_chirp_notify_add_not_base_64_chirp_id.json")

    Assert.assertThrows(JsonParseException::class.java) { parse(jsonInvalid1) }
    Assert.assertThrows(JsonParseException::class.java) { parse(jsonInvalid2) }
  }

  companion object {
    private val CHIRP_ID = Base64DataUtils.generateMessageID()
    private val CHANNEL = fromString("/root/laoId/social/myChannel")
    private const val TIMESTAMP: Long = 1631280815
    private val NOTIFY_ADD_CHIRP = NotifyAddChirp(CHIRP_ID, CHANNEL, TIMESTAMP)
  }
}
