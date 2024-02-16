package com.github.dedis.popstellar.model.objects

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.dedis.popstellar.model.objects.Meeting.Companion.generateCreateMeetingId
import com.github.dedis.popstellar.model.objects.event.EventState
import com.github.dedis.popstellar.utility.security.HashSHA256.hash
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MeetingTest {
  @Test
  fun correctId() {
    Assert.assertEquals(
      hash("M", LAO_ID, CREATION.toString(), NAME),
      generateCreateMeetingId(LAO_ID, CREATION, NAME)
    )
  }

  @Test
  fun testCreateState() {
    val meeting =
      Meeting(
        ID,
        NAME,
        CREATION,
        START + 10,
        END + 10,
        LOCATION,
        LAST_MODIFIED,
        MODIFICATION_ID,
        MODIFICATION_SIGNATURES
      )
    Assert.assertEquals(EventState.CREATED, meeting.state)
  }

  @Test
  fun testOpenState() {
    val meeting =
      Meeting(
        ID,
        NAME,
        CREATION,
        CREATION,
        END,
        LOCATION,
        LAST_MODIFIED,
        MODIFICATION_ID,
        MODIFICATION_SIGNATURES
      )
    Assert.assertEquals(EventState.OPENED, meeting.state)
  }

  fun testClosedState() {
    val meeting =
      Meeting(
        ID,
        NAME,
        CREATION,
        CREATION,
        CREATION,
        LOCATION,
        LAST_MODIFIED,
        MODIFICATION_ID,
        MODIFICATION_SIGNATURES
      )
    Assert.assertEquals(EventState.CLOSED, meeting.state)
  }

  companion object {
    private const val LAO_ID = "LAO_ID"
    private const val ID = "ID"
    private const val NAME = "MEETING_NAME"
    private const val LOCATION = "Test Location"
    private val CREATION = System.currentTimeMillis() / 1000
    private val START = CREATION + 1
    private val END = START + 1
    private val LAST_MODIFIED = CREATION
    private const val MODIFICATION_ID = "MOD_ID"
    private val MODIFICATION_SIGNATURES: List<String> = ArrayList()
  }
}
