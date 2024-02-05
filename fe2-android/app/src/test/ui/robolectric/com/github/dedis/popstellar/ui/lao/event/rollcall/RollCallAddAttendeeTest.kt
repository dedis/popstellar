package com.github.dedis.popstellar.ui.lao.event.rollcall

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.github.dedis.popstellar.model.objects.Lao
import com.github.dedis.popstellar.model.objects.view.LaoView
import com.github.dedis.popstellar.repository.LAORepository
import com.github.dedis.popstellar.testutils.Base64DataUtils
import com.github.dedis.popstellar.testutils.BundleBuilder
import com.github.dedis.popstellar.testutils.MockitoKotlinHelpers
import com.github.dedis.popstellar.testutils.UITestUtils.forceTypeText
import com.github.dedis.popstellar.testutils.fragment.ActivityFragmentScenarioRule
import com.github.dedis.popstellar.testutils.pages.lao.LaoActivityPageObject
import com.github.dedis.popstellar.testutils.pages.scanning.QrScanningPageObject
import com.github.dedis.popstellar.ui.lao.LaoActivity
import com.github.dedis.popstellar.ui.qrcode.QrScannerFragment
import com.github.dedis.popstellar.ui.qrcode.QrScannerFragment.Companion.newInstance
import com.github.dedis.popstellar.ui.qrcode.ScanningAction
import com.github.dedis.popstellar.utility.error.UnknownLaoException
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.reactivex.subjects.BehaviorSubject
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoTestRule

@LargeTest
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class RollCallAddAttendeeTest {
  @BindValue @Mock lateinit var repository: LAORepository

  @JvmField @Rule var rule = InstantTaskExecutorRule()

  @JvmField @Rule(order = 0) val mockitoRule: MockitoTestRule = MockitoJUnit.testRule(this)

  @JvmField @Rule(order = 1) val hiltRule = HiltAndroidRule(this)

  @JvmField
  @Rule(order = 2)
  val setupRule: ExternalResource =
    object : ExternalResource() {
      @Throws(UnknownLaoException::class)
      override fun before() {
        hiltRule.inject()
        Mockito.`when`(repository.getLaoObservable(ArgumentMatchers.anyString()))
          .thenReturn(laoSubject)
        Mockito.`when`(repository.getLaoView(MockitoKotlinHelpers.any())).thenReturn(LaoView(LAO))
      }
    }

  @JvmField
  @Rule(order = 3)
  var activityScenarioRule: ActivityFragmentScenarioRule<LaoActivity, QrScannerFragment> =
    ActivityFragmentScenarioRule.launchIn(
      LaoActivity::class.java,
      BundleBuilder().putString(LaoActivityPageObject.laoIdExtra(), LAO_ID).build(),
      LaoActivityPageObject.containerId(),
      QrScannerFragment::class.java
    ) {
      newInstance(ScanningAction.ADD_ROLL_CALL_ATTENDEE)
    }

  @Test
  fun addButtonIsDisplayed() {
    QrScanningPageObject.openManualButton()
      .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
  }

  @Test
  fun addingAttendeeManuallyUpdatesCount() {
    QrScanningPageObject.openManualButton().perform(ViewActions.click())
    QrScanningPageObject.manualAddEditText().perform(forceTypeText(VALID_RC_MANUAL_INPUT))
    QrScanningPageObject.manualAddConfirm().perform(ViewActions.click())

    // Since we haven't mocked for the viewModel to fetch the organizer token, adding an attendee
    // should result in a total of one attendee
    QrScanningPageObject.attendeeCount().check(ViewAssertions.matches(ViewMatchers.withText("1")))
  }

  @Test
  fun addingInvalidJsonFormatDoesNotAddAttendees() {
    QrScanningPageObject.openManualButton().perform(ViewActions.click())
    QrScanningPageObject.manualAddEditText().perform(forceTypeText(JSON_INVALID_INPUT))
    QrScanningPageObject.manualAddConfirm().perform(ViewActions.click())
    QrScanningPageObject.attendeeCount().check(ViewAssertions.matches(ViewMatchers.withText("0")))
  }

  @Test
  fun addingValidNonRcFormatDoesNotAddAttendees() {
    QrScanningPageObject.openManualButton().perform(ViewActions.click())
    QrScanningPageObject.manualAddEditText().perform(forceTypeText(VALID_WITNESS_MANUAL_INPUT))
    QrScanningPageObject.manualAddConfirm().perform(ViewActions.click())
    QrScanningPageObject.attendeeCount().check(ViewAssertions.matches(ViewMatchers.withText("0")))
  }

  @Test
  fun addingKeyFormatDoesNotAddAttendees() {
    QrScanningPageObject.openManualButton().perform(ViewActions.click())
    QrScanningPageObject.manualAddEditText().perform(forceTypeText(INVALID_KEY_FORMAT_INPUT))
    QrScanningPageObject.manualAddConfirm().perform(ViewActions.click())
    QrScanningPageObject.attendeeCount().check(ViewAssertions.matches(ViewMatchers.withText("0")))
  }

  companion object {
    private const val LAO_NAME = "lao"
    private val SENDER_KEY = Base64DataUtils.generateKeyPair()
    private val SENDER = SENDER_KEY.publicKey
    private val LAO = Lao(LAO_NAME, SENDER, 10223421)
    private val LAO_ID = LAO.id
    private val POP_TOKEN = Base64DataUtils.generatePoPToken().publicKey.encoded
    private val VALID_RC_MANUAL_INPUT = "{\"pop_token\": \"$POP_TOKEN\"}"
    val JSON_INVALID_INPUT = "{pop_token:$POP_TOKEN"
    val VALID_WITNESS_MANUAL_INPUT = "{\"main_public_key\": \"$POP_TOKEN\"}"
    const val INVALID_KEY_FORMAT_INPUT = "{\"pop_token\": \"invalid_key\"}"
    private val laoSubject = BehaviorSubject.createDefault(LaoView(LAO))
  }
}
