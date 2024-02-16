package com.github.dedis.popstellar.ui.lao.event.meeting

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.dedis.popstellar.model.objects.Lao
import com.github.dedis.popstellar.model.objects.Meeting
import com.github.dedis.popstellar.model.objects.Meeting.Companion.generateCreateMeetingId
import com.github.dedis.popstellar.model.objects.view.LaoView
import com.github.dedis.popstellar.repository.LAORepository
import com.github.dedis.popstellar.repository.MeetingRepository
import com.github.dedis.popstellar.repository.remote.GlobalNetworkManager
import com.github.dedis.popstellar.testutils.Base64DataUtils
import com.github.dedis.popstellar.testutils.BundleBuilder
import com.github.dedis.popstellar.testutils.MessageSenderHelper
import com.github.dedis.popstellar.testutils.MockitoKotlinHelpers
import com.github.dedis.popstellar.testutils.fragment.ActivityFragmentScenarioRule
import com.github.dedis.popstellar.testutils.pages.lao.LaoActivityPageObject
import com.github.dedis.popstellar.testutils.pages.lao.MeetingFragmentPageObject
import com.github.dedis.popstellar.ui.lao.LaoActivity
import com.github.dedis.popstellar.ui.lao.event.meeting.MeetingFragment.Companion.newInstance
import com.github.dedis.popstellar.utility.Constants
import com.github.dedis.popstellar.utility.error.UnknownLaoException
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
class MeetingFragmentTest {
  @Inject lateinit var meetingRepository: MeetingRepository

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
      @Throws(UnknownLaoException::class)
      override fun before() {
        hiltRule.inject()

        Mockito.`when`(laoRepo.getLaoObservable(ArgumentMatchers.anyString()))
          .thenReturn(laoSubject)
        Mockito.`when`(laoRepo.getLaoView(MockitoKotlinHelpers.any())).thenAnswer { LaoView(LAO) }

        meetingRepository.updateMeeting(LAO_ID, MEETING)
        Mockito.`when`(keyManager.mainPublicKey).thenReturn(SENDER)
        messageSenderHelper.setupMock()
      }
    }

  @JvmField
  @Rule(order = 3)
  var activityScenarioRule: ActivityFragmentScenarioRule<LaoActivity, MeetingFragment> =
    ActivityFragmentScenarioRule.launchIn(
      LaoActivity::class.java,
      BundleBuilder().putString(LaoActivityPageObject.laoIdExtra(), LAO_ID).build(),
      LaoActivityPageObject.containerId(),
      MeetingFragment::class.java,
      { newInstance(MEETING.id) },
      BundleBuilder().putString(Constants.MEETING_ID, MEETING.id).build()
    )

  @Test
  fun rollCallTitleMatches() {
    MeetingFragmentPageObject.meetingTitle()
      .check(ViewAssertions.matches(ViewMatchers.withText(MEETING_TITLE)))
  }

  @Test
  fun datesDisplayedMatches() {
    val startTime = Date(MEETING.startTimestampInMillis)
    val endTime = Date(MEETING.endTimestampInMillis)
    val startTimeText = DATE_FORMAT.format(startTime)
    val endTimeText = DATE_FORMAT.format(endTime)

    MeetingFragmentPageObject.meetingStartTime()
      .check(ViewAssertions.matches(ViewMatchers.withText(startTimeText)))
    MeetingFragmentPageObject.meetingEndTime()
      .check(ViewAssertions.matches(ViewMatchers.withText(endTimeText)))
  }

  @Test
  fun statusCreatedTest() {
    val timeSec = System.currentTimeMillis() / 1000
    val meeting =
      Meeting(
        MEETING_ID,
        MEETING_TITLE,
        timeSec,
        timeSec + 10,
        timeSec + 20,
        LOCATION,
        timeSec,
        MODIFICATION_ID,
        MODIFICATION_SIGNATURES
      )
    meetingRepository.updateMeeting(LAO_ID, meeting)
    MeetingFragmentPageObject.meetingStatusText()
      .check(ViewAssertions.matches(ViewMatchers.withText("Not yet opened")))
  }

  @Test
  fun statusOpenedTest() {
    val timeSec = System.currentTimeMillis() / 1000
    val meeting =
      Meeting(
        MEETING_ID,
        MEETING_TITLE,
        timeSec,
        timeSec,
        timeSec + 10,
        LOCATION,
        timeSec,
        MODIFICATION_ID,
        MODIFICATION_SIGNATURES
      )
    meetingRepository.updateMeeting(LAO_ID, meeting)
    MeetingFragmentPageObject.meetingStatusText()
      .check(ViewAssertions.matches(ViewMatchers.withText("Open")))
  }

  @Test
  fun statusClosedTest() {
    val timeSec = System.currentTimeMillis() / 1000 - 10
    val meeting =
      Meeting(
        MEETING_ID,
        MEETING_TITLE,
        timeSec,
        timeSec,
        timeSec + 5,
        LOCATION,
        timeSec,
        MODIFICATION_ID,
        MODIFICATION_SIGNATURES
      )
    meetingRepository.updateMeeting(LAO_ID, meeting)
    MeetingFragmentPageObject.meetingStatusText()
      .check(ViewAssertions.matches(ViewMatchers.withText("Closed")))
  }

  @Test
  fun locationVisibilityTest() {
    // Here the location text should be visible as non empty
    MeetingFragmentPageObject.meetingLocationText()
      .check(
        ViewAssertions.matches(
          ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)
        )
      )
  }

  companion object {
    private const val LAO_NAME = "lao"
    private val SENDER_KEY = Base64DataUtils.generateKeyPair()
    private val SENDER = SENDER_KEY.publicKey
    private val LAO = Lao(LAO_NAME, SENDER, 10223421)
    private val LAO_ID = LAO.id
    private const val MEETING_TITLE = "Title"
    private const val CREATION: Long = 10323411
    private const val START = CREATION + 10
    private const val END = START + 10
    private const val LOCATION = "EPFL"
    private const val MODIFICATION_ID = "MOD_ID"
    private val MODIFICATION_SIGNATURES: List<String> = ArrayList()
    private val laoSubject = BehaviorSubject.createDefault(LaoView(LAO))
    private val DATE_FORMAT: DateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm z", Locale.ENGLISH)
    private val MEETING_ID = generateCreateMeetingId(LAO_ID, CREATION, MEETING_TITLE)
    private val MEETING =
      Meeting(
        MEETING_ID,
        MEETING_TITLE,
        CREATION,
        START,
        END,
        LOCATION,
        CREATION,
        MODIFICATION_ID,
        MODIFICATION_SIGNATURES
      )
  }
}
