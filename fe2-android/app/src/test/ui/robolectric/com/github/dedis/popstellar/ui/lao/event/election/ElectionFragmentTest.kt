package com.github.dedis.popstellar.ui.lao.event.election

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.github.dedis.popstellar.model.network.method.message.data.election.ElectionQuestion
import com.github.dedis.popstellar.model.network.method.message.data.election.ElectionQuestion.Question
import com.github.dedis.popstellar.model.network.method.message.data.election.ElectionVersion
import com.github.dedis.popstellar.model.objects.Election.Companion.generateElectionSetupId
import com.github.dedis.popstellar.model.objects.Election.ElectionBuilder
import com.github.dedis.popstellar.model.objects.Lao
import com.github.dedis.popstellar.model.objects.RollCall
import com.github.dedis.popstellar.model.objects.event.EventState
import com.github.dedis.popstellar.model.objects.view.LaoView
import com.github.dedis.popstellar.repository.ElectionRepository
import com.github.dedis.popstellar.repository.LAORepository
import com.github.dedis.popstellar.repository.RollCallRepository
import com.github.dedis.popstellar.repository.remote.GlobalNetworkManager
import com.github.dedis.popstellar.testutils.Base64DataUtils
import com.github.dedis.popstellar.testutils.BundleBuilder
import com.github.dedis.popstellar.testutils.MessageSenderHelper
import com.github.dedis.popstellar.testutils.MockitoKotlinHelpers
import com.github.dedis.popstellar.testutils.UITestUtils.dialogNegativeButton
import com.github.dedis.popstellar.testutils.UITestUtils.dialogPositiveButton
import com.github.dedis.popstellar.testutils.fragment.ActivityFragmentScenarioRule
import com.github.dedis.popstellar.testutils.pages.lao.LaoActivityPageObject
import com.github.dedis.popstellar.testutils.pages.lao.event.election.ElectionFragmentPageObject
import com.github.dedis.popstellar.ui.lao.LaoActivity
import com.github.dedis.popstellar.ui.lao.event.election.fragments.ElectionFragment
import com.github.dedis.popstellar.utility.error.UnknownLaoException
import com.github.dedis.popstellar.utility.error.keys.KeyException
import com.github.dedis.popstellar.utility.security.KeyManager
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.reactivex.subjects.BehaviorSubject
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import org.hamcrest.Matchers
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
class ElectionFragmentTest {
  private val ROLL_CALL =
    RollCall(
      LAO.id,
      LAO.id,
      TITLE,
      CREATION,
      START,
      END,
      EventState.CLOSED,
      LinkedHashSet(),
      LOCATION,
      ROLL_CALL_DESC
    )

  @Inject lateinit var electionRepository: ElectionRepository

  @Inject lateinit var rollCallRepo: RollCallRepository

  @BindValue @Mock lateinit var repository: LAORepository

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

        electionRepository.updateElection(ELECTION)
        Mockito.`when`(repository.getLaoObservable(ArgumentMatchers.anyString()))
          .thenReturn(laoSubject)
        Mockito.`when`(repository.getLaoView(MockitoKotlinHelpers.any())).thenAnswer {
          LaoView(LAO)
        }
        rollCallRepo.updateRollCall(LAO_ID, ROLL_CALL)

        Mockito.`when`(keyManager.mainPublicKey).thenReturn(SENDER)
        Mockito.`when`(networkManager.messageSender).thenReturn(messageSenderHelper.mockedSender)
        messageSenderHelper.setupMock()
      }
    }

  @JvmField
  @Rule(order = 3)
  var activityScenarioRule: ActivityFragmentScenarioRule<LaoActivity, ElectionFragment> =
    ActivityFragmentScenarioRule.launchIn(
      LaoActivity::class.java,
      BundleBuilder().putString(LaoActivityPageObject.laoIdExtra(), LAO_ID).build(),
      LaoActivityPageObject.containerId(),
      ElectionFragment::class.java
    ) {
      ElectionFragment.newInstance(ELECTION.id)
    }

  @Test
  fun electionTitleMatches() {
    ElectionFragmentPageObject.electionFragmentTitle()
      .check(ViewAssertions.matches(ViewMatchers.withText(TITLE)))
  }

  @Test
  fun statusCreatedTests() {
    ElectionFragmentPageObject.electionFragmentStatus()
      .check(ViewAssertions.matches(ViewMatchers.withText("Not yet opened")))
  }

  @Test
  fun datesDisplayedMatches() {
    val startTime = Date(ELECTION.startTimestampInMillis)
    val endTime = Date(ELECTION.endTimestampInMillis)
    val startTimeText = DATE_FORMAT.format(startTime)
    val endTimeText = DATE_FORMAT.format(endTime)

    ElectionFragmentPageObject.electionFragmentStartTime()
      .check(ViewAssertions.matches(ViewMatchers.withText(startTimeText)))
    ElectionFragmentPageObject.electionFragmentEndTime()
      .check(ViewAssertions.matches(ViewMatchers.withText(endTimeText)))
  }

  @Test
  fun managementButtonIsDisplayed() {
    ElectionFragmentPageObject.electionManagementButton()
      .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
  }

  @Test
  fun managementButtonOpensElectionWhenCreated() {
    ElectionFragmentPageObject.electionManagementButton()
      .check(ViewAssertions.matches(ViewMatchers.withText("OPEN")))
    ElectionFragmentPageObject.electionManagementButton().perform(ViewActions.click())
    dialogPositiveButton().performClick()
    // Wait for the main thread to finish executing the calls made above
    // before asserting their effect
    InstrumentationRegistry.getInstrumentation().waitForIdleSync()

    Mockito.verify(messageSenderHelper.mockedSender)
      .publish(
        MockitoKotlinHelpers.any(),
        MockitoKotlinHelpers.eq(ELECTION.channel),
        MockitoKotlinHelpers.any()
      )
    messageSenderHelper.assertSubscriptions()
  }

  @Test
  fun actionButtonCreatedTest() {
    ElectionFragmentPageObject.electionActionButton()
      .check(ViewAssertions.matches(ViewMatchers.withText("VOTE")))
    ElectionFragmentPageObject.electionActionButton()
      .check(ViewAssertions.matches(Matchers.not(ViewMatchers.isEnabled())))
  }

  @Test
  fun statusOpenTest() {
    openElection()
    ElectionFragmentPageObject.electionFragmentStatus()
      .check(ViewAssertions.matches(ViewMatchers.withText("Open")))
  }

  @Test
  fun managementButtonEndElectionWhenOpened() {
    openElection()
    ElectionFragmentPageObject.electionManagementButton()
      .check(ViewAssertions.matches(ViewMatchers.withText("CLOSE")))
    ElectionFragmentPageObject.electionManagementButton().perform(ViewActions.click())
    dialogPositiveButton().performClick()
    // Wait for the main thread to finish executing the calls made above
    // before asserting their effect
    InstrumentationRegistry.getInstrumentation().waitForIdleSync()

    Mockito.verify(messageSenderHelper.mockedSender)
      .publish(
        MockitoKotlinHelpers.any(),
        MockitoKotlinHelpers.eq(ELECTION.channel),
        MockitoKotlinHelpers.any()
      )
    messageSenderHelper.assertSubscriptions()
  }

  @Test
  @Throws(KeyException::class)
  fun actionButtonNotEnabledOpenTest() {
    Mockito.doAnswer {
        throw object : KeyException("") {
          override val userMessage: Int
            get() = 0

          override val userMessageArguments: Array<Any?>
            get() = emptyArray()
        }
      }
      .`when`(keyManager)
      .getValidPoPToken(MockitoKotlinHelpers.any(), MockitoKotlinHelpers.any())

    activityScenarioRule.scenario.recreate()

    openElection()
    ElectionFragmentPageObject.electionActionButton()
      .check(ViewAssertions.matches(ViewMatchers.withText("VOTE")))
    ElectionFragmentPageObject.electionActionButton()
      .check(ViewAssertions.matches(ViewMatchers.isNotEnabled()))
  }

  @Test
  fun actionButtonEnabledOpenTest() {
    openElection()
    ElectionFragmentPageObject.electionActionButton()
      .check(ViewAssertions.matches(ViewMatchers.withText("VOTE")))
    ElectionFragmentPageObject.electionActionButton()
      .check(ViewAssertions.matches(ViewMatchers.isEnabled()))
  }

  @Test
  fun statusClosedTest() {
    closeElection()
    ElectionFragmentPageObject.electionFragmentStatus()
      .check(ViewAssertions.matches(ViewMatchers.withText("Waiting for results")))
  }

  @Test
  fun managementButtonClosedTest() {
    closeElection()
    ElectionFragmentPageObject.electionManagementButton()
      .check(ViewAssertions.matches(Matchers.not(ViewMatchers.isDisplayed())))
  }

  @Test
  fun actionButtonClosedTest() {
    closeElection()
    ElectionFragmentPageObject.electionActionButton()
      .check(ViewAssertions.matches(ViewMatchers.withText("Results")))
    ElectionFragmentPageObject.electionActionButton()
      .check(ViewAssertions.matches(Matchers.not(ViewMatchers.isEnabled())))
  }

  @Test
  fun statusResultsTest() {
    receiveResults()
    ElectionFragmentPageObject.electionFragmentStatus()
      .check(ViewAssertions.matches(ViewMatchers.withText("Finished")))
  }

  @Test
  fun managementButtonResultsTest() {
    receiveResults()
    ElectionFragmentPageObject.electionManagementButton()
      .check(ViewAssertions.matches(Matchers.not(ViewMatchers.isDisplayed())))
  }

  @Test
  fun actionButtonResultsTest() {
    receiveResults()
    ElectionFragmentPageObject.electionActionButton()
      .check(ViewAssertions.matches(ViewMatchers.withText("Results")))
    ElectionFragmentPageObject.electionActionButton()
      .check(ViewAssertions.matches(ViewMatchers.isEnabled()))
  }

  @Test
  fun openButtonDisplaysDialogOnclick() {
    ElectionFragmentPageObject.electionManagementButton().perform(ViewActions.click())
    ViewMatchers.assertThat(
      dialogPositiveButton(),
      Matchers.allOf(ViewMatchers.withText("Yes"), ViewMatchers.isDisplayed())
    )
    ViewMatchers.assertThat(
      dialogNegativeButton(),
      Matchers.allOf(ViewMatchers.withText("No"), ViewMatchers.isDisplayed())
    )
  }

  @Test
  fun closeButtonDisplaysDialogOnclick() {
    openElection()
    ElectionFragmentPageObject.electionManagementButton().perform(ViewActions.click())
    ViewMatchers.assertThat(
      dialogPositiveButton(),
      Matchers.allOf(ViewMatchers.withText("Yes"), ViewMatchers.isDisplayed())
    )
    ViewMatchers.assertThat(
      dialogNegativeButton(),
      Matchers.allOf(ViewMatchers.withText("No"), ViewMatchers.isDisplayed())
    )
  }

  private fun openElection() {
    electionRepository.updateElection(ELECTION.builder().setState(EventState.OPENED).build())
  }

  private fun closeElection() {
    electionRepository.updateElection(ELECTION.builder().setState(EventState.CLOSED).build())
  }

  private fun receiveResults() {
    electionRepository.updateElection(ELECTION.builder().setState(EventState.RESULTS_READY).build())
  }

  companion object {
    private const val LAO_NAME = "lao"
    private val SENDER_KEY = Base64DataUtils.generateKeyPair()
    private val SENDER = SENDER_KEY.publicKey
    private val CREATION = Instant.now().epochSecond - 1000
    private val LAO = Lao(LAO_NAME, SENDER, CREATION)
    private val LAO_ID = LAO.id
    private const val TITLE = "Election name"
    private val START = CREATION + 10
    private val END = CREATION + 20
    private const val ROLL_CALL_DESC = ""
    private const val LOCATION = "EPFL"
    private val ELECTION_ID = generateElectionSetupId(LAO_ID, CREATION, TITLE)
    private val ELECTION_QUESTION_1 =
      ElectionQuestion(
        ELECTION_ID,
        Question("ElectionQuestion", "Plurality", mutableListOf("1", "2"), false)
      )
    private val ELECTION_QUESTION_2 =
      ElectionQuestion(
        ELECTION_ID,
        Question("ElectionQuestion2", "Plurality", mutableListOf("a", "b"), false)
      )
    private val ELECTION =
      ElectionBuilder(LAO_ID, CREATION, TITLE)
        .setElectionVersion(ElectionVersion.OPEN_BALLOT)
        .setElectionQuestions(listOf(ELECTION_QUESTION_1, ELECTION_QUESTION_2))
        .setStart(START)
        .setEnd(END)
        .setState(EventState.CREATED)
        .build()
    private val laoSubject = BehaviorSubject.createDefault(LaoView(LAO))
    private val DATE_FORMAT: DateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm z", Locale.ENGLISH)
  }
}
