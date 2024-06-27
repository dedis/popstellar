package com.github.dedis.popstellar.model.network.method.message.data.rollcall

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.dedis.popstellar.model.network.JsonTestUtils.testData
import com.github.dedis.popstellar.model.network.method.message.data.Action
import com.github.dedis.popstellar.model.network.method.message.data.Objects
import com.github.dedis.popstellar.model.objects.event.EventState
import com.github.dedis.popstellar.model.objects.event.EventType
import com.github.dedis.popstellar.model.objects.security.Base64URLData
import com.github.dedis.popstellar.utility.security.HashSHA256.hash
import junit.framework.TestCase.assertNotNull
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
  fun constructor1SucceedsWithValidData() {
    val openRollCall = OpenRollCall(LAO_ID, CREATE_ROLL_CALL.id, TIME, EventState.CREATED)
    assertNotNull(openRollCall)
  }

  @Test(expected = IllegalArgumentException::class)
  fun constructor1FailsWhenLaoIdEmpty() {
    OpenRollCall(EMPTY_B64, CREATE_ROLL_CALL.id, TIME, EventState.CREATED)
  }

  @Test(expected = IllegalArgumentException::class)
  fun constructor1FailsWhenLaoIdNotBase64() {
    OpenRollCall(INVALID_B64, CREATE_ROLL_CALL.id, TIME, EventState.CREATED)
  }

  @Test(expected = IllegalArgumentException::class)
  fun constructor1FailsWhenOpensNotBase64() {
    OpenRollCall(LAO_ID, INVALID_B64, TIME, EventState.CREATED)
  }

  @Test(expected = IllegalArgumentException::class)
  fun constructor1FailsWhenOpensEmpty() {
    OpenRollCall(LAO_ID, EMPTY_B64, TIME, EventState.CREATED)
  }

  @Test(expected = IllegalArgumentException::class)
  fun constructor1FailsWhenOpenedAtNegative() {
    OpenRollCall(LAO_ID, CREATE_ROLL_CALL.id, -1, EventState.CREATED)
  }

  @Test
  fun constructor2SucceedsWithValidDataOPENAction() {
    val openRollCall = OpenRollCall(ID, CREATE_ROLL_CALL.id, TIME, Action.OPEN.action)
    assertNotNull(openRollCall)
  }

  fun constructor2SucceedsWithValidDataREOPENAction() {
    val openRollCall = OpenRollCall(ID, CREATE_ROLL_CALL.id, TIME, Action.REOPEN.action)
    assertNotNull(openRollCall)
  }

  @Test(expected = IllegalArgumentException::class)
  fun constructor2FailsWhenUpdateIdEmpty() {
    OpenRollCall(EMPTY_B64, CREATE_ROLL_CALL.id, TIME, Action.OPEN.action)
  }

  @Test(expected = IllegalArgumentException::class)
  fun constructor2FailsWhenUpdateIdNotBase64() {
    OpenRollCall(INVALID_B64, CREATE_ROLL_CALL.id, TIME, Action.OPEN.action)
  }

  @Test(expected = IllegalArgumentException::class)
  fun constructor2FailsWhenOpensEmpty() {
    OpenRollCall(ID, EMPTY_B64, TIME, Action.OPEN.action)
  }

  @Test(expected = IllegalArgumentException::class)
  fun constructor2FailsWhenOpensNotBase64() {
    OpenRollCall(ID, INVALID_B64, TIME, Action.OPEN.action)
  }

  @Test(expected = IllegalArgumentException::class)
  fun constructor2FailsWhenOpenedAtNegative() {
    OpenRollCall(ID, CREATE_ROLL_CALL.id, -1, Action.OPEN.action)
  }

  @Test(expected = IllegalArgumentException::class)
  fun constructor2FailsWhenActionInvalid() {
    for (action in Action.values()) {
      if (action != Action.OPEN && action != Action.REOPEN) {
        OpenRollCall(ID, CREATE_ROLL_CALL.id, TIME, action.action)
      }
    }
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
    private val REOPEN_ROLL_CALL = OpenRollCall(LAO_ID, CREATE_ROLL_CALL.id, TIME, EventState.CLOSED)
    private val ID = hash(EventType.ROLL_CALL.suffix, LAO_ID, CREATE_ROLL_CALL.id, TIME.toString())

    private const val INVALID_B64 = "invalidBase64String"
    private val EMPTY_B64 = Base64URLData("".toByteArray()).encoded
  }
}
