package com.github.dedis.popstellar.model.network.method.message.data.rollcall

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.dedis.popstellar.model.network.JsonTestUtils.testData
import com.github.dedis.popstellar.model.network.method.message.data.Action
import com.github.dedis.popstellar.model.network.method.message.data.Objects
import com.github.dedis.popstellar.model.objects.event.EventState
import com.github.dedis.popstellar.model.objects.event.EventType
import com.github.dedis.popstellar.utility.security.HashSHA256.hash
import java.time.Instant
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CloseRollCallTest {
  @Test
  fun generateCloseRollCallIdTest() {
    // Hash('R'||lao_id||closes||closed_at)
    val expectedId =
      hash(
        EventType.ROLL_CALL.suffix,
        LAO_ID,
        CLOSE_ROLL_CALL.closes,
        CLOSE_ROLL_CALL.closedAt.toString()
      )
    MatcherAssert.assertThat(CLOSE_ROLL_CALL.updateId, CoreMatchers.`is`(expectedId))
  }

  @Test
  fun objectTest() {
    MatcherAssert.assertThat(
      CLOSE_ROLL_CALL.`object`,
      CoreMatchers.`is`(Objects.ROLL_CALL.`object`)
    )
  }

  @Test
  fun actionTest() {
    MatcherAssert.assertThat(CLOSE_ROLL_CALL.action, CoreMatchers.`is`(Action.CLOSE.action))
  }

  @Test
  fun attendeesListTest() {
    MatcherAssert.assertThat(CLOSE_ROLL_CALL.attendees, CoreMatchers.`is`(ArrayList<Any>()))
  }

  @Test
  fun closedAtTest() {
    MatcherAssert.assertThat(CLOSE_ROLL_CALL.closedAt, CoreMatchers.`is`(TIME))
  }

  @Test
  fun closesTest() {
    MatcherAssert.assertThat(CLOSE_ROLL_CALL.closes, CoreMatchers.`is`(OPEN_ROLL_CALL.updateId))
  }

  @Test
  fun jsonValidationTest() {
    testData(CLOSE_ROLL_CALL)
  }

  @Test
  fun equalsTest() {
    Assert.assertEquals(CLOSE_ROLL_CALL, CLOSE_ROLL_CALL)
    Assert.assertNotEquals(null, CLOSE_ROLL_CALL)
    val closeRollCall = CloseRollCall(LAO_ID, OPEN_ROLL_CALL.updateId, TIME, ArrayList())
    Assert.assertEquals(CLOSE_ROLL_CALL, closeRollCall)
  }

  @Test
  fun hashCodeTest() {
    val updateId =
      hash(EventType.ROLL_CALL.suffix, LAO_ID, OPEN_ROLL_CALL.updateId, TIME.toString())
    Assert.assertEquals(
      java.util.Objects.hash(updateId, OPEN_ROLL_CALL.updateId, TIME, ArrayList<Any>()).toLong(),
      CLOSE_ROLL_CALL.hashCode().toLong()
    )
  }

  companion object {
    private val LAO_ID = hash("LAO_ID")
    private const val NAME = "NAME"
    private val TIME = Instant.now().epochSecond
    private const val LOCATION = "Location"
    private val CREATE_ROLL_CALL = CreateRollCall(NAME, TIME, TIME, TIME, LOCATION, null, LAO_ID)
    private val OPEN_ROLL_CALL = OpenRollCall(LAO_ID, CREATE_ROLL_CALL.id, TIME, EventState.CREATED)
    private val CLOSE_ROLL_CALL = CloseRollCall(LAO_ID, OPEN_ROLL_CALL.updateId, TIME, ArrayList())
  }
}
