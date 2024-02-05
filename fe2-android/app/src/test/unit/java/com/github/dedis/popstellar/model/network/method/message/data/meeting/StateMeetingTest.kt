package com.github.dedis.popstellar.model.network.method.message.data.meeting

import com.github.dedis.popstellar.model.network.method.message.data.Action
import com.github.dedis.popstellar.model.objects.Lao.Companion.generateLaoId
import com.github.dedis.popstellar.model.objects.event.EventType
import com.github.dedis.popstellar.testutils.Base64DataUtils
import com.github.dedis.popstellar.utility.security.HashSHA256.hash
import java.time.Instant
import java.util.Objects
import org.junit.Assert
import org.junit.Test

class StateMeetingTest {
  @Test
  fun id() {
    Assert.assertEquals(ID, STATE_MEETING.id)
  }

  @Test
  fun name() {
    Assert.assertEquals(NAME, STATE_MEETING.name)
  }

  @Test
  fun creation() {
    Assert.assertEquals(CREATION, STATE_MEETING.creation)
  }

  @Test
  fun lastModified() {
    Assert.assertEquals(LAST_MODIFIED, STATE_MEETING.lastModified)
  }

  @Test
  fun location() {
    Assert.assertEquals(LOCATION, STATE_MEETING.getLocation().orElse(""))
  }

  @Test
  fun start() {
    Assert.assertEquals(START, STATE_MEETING.start)
  }

  @Test
  fun end() {
    Assert.assertEquals(END, STATE_MEETING.end)
  }

  @Test
  fun modificationId() {
    Assert.assertEquals(MODIFICATION_ID, STATE_MEETING.modificationId)
  }

  @Test
  fun modificationSignatures() {
    Assert.assertEquals(MODIFICATION_SIGNATURES, STATE_MEETING.modificationSignatures)
  }

  @Test
  fun `object`() {
    Assert.assertEquals("meeting", STATE_MEETING.`object`)
  }

  @Test
  fun action() {
    Assert.assertEquals(Action.STATE.action, STATE_MEETING.action)
  }

  @Test
  fun testEquals() {
    Assert.assertEquals(STATE_MEETING, STATE_MEETING)
    Assert.assertNotEquals(null, STATE_MEETING)
    val stateMeeting =
      StateMeeting(
        LAO_ID,
        ID,
        NAME,
        CREATION,
        LAST_MODIFIED,
        LOCATION,
        START,
        END,
        MODIFICATION_ID,
        MODIFICATION_SIGNATURES
      )
    Assert.assertEquals(STATE_MEETING, stateMeeting)
  }

  @Test
  fun testHashCode() {
    Assert.assertEquals(
      Objects.hash(
          ID,
          NAME,
          CREATION,
          LAST_MODIFIED,
          LOCATION,
          START,
          END,
          MODIFICATION_ID,
          MODIFICATION_SIGNATURES
        )
        .toLong(),
      STATE_MEETING.hashCode().toLong()
    )
  }

  @Test
  fun testToString() {
    val expected =
      String.format(
        "StateMeeting{id='%s', name='%s', creation=%d, lastModified=%d, location='%s', start=%d, end=%d, modificationId='%s', modificationSignatures=%s}",
        ID,
        NAME,
        CREATION,
        LAST_MODIFIED,
        LOCATION,
        START,
        END,
        MODIFICATION_ID,
        MODIFICATION_SIGNATURES
      )
    Assert.assertEquals(expected, STATE_MEETING.toString())
  }

  @Test
  fun nonCoherentIdInConstructorThrowsException() {
    Assert.assertThrows(IllegalArgumentException::class.java) {
      StateMeeting(
        LAO_ID,
        "random id",
        NAME,
        CREATION,
        LAST_MODIFIED,
        LOCATION,
        START,
        END,
        MODIFICATION_ID,
        MODIFICATION_SIGNATURES
      )
    }
  }

  companion object {
    private val LAO_ID =
      generateLaoId(Base64DataUtils.generatePublicKey(), Instant.now().epochSecond, "Lao name")
    private const val NAME = "name"
    private const val LOCATION = "location"
    private const val MODIFICATION_ID = "modif id"
    private val CREATION = Instant.now().epochSecond
    private val START = CREATION + 1
    private val LAST_MODIFIED = START + 1
    private val END = START + 5
    private const val SIGNATURE_1 = "signature 1"
    private const val SIGNATURE_2 = "signature 2"
    private val ID = hash(EventType.MEETING.suffix, LAO_ID, CREATION.toString(), NAME)
    private val MODIFICATION_SIGNATURES = listOf(SIGNATURE_1, SIGNATURE_2)
    private val STATE_MEETING =
      StateMeeting(
        LAO_ID,
        ID,
        NAME,
        CREATION,
        LAST_MODIFIED,
        LOCATION,
        START,
        END,
        MODIFICATION_ID,
        MODIFICATION_SIGNATURES
      )
  }
}
