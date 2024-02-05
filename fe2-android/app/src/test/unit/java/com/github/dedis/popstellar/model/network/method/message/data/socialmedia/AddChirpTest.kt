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
class AddChirpTest {
  @Test
  fun createAddChirpWithTextTooLongTest() {
    val textTooLong =
      ("This text should be way over three hundred characters which is the current limit of the" +
        " text within a chirp, and if I try to set the chirp's text to this, it  should" +
        " throw and IllegalArgumentException() so I hope it does otherwise I might have" +
        " screwed something up. But normally it is not that hard to write enough to reach" +
        " the threshold.")
    Assert.assertThrows(IllegalArgumentException::class.java) {
      AddChirp(textTooLong, PARENT_ID, TIMESTAMP)
    }
  }

  @Test
  fun objectTest() {
    Assert.assertEquals(Objects.CHIRP.`object`, ADD_CHIRP.`object`)
  }

  @Test
  fun actionTest() {
    Assert.assertEquals(Action.ADD.action, ADD_CHIRP.action)
  }

  @Test
  fun textTest() {
    Assert.assertEquals(TEXT, ADD_CHIRP.text)
  }

  @Test
  fun parentIdTest() {
    Assert.assertEquals(PARENT_ID, ADD_CHIRP.getParentId().get())
  }

  @Test
  fun timestampTest() {
    Assert.assertEquals(TIMESTAMP, ADD_CHIRP.timestamp)
  }

  @Test
  fun equalsTest() {
    Assert.assertEquals(ADD_CHIRP, AddChirp(TEXT, PARENT_ID, TIMESTAMP))
    val random = "random"
    Assert.assertNotEquals(ADD_CHIRP, AddChirp(random, PARENT_ID, TIMESTAMP))
    Assert.assertNotEquals(
      ADD_CHIRP,
      AddChirp(TEXT, Base64DataUtils.generateMessageIDOtherThan(PARENT_ID), TIMESTAMP)
    )
    Assert.assertNotEquals(ADD_CHIRP, AddChirp(TEXT, PARENT_ID, TIMESTAMP + 1))
  }

  @Test
  fun jsonValidationTest() {
    testData(ADD_CHIRP)
    val path =
      "protocol/examples/messageData/chirp_add_publish/wrong_chirp_add_publish_negative_time.json"
    val invalidJson = loadFile(path)
    Assert.assertThrows(JsonParseException::class.java) { parse(invalidJson) }
  }

  companion object {
    private const val TEXT = "Hello guys"
    private val PARENT_ID = Base64DataUtils.generateMessageID()
    private const val TIMESTAMP: Long = 1631280815
    private val ADD_CHIRP = AddChirp(TEXT, PARENT_ID, TIMESTAMP)
  }
}
