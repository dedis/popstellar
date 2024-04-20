package com.github.dedis.popstellar.ui.lao.event.rollcall

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.github.dedis.popstellar.model.objects.Lao
import com.github.dedis.popstellar.model.objects.RollCall
import com.github.dedis.popstellar.model.objects.RollCall.Companion.closeRollCall
import com.github.dedis.popstellar.model.objects.RollCall.Companion.openRollCall
import com.github.dedis.popstellar.model.objects.event.EventState
import com.github.dedis.popstellar.model.objects.view.LaoView
import com.github.dedis.popstellar.repository.LAORepository
import com.github.dedis.popstellar.repository.RollCallRepository
import com.github.dedis.popstellar.repository.remote.GlobalNetworkManager
import com.github.dedis.popstellar.testutils.Base64DataUtils
import com.github.dedis.popstellar.testutils.BundleBuilder
import com.github.dedis.popstellar.testutils.MessageSenderHelper
import com.github.dedis.popstellar.testutils.MockitoKotlinHelpers
import com.github.dedis.popstellar.testutils.fragment.ActivityFragmentScenarioRule
import com.github.dedis.popstellar.testutils.pages.lao.LaoActivityPageObject
import com.github.dedis.popstellar.testutils.pages.lao.event.rollcall.RollCallFragmentPageObject
import com.github.dedis.popstellar.ui.lao.LaoActivity
import com.github.dedis.popstellar.ui.lao.LaoActivity.Companion.setCurrentFragment
import com.github.dedis.popstellar.ui.lao.event.rollcall.RollCallFragment.Companion.newInstance
import com.github.dedis.popstellar.utility.Constants
import com.github.dedis.popstellar.utility.error.UnknownLaoException
import com.github.dedis.popstellar.utility.error.keys.KeyException
import com.github.dedis.popstellar.utility.security.KeyManager
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.reactivex.subjects.BehaviorSubject
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoTestRule

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class RollCallFragmentTest {
  private val ROLL_CALL =
    RollCall(
      LAO.id,
      LAO.id,
      ROLL_CALL_TITLE,
      CREATION,
      ROLL_CALL_START,
      ROLL_CALL_END,
      EventState.CREATED,
      HashSet(),
      LOCATION,
      ROLL_CALL_EMPTY_DESC
    )
  private val ROLL_CALL_2 =
    RollCall(
      LAO.id + "2",
      LAO.id + "2",
      ROLL_CALL_TITLE + "2",
      CREATION + 1,
      ROLL_CALL_START + 3,
      ROLL_CALL_END + 3,
      EventState.CREATED,
      HashSet(),
      LOCATION,
      ROLL_CALL_DESC
    )

  @Inject lateinit var rollCallRepo: RollCallRepository

  @BindValue @Mock lateinit var laoRepo: LAORepository

  @BindValue @Mock lateinit var networkManager: GlobalNetworkManager

  @BindValue @Mock lateinit var keyManager: KeyManager

  var messageSenderHelper = MessageSenderHelper()

  @JvmField @Rule var rule = InstantTaskExecutorRule()

  @JvmField @Rule(order = 0) val mockitoRule: MockitoTestRule = MockitoJUnit.testRule(this)

  @JvmField @Rule(order = 1) val hiltRule = HiltAndroidRule(this)

  @JvmField
  @Rule(order = 2)
  val setupRule: ExternalResource =
    object : ExternalResource() {
      @Throws(UnknownLaoException::class, KeyException::class)
      override fun before() {
        hiltRule.inject()

        Mockito.`when`(laoRepo.getLaoObservable(ArgumentMatchers.anyString()))
          .thenReturn(laoSubject)
        Mockito.`when`(laoRepo.getLaoView(MockitoKotlinHelpers.any())).thenAnswer { LaoView(LAO) }
        rollCallRepo.updateRollCall(LAO_ID, ROLL_CALL)
        rollCallRepo.updateRollCall(LAO_ID, ROLL_CALL_2)

        Mockito.`when`(keyManager.mainPublicKey).thenReturn(SENDER)
        Mockito.`when`(networkManager.messageSender).thenReturn(messageSenderHelper.mockedSender)
        messageSenderHelper.setupMock()
        Mockito.`when`(
            keyManager.getPoPToken(MockitoKotlinHelpers.any(), MockitoKotlinHelpers.any())
          )
          .thenReturn(POP_TOKEN)
      }
    }

  @JvmField
  @Rule(order = 3)
  var activityScenarioRule: ActivityFragmentScenarioRule<LaoActivity, RollCallFragment> =
    ActivityFragmentScenarioRule.launchIn(
      LaoActivity::class.java,
      BundleBuilder().putString(LaoActivityPageObject.laoIdExtra(), LAO_ID).build(),
      LaoActivityPageObject.containerId(),
      RollCallFragment::class.java,
      { newInstance(ROLL_CALL) },
      BundleBuilder().putString(Constants.ROLL_CALL_ID, ROLL_CALL.persistentId).build()
    )

  @Test
  fun rollCallTitleMatches() {
    RollCallFragmentPageObject.rollCallTitle()
      .check(ViewAssertions.matches(ViewMatchers.withText(ROLL_CALL_TITLE)))
  }

  @Test
  fun statusCreatedTest() {
    RollCallFragmentPageObject.rollCallStatusText()
      .check(ViewAssertions.matches(ViewMatchers.withText("Not yet opened")))
  }

  @Test
  fun datesDisplayedMatches() {
    val startTime = Date(ROLL_CALL.startTimestampInMillis)
    val endTime = Date(ROLL_CALL.endTimestampInMillis)
    val startTimeText = DATE_FORMAT.format(startTime)
    val endTimeText = DATE_FORMAT.format(endTime)

    RollCallFragmentPageObject.rollCallStartTime()
      .check(ViewAssertions.matches(ViewMatchers.withText(startTimeText)))
    RollCallFragmentPageObject.rollCallEndTime()
      .check(ViewAssertions.matches(ViewMatchers.withText(endTimeText)))
  }

  @Test
  fun managementButtonIsDisplayed() {
    RollCallFragmentPageObject.managementButton()
      .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
  }

  @Test
  fun scanButtonIsNotDisplayedWhenCreatedTest() {
    RollCallFragmentPageObject.rollCallScanButton()
      .check(
        ViewAssertions.matches(ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.GONE))
      )
  }

  @Test
  fun managementButtonOpensRollCallWhenCreated() {
    RollCallFragmentPageObject.managementButton()
      .check(ViewAssertions.matches(ViewMatchers.withText("OPEN")))
    RollCallFragmentPageObject.managementButton().perform(ViewActions.click())
    // Wait for the main thread to finish executing the calls made above
    // before asserting their effect
    InstrumentationRegistry.getInstrumentation().waitForIdleSync()

    Mockito.verify(messageSenderHelper.mockedSender)
      .publish(
        MockitoKotlinHelpers.any(),
        MockitoKotlinHelpers.eq(LAO.channel),
        MockitoKotlinHelpers.any()
      )
    messageSenderHelper.assertSubscriptions()
  }

  @Test
  fun statusOpenedTest() {
    rollCallRepo.updateRollCall(LAO_ID, openRollCall(ROLL_CALL))
    RollCallFragmentPageObject.rollCallStatusText()
      .check(ViewAssertions.matches(ViewMatchers.withText("Open")))
  }

  @Test
  fun scanButtonIsDisplayedWhenOpenedTest() {
    rollCallRepo.updateRollCall(LAO_ID, openRollCall(ROLL_CALL))
    RollCallFragmentPageObject.rollCallScanButton()
      .check(
        ViewAssertions.matches(
          ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)
        )
      )
  }

  @Test
  fun managementButtonCloseRollCallWhenOpened() {
    rollCallRepo.updateRollCall(LAO_ID, openRollCall(ROLL_CALL))

    // Mock the fact that the rollcall was successfully opened
    RollCallFragmentPageObject.managementButton()
      .check(ViewAssertions.matches(ViewMatchers.withText("CLOSE")))
    RollCallFragmentPageObject.managementButton().perform(ViewActions.click())
    // Wait for the main thread to finish executing the calls made above
    // before asserting their effect
    InstrumentationRegistry.getInstrumentation().waitForIdleSync()

    Mockito.verify(messageSenderHelper.mockedSender)
      .publish(
        MockitoKotlinHelpers.any(),
        MockitoKotlinHelpers.eq(LAO.channel),
        MockitoKotlinHelpers.any()
      )
    messageSenderHelper.assertSubscriptions()
  }

  @Test
  fun scanButtonOpenScanningTest() {
    rollCallRepo.updateRollCall(LAO_ID, openRollCall(ROLL_CALL))
    RollCallFragmentPageObject.rollCallScanButton().perform(ViewActions.click())
    LaoActivityPageObject.fragmentContainer()
      .check(
        ViewAssertions.matches(
          ViewMatchers.withChild(ViewMatchers.withId(LaoActivityPageObject.qrCodeFragmentId()))
        )
      )
  }

  @Test
  fun statusClosedTest() {
    rollCallRepo.updateRollCall(LAO_ID, closeRollCall(ROLL_CALL))
    RollCallFragmentPageObject.rollCallStatusText()
      .check(ViewAssertions.matches(ViewMatchers.withText("Closed")))
  }

  @Test
  fun scanButtonIsNotDisplayedWhenClosedTest() {
    rollCallRepo.updateRollCall(LAO_ID, closeRollCall(ROLL_CALL))
    RollCallFragmentPageObject.rollCallScanButton()
      .check(
        ViewAssertions.matches(ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.GONE))
      )
  }

  @Test
  fun managementButtonClosedTest() {
    rollCallRepo.updateRollCall(LAO_ID, closeRollCall(ROLL_CALL))
    RollCallFragmentPageObject.managementButton()
      .check(ViewAssertions.matches(ViewMatchers.withText("REOPEN")))
  }

  @Test
  fun blockOpenRollCallTest() {
    rollCallRepo.updateRollCall(LAO_ID, openRollCall(ROLL_CALL_2))
    RollCallFragmentPageObject.managementButton()
      .check(ViewAssertions.matches(ViewMatchers.withText("OPEN")))
    RollCallFragmentPageObject.managementButton().perform(ViewActions.click())
    // Wait for the main thread to finish executing the calls made above
    // before asserting their effect
    InstrumentationRegistry.getInstrumentation().waitForIdleSync()

    // Assert state of roll call is unchanged
    Assert.assertNotEquals(ROLL_CALL.state, EventState.OPENED)
    RollCallFragmentPageObject.managementButton()
      .check(ViewAssertions.matches(ViewMatchers.withText("OPEN")))
  }

  @Test
  fun attendeesTextTest() {
    // Assert that when the roll call is not opened, the organizer has no attendees view
    RollCallFragmentPageObject.rollCallAttendeesText()
      .check(
        ViewAssertions.matches(
          ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.INVISIBLE)
        )
      )

    // Open the roll call
    rollCallRepo.updateRollCall(LAO_ID, openRollCall(ROLL_CALL))
    RollCallFragmentPageObject.rollCallAttendeesText()
      .check(
        ViewAssertions.matches(
          ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)
        )
      )
    RollCallFragmentPageObject.rollCallAttendeesText()
      .check(ViewAssertions.matches(ViewMatchers.withText("Scanned tokens : 0")))

    // Close the roll call
    rollCallRepo.updateRollCall(LAO_ID, closeRollCall(ROLL_CALL))

    // Check that it has switched from scanned tokens to attendees
    RollCallFragmentPageObject.rollCallAttendeesText()
      .check(
        ViewAssertions.matches(
          ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)
        )
      )
    RollCallFragmentPageObject.rollCallAttendeesText()
      .check(ViewAssertions.matches(ViewMatchers.withText("Attendees : 0")))
  }

  @Test
  fun attendeesListTest() {
    // Assert that when the roll call is not opened, the organizer has no attendees view
    RollCallFragmentPageObject.rollCallListAttendees()
      .check(
        ViewAssertions.matches(
          ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.INVISIBLE)
        )
      )

    // Open the roll call
    rollCallRepo.updateRollCall(LAO_ID, openRollCall(ROLL_CALL))
    RollCallFragmentPageObject.rollCallListAttendees()
      .check(
        ViewAssertions.matches(
          ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)
        )
      )
    // Assert that no scanned participant is present
    RollCallFragmentPageObject.rollCallListAttendees()
      .check(ViewAssertions.matches(ViewMatchers.hasChildCount(0)))
  }

  @Test
  @Throws(UnknownLaoException::class)
  fun qrCodeVisibilityTest() {
    // Fake to be a client
    fakeClientLao()
    // Check visibility as client
    rollCallRepo.updateRollCall(LAO_ID, openRollCall(ROLL_CALL))
    RollCallFragmentPageObject.rollCallQRCode()
      .check(
        ViewAssertions.matches(
          ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)
        )
      )
  }

  @Test
  @Throws(UnknownLaoException::class)
  fun popTokenTest() {
    // Fake to be a client
    fakeClientLao()
    // Check visibility as client
    rollCallRepo.updateRollCall(LAO_ID, openRollCall(ROLL_CALL))
    RollCallFragmentPageObject.rollCallPopTokenText()
      .check(
        ViewAssertions.matches(
          ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)
        )
      )
      .check(ViewAssertions.matches(ViewMatchers.withText(POP_TOKEN)))
  }

  @Test
  fun reopenButtonVisibilityTest() {
    // Close the roll call 1
    rollCallRepo.updateRollCall(LAO_ID, closeRollCall(ROLL_CALL))
    // Close then the roll call 2 (it becomes last closed)
    rollCallRepo.updateRollCall(LAO_ID, closeRollCall(ROLL_CALL_2))
    RollCallFragmentPageObject.managementButton()
      .check(
        ViewAssertions.matches(ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.GONE))
      )
  }

  @Test
  fun locationDropdownShowTest() {
    // Here the location text must be hidden
    RollCallFragmentPageObject.rollCallLocationText()
      .check(
        ViewAssertions.matches(ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.GONE))
      )
    RollCallFragmentPageObject.rollCallLocationCard().perform(ViewActions.click())

    // Wait for the main thread to finish executing the calls made above
    // before asserting their effect
    InstrumentationRegistry.getInstrumentation().waitForIdleSync()

    // Check that the location text is properly displayed
    RollCallFragmentPageObject.rollCallLocationText()
      .check(
        ViewAssertions.matches(
          ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)
        )
      )
    RollCallFragmentPageObject.rollCallLocationText()
      .check(ViewAssertions.matches(ViewMatchers.withText(LOCATION)))
  }

  @Test
  fun locationDropdownHideTest() {
    // Click two times to show and then hide
    RollCallFragmentPageObject.rollCallLocationCard().perform(ViewActions.click())
    RollCallFragmentPageObject.rollCallLocationCard().perform(ViewActions.click())

    // Wait for the main thread to finish executing the calls made above
    // before asserting their effect
    InstrumentationRegistry.getInstrumentation().waitForIdleSync()

    // Check that the location text is properly displayed
    RollCallFragmentPageObject.rollCallLocationText()
      .check(
        ViewAssertions.matches(ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.GONE))
      )
  }

  @Test
  fun descriptionDropdownShowTest() {
    openRollCallWithDescription()

    // Here the location text must be hidden
    RollCallFragmentPageObject.rollCallDescriptionText()
      .check(
        ViewAssertions.matches(ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.GONE))
      )
    RollCallFragmentPageObject.rollCallDescriptionCard().perform(ViewActions.click())

    // Wait for the main thread to finish executing the calls made above
    // before asserting their effect
    InstrumentationRegistry.getInstrumentation().waitForIdleSync()

    // Check that the location text is properly displayed
    RollCallFragmentPageObject.rollCallDescriptionText()
      .check(
        ViewAssertions.matches(
          ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)
        )
      )
    RollCallFragmentPageObject.rollCallDescriptionText()
      .check(ViewAssertions.matches(ViewMatchers.withText(ROLL_CALL_DESC)))
  }

  @Test
  fun descriptionDropdownHideTest() {
    openRollCallWithDescription()

    // Click two times to show and then hide
    RollCallFragmentPageObject.rollCallDescriptionCard().perform(ViewActions.click())
    RollCallFragmentPageObject.rollCallDescriptionCard().perform(ViewActions.click())

    // Wait for the main thread to finish executing the calls made above
    // before asserting their effect
    InstrumentationRegistry.getInstrumentation().waitForIdleSync()

    // Check that the location text is properly displayed
    RollCallFragmentPageObject.rollCallDescriptionText()
      .check(
        ViewAssertions.matches(ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.GONE))
      )
  }

  /** Utility function to create a LAO when the user is not the organizer */
  @Throws(UnknownLaoException::class)
  private fun fakeClientLao() {
    Mockito.`when`(laoRepo.getLaoObservable(ArgumentMatchers.anyString())).thenReturn(laoSubject2)
    Mockito.`when`(laoRepo.getLaoView(MockitoKotlinHelpers.any())).thenAnswer { LaoView(LAO_2) }
    rollCallRepo.updateRollCall(LAO_ID2, ROLL_CALL)
    rollCallRepo.updateRollCall(LAO_ID2, ROLL_CALL_2)
    Mockito.`when`(keyManager.mainPublicKey).thenReturn(SENDER_2)
  }

  /** Utility function to open the fragment of an alternative roll call */
  private fun openRollCallWithDescription() {
    activityScenarioRule.scenario.onActivity { activity: LaoActivity ->
      setCurrentFragment(activity.supportFragmentManager, RollCallFragmentPageObject.fragmentId()) {
        newInstance(ROLL_CALL_2.persistentId)
      }
    }
  }

  companion object {
    private const val LAO_NAME = "lao"
    private val SENDER_KEY = Base64DataUtils.generateKeyPair()
    private val SENDER = SENDER_KEY.publicKey
    private val SENDER_2 = Base64DataUtils.generateKeyPair().publicKey
    private val LAO = Lao(LAO_NAME, SENDER, 10223421)
    private val LAO_2 = Lao(LAO_NAME + "2", SENDER_2, 10223422)
    private val LAO_ID = LAO.id
    private val LAO_ID2 = LAO_2.id
    private const val ROLL_CALL_TITLE = "RC title"
    private const val CREATION: Long = 10323411
    private const val ROLL_CALL_START: Long = 10323421
    private const val ROLL_CALL_END: Long = 10323431
    private const val ROLL_CALL_EMPTY_DESC = ""
    private const val ROLL_CALL_DESC = "description"
    private const val LOCATION = "EPFL"
    private val laoSubject = BehaviorSubject.createDefault(LaoView(LAO))
    private val laoSubject2 = BehaviorSubject.createDefault(LaoView(LAO_2))
    private val DATE_FORMAT: DateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm z", Locale.ENGLISH)
    private val POP_TOKEN = Base64DataUtils.generatePoPToken()
  }
}
