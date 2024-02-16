package com.github.dedis.popstellar.model.objects

import com.github.dedis.popstellar.testutils.Base64DataUtils
import org.junit.Assert
import org.junit.Test

class PendingUpdateTest {
  @Test
  fun testGetModificationTime() {
    val pendingUpdate = PendingUpdate(MODIFICATION1, MESSAGE_ID1)
    Assert.assertEquals(MODIFICATION1, pendingUpdate.modificationTime)
  }

  @Test
  fun testGetMessageId() {
    val pendingUpdate = PendingUpdate(MODIFICATION1, MESSAGE_ID1)
    Assert.assertEquals(MESSAGE_ID1, pendingUpdate.messageId)
  }

  @Test
  fun testEqualsAndHashCode() {
    Assert.assertEquals(PENDING_UPDATE1, PENDING_UPDATE2)
    Assert.assertEquals(PENDING_UPDATE1.hashCode().toLong(), PENDING_UPDATE2.hashCode().toLong())
    Assert.assertNotEquals(PENDING_UPDATE1, PENDING_UPDATE3)
    Assert.assertNotEquals(PENDING_UPDATE1.hashCode().toLong(), PENDING_UPDATE3.hashCode().toLong())
    Assert.assertNotEquals(PENDING_UPDATE1, PENDING_UPDATE4)
    Assert.assertNotEquals(PENDING_UPDATE1.hashCode().toLong(), PENDING_UPDATE4.hashCode().toLong())
  }

  @Test
  fun testCompareTo() {
    Assert.assertTrue(PENDING_UPDATE1 < PENDING_UPDATE3)
    Assert.assertTrue(PENDING_UPDATE3 > PENDING_UPDATE1)
    Assert.assertEquals(0, PENDING_UPDATE1.compareTo(PENDING_UPDATE2).toLong())
  }

  companion object {
    private val MESSAGE_ID1 = Base64DataUtils.generateMessageID()
    private val MESSAGE_ID2 = Base64DataUtils.generateMessageID()
    private const val MODIFICATION1 = 1000L
    private const val MODIFICATION2 = 2000L
    private val PENDING_UPDATE1 = PendingUpdate(MODIFICATION1, MESSAGE_ID1)
    private val PENDING_UPDATE2 = PendingUpdate(MODIFICATION1, MESSAGE_ID1)
    private val PENDING_UPDATE3 = PendingUpdate(MODIFICATION2, MESSAGE_ID1)
    private val PENDING_UPDATE4 = PendingUpdate(MODIFICATION1, MESSAGE_ID2)
  }
}
