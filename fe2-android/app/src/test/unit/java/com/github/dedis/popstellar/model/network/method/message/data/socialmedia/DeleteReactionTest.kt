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
class DeleteReactionTest {
  @Test
  fun objectTest() {
    Assert.assertEquals(Objects.REACTION.`object`, DELETE_REACTION.`object`)
  }

  @Test
  fun actionTest() {
    Assert.assertEquals(Action.DELETE.action, DELETE_REACTION.action)
  }

  @Test
  fun chirpIdTest() {
    Assert.assertEquals(REACTION_ID, DELETE_REACTION.reactionID)
  }

  @Test
  fun timestampTest() {
    Assert.assertEquals(TIMESTAMP, DELETE_REACTION.timestamp)
  }

  @Test
  fun equalsTest() {
    Assert.assertEquals(DELETE_REACTION, DeleteReaction(REACTION_ID, TIMESTAMP))
    Assert.assertNotEquals(
      DELETE_REACTION,
      DeleteReaction(Base64DataUtils.generateMessageIDOtherThan(REACTION_ID), TIMESTAMP)
    )
    Assert.assertNotEquals(DELETE_REACTION, DeleteReaction(REACTION_ID, TIMESTAMP + 1))
  }

  @Test
  fun jsonValidationTest() {
    testData(DELETE_REACTION)
    val pathDir = "protocol/examples/messageData/reaction_delete/"
    val jsonInvalid1 = loadFile(pathDir + "wrong_reaction_delete_negative_time.json")
    val jsonInvalid2 = loadFile(pathDir + "wrong_reaction_delete_not_base_64_reaction_id.json")

    Assert.assertThrows(JsonParseException::class.java) { parse(jsonInvalid1) }
    Assert.assertThrows(JsonParseException::class.java) { parse(jsonInvalid2) }
  }

  companion object {
    private val REACTION_ID = Base64DataUtils.generateMessageID()
    private val TIMESTAMP = Instant.now().epochSecond
    private val DELETE_REACTION = DeleteReaction(REACTION_ID, TIMESTAMP)
  }
}
