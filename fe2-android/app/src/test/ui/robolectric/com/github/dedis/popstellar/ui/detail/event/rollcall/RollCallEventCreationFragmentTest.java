package com.github.dedis.popstellar.ui.detail.event.rollcall;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.github.dedis.popstellar.testutils.fragment.FragmentScenarioRule;
import com.github.dedis.popstellar.ui.lao.event.rollcall.RollCallCreationFragment;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import dagger.hilt.android.testing.HiltAndroidRule;
import dagger.hilt.android.testing.HiltAndroidTest;

import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static com.github.dedis.popstellar.testutils.UITestUtils.dialogPositiveButton;
import static com.github.dedis.popstellar.testutils.UITestUtils.getLastDialog;
import static com.github.dedis.popstellar.testutils.pages.detail.event.EventCreationPageObject.*;
import static org.hamcrest.MatcherAssert.assertThat;

@LargeTest
@HiltAndroidTest
@RunWith(AndroidJUnit4.class)
public class RollCallEventCreationFragmentTest {

  private static final DateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy", Locale.FRENCH);
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
    MONTH_OF_YEAR = today.get(Calendar.MONTH);
    DAY_OF_MONTH = today.get(Calendar.DAY_OF_MONTH);
    DATE = DATE_FORMAT.format(today.getTime());
  }

  private final FragmentScenarioRule<RollCallCreationFragment> fragmentRule =
      FragmentScenarioRule.launch(RollCallCreationFragment.class);

  @Rule
  public final RuleChain chain =
      RuleChain.outerRule(new HiltAndroidRule(this)).around(fragmentRule);

  @Test
  public void canLaunchDatePickerFragmentFromStartDateButton() {
    startDateView().perform(click());
    assertThat(getLastDialog(DatePickerDialog.class).getDatePicker(), isDisplayed());
  }

  @Test
  public void canLaunchDatePickerFragmentFromEndDateButton() {
    endDateView().perform(click());
    assertThat(getLastDialog(DatePickerDialog.class).getDatePicker(), isDisplayed());
  }

  @Test
  public void canChooseRandomDate() {
    startDateView().perform(click());

    getLastDialog(DatePickerDialog.class).updateDate(YEAR, MONTH_OF_YEAR, DAY_OF_MONTH);
    dialogPositiveButton().performClick();

    startDateView().check(matches(withText(DATE)));
  }

  @Test
  public void datePickerChoosesTodayByDefault() {
    final Calendar currentCalendar = Calendar.getInstance();
    final String date = DATE_FORMAT.format(currentCalendar.getTime());

    startDateView().perform(click());
    dialogPositiveButton().performClick();
    startDateView().check(matches(withText(date)));
  }

  @Test
  public void startDateAndEndDateCanBothBeSameDay() {
    startDateView().perform(click());
    getLastDialog(DatePickerDialog.class).updateDate(YEAR, MONTH_OF_YEAR, DAY_OF_MONTH);
    dialogPositiveButton().performClick();
    startDateView().check(matches(withText(DATE)));

    endDateView().perform(click());
    getLastDialog(DatePickerDialog.class).updateDate(YEAR, MONTH_OF_YEAR, DAY_OF_MONTH);
    dialogPositiveButton().performClick();
    endDateView().check(matches(withText(DATE)));
  }

  @Test
  public void canChooseRandomStartTimeWhenNoDate() {
    startTimeView().perform(click());
    getLastDialog(TimePickerDialog.class).updateTime(HOURS, MINUTES);
    dialogPositiveButton().performClick();
    startTimeView().check(matches(withText(TIME)));
  }

  @Test
  public void canChooseRandomEndTimeWhenNoDate() {
    endTimeView().perform(click());
    getLastDialog(TimePickerDialog.class).updateTime(HOURS, MINUTES);
    dialogPositiveButton().performClick();
    endTimeView().check(matches(withText(TIME)));
  }

  @Test
  public void canChooseRandomStartTimeWhenStartDateFilled() {
    startDateView().perform(click());
    getLastDialog(DatePickerDialog.class).updateDate(YEAR, MONTH_OF_YEAR, DAY_OF_MONTH);
    dialogPositiveButton().performClick();
    startDateView().check(matches(withText(DATE)));

    startTimeView().perform(click());
    getLastDialog(TimePickerDialog.class).updateTime(HOURS, MINUTES);
    dialogPositiveButton().performClick();
    startTimeView().check(matches(withText(TIME)));
  }

  @Test
  public void canChooseRandomStartTimeWhenEndDateFilled() {
    endDateView().perform(click());
    getLastDialog(DatePickerDialog.class).updateDate(YEAR, MONTH_OF_YEAR, DAY_OF_MONTH);
    dialogPositiveButton().performClick();
    endDateView().check(matches(withText(DATE)));

    startTimeView().perform(click());
    getLastDialog(TimePickerDialog.class).updateTime(HOURS, MINUTES);
    dialogPositiveButton().performClick();
    startTimeView().check(matches(withText(TIME)));
  }

  @Test
  public void canChooseRandomEndTimeWhenStartDateFilled() {
    startDateView().perform(click());
    getLastDialog(DatePickerDialog.class).updateDate(YEAR, MONTH_OF_YEAR, DAY_OF_MONTH);
    dialogPositiveButton().performClick();
    startDateView().check(matches(withText(DATE)));

    endTimeView().perform(click());
    getLastDialog(TimePickerDialog.class).updateTime(HOURS, MINUTES);
    dialogPositiveButton().performClick();
    endTimeView().check(matches(withText(TIME)));
  }

  @Test
  public void canChooseRandomEndTimeWhenEndDateFilled() {
    endDateView().perform(click());
    getLastDialog(DatePickerDialog.class).updateDate(YEAR, MONTH_OF_YEAR, DAY_OF_MONTH);
    dialogPositiveButton().performClick();
    endDateView().check(matches(withText(DATE)));

    endTimeView().perform(click());
    getLastDialog(TimePickerDialog.class).updateTime(HOURS, MINUTES);
    dialogPositiveButton().performClick();
    endTimeView().check(matches(withText(TIME)));
  }

  @Test
  public void canChooseStartTimeBeforeEndTimeWhenSameDayEvent() {
    endTimeView().perform(click());
    getLastDialog(TimePickerDialog.class).updateTime(HOURS, MINUTES);
    dialogPositiveButton().performClick();
    endTimeView().check(matches(withText(TIME)));

    startTimeView().perform(click());
    getLastDialog(TimePickerDialog.class).updateTime(HOURS - 1, MINUTES);
    dialogPositiveButton().performClick();
    startTimeView().check(matches(withText("" + (HOURS - 1) + ":" + MINUTES)));
  }

  @Test
  public void canChooseEndTimeBeforeStartTimeWhenSameDayEvent() {
    startDateView().perform(click());
    getLastDialog(DatePickerDialog.class).updateDate(YEAR, MONTH_OF_YEAR, DAY_OF_MONTH);
    dialogPositiveButton().performClick();
    startDateView().check(matches(withText(DATE)));

    endDateView().perform(click());
    getLastDialog(DatePickerDialog.class).updateDate(YEAR, MONTH_OF_YEAR, DAY_OF_MONTH);
    dialogPositiveButton().performClick();
    endDateView().check(matches(withText(DATE)));

    startTimeView().perform(click());
    getLastDialog(TimePickerDialog.class).updateTime(HOURS, MINUTES);
    dialogPositiveButton().performClick();
    startTimeView().check(matches(withText(TIME)));

    endTimeView().perform(click());
    getLastDialog(TimePickerDialog.class).updateTime(HOURS + 1, MINUTES);
    dialogPositiveButton().performClick();
    endTimeView().check(matches(withText(String.format(TIME_FORMAT, HOURS + 1, MINUTES))));
  }
}
