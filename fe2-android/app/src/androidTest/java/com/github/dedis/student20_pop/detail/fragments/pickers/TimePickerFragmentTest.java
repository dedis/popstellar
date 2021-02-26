package com.github.dedis.student20_pop.detail.fragments.pickers;

public class TimePickerFragmentTest {
  //  private final int YEAR = 2022;
  //  private final int MONTH_OF_YEAR = 10;
  //  private final int DAY_OF_MONTH = 10;
  //  private final String DATE = "" + DAY_OF_MONTH + "/" + MONTH_OF_YEAR + "/" + YEAR;
  //  private final int HOURS = 12;
  //  private final int MINUTES = 15;
  //  private final String TIME = "" + HOURS + ":" + MINUTES;
  //
  //  @Rule
  //  public ActivityScenarioRule<OrganizerActivity> activityScenarioRule =
  //      new ActivityScenarioRule<>(OrganizerActivity.class);
  //
  //  private View decorView;
  //
  //  @Before
  //  public void setUp() {
  //    activityScenarioRule
  //        .getScenario()
  //        .onActivity(
  //            new ActivityScenario.ActivityAction<OrganizerActivity>() {
  //
  //              /**
  //               * This method is invoked on the main thread with the reference to the Activity.
  //               *
  //               * @param activity an Activity instrumented by the {@link ActivityScenario}. It
  // never
  //               *     be null.
  //               */
  //              @Override
  //              public void perform(OrganizerActivity activity) {
  //                decorView = activity.getWindow().getDecorView();
  //              }
  //            });
  //
  //    onView(
  //            allOf(
  //                withId(R.id.add_future_event_button),
  //                withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
  //        .perform(click());
  //
  // onView(withText(getApplicationContext().getString(R.string.meeting_event))).perform(click());
  //    onView(withId(R.id.fragment_meeting_event_creation)).check(matches(isDisplayed()));
  //  }
  //
  //  @Test
  //  public void canLaunchTimePickerFragmentFromStartTimeButton() {
  //    onView(withId(R.id.start_time_edit_text)).perform(click());
  //
  //    onView(withClassName(Matchers.equalTo(TimePicker.class.getName())))
  //        .check(matches(isDisplayed()));
  //  }
  //
  //  @Test
  //  public void canLaunchTimePickerFragmentFromEndTimeButton() {
  //    onView(withId(R.id.end_time_edit_text)).perform(click());
  //    onView(withClassName(Matchers.equalTo(TimePicker.class.getName())))
  //        .check(matches(isDisplayed()));
  //  }
  //
  //  @Test
  //  public void canChooseRandomStartTimeWhenNoDate() {
  //    onView(withId(R.id.start_time_edit_text)).perform(click());
  //    onView(withClassName(Matchers.equalTo(TimePicker.class.getName())))
  //        .perform(PickerActions.setTime(HOURS, MINUTES));
  //    onView(withId(android.R.id.button1)).perform(click());
  //    onView(withId(R.id.start_time_edit_text)).check(matches(withText(TIME)));
  //  }
  //
  //  @Test
  //  public void canChooseRandomEndTimeWhenNoDate() {
  //    onView(withId(R.id.end_time_edit_text)).perform(click());
  //    onView(withClassName(Matchers.equalTo(TimePicker.class.getName())))
  //        .perform(PickerActions.setTime(HOURS, MINUTES));
  //    onView(withId(android.R.id.button1)).perform(click());
  //    onView(withId(R.id.end_time_edit_text)).check(matches(withText(TIME)));
  //  }
  //
  //  @Test
  //  public void canChooseRandomStartTimeWhenStartDateFilled() {
  //    onView(withId(R.id.start_date_edit_text)).perform(click());
  //    onView(withClassName(Matchers.equalTo(DatePicker.class.getName())))
  //        .perform(PickerActions.setDate(YEAR, MONTH_OF_YEAR, DAY_OF_MONTH));
  //    onView(withId(android.R.id.button1)).perform(click());
  //    onView(withId(R.id.start_date_edit_text)).check(matches(withText(DATE)));
  //
  //    onView(withId(R.id.start_time_edit_text)).perform(click());
  //    onView(withClassName(Matchers.equalTo(TimePicker.class.getName())))
  //        .perform(PickerActions.setTime(HOURS, MINUTES));
  //    onView(withId(android.R.id.button1)).perform(click());
  //    onView(withId(R.id.start_time_edit_text)).check(matches(withText(TIME)));
  //  }
  //
  //  @Test
  //  public void canChooseRandomStartTimeWhenEndDateFilled() {
  //    onView(withId(R.id.end_date_edit_text)).perform(click());
  //    onView(withClassName(Matchers.equalTo(DatePicker.class.getName())))
  //        .perform(PickerActions.setDate(YEAR, MONTH_OF_YEAR, DAY_OF_MONTH));
  //    onView(withId(android.R.id.button1)).perform(click());
  //    onView(withId(R.id.end_date_edit_text)).check(matches(withText(DATE)));
  //
  //    onView(withId(R.id.start_time_edit_text)).perform(click());
  //    onView(withClassName(Matchers.equalTo(TimePicker.class.getName())))
  //        .perform(PickerActions.setTime(HOURS, MINUTES));
  //    onView(withId(android.R.id.button1)).perform(click());
  //    onView(withId(R.id.start_time_edit_text)).check(matches(withText(TIME)));
  //  }
  //
  //  @Test
  //  public void canChooseRandomEndTimeWhenStartDateFilled() {
  //    onView(withId(R.id.start_date_edit_text)).perform(click());
  //    onView(withClassName(Matchers.equalTo(DatePicker.class.getName())))
  //        .perform(PickerActions.setDate(YEAR, MONTH_OF_YEAR, DAY_OF_MONTH));
  //    onView(withId(android.R.id.button1)).perform(click());
  //    onView(withId(R.id.start_date_edit_text)).check(matches(withText(DATE)));
  //
  //    onView(withId(R.id.end_time_edit_text)).perform(click());
  //    onView(withClassName(Matchers.equalTo(TimePicker.class.getName())))
  //        .perform(PickerActions.setTime(HOURS, MINUTES));
  //    onView(withId(android.R.id.button1)).perform(click());
  //    onView(withId(R.id.end_time_edit_text)).check(matches(withText(TIME)));
  //  }
  //
  //  @Test
  //  public void canChooseRandomEndTimeWhenEndDateFilled() {
  //    onView(withId(R.id.end_date_edit_text)).perform(click());
  //    onView(withClassName(Matchers.equalTo(DatePicker.class.getName())))
  //        .perform(PickerActions.setDate(YEAR, MONTH_OF_YEAR, DAY_OF_MONTH));
  //    onView(withId(android.R.id.button1)).perform(click());
  //    onView(withId(R.id.end_date_edit_text)).check(matches(withText(DATE)));
  //
  //    onView(withId(R.id.end_time_edit_text)).perform(click());
  //    onView(withClassName(Matchers.equalTo(TimePicker.class.getName())))
  //        .perform(PickerActions.setTime(HOURS, MINUTES));
  //    onView(withId(android.R.id.button1)).perform(click());
  //    onView(withId(R.id.end_time_edit_text)).check(matches(withText(TIME)));
  //  }
  //
  //  @Test
  //  @Ignore("Issue with Toast showing")
  //  public void endTimeBeforeStartTimeWhenSameDayEventShowsToast() {
  //    String expectedWarning =
  //        getApplicationContext().getString(R.string.end_time_before_start_time_not_allowed);
  //
  //    onView(withId(R.id.start_date_edit_text)).perform(click());
  //    onView(withClassName(Matchers.equalTo(DatePicker.class.getName())))
  //        .perform(PickerActions.setDate(YEAR, MONTH_OF_YEAR, DAY_OF_MONTH));
  //    onView(withId(android.R.id.button1)).perform(click());
  //    onView(withId(R.id.start_date_edit_text)).check(matches(withText(DATE)));
  //
  //    onView(withId(R.id.end_date_edit_text)).perform(click());
  //    onView(withClassName(Matchers.equalTo(DatePicker.class.getName())))
  //        .perform(PickerActions.setDate(YEAR, MONTH_OF_YEAR, DAY_OF_MONTH));
  //    onView(withId(android.R.id.button1)).perform(click());
  //    onView(withId(R.id.end_date_edit_text)).check(matches(withText(DATE)));
  //
  //    onView(withId(R.id.start_time_edit_text)).perform(click());
  //    onView(withClassName(Matchers.equalTo(TimePicker.class.getName())))
  //        .perform(PickerActions.setTime(HOURS, MINUTES));
  //    onView(withId(android.R.id.button1)).perform(click());
  //
  //    onView(withId(R.id.end_time_edit_text)).perform(click());
  //    onView(withClassName(Matchers.equalTo(TimePicker.class.getName())))
  //        .perform(PickerActions.setTime(HOURS - 1, MINUTES));
  //    onView(withId(android.R.id.button1)).perform(click());
  //
  //    onView(withText(expectedWarning))
  //        .inRoot(withDecorView(not(decorView)))
  //        .check(matches(isDisplayed()));
  //    onView(withId(R.id.end_time_edit_text))
  //
  // .check(matches(withHint(getApplicationContext().getString(R.string.end_time_optional))));
  //  }
  //
  //  @Test
  //  @Ignore("Issue with Toast showing")
  //  public void startTimeAfterEndTimeWhenSameDayEventShowsToast() {
  //    String expectedWarning =
  //        getApplicationContext().getString(R.string.start_time_after_end_time_not_allowed);
  //
  //    onView(withId(R.id.start_date_edit_text)).perform(click());
  //    onView(withClassName(Matchers.equalTo(DatePicker.class.getName())))
  //        .perform(PickerActions.setDate(YEAR, MONTH_OF_YEAR, DAY_OF_MONTH));
  //    onView(withId(android.R.id.button1)).perform(click());
  //    onView(withId(R.id.start_date_edit_text)).check(matches(withText(DATE)));
  //
  //    onView(withId(R.id.end_date_edit_text)).perform(click());
  //    onView(withClassName(Matchers.equalTo(DatePicker.class.getName())))
  //        .perform(PickerActions.setDate(YEAR, MONTH_OF_YEAR, DAY_OF_MONTH));
  //    onView(withId(android.R.id.button1)).perform(click());
  //    onView(withId(R.id.end_date_edit_text)).check(matches(withText(DATE)));
  //
  //    onView(withId(R.id.end_time_edit_text)).perform(click());
  //    onView(withClassName(Matchers.equalTo(TimePicker.class.getName())))
  //        .perform(PickerActions.setTime(HOURS, MINUTES));
  //    onView(withId(android.R.id.button1)).perform(click());
  //
  //    onView(withId(R.id.start_time_edit_text)).perform(click());
  //    onView(withClassName(Matchers.equalTo(TimePicker.class.getName())))
  //        .perform(PickerActions.setTime(HOURS + 1, MINUTES));
  //    onView(withId(android.R.id.button1)).perform(click());
  //
  //    onView(withText(expectedWarning))
  //        .inRoot(withDecorView(not(decorView)))
  //        .check(matches(isDisplayed()));
  //    onView(withId(R.id.start_time_edit_text))
  //
  // .check(matches(withHint(getApplicationContext().getString(R.string.start_time_required))));
  //  }
  //
  //  @Test
  //  public void canChooseStartTimeBeforeEndTimeWhenSameDayEvent() {
  //    onView(withId(R.id.start_date_edit_text)).perform(click());
  //    onView(withClassName(Matchers.equalTo(DatePicker.class.getName())))
  //        .perform(PickerActions.setDate(YEAR, MONTH_OF_YEAR, DAY_OF_MONTH));
  //    onView(withId(android.R.id.button1)).perform(click());
  //    onView(withId(R.id.start_date_edit_text)).check(matches(withText(DATE)));
  //
  //    onView(withId(R.id.end_date_edit_text)).perform(click());
  //    onView(withClassName(Matchers.equalTo(DatePicker.class.getName())))
  //        .perform(PickerActions.setDate(YEAR, MONTH_OF_YEAR, DAY_OF_MONTH));
  //    onView(withId(android.R.id.button1)).perform(click());
  //    onView(withId(R.id.end_date_edit_text)).check(matches(withText(DATE)));
  //
  //    onView(withId(R.id.end_time_edit_text)).perform(click());
  //    onView(withClassName(Matchers.equalTo(TimePicker.class.getName())))
  //        .perform(PickerActions.setTime(HOURS, MINUTES));
  //    onView(withId(android.R.id.button1)).perform(click());
  //    onView(withId(R.id.end_time_edit_text)).check(matches(withText(TIME)));
  //
  //    onView(withId(R.id.start_time_edit_text)).perform(click());
  //    onView(withClassName(Matchers.equalTo(TimePicker.class.getName())))
  //        .perform(PickerActions.setTime(HOURS - 1, MINUTES));
  //    onView(withId(android.R.id.button1)).perform(click());
  //    onView(withId(R.id.start_time_edit_text))
  //        .check(matches(withText("" + (HOURS - 1) + ":" + MINUTES)));
  //  }
  //
  //  @Test
  //  public void canChooseEndTimeBeforeStartTimeWhenSameDayEvent() {
  //    onView(withId(R.id.start_date_edit_text)).perform(click());
  //    onView(withClassName(Matchers.equalTo(DatePicker.class.getName())))
  //        .perform(PickerActions.setDate(YEAR, MONTH_OF_YEAR, DAY_OF_MONTH));
  //    onView(withId(android.R.id.button1)).perform(click());
  //    onView(withId(R.id.start_date_edit_text)).check(matches(withText(DATE)));
  //
  //    onView(withId(R.id.end_date_edit_text)).perform(click());
  //    onView(withClassName(Matchers.equalTo(DatePicker.class.getName())))
  //        .perform(PickerActions.setDate(YEAR, MONTH_OF_YEAR, DAY_OF_MONTH));
  //    onView(withId(android.R.id.button1)).perform(click());
  //    onView(withId(R.id.end_date_edit_text)).check(matches(withText(DATE)));
  //
  //    onView(withId(R.id.start_time_edit_text)).perform(click());
  //    onView(withClassName(Matchers.equalTo(TimePicker.class.getName())))
  //        .perform(PickerActions.setTime(HOURS, MINUTES));
  //    onView(withId(android.R.id.button1)).perform(click());
  //    onView(withId(R.id.start_time_edit_text)).check(matches(withText(TIME)));
  //
  //    onView(withId(R.id.end_time_edit_text)).perform(click());
  //    onView(withClassName(Matchers.equalTo(TimePicker.class.getName())))
  //        .perform(PickerActions.setTime(HOURS + 1, MINUTES));
  //    onView(withId(android.R.id.button1)).perform(click());
  //    onView(withId(R.id.end_time_edit_text))
  //        .check(matches(withText("" + (HOURS + 1) + ":" + MINUTES)));
  //  }
}
