package com.github.dedis.popstellar.model.network.method.message.data.socialmedia

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.dedis.popstellar.model.network.JsonTestUtils.loadFile
import com.github.dedis.popstellar.model.network.JsonTestUtils.parse
import com.github.dedis.popstellar.model.network.JsonTestUtils.testData
import com.github.dedis.popstellar.model.network.method.message.data.Action
import com.github.dedis.popstellar.model.network.method.message.data.Objects
import com.github.dedis.popstellar.testutils.Base64DataUtils
import com.google.gson.JsonParseException
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DeleteChirpTest {
  @Test
  fun objectTest() {
    Assert.assertEquals(Objects.CHIRP.`object`, DELETE_CHIRP.`object`)
  }

  @Test
  fun actionTest() {
    Assert.assertEquals(Action.DELETE.action, DELETE_CHIRP.action)
  }

  @Test
  fun chirpIdTest() {
    Assert.assertEquals(CHIRP_ID, DELETE_CHIRP.chirpId)
  }

  @Test
  fun timestampTest() {
    Assert.assertEquals(TIMESTAMP, DELETE_CHIRP.timestamp)
  }

  @Test
  fun equalsTest() {
    Assert.assertEquals(DELETE_CHIRP, DeleteChirp(CHIRP_ID, TIMESTAMP))
    Assert.assertNotEquals(
      DELETE_CHIRP,
      DeleteChirp(Base64DataUtils.generateMessageIDOtherThan(CHIRP_ID), TIMESTAMP)
    )
    Assert.assertNotEquals(DELETE_CHIRP, DeleteChirp(CHIRP_ID, TIMESTAMP + 1))
  }

  @Test
  fun jsonValidationTest() {
    testData(DELETE_CHIRP)
    val pathDir = "protocol/examples/messageData/chirp_delete_publish/"
    val jsonInvalid1 = loadFile(pathDir + "wrong_chirp_delete_publish_negative_time.json")
    val jsonInvalid2 = loadFile(pathDir + "wrong_chirp_delete_publish_not_base_64_chirp_id.json")

    Assert.assertThrows(JsonParseException::class.java) { parse(jsonInvalid1) }
    Assert.assertThrows(JsonParseException::class.java) { parse(jsonInvalid2) }
  }

  companion object {
    private val CHIRP_ID = Base64DataUtils.generateMessageID()
    private const val TIMESTAMP: Long = 1631280815
    private val DELETE_CHIRP = DeleteChirp(CHIRP_ID, TIMESTAMP)
  }
}
