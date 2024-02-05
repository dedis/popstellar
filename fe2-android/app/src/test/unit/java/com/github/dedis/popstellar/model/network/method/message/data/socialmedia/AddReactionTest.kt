package com.github.dedis.popstellar.model.network.method.message.data.socialmedia

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.dedis.popstellar.model.network.JsonTestUtils.loadFile
import com.github.dedis.popstellar.model.network.JsonTestUtils.parse
import com.github.dedis.popstellar.model.network.JsonTestUtils.testData
import com.github.dedis.popstellar.model.network.method.message.data.Action
import com.github.dedis.popstellar.model.network.method.message.data.Objects
import com.github.dedis.popstellar.testutils.Base64DataUtils
import com.google.gson.JsonParseException
import java.time.Instant
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AddReactionTest {
  @Test
  fun objectTest() {
    Assert.assertEquals(Objects.REACTION.`object`, ADD_REACTION.`object`)
  }

  @Test
  fun actionTest() {
    Assert.assertEquals(Action.ADD.action, ADD_REACTION.action)
  }

  @Test
  fun codepointTest() {
    Assert.assertEquals(CODE_POINT, ADD_REACTION.codepoint)
  }

  @Test
  fun chirpIdTest() {
    Assert.assertEquals(CHIRP_ID, ADD_REACTION.chirpId)
  }

  @Test
  fun timestampTest() {
    Assert.assertEquals(TIMESTAMP, ADD_REACTION.timestamp)
  }

  @Test
  fun equalsTest() {
    Assert.assertEquals(ADD_REACTION, AddReaction(CODE_POINT, CHIRP_ID, TIMESTAMP))
    Assert.assertNotEquals(
      ADD_REACTION,
      AddReaction("❤️", Base64DataUtils.generateMessageIDOtherThan(CHIRP_ID), TIMESTAMP)
    )
    Assert.assertNotEquals(
      ADD_REACTION,
      AddReaction(CODE_POINT, Base64DataUtils.generateMessageIDOtherThan(CHIRP_ID), TIMESTAMP)
    )
    Assert.assertNotEquals(ADD_REACTION, AddReaction(CODE_POINT, CHIRP_ID, TIMESTAMP + 1))
  }

  @Test
  fun jsonValidationTest() {
    testData(ADD_REACTION)
    val pathDir = "protocol/examples/messageData/reaction_add/"
    val jsonInvalid1 = loadFile(pathDir + "wrong_reaction_add_negative_time.json")
    val jsonInvalid2 = loadFile(pathDir + "wrong_reaction_add_not_base_64_chirp_id.json")

    Assert.assertThrows(JsonParseException::class.java) { parse(jsonInvalid1) }
    Assert.assertThrows(JsonParseException::class.java) { parse(jsonInvalid2) }
  }

  companion object {
    private const val CODE_POINT = "\uD83D\uDC4D"
    private val CHIRP_ID = Base64DataUtils.generateMessageID()
    private val TIMESTAMP = Instant.now().epochSecond
    private val ADD_REACTION = AddReaction(CODE_POINT, CHIRP_ID, TIMESTAMP)
  }
}
