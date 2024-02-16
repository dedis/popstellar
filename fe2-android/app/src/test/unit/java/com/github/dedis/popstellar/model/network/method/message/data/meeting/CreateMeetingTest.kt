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

class CreateMeetingTest {
  @Test
  fun id() {
    Assert.assertEquals(ID, CREATE_MEETING.id)
  }

  @Test
  fun name() {
    Assert.assertEquals(NAME, CREATE_MEETING.name)
  }

  @Test
  fun creation() {
    Assert.assertEquals(CREATION, CREATE_MEETING.creation)
  }

  @Test
  fun location() {
    Assert.assertEquals(LOCATION, CREATE_MEETING.getLocation().orElse(""))
  }

  @Test
  fun start() {
    Assert.assertEquals(START, CREATE_MEETING.start)
  }

  @Test
  fun end() {
    Assert.assertEquals(END, CREATE_MEETING.end)
  }

  @Test
  fun `object`() {
    Assert.assertEquals("meeting", CREATE_MEETING.`object`)
  }

  @Test
  fun action() {
    Assert.assertEquals(Action.CREATE.action, CREATE_MEETING.action)
  }

  @Test
  fun testEquals() {
    Assert.assertEquals(CREATE_MEETING, CREATE_MEETING)
    Assert.assertNotEquals(null, CREATE_MEETING)
    val createMeeting1 = CreateMeeting(LAO_ID, NAME, CREATION, LOCATION, START, END)
    Assert.assertEquals(CREATE_MEETING, createMeeting1)
  }

  @Test
  fun testHashCode() {
    Assert.assertEquals(
      Objects.hash(ID, NAME, CREATION, LOCATION, START, END).toLong(),
      CREATE_MEETING.hashCode().toLong()
    )
  }

  @Test
  fun testToString() {
    val expected =
      String.format(
        "CreateMeeting{id='%s', name='%s', creation=%d, location='%s', start=%d, end=%d}",
        ID,
        NAME,
        CREATION,
        LOCATION,
        START,
        END
      )
    Assert.assertEquals(expected, CREATE_MEETING.toString())
  }

  @Test
  fun nonCoherentIdInConstructorThrowsException() {
    Assert.assertThrows(IllegalArgumentException::class.java) {
      CreateMeeting(LAO_ID, "random id", NAME, CREATION, LOCATION, START, END)
    }
  }

  companion object {
    private val LAO_ID =
      generateLaoId(Base64DataUtils.generatePublicKey(), Instant.now().epochSecond, "Lao name")
    private const val NAME = "name"
    private const val LOCATION = "location"
    private val CREATION = Instant.now().epochSecond
    private val START = CREATION + 1
    private val END = START + 5
    private val ID = hash(EventType.MEETING.suffix, LAO_ID, CREATION.toString(), NAME)
    private val CREATE_MEETING = CreateMeeting(LAO_ID, ID, NAME, CREATION, LOCATION, START, END)
  }
}
