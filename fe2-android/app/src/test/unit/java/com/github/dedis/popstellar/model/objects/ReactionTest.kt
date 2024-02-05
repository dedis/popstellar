package com.github.dedis.popstellar.model.objects

import com.github.dedis.popstellar.model.objects.security.MessageID
import com.github.dedis.popstellar.testutils.Base64DataUtils
import org.junit.Assert
import org.junit.Test
import java.time.Instant

class ReactionTest {
  @Test
  fun createReactionWithEmptyIdFails() {
    Assert.assertThrows(
      IllegalArgumentException::class.java
    ) { Reaction(EMPTY_MESSAGE_ID, SENDER, EMOJI, CHIRP_ID, TIMESTAMP) }
  }

  @Test
  fun createReactionWithEmptyChirpIdFails() {
    Assert.assertThrows(
      IllegalArgumentException::class.java
    ) { Reaction(REACTION_ID, SENDER, EMOJI, EMPTY_MESSAGE_ID, TIMESTAMP) }
  }

  @Test
  fun createReactionWithFutureTimestampFails() {
    Assert.assertThrows(
      IllegalArgumentException::class.java
    ) { Reaction(REACTION_ID, SENDER, EMOJI, CHIRP_ID, TIMESTAMP + 100000) }
  }

  @Test
  fun createReactionWithPastTimestampFails() {
    Assert.assertThrows(
      IllegalArgumentException::class.java
    ) { Reaction(REACTION_ID, SENDER, EMOJI, CHIRP_ID, -1) }
  }

  @Test
  fun createReactionWithWrongEmojiChirpIdFails() {
    Assert.assertThrows(
      IllegalArgumentException::class.java
    ) { Reaction(REACTION_ID, SENDER, "\uD93D\uDC4D", CHIRP_ID, TIMESTAMP) }
  }

  @Test
  fun createDeletedReaction() {
    val deleted = REACTION.deleted()
    Assert.assertEquals(REACTION.id, deleted.id)
    Assert.assertEquals(REACTION.sender, deleted.sender)
    Assert.assertEquals(REACTION.chirpId, deleted.chirpId)
    Assert.assertEquals(REACTION.timestamp, deleted.timestamp)
    Assert.assertEquals(REACTION.codepoint, deleted.codepoint)
    Assert.assertTrue(deleted.isDeleted)
  }

  companion object {
    private val REACTION_ID = Base64DataUtils.generateMessageID()
    private val CHIRP_ID = Base64DataUtils.generateMessageIDOtherThan(REACTION_ID)
    private val SENDER = Base64DataUtils.generatePublicKey()
    private const val EMOJI = "\uD83D\uDC4D"
    private val TIMESTAMP = Instant.now().epochSecond
    private val EMPTY_MESSAGE_ID = MessageID("")
    private val REACTION = Reaction(REACTION_ID, SENDER, EMOJI, CHIRP_ID, TIMESTAMP)
  }
}