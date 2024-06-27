package com.github.dedis.popstellar.model.network.method.message.data.rollcall

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.dedis.popstellar.model.network.JsonTestUtils.testData
import com.github.dedis.popstellar.model.network.method.message.data.Action
import com.github.dedis.popstellar.model.network.method.message.data.Objects
import com.github.dedis.popstellar.model.objects.event.EventType
import com.github.dedis.popstellar.model.objects.security.Base64URLData
import com.github.dedis.popstellar.utility.security.HashSHA256.hash
import junit.framework.TestCase.assertNotNull
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class CreateRollCallTest {
  @Test
  fun generateCreateRollCallIdTest() {
    // Hash('R'||lao_id||creation||NAME)
    val expectedId =
      hash(
        EventType.ROLL_CALL.suffix,
        LAO_ID,
        CREATE_ROLL_CALL.creation.toString(),
        CREATE_ROLL_CALL.name
      )
    MatcherAssert.assertThat(CREATE_ROLL_CALL.id, CoreMatchers.`is`(expectedId))
  }

  @Test
  fun objectTest() {
    MatcherAssert.assertThat(
      CREATE_ROLL_CALL.`object`,
      CoreMatchers.`is`(Objects.ROLL_CALL.`object`)
    )
  }

  @Test
  fun actionTest() {
    MatcherAssert.assertThat(CREATE_ROLL_CALL.action, CoreMatchers.`is`(Action.CREATE.action))
  }

  @Test
  fun nameTest() {
    MatcherAssert.assertThat(CREATE_ROLL_CALL.name, CoreMatchers.`is`(NAME))
  }

  @Test
  fun proposedStartTest() {
    MatcherAssert.assertThat(CREATE_ROLL_CALL.proposedStart, CoreMatchers.`is`(NOW))
  }

  @Test
  fun proposedEndTest() {
    MatcherAssert.assertThat(CREATE_ROLL_CALL.proposedEnd, CoreMatchers.`is`(END))
  }

  @Test
  fun descriptionTest() {
    MatcherAssert.assertThat(CREATE_ROLL_CALL.location, CoreMatchers.`is`(LOCATION))
  }

  @Test
  fun constructor1SucceedsWithValidData() {
    val createRollCall = CreateRollCall(NAME, NOW, NOW, END, LOCATION, null, LAO_ID)
    assertNotNull(createRollCall)
  }

  fun constructor1SucceedsWithValidDataDescription() {
    CreateRollCall(NAME, NOW, NOW, END, LOCATION, "super cool description", LAO_ID)
  }

  @Test(expected = IllegalArgumentException::class)
  fun constructor1FailsWhenNameEmpty() {
    CreateRollCall("", NOW, NOW, END, LOCATION, null, LAO_ID)
  }

  @Test(expected = IllegalArgumentException::class)
  fun constructor1FailsWhenLAOIdEmpty() {
    CreateRollCall(NAME, NOW, NOW, END, LOCATION, null, EMPTY_B64)
  }

  @Test(expected = IllegalArgumentException::class)
  fun constructor1FailsWhenLAOIdNotB64() {
    CreateRollCall(NAME, NOW, NOW, END, LOCATION, null, INVALID_B64)
  }

  @Test(expected = IllegalArgumentException::class)
  fun constructor1FailsWhenCreationNegative() {
    CreateRollCall(NAME, -1, NOW, END, LOCATION, null, LAO_ID)
  }

  @Test(expected = IllegalArgumentException::class)
  fun constructor1FailsWhenProposedStartNegative() {
    CreateRollCall(NAME, NOW, -1, END, LOCATION, null, LAO_ID)
  }

  @Test(expected = IllegalArgumentException::class)
  fun constructor1FailsWhenProposedEndNegative() {
    CreateRollCall(NAME, NOW, NOW, -1, LOCATION, null, LAO_ID)
  }

  @Test(expected = IllegalArgumentException::class)
  fun constructor1FailsWhenLocationEmpty() {
    CreateRollCall(NAME, NOW, NOW, END, EMPTY_STRING, null, LAO_ID)
  }

  @Test(expected = IllegalArgumentException::class)
  fun constructor1FailsWhenDescriptionEmpty() {
    CreateRollCall(NAME, NOW, NOW, END, LOCATION, EMPTY_STRING, LAO_ID)
  }

  @Test
  fun constructorSucceedsWithValidData() {
    val createRollCall = CreateRollCall(ID, NAME, NOW, NOW, END, LOCATION, null)
    assertNotNull(createRollCall)
  }

  fun constructorSucceedsWithValidDataDescription() {
    CreateRollCall(ID, NAME, NOW, NOW, END, LOCATION, "super cool description")
  }

  @Test(expected = IllegalArgumentException::class)
  fun constructor2FailsWhenNameEmpty() {
    CreateRollCall(ID, "", NOW, NOW, END, LOCATION, null)
  }

  @Test(expected = IllegalArgumentException::class)
  fun constructor2FailsWhenIdEmpty() {
    CreateRollCall(EMPTY_B64, NAME, NOW, NOW, END, LOCATION, null)
  }

  @Test(expected = IllegalArgumentException::class)
  fun constructor2FailsWhenIdNotB64() {
    CreateRollCall(INVALID_B64, NAME, NOW, NOW, END, LOCATION, null)
  }

  @Test(expected = IllegalArgumentException::class)
  fun constructor2FailsWhenCreationNegative() {
    CreateRollCall(ID, NAME, -1, NOW, END, LOCATION, null)
  }

  @Test(expected = IllegalArgumentException::class)
  fun constructor2FailsWhenProposedStartNegative() {
    CreateRollCall(ID, NAME, NOW, -1, END, LOCATION, null)
  }

  @Test(expected = IllegalArgumentException::class)
  fun constructor2FailsWhenProposedEndNegative() {
    CreateRollCall(ID, NAME, NOW, NOW, -1, LOCATION, null)
  }

  @Test(expected = IllegalArgumentException::class)
  fun constructor2FailsWhenLocationEmpty() {
    CreateRollCall(ID, NAME, NOW, NOW, END, EMPTY_STRING, null)
  }

  @Test(expected = IllegalArgumentException::class)
  fun constructor2FailsWhenDescriptionEmpty() {
    CreateRollCall(ID, NAME, NOW, NOW, END, LOCATION, EMPTY_STRING)
  }


  @Test
  fun jsonValidationTest() {
    testData(CREATE_ROLL_CALL)
  }

  @Test
  fun equalsTest() {
    Assert.assertEquals(CREATE_ROLL_CALL, CREATE_ROLL_CALL)
    Assert.assertNotEquals(null, CREATE_ROLL_CALL)
    val createRollCall = CreateRollCall(ID, NAME, NOW, NOW, END, LOCATION, null)
    Assert.assertEquals(CREATE_ROLL_CALL, createRollCall)
  }

  @Test
  fun hashCodeTest() {
    Assert.assertEquals(
      java.util.Objects.hash(ID, NAME, NOW, NOW, END, LOCATION, null).toLong(),
      CREATE_ROLL_CALL.hashCode().toLong()
    )
  }

  companion object {
    private const val LAO_ID = "fEvAfdtNrykd9NPYl9ReHLX-6IP6SFLKTZJLeGUHZ_U="
    private const val NAME = "NAME"
    private val NOW = Instant.now().epochSecond
    private val END = NOW + 30L
    private const val LOCATION = "Location"
    private const val EMPTY_STRING = ""
    private val INVALID_B64 = "invalidBase64String"
    private val EMPTY_B64 = Base64URLData("".toByteArray()).encoded
    private val CREATE_ROLL_CALL = CreateRollCall(NAME, NOW, NOW, END, LOCATION, null, LAO_ID)
    private val ID = hash(EventType.ROLL_CALL.suffix, LAO_ID, NOW.toString(), NAME)
  }
}
