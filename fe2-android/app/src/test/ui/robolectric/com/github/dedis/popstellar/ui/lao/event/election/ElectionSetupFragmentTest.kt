package com.github.dedis.popstellar.ui.lao.event.election

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.github.dedis.popstellar.model.network.method.message.data.election.ElectionSetup
import com.github.dedis.popstellar.model.network.method.message.data.election.ElectionVersion
import com.github.dedis.popstellar.model.objects.Lao
import com.github.dedis.popstellar.model.objects.security.PublicKey
import com.github.dedis.popstellar.model.objects.view.LaoView
import com.github.dedis.popstellar.repository.LAORepository
import com.github.dedis.popstellar.repository.remote.GlobalNetworkManager
import com.github.dedis.popstellar.repository.remote.MessageSender
import com.github.dedis.popstellar.testutils.BundleBuilder
import com.github.dedis.popstellar.testutils.MockitoKotlinHelpers
import com.github.dedis.popstellar.testutils.UITestUtils.dialogPositiveButton
import com.github.dedis.popstellar.testutils.UITestUtils.getLastDialog
import com.github.dedis.popstellar.testutils.fragment.ActivityFragmentScenarioRule
import com.github.dedis.popstellar.testutils.pages.lao.LaoActivityPageObject
import com.github.dedis.popstellar.testutils.pages.lao.event.EventCreationPageObject
import com.github.dedis.popstellar.testutils.pages.lao.event.election.ElectionSetupPageObject
import com.github.dedis.popstellar.ui.lao.LaoActivity
import com.github.dedis.popstellar.ui.lao.event.election.adapters.ElectionSetupViewPagerAdapter.Companion.hasDuplicate
import com.github.dedis.popstellar.ui.lao.event.election.fragments.ElectionSetupFragment
import com.github.dedis.popstellar.utility.error.UnknownLaoException
import com.github.dedis.popstellar.utility.handler.MessageHandler
import com.github.dedis.popstellar.utility.security.KeyManager
import com.google.gson.Gson
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.reactivex.Completable
import io.reactivex.subjects.BehaviorSubject
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.Assert
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoTestRule
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

@LargeTest
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class ElectionSetupFragmentTest {
  @Inject
  lateinit var keyManager: KeyManager

  @Inject
  lateinit var messageHandler: MessageHandler

  @Inject
  lateinit var gson: Gson

  @BindValue
  @Mock
  lateinit var repository: LAORepository

  @BindValue
  @Mock
  lateinit var globalNetworkManager: GlobalNetworkManager

  @Mock
  lateinit var messageSender: MessageSender

  @JvmField
  @Rule(order = 0)
  val mockitoRule: MockitoTestRule = MockitoJUnit.testRule(this)

  @JvmField
  @Rule(order = 1)
  val hiltRule = HiltAndroidRule(this)

  @JvmField
  @Rule(order = 2)
  val setupRule: ExternalResource =
    object : ExternalResource() {
      @Throws(UnknownLaoException::class)
      override fun before() {
        // Injection with hilt
        hiltRule.inject()

        Mockito.`when`(repository.getLaoObservable(MockitoKotlinHelpers.any()))
          .thenReturn(BehaviorSubject.createDefault(LaoView(LAO)))
        Mockito.`when`(repository.getLaoView(MockitoKotlinHelpers.any())).thenAnswer {
          LaoView(LAO)
        }
        Mockito.`when`(globalNetworkManager.messageSender).thenReturn(messageSender)
        Mockito.`when`(
            messageSender.publish(
              MockitoKotlinHelpers.any(),
              MockitoKotlinHelpers.any(),
              MockitoKotlinHelpers.any()
            )
          )
          .then { Completable.complete() }
        Mockito.`when`(
            messageSender.publish(MockitoKotlinHelpers.any(), MockitoKotlinHelpers.any())
          )
          .then { Completable.complete() }
        Mockito.`when`(messageSender.subscribe(MockitoKotlinHelpers.any())).then {
          Completable.complete()
        }
      }
    }

  @JvmField
  @Rule(order = 3)
  val fragmentRule: ActivityFragmentScenarioRule<LaoActivity, ElectionSetupFragment> =
    ActivityFragmentScenarioRule.launchIn(
      LaoActivity::class.java,
      BundleBuilder().putString(LaoActivityPageObject.laoIdExtra(), LAO.id).build(),
      LaoActivityPageObject.containerId(),
      ElectionSetupFragment::class.java
    ) {
      ElectionSetupFragment()
    }

  private val interceptedElectionSetupMsg: ElectionSetup
    get() {
      val captor = MockitoKotlinHelpers.argumentCaptor<ElectionSetup>()
      Mockito.verify(messageSender, Mockito.atLeast(1))
        .publish(
          MockitoKotlinHelpers.any(),
          MockitoKotlinHelpers.any(),
          MockitoKotlinHelpers.capture(captor)
        )
      return captor.value
    }

  @Test
  fun canLaunchDatePickerFragmentFromStartDateButton() {
    EventCreationPageObject.startDateView().perform(ViewActions.click())

    MatcherAssert.assertThat(
      getLastDialog(DatePickerDialog::class.java).datePicker,
      ViewMatchers.isDisplayed()
    )
  }

  @Test
  fun canLaunchDatePickerFragmentFromEndDateButton() {
    EventCreationPageObject.endDateView().perform(ViewActions.click())

    MatcherAssert.assertThat(
      getLastDialog(DatePickerDialog::class.java).datePicker,
      ViewMatchers.isDisplayed()
    )
  }

  @Test
  fun canChooseRandomDate() {
    EventCreationPageObject.startDateView().perform(ViewActions.click())
    getLastDialog(DatePickerDialog::class.java).updateDate(YEAR, MONTH_OF_YEAR, DAY_OF_MONTH)
    dialogPositiveButton().performClick()

    EventCreationPageObject.startDateView()
      .check(ViewAssertions.matches(ViewMatchers.withText(DATE)))
  }

  @Test
  fun datePickerChoosesTodayByDefault() {
    val currentCalendar = Calendar.getInstance()
    val date = DATE_FORMAT.format(currentCalendar.time)
    EventCreationPageObject.startDateView().perform(ViewActions.click())
    dialogPositiveButton().performClick()

    EventCreationPageObject.startDateView()
      .check(ViewAssertions.matches(ViewMatchers.withText(date)))
  }

  @Test
  fun startDateAndEndDateCanBothBeSameDay() {
    EventCreationPageObject.startDateView().perform(ViewActions.click())

    getLastDialog(DatePickerDialog::class.java).updateDate(YEAR, MONTH_OF_YEAR, DAY_OF_MONTH)
    dialogPositiveButton().performClick()

    EventCreationPageObject.startDateView()
      .check(ViewAssertions.matches(ViewMatchers.withText(DATE)))
    EventCreationPageObject.endDateView().perform(ViewActions.click())

    getLastDialog(DatePickerDialog::class.java).updateDate(YEAR, MONTH_OF_YEAR, DAY_OF_MONTH)
    dialogPositiveButton().performClick()

    EventCreationPageObject.endDateView().check(ViewAssertions.matches(ViewMatchers.withText(DATE)))
  }

  @Test
  fun canChooseRandomStartTimeWhenNoDate() {
    EventCreationPageObject.startTimeView().perform(ViewActions.click())
    getLastDialog(TimePickerDialog::class.java).updateTime(HOURS, MINUTES)
    dialogPositiveButton().performClick()

    EventCreationPageObject.startTimeView()
      .check(ViewAssertions.matches(ViewMatchers.withText(TIME)))
  }

  @Test
  fun canChooseRandomEndTimeWhenNoDate() {
    EventCreationPageObject.endTimeView().perform(ViewActions.click())
    getLastDialog(TimePickerDialog::class.java).updateTime(HOURS, MINUTES)
    dialogPositiveButton().performClick()

    EventCreationPageObject.endTimeView().check(ViewAssertions.matches(ViewMatchers.withText(TIME)))
  }

  @Test
  fun canChooseRandomStartTimeWhenStartDateFilled() {
    EventCreationPageObject.startDateView().perform(ViewActions.click())
    getLastDialog(DatePickerDialog::class.java)
      .datePicker
      .updateDate(YEAR, MONTH_OF_YEAR, DAY_OF_MONTH)
    dialogPositiveButton().performClick()

    EventCreationPageObject.startDateView()
      .check(ViewAssertions.matches(ViewMatchers.withText(DATE)))

    EventCreationPageObject.startTimeView().perform(ViewActions.click())
    getLastDialog(TimePickerDialog::class.java).updateTime(HOURS, MINUTES)
    dialogPositiveButton().performClick()

    EventCreationPageObject.startTimeView()
      .check(ViewAssertions.matches(ViewMatchers.withText(TIME)))
  }

  @Test
  fun canChooseRandomStartTimeWhenEndDateFilled() {
    EventCreationPageObject.endDateView().perform(ViewActions.click())
    getLastDialog(DatePickerDialog::class.java).updateDate(YEAR, MONTH_OF_YEAR, DAY_OF_MONTH)
    dialogPositiveButton().performClick()

    EventCreationPageObject.endDateView().check(ViewAssertions.matches(ViewMatchers.withText(DATE)))

    EventCreationPageObject.startTimeView().perform(ViewActions.click())
    getLastDialog(TimePickerDialog::class.java).updateTime(HOURS, MINUTES)
    dialogPositiveButton().performClick()

    EventCreationPageObject.startTimeView()
      .check(ViewAssertions.matches(ViewMatchers.withText(TIME)))
  }

  @Test
  fun canChooseRandomEndTimeWhenStartDateFilled() {
    EventCreationPageObject.startDateView().perform(ViewActions.click())
    getLastDialog(DatePickerDialog::class.java).updateDate(YEAR, MONTH_OF_YEAR, DAY_OF_MONTH)
    dialogPositiveButton().performClick()

    EventCreationPageObject.startDateView()
      .check(ViewAssertions.matches(ViewMatchers.withText(DATE)))

    EventCreationPageObject.endTimeView().perform(ViewActions.click())
    getLastDialog(TimePickerDialog::class.java).updateTime(HOURS, MINUTES)
    dialogPositiveButton().performClick()

    EventCreationPageObject.endTimeView().check(ViewAssertions.matches(ViewMatchers.withText(TIME)))
  }

  @Test
  fun canChooseRandomEndTimeWhenEndDateFilled() {
    EventCreationPageObject.endDateView().perform(ViewActions.click())
    getLastDialog(DatePickerDialog::class.java).updateDate(YEAR, MONTH_OF_YEAR, DAY_OF_MONTH)
    dialogPositiveButton().performClick()

    EventCreationPageObject.endDateView().check(ViewAssertions.matches(ViewMatchers.withText(DATE)))

    EventCreationPageObject.endTimeView().perform(ViewActions.click())
    getLastDialog(TimePickerDialog::class.java).updateTime(HOURS, MINUTES)
    dialogPositiveButton().performClick()

    EventCreationPageObject.endTimeView().check(ViewAssertions.matches(ViewMatchers.withText(TIME)))
  }

  @Test
  fun canChooseStartTimeBeforeEndTimeWhenSameDayEvent() {
    EventCreationPageObject.endTimeView().perform(ViewActions.click())
    getLastDialog(TimePickerDialog::class.java).updateTime(HOURS, MINUTES)
    dialogPositiveButton().performClick()

    EventCreationPageObject.endTimeView().check(ViewAssertions.matches(ViewMatchers.withText(TIME)))

    EventCreationPageObject.startTimeView().perform(ViewActions.click())
    getLastDialog(TimePickerDialog::class.java).updateTime(HOURS - 1, MINUTES)
    dialogPositiveButton().performClick()

    EventCreationPageObject.startTimeView()
      .check(ViewAssertions.matches(ViewMatchers.withText("" + (HOURS - 1) + ":" + MINUTES)))
  }

  @Test
  fun canChooseEndTimeBeforeStartTimeWhenSameDayEvent() {
    EventCreationPageObject.startDateView().perform(ViewActions.click())
    getLastDialog(DatePickerDialog::class.java).updateDate(YEAR, MONTH_OF_YEAR, DAY_OF_MONTH)
    dialogPositiveButton().performClick()

    EventCreationPageObject.startDateView()
      .check(ViewAssertions.matches(ViewMatchers.withText(DATE)))

    EventCreationPageObject.endDateView().perform(ViewActions.click())
    getLastDialog(DatePickerDialog::class.java).updateDate(YEAR, MONTH_OF_YEAR, DAY_OF_MONTH)
    dialogPositiveButton().performClick()

    EventCreationPageObject.endDateView().check(ViewAssertions.matches(ViewMatchers.withText(DATE)))

    EventCreationPageObject.startTimeView().perform(ViewActions.click())
    getLastDialog(TimePickerDialog::class.java).updateTime(HOURS, MINUTES)
    dialogPositiveButton().performClick()

    EventCreationPageObject.startTimeView()
      .check(ViewAssertions.matches(ViewMatchers.withText(TIME)))

    EventCreationPageObject.endTimeView().perform(ViewActions.click())
    getLastDialog(TimePickerDialog::class.java).updateTime(HOURS + 1, MINUTES)
    dialogPositiveButton().performClick()

    EventCreationPageObject.endTimeView()
      .check(
        ViewAssertions.matches(
          ViewMatchers.withText(String.format(TIME_FORMAT, HOURS + 1, MINUTES))
        )
      )
  }

  @Test
  fun cannotChooseStartTimeTooFarInPast() {
    val today = Calendar.getInstance()
    today.add(Calendar.MINUTE, -10)
    val year = today[Calendar.YEAR]
    val monthOfYear = today[Calendar.MONTH]
    val dayOfMonth = today[Calendar.DAY_OF_MONTH]
    val hourOfDay = today[Calendar.HOUR_OF_DAY]
    val minutes = today[Calendar.MINUTE]

    EventCreationPageObject.startDateView().perform(ViewActions.click())
    getLastDialog(DatePickerDialog::class.java).updateDate(year, monthOfYear, dayOfMonth)
    dialogPositiveButton().performClick()

    EventCreationPageObject.startTimeView().perform(ViewActions.click())
    getLastDialog(TimePickerDialog::class.java).updateTime(hourOfDay, minutes)
    dialogPositiveButton().performClick()

    EventCreationPageObject.startTimeView().check(ViewAssertions.matches(ViewMatchers.withText("")))
  }

  @Test
  fun cannotChooseStartTimeInPastDay() {
    val today = Calendar.getInstance()
    today.add(Calendar.MINUTE, -1430)
    val year = today[Calendar.YEAR]
    val monthOfYear = today[Calendar.MONTH]
    val dayOfMonth = today[Calendar.DAY_OF_MONTH]
    val hourOfDay = today[Calendar.HOUR_OF_DAY]
    val minutes = today[Calendar.MINUTE]

    EventCreationPageObject.startDateView().perform(ViewActions.click())
    getLastDialog(DatePickerDialog::class.java).updateDate(year, monthOfYear, dayOfMonth)
    dialogPositiveButton().performClick()

    EventCreationPageObject.startTimeView().perform(ViewActions.click())
    getLastDialog(TimePickerDialog::class.java).updateTime(hourOfDay, minutes)
    dialogPositiveButton().performClick()

    EventCreationPageObject.startTimeView().check(ViewAssertions.matches(ViewMatchers.withText("")))
  }

  @Test
  @Ignore("Not implemented")
  fun choosingStartDateInvalidateAStartTimeInPast() {
    val today = Calendar.getInstance()
    today.add(Calendar.MINUTE, -10)
    val year = today[Calendar.YEAR]
    val monthOfYear = today[Calendar.MONTH] + 1
    val dayOfMonth = today[Calendar.DAY_OF_MONTH]
    val hourOfDay = today[Calendar.HOUR_OF_DAY]
    val minutes = today[Calendar.MINUTE]

    EventCreationPageObject.startTimeView().perform(ViewActions.click())
    getLastDialog(TimePickerDialog::class.java).updateTime(hourOfDay, minutes)
    dialogPositiveButton().performClick()

    EventCreationPageObject.startTimeView()
      .check(
        ViewAssertions.matches(
          ViewMatchers.withText(String.format(TIME_FORMAT, hourOfDay, minutes))
        )
      )

    EventCreationPageObject.startDateView().perform(ViewActions.click())
    getLastDialog(DatePickerDialog::class.java).updateDate(year, monthOfYear, dayOfMonth)
    dialogPositiveButton().performClick()

    EventCreationPageObject.startTimeView().check(ViewAssertions.matches(ViewMatchers.withText("")))
  }

  private fun pickValidDateAndTime() {
    EventCreationPageObject.startDateView().perform(ViewActions.click())
    getLastDialog(DatePickerDialog::class.java).updateDate(YEAR, MONTH_OF_YEAR, DAY_OF_MONTH)
    dialogPositiveButton().performClick()
    EventCreationPageObject.endDateView().perform(ViewActions.click())
    getLastDialog(DatePickerDialog::class.java).updateDate(YEAR, MONTH_OF_YEAR, DAY_OF_MONTH)
    dialogPositiveButton().performClick()
    EventCreationPageObject.startTimeView().perform(ViewActions.click())
    getLastDialog(TimePickerDialog::class.java).updateTime(HOURS, MINUTES)
    dialogPositiveButton().performClick()
    EventCreationPageObject.endTimeView().perform(ViewActions.click())
    getLastDialog(TimePickerDialog::class.java).updateTime(HOURS + 1, MINUTES)
    dialogPositiveButton().performClick()
  }

  @Test
  fun multiplePluralityQuestionsTest() {
    ElectionSetupPageObject.electionName()
      .perform(
        ViewActions.click(),
        ViewActions.typeText(ELECTION_NAME),
        ViewActions.closeSoftKeyboard()
      )
    pickValidDateAndTime()

    // Add Question 1, with 3 ballots options, no write in
    ElectionSetupPageObject.questionText()
      .perform(
        ViewActions.click(),
        ViewActions.typeText("Question 1"),
        ViewActions.closeSoftKeyboard()
      )
    ElectionSetupPageObject.addBallot().perform(ViewActions.click())
    for (i in 0..2) {
      ElectionSetupPageObject.ballotOptionAtPosition(i)
        .perform(
          ViewActions.click(),
          ViewActions.typeText("answer 1.$i"),
          ViewActions.closeSoftKeyboard()
        )
    }

    // Add Question 2, with 2 ballots options, with write in
    ElectionSetupPageObject.addQuestion().perform(ViewActions.click())
    ElectionSetupPageObject.questionText()
      .perform(
        ViewActions.click(),
        ViewActions.typeText("Question 2"),
        ViewActions.closeSoftKeyboard()
      )
    ElectionSetupPageObject.writeIn().perform(ViewActions.click())
    for (i in 0..1) {
      ElectionSetupPageObject.ballotOptionAtPosition(i)
        .perform(
          ViewActions.click(),
          ViewActions.typeText("answer 2.$i"),
          ViewActions.closeSoftKeyboard()
        )
    }

    // Submit and intercept the ElectionSetup message
    val minCreation = Instant.now().epochSecond
    ElectionSetupPageObject.submit().perform(ViewActions.scrollTo()).perform(ViewActions.click())
    val electionSetup = interceptedElectionSetupMsg
    val maxCreation = Instant.now().epochSecond

    // Check that the creation time was when we submit the ElectionSetup
    Assert.assertTrue(minCreation <= electionSetup.creation)
    Assert.assertTrue(maxCreation >= electionSetup.creation)

    // Check the start/end time
    val calendar = Calendar.getInstance()
    calendar[YEAR, MONTH_OF_YEAR, DAY_OF_MONTH, HOURS, MINUTES] = 0
    val expectedStartTime = calendar.toInstant().epochSecond
    calendar[YEAR, MONTH_OF_YEAR, DAY_OF_MONTH, HOURS + 1, MINUTES] = 0
    val expectedEndTime = calendar.toInstant().epochSecond
    Assert.assertEquals(expectedStartTime, electionSetup.startTime)
    Assert.assertEquals(expectedEndTime, electionSetup.endTime)
    Assert.assertEquals(ELECTION_NAME, electionSetup.name)
    Assert.assertEquals(LAO.id, electionSetup.lao)
    val questionList = electionSetup.questions
    Assert.assertEquals(2, questionList.size.toLong())
    val question1 = questionList[0]
    val question2 = questionList[1]

    // Check the Questions 1
    Assert.assertEquals("Question 1", question1.question)
    Assert.assertFalse(question1.writeIn)
    val ballotOptions1 = question1.ballotOptions
    Assert.assertEquals(3, ballotOptions1.size.toLong())
    for (i in 0..2) {
      Assert.assertEquals("answer 1.$i", ballotOptions1[i])
    }

    // Check the Questions 2
    Assert.assertEquals("Question 2", question2.question)
    // assertTrue(question2.getWriteIn());
    val ballotOptions2 = question2.ballotOptions
    Assert.assertEquals(2, ballotOptions2.size.toLong())
    for (i in 0..1) {
      Assert.assertEquals("answer 2.$i", ballotOptions2[i])
    }
  }

  @Test
  fun cannotSubmitWithoutElectionNameElectionTest() {
    pickValidDateAndTime()

    // Add Question 1, with 2 ballots options, no write in
    ElectionSetupPageObject.questionText()
      .perform(
        ViewActions.click(),
        ViewActions.typeText("Question 1"),
        ViewActions.closeSoftKeyboard()
      )
    for (i in 0..1) {
      ElectionSetupPageObject.ballotOptionAtPosition(i)
        .perform(
          ViewActions.click(),
          ViewActions.typeText("answer 1.$i"),
          ViewActions.closeSoftKeyboard()
        )
    }
    ElectionSetupPageObject.submit().check(ViewAssertions.matches(ViewMatchers.isNotEnabled()))
    ElectionSetupPageObject.electionName()
      .perform(
        ViewActions.click(),
        ViewActions.typeText(ELECTION_NAME),
        ViewActions.closeSoftKeyboard()
      )
    ElectionSetupPageObject.submit().check(ViewAssertions.matches(ViewMatchers.isEnabled()))
  }

  @Test
  fun canWithoutDateAndTimeTest() {
    // Since now suggested start and end time are provided
    ElectionSetupPageObject.electionName()
      .perform(
        ViewActions.click(),
        ViewActions.typeText(ELECTION_NAME),
        ViewActions.closeSoftKeyboard()
      )
    // Add Question 1, with 2 ballots options, no write in
    ElectionSetupPageObject.questionText()
      .perform(
        ViewActions.click(),
        ViewActions.typeText("Question 1"),
        ViewActions.closeSoftKeyboard()
      )
    for (i in 0..1) {
      ElectionSetupPageObject.ballotOptionAtPosition(i)
        .perform(
          ViewActions.click(),
          ViewActions.typeText("answer 1.$i"),
          ViewActions.closeSoftKeyboard()
        )
    }
    ElectionSetupPageObject.submit().check(ViewAssertions.matches(ViewMatchers.isEnabled()))
  }

  @Test
  fun cannotSubmitWithoutQuestionTest() {
    ElectionSetupPageObject.electionName()
      .perform(
        ViewActions.click(),
        ViewActions.typeText(ELECTION_NAME),
        ViewActions.closeSoftKeyboard()
      )
    pickValidDateAndTime()

    // add 2 ballots options
    for (i in 0..1) {
      ElectionSetupPageObject.ballotOptionAtPosition(i)
        .perform(
          ViewActions.click(),
          ViewActions.typeText("answer 1.$i"),
          ViewActions.closeSoftKeyboard()
        )
    }
    ElectionSetupPageObject.submit().check(ViewAssertions.matches(ViewMatchers.isNotEnabled()))
    ElectionSetupPageObject.questionText()
      .perform(
        ViewActions.click(),
        ViewActions.typeText("Question 1"),
        ViewActions.closeSoftKeyboard()
      )
    ElectionSetupPageObject.submit().check(ViewAssertions.matches(ViewMatchers.isEnabled()))
  }

  @Test
  fun cannotSubmitWithoutAllBallotTest() {
    ElectionSetupPageObject.electionName()
      .perform(
        ViewActions.click(),
        ViewActions.typeText(ELECTION_NAME),
        ViewActions.closeSoftKeyboard()
      )
    pickValidDateAndTime()
    ElectionSetupPageObject.questionText()
      .perform(
        ViewActions.click(),
        ViewActions.typeText("Question 1"),
        ViewActions.closeSoftKeyboard()
      )
    ElectionSetupPageObject.ballotOptionAtPosition(0)
      .perform(
        ViewActions.click(),
        ViewActions.typeText("answer 1.0"),
        ViewActions.closeSoftKeyboard()
      )
    ElectionSetupPageObject.submit().check(ViewAssertions.matches(ViewMatchers.isNotEnabled()))
    ElectionSetupPageObject.ballotOptionAtPosition(1)
      .perform(
        ViewActions.click(),
        ViewActions.typeText("answer 1.1"),
        ViewActions.closeSoftKeyboard()
      )
    ElectionSetupPageObject.submit().check(ViewAssertions.matches(ViewMatchers.isEnabled()))
  }

  @Test
  fun cannotSubmitWithIdenticalBallotTest() {
    ElectionSetupPageObject.electionName()
      .perform(
        ViewActions.click(),
        ViewActions.typeText(ELECTION_NAME),
        ViewActions.closeSoftKeyboard()
      )
    pickValidDateAndTime()

    // Add Question 1, with 2 identical ballots options, no write in
    ElectionSetupPageObject.questionText()
      .perform(
        ViewActions.click(),
        ViewActions.typeText("Question 1"),
        ViewActions.closeSoftKeyboard()
      )
    for (i in 0..1) {
      ElectionSetupPageObject.ballotOptionAtPosition(i)
        .perform(
          ViewActions.click(),
          ViewActions.typeText("answer 1.0"),
          ViewActions.closeSoftKeyboard()
        )
    }
    ElectionSetupPageObject.submit().check(ViewAssertions.matches(ViewMatchers.isNotEnabled()))
  }

  /** Basic test for sanity of spinner content */
  @Test
  fun canChooseVotingVersion() {
    // By default, the spinner is set to OPEN_BALLOT
    ElectionSetupPageObject.versionChoice().perform(ViewActions.click())
    Espresso.onData(Matchers.anything()).atPosition(0).perform(ViewActions.click())
    ElectionSetupPageObject.versionChoice()
      .check(
        ViewAssertions.matches(
          ViewMatchers.withSpinnerText(
            Matchers.containsString(ElectionVersion.OPEN_BALLOT.stringBallotVersion)
          )
        )
      )
    ElectionSetupPageObject.versionChoice().perform(ViewActions.click())
    Espresso.onData(Matchers.anything()).atPosition(1).perform(ViewActions.click())
    ElectionSetupPageObject.versionChoice()
      .check(
        ViewAssertions.matches(
          ViewMatchers.withSpinnerText(
            Matchers.containsString(ElectionVersion.SECRET_BALLOT.stringBallotVersion)
          )
        )
      )
  }

  @Test
  fun cannotSubmitWithIdenticalQuestionName() {
    ElectionSetupPageObject.electionName()
      .perform(
        ViewActions.click(),
        ViewActions.typeText(ELECTION_NAME),
        ViewActions.closeSoftKeyboard()
      )
    pickValidDateAndTime()

    // Add Question 1, with 3 ballots options, no write in
    ElectionSetupPageObject.questionText()
      .perform(
        ViewActions.click(),
        ViewActions.typeText("Question 1"),
        ViewActions.closeSoftKeyboard()
      )
    ElectionSetupPageObject.addBallot().perform(ViewActions.click())
    for (i in 0..2) {
      ElectionSetupPageObject.ballotOptionAtPosition(i)
        .perform(
          ViewActions.click(),
          ViewActions.typeText("answer 1.$i"),
          ViewActions.closeSoftKeyboard()
        )
    }

    // Add Question 2, with 2 ballots options, with write in, with same name as Question 1
    ElectionSetupPageObject.addQuestion().perform(ViewActions.click())
    ElectionSetupPageObject.questionText()
      .perform(
        ViewActions.click(),
        ViewActions.typeText("Question 1"),
        ViewActions.closeSoftKeyboard()
      )
    ElectionSetupPageObject.writeIn().perform(ViewActions.click())
    for (i in 0..1) {
      ElectionSetupPageObject.ballotOptionAtPosition(i)
        .perform(
          ViewActions.click(),
          ViewActions.typeText("answer 2.$i"),
          ViewActions.closeSoftKeyboard()
        )
    }

    // Ensure that the submit button is not enabled
    ElectionSetupPageObject.submit().check(ViewAssertions.matches(ViewMatchers.isNotEnabled()))
  }

  @Test
  fun detectDuplicates() {
    val questionsDuplicated: List<String> =
      ArrayList(mutableListOf("Question 1", "Question 2", "Question 2", "Question 4"))
    val questionsNotDuplicated: List<String> =
      ArrayList(mutableListOf("Question 1", "Question 2", "Question 3", "Question 4"))
    Assert.assertTrue(hasDuplicate(questionsDuplicated))
    Assert.assertFalse(hasDuplicate(questionsNotDuplicated))
  }

  companion object {
    private val DATE_FORMAT: DateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.FRENCH)
    private const val TIME_FORMAT = "%02d:%02d"
    private var YEAR = 0
    private var MONTH_OF_YEAR = 0
    private var DAY_OF_MONTH = 0
    private val DATE: String
    private const val HOURS = 12
    private const val MINUTES = 15
    private val TIME = String.format(TIME_FORMAT, HOURS, MINUTES)
    private const val ELECTION_NAME = "aaaaaaaaaaa"
    private const val LAO_NAME = "bbbbbbbbbbbb"
    private val ORGANIZER = PublicKey("5c2zk_5uCrrNmdUhQAloCDqYJAC2rD4KHo9gGNFVS9c")
    private val LAO = Lao(LAO_NAME, ORGANIZER, 0)

    init {
      // Make sure the date is always in the future
      val today = Calendar.getInstance()
      today.add(Calendar.MONTH, 13)
      YEAR = today[Calendar.YEAR]
      MONTH_OF_YEAR = today[Calendar.MONTH]
      DAY_OF_MONTH = today[Calendar.DAY_OF_MONTH]
      DATE = DATE_FORMAT.format(today.time)
    }
  }
}
