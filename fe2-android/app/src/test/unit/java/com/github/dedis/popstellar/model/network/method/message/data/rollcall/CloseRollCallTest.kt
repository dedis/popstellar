package com.github.dedis.popstellar.model.network.method.message.data.rollcall

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.dedis.popstellar.model.network.JsonTestUtils.testData
import com.github.dedis.popstellar.model.network.method.message.data.Action
import com.github.dedis.popstellar.model.network.method.message.data.Objects
import com.github.dedis.popstellar.model.objects.event.EventState
import com.github.dedis.popstellar.model.objects.event.EventType
import com.github.dedis.popstellar.model.objects.security.Base64URLData
import com.github.dedis.popstellar.testutils.Base64DataUtils
import com.github.dedis.popstellar.utility.MessageValidator
import com.github.dedis.popstellar.utility.security.HashSHA256.hash
import junit.framework.TestCase.assertNotNull
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.util.Collections

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

  @Test
  fun constructorSucceedsWithValidData() {
    val closeRollCall = CloseRollCall(validBase64LaoId, validBase64Closes, pastTime, validAttendees)
    assertNotNull(closeRollCall)
  }

  @Test(expected = IllegalArgumentException::class)
  fun constructorFailsWhenAttendeesNotSorted() {
    val unsortedAttendees = ArrayList(attendees).toMutableList()
    if (unsortedAttendees[0].toString() > unsortedAttendees[1].toString()) {
      Collections.swap(unsortedAttendees, 0, 1)
    }

    CloseRollCall(validBase64LaoId, validBase64Closes, pastTime, unsortedAttendees)
  }

  @Test(expected = IllegalArgumentException::class)
  fun constructorFailsWhenClosedAtInFuture() {
    CloseRollCall(validBase64LaoId, validBase64Closes, futureTime, validAttendees)
  }

  @Test(expected = IllegalArgumentException::class)
  fun constructorFailsWhenTooLongPastTime() {
    CloseRollCall(validBase64LaoId, validBase64Closes, tooLongPastTime, validAttendees)
  }

  @Test(expected = IllegalArgumentException::class)
  fun constructorFailsWhenLaoIdNotBase64() {
    CloseRollCall(invalidBase64, validBase64Closes, pastTime, validAttendees)
  }

  @Test(expected = IllegalArgumentException::class)
  fun constructorFailsWhenLaoIdEmpty() {
    CloseRollCall(emptyBase64, validBase64Closes, pastTime, validAttendees)
  }

  @Test(expected = IllegalArgumentException::class)
  fun constructorFailsWhenClosesNotBase64() {
    CloseRollCall(validBase64LaoId, invalidBase64, pastTime, validAttendees)
  }

  @Test(expected = IllegalArgumentException::class)
  fun constructorFailsWhenClosesEmpty() {
    CloseRollCall(validBase64LaoId, emptyBase64, pastTime, validAttendees)
  }

  @Test(expected = IllegalArgumentException::class)
  fun constructorFailsWhenClosedAtNegative() {
    CloseRollCall(validBase64LaoId, validBase64Closes, negativeClosedAt, validAttendees)
  }

  companion object {
    private val LAO_ID = hash("LAO_ID")
    private const val NAME = "NAME"
    private val TIME = Instant.now().epochSecond
    private const val LOCATION = "Location"
    private val CREATE_ROLL_CALL = CreateRollCall(NAME, TIME, TIME, TIME, LOCATION, null, LAO_ID)
    private val OPEN_ROLL_CALL = OpenRollCall(LAO_ID, CREATE_ROLL_CALL.id, TIME, EventState.CREATED)
    private val CLOSE_ROLL_CALL = CloseRollCall(LAO_ID, OPEN_ROLL_CALL.updateId, TIME, ArrayList())

    private val attendees = listOf(
      Base64DataUtils.generatePublicKey(),
      Base64DataUtils.generatePublicKey(),
      Base64DataUtils.generatePublicKey(),
      Base64DataUtils.generatePublicKey(),
      Base64DataUtils.generatePublicKey(),
      Base64DataUtils.generatePublicKey()
    )

    private val validAttendees = attendees.sortedBy { it.toString() }

    private val pastTime = Instant.now().epochSecond - 1000
    private val futureTime = Instant.now().epochSecond + 10000
    private val tooLongPastTime = Instant.now().epochSecond - MessageValidator.MessageValidatorBuilder.VALID_PAST_DELAY
    private val negativeClosedAt = (-1).toLong()

    private val invalidBase64 = "invalidBase64String"
    private val emptyBase64 = Base64URLData("".toByteArray()).encoded
    private val validBase64LaoId = Base64URLData("validLaoId".toByteArray()).encoded
    private val validBase64Closes = Base64URLData("validCloses".toByteArray()).encoded

  }
}
