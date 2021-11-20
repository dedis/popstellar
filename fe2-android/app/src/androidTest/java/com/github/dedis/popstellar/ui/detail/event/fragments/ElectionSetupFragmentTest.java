package com.github.dedis.popstellar.ui.detail.event.fragments;

import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static com.github.dedis.popstellar.pages.detail.event.EventCreationPageObject.datePicker;
import static com.github.dedis.popstellar.pages.detail.event.EventCreationPageObject.endDateView;
import static com.github.dedis.popstellar.pages.detail.event.EventCreationPageObject.endTimeView;
import static com.github.dedis.popstellar.pages.detail.event.EventCreationPageObject.pickerAcceptButton;
import static com.github.dedis.popstellar.pages.detail.event.EventCreationPageObject.startDateView;
import static com.github.dedis.popstellar.pages.detail.event.EventCreationPageObject.startTimeView;
import static com.github.dedis.popstellar.pages.detail.event.EventCreationPageObject.timePicker;

import android.icu.util.Calendar;

import androidx.test.espresso.contrib.PickerActions;
import androidx.test.filters.LargeTest;
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;

import com.github.dedis.popstellar.testutils.FragmentScenarioRule;
import com.github.dedis.popstellar.ui.detail.event.election.fragments.ElectionSetupFragment;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@LargeTest
@RunWith(AndroidJUnit4ClassRunner.class)
public class ElectionSetupFragmentTest {

  private static final String DATE_FORMAT = "%02d/%02d/%02d";
  private static final String TIME_FORMAT = "%02d:%02d";

  private static final int YEAR;
  private static final int MONTH_OF_YEAR;
  private static final int DAY_OF_MONTH;
  private static final String DATE;

  private static final int HOURS = 12;
  private static final int MINUTES = 15;
  private static final String TIME = String.format(TIME_FORMAT, HOURS, MINUTES);

  static {
    // Make sure the date is always in the future
    Calendar today = Calendar.getInstance();
    today.add(Calendar.MONTH, 13);

    YEAR = today.get(Calendar.YEAR);
    MONTH_OF_YEAR = today.get(Calendar.MONTH) + 1;
    DAY_OF_MONTH = today.get(Calendar.DAY_OF_MONTH);

    DATE = String.format(DATE_FORMAT, DAY_OF_MONTH, MONTH_OF_YEAR, YEAR);
  }

  @Rule
  public final FragmentScenarioRule<ElectionSetupFragment> fragmentRule =
      FragmentScenarioRule.launchInContainer(ElectionSetupFragment.class);

  @Test
  public void canLaunchDatePickerFragmentFromStartDateButton() {
    startDateView().perform(click());
    datePicker().check(matches(isDisplayed()));
  }

  @Test
  public void canLaunchDatePickerFragmentFromEndDateButton() {
    endDateView().perform(click());
    datePicker().check(matches(isDisplayed()));
  }

  @Test
  public void canChooseRandomDate() {
    startDateView().perform(click());

    datePicker().perform(PickerActions.setDate(YEAR, MONTH_OF_YEAR, DAY_OF_MONTH));
    pickerAcceptButton().perform(click());

    startDateView().check(matches(withText(DATE)));
  }

  @Test
  public void datePickerChoosesTodayByDefault() {
    final Calendar currentCalendar = Calendar.getInstance();
    int year = currentCalendar.get(Calendar.YEAR);
    int month = currentCalendar.get(Calendar.MONTH) + 1;
    int day = currentCalendar.get(Calendar.DAY_OF_MONTH);
    final String date = String.format(DATE_FORMAT, day, month, year);

    startDateView().perform(click());
    pickerAcceptButton().perform(click());
    startDateView().check(matches(withText(date)));
  }

  @Test
  public void startDateAndEndDateCanBothBeSameDay() {
    startDateView().perform(click());
    datePicker().perform(PickerActions.setDate(YEAR, MONTH_OF_YEAR, DAY_OF_MONTH));
    pickerAcceptButton().perform(click());
    startDateView().check(matches(withText(DATE)));

    endDateView().perform(click());
    datePicker().perform(PickerActions.setDate(YEAR, MONTH_OF_YEAR, DAY_OF_MONTH));
    pickerAcceptButton().perform(click());
    endDateView().check(matches(withText(DATE)));
  }

  @Test
  public void canLaunchTimePickerFragmentFromStartTimeButton() {
    startTimeView().perform(click());

    timePicker().check(matches(isDisplayed()));
  }

  @Test
  public void canLaunchTimePickerFragmentFromEndTimeButton() {
    endTimeView().perform(click());
    timePicker().check(matches(isDisplayed()));
  }

  @Test
  public void canChooseRandomStartTimeWhenNoDate() {
    startTimeView().perform(click());
    timePicker().perform(PickerActions.setTime(HOURS, MINUTES));
    pickerAcceptButton().perform(click());
    startTimeView().check(matches(withText(TIME)));
  }

  @Test
  public void canChooseRandomEndTimeWhenNoDate() {
    endTimeView().perform(click());
    timePicker().perform(PickerActions.setTime(HOURS, MINUTES));
    pickerAcceptButton().perform(click());
    endTimeView().check(matches(withText(TIME)));
  }

  @Test
  public void canChooseRandomStartTimeWhenStartDateFilled() {
    startDateView().perform(click());
    datePicker().perform(PickerActions.setDate(YEAR, MONTH_OF_YEAR, DAY_OF_MONTH));
    pickerAcceptButton().perform(click());
    startDateView().check(matches(withText(DATE)));

    startTimeView().perform(click());
    timePicker().perform(PickerActions.setTime(HOURS, MINUTES));
    pickerAcceptButton().perform(click());
    startTimeView().check(matches(withText(TIME)));
  }

  @Test
  public void canChooseRandomStartTimeWhenEndDateFilled() {
    endDateView().perform(click());
    datePicker().perform(PickerActions.setDate(YEAR, MONTH_OF_YEAR, DAY_OF_MONTH));
    pickerAcceptButton().perform(click());
    endDateView().check(matches(withText(DATE)));

    startTimeView().perform(click());
    timePicker().perform(PickerActions.setTime(HOURS, MINUTES));
    pickerAcceptButton().perform(click());
    startTimeView().check(matches(withText(TIME)));
  }

  @Test
  public void canChooseRandomEndTimeWhenStartDateFilled() {
    startDateView().perform(click());
    datePicker().perform(PickerActions.setDate(YEAR, MONTH_OF_YEAR, DAY_OF_MONTH));
    pickerAcceptButton().perform(click());
    startDateView().check(matches(withText(DATE)));

    endTimeView().perform(click());
    timePicker().perform(PickerActions.setTime(HOURS, MINUTES));
    pickerAcceptButton().perform(click());
    endTimeView().check(matches(withText(TIME)));
  }

  @Test
  public void canChooseRandomEndTimeWhenEndDateFilled() {
    endDateView().perform(click());
    datePicker().perform(PickerActions.setDate(YEAR, MONTH_OF_YEAR, DAY_OF_MONTH));
    pickerAcceptButton().perform(click());
    endDateView().check(matches(withText(DATE)));

    endTimeView().perform(click());
    timePicker().perform(PickerActions.setTime(HOURS, MINUTES));
    pickerAcceptButton().perform(click());
    endTimeView().check(matches(withText(TIME)));
  }

  @Test
  public void canChooseStartTimeBeforeEndTimeWhenSameDayEvent() {
    startDateView().perform(click());
    datePicker().perform(PickerActions.setDate(YEAR, MONTH_OF_YEAR, DAY_OF_MONTH));
    pickerAcceptButton().perform(click());
    startDateView().check(matches(withText(DATE)));

    endDateView().perform(click());
    datePicker().perform(PickerActions.setDate(YEAR, MONTH_OF_YEAR, DAY_OF_MONTH));
    pickerAcceptButton().perform(click());
    endDateView().check(matches(withText(DATE)));

    endTimeView().perform(click());
    timePicker().perform(PickerActions.setTime(HOURS, MINUTES));
    pickerAcceptButton().perform(click());
    endTimeView().check(matches(withText(TIME)));

    startTimeView().perform(click());
    timePicker().perform(PickerActions.setTime(HOURS - 1, MINUTES));
    pickerAcceptButton().perform(click());
    startTimeView().check(matches(withText("" + (HOURS - 1) + ":" + MINUTES)));
  }

  @Test
  public void canChooseEndTimeBeforeStartTimeWhenSameDayEvent() {
    startDateView().perform(click());
    datePicker().perform(PickerActions.setDate(YEAR, MONTH_OF_YEAR, DAY_OF_MONTH));
    pickerAcceptButton().perform(click());
    startDateView().check(matches(withText(DATE)));

    endDateView().perform(click());
    datePicker().perform(PickerActions.setDate(YEAR, MONTH_OF_YEAR, DAY_OF_MONTH));
    pickerAcceptButton().perform(click());
    endDateView().check(matches(withText(DATE)));

    startTimeView().perform(click());
    timePicker().perform(PickerActions.setTime(HOURS, MINUTES));
    pickerAcceptButton().perform(click());
    startTimeView().check(matches(withText(TIME)));

    endTimeView().perform(click());
    timePicker().perform(PickerActions.setTime(HOURS + 1, MINUTES));
    pickerAcceptButton().perform(click());
    endTimeView().check(matches(withText(String.format(TIME_FORMAT, HOURS + 1, MINUTES))));
  }
}
