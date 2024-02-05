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
class OpenRollCallTest {
  @Test
  fun generateOpenRollCallIdTest() {
    // Hash('R'||lao_id||opens||opened_at)
    val expectedId =
      hash(
        EventType.ROLL_CALL.suffix,
        LAO_ID,
        REOPEN_ROLL_CALL.opens,
        REOPEN_ROLL_CALL.openedAt.toString()
      )
    MatcherAssert.assertThat(REOPEN_ROLL_CALL.updateId, CoreMatchers.`is`(expectedId))
  }

  @Test
  fun objectTest() {
    MatcherAssert.assertThat(
      REOPEN_ROLL_CALL.`object`,
      CoreMatchers.`is`(Objects.ROLL_CALL.`object`)
    )
  }

  @Test
  fun actionTest() {
    MatcherAssert.assertThat(OPEN_ROLL_CALL.action, CoreMatchers.`is`(Action.OPEN.action))
    MatcherAssert.assertThat(REOPEN_ROLL_CALL.action, CoreMatchers.`is`(Action.REOPEN.action))
  }

  @Test
  fun openedAtTest() {
    MatcherAssert.assertThat(REOPEN_ROLL_CALL.openedAt, CoreMatchers.`is`(TIME))
  }

  @Test
  fun opensTest() {
    MatcherAssert.assertThat(REOPEN_ROLL_CALL.opens, CoreMatchers.`is`(CREATE_ROLL_CALL.id))
  }

  @Test
  fun jsonValidationTest() {
    testData(REOPEN_ROLL_CALL)
  }

  @Test
  fun equalsTest() {
    Assert.assertEquals(OPEN_ROLL_CALL, OPEN_ROLL_CALL)
    Assert.assertNotEquals(null, OPEN_ROLL_CALL)
    val openRollCall = OpenRollCall(ID, CREATE_ROLL_CALL.id, TIME, Action.OPEN.action)
    Assert.assertEquals(OPEN_ROLL_CALL, openRollCall)
  }

  @Test
  fun hashCodeTest() {
    Assert.assertEquals(
      java.util.Objects.hash(ID, CREATE_ROLL_CALL.id, TIME, Action.OPEN.action).toLong(),
      OPEN_ROLL_CALL.hashCode().toLong()
    )
  }

  companion object {
    private val LAO_ID = hash("LAO_ID")
    private const val NAME = "NAME"
    private val TIME = Instant.now().epochSecond
    private const val LOCATION = "Location"
    private val CREATE_ROLL_CALL = CreateRollCall(NAME, TIME, TIME, TIME, LOCATION, null, LAO_ID)
    private val OPEN_ROLL_CALL = OpenRollCall(LAO_ID, CREATE_ROLL_CALL.id, TIME, EventState.CREATED)
    private val REOPEN_ROLL_CALL =
      OpenRollCall(LAO_ID, CREATE_ROLL_CALL.id, TIME, EventState.CLOSED)
    private val ID = hash(EventType.ROLL_CALL.suffix, LAO_ID, CREATE_ROLL_CALL.id, TIME.toString())
  }
}
