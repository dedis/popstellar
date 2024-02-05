package com.github.dedis.popstellar.ui.lao.event.rollcall

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.github.dedis.popstellar.testutils.UITestUtils.dialogPositiveButton
import com.github.dedis.popstellar.testutils.UITestUtils.getLastDialog
import com.github.dedis.popstellar.testutils.fragment.FragmentScenarioRule
import com.github.dedis.popstellar.testutils.pages.lao.event.EventCreationPageObject
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import org.hamcrest.MatcherAssert
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

@LargeTest
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class RollCallEventCreationFragmentTest {

  private val fragmentRule = FragmentScenarioRule.launch(RollCallCreationFragment::class.java)

  @JvmField
  @Rule
  val chain: RuleChain = RuleChain.outerRule(HiltAndroidRule(this)).around(fragmentRule)

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
    getLastDialog(DatePickerDialog::class.java).updateDate(YEAR, MONTH_OF_YEAR, DAY_OF_MONTH)
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

  companion object {
    private val DATE_FORMAT: DateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.FRENCH)
    private const val TIME_FORMAT = "%02d:%02d"
    private var YEAR = 0
    private var MONTH_OF_YEAR = 0
    private var DAY_OF_MONTH = 0
    private var DATE: String? = null
    private const val HOURS = 12
    private const val MINUTES = 15
    private val TIME = String.format(TIME_FORMAT, HOURS, MINUTES)

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
