package com.github.dedis.popstellar.model.objects

import com.github.dedis.popstellar.model.objects.security.MessageID
import com.github.dedis.popstellar.testutils.Base64DataUtils
import org.junit.Assert
import org.junit.Test

class ChirpTest {
  @Test
  fun createChirpWithEmptyIdFails() {
    Assert.assertThrows(
      IllegalArgumentException::class.java
    ) { Chirp(EMPTY_MESSAGE_ID, SENDER, TEXT, TIMESTAMP, EMPTY_MESSAGE_ID, LAO_ID) }
  }

  @Test
  fun createChirpWithNegativeTimestampFails() {
    Assert.assertThrows(
      IllegalArgumentException::class.java
    ) { Chirp(ID, SENDER, TEXT, -5, EMPTY_MESSAGE_ID, LAO_ID) }
  }

  @Test
  fun createChirpWithTooManyCharactersFails() {
    val textTooLong =
      ("This text should be way over three hundred characters which is the current limit of the"
          + " text within a chirp, and if I try to set the chirp's text to this, it  should"
          + " throw and IllegalArgumentException() so I hope it does otherwise I might have"
          + " screwed something up. But normally it is not that hard to write enough to reach"
          + " the threshold.")
    Assert.assertThrows(
      IllegalArgumentException::class.java
    ) { Chirp(ID, SENDER, textTooLong, TIMESTAMP, EMPTY_MESSAGE_ID, LAO_ID) }
  }

  @Test
  fun deletedChirpProducesASimilarChirpWithEmptyTextAndDeletedProperty() {
    val chirp = Chirp(ID, SENDER, TEXT, TIMESTAMP, EMPTY_MESSAGE_ID, LAO_ID)
    val deleted = chirp.deleted()
    Assert.assertEquals(chirp.id, deleted.id)
    Assert.assertEquals(chirp.sender, deleted.sender)
    Assert.assertEquals("", deleted.text)
    Assert.assertEquals(chirp.timestamp, deleted.timestamp)
    Assert.assertEquals(chirp.parentId, deleted.parentId)
    Assert.assertTrue(deleted.isDeleted)
    Assert.assertEquals(chirp.laoId, deleted.laoId)
  }

  @Test
  fun laoIdIsStoredCorrectlyInChirp() {
    val chirp = Chirp(ID, SENDER, TEXT, TIMESTAMP, EMPTY_MESSAGE_ID, LAO_ID)
    Assert.assertEquals(LAO_ID, chirp.laoId)
  }

  companion object {
    // By definition, a chirp having no parent has an empty message id as parent
    private val ID = Base64DataUtils.generateMessageID()
    private val SENDER = Base64DataUtils.generatePublicKey()
    private const val TEXT = "This is a Chirp !"
    private const val TIMESTAMP: Long = 10000
    private val EMPTY_MESSAGE_ID = MessageID("")
    private val LAO_ID = Lao.generateLaoId(Base64DataUtils.generatePublicKey(), 1000, "LAO")
  }
}