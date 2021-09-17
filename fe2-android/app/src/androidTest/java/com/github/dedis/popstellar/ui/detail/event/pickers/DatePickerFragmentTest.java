package com.github.dedis.popstellar.ui.detail.event.pickers;

public class DatePickerFragmentTest {
  //  private final int YEAR = 2022;
  //  private final int MONTH_OF_YEAR = 10;
  //  private final int DAY_OF_MONTH = 10;
  //  private final String DATE = "" + DAY_OF_MONTH + "/" + MONTH_OF_YEAR + "/" + YEAR;
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
  //  public void canLaunchDatePickerFragmentFromStartDateButton() {
  //    onView(withId(R.id.start_date_edit_text)).perform(click());
  //    onView(withClassName(Matchers.equalTo(DatePicker.class.getName())))
  //        .check(matches(isDisplayed()));
  //  }
  //
  //  @Test
  //  public void canLaunchDatePickerFragmentFromEndDateButton() {
  //    onView(withId(R.id.end_date_edit_text)).perform(click());
  //    onView(withClassName(Matchers.equalTo(DatePicker.class.getName())))
  //        .check(matches(isDisplayed()));
  //  }
  //
  //  @Test
  //  public void canChooseRandomDate() {
  //    onView(withId(R.id.start_date_edit_text)).perform(click());
  //    onView(withClassName(Matchers.equalTo(DatePicker.class.getName())))
  //        .perform(PickerActions.setDate(YEAR, MONTH_OF_YEAR, DAY_OF_MONTH));
  //    onView(withId(android.R.id.button1)).perform(click());
  //    onView(withId(R.id.start_date_edit_text)).check(matches(withText(DATE)));
  //  }
  //
  //  @Test
  //  public void datePickerChoosesTodayByDefault() {
  //    final Calendar currentCalendar = Calendar.getInstance();
  //    int year = currentCalendar.get(Calendar.YEAR);
  //    int month = currentCalendar.get(Calendar.MONTH) + 1;
  //    int day = currentCalendar.get(Calendar.DAY_OF_MONTH);
  //    final String DATE =
  //        (day < 10 ? "0" : "") + day + "/" + (month < 10 ? "0" : "") + month + "/" + year;
  //    onView(withId(R.id.start_date_edit_text)).perform(click());
  //    onView(withId(android.R.id.button1)).perform(click());
  //    onView(withId(R.id.start_date_edit_text)).check(matches(withText(DATE)));
  //  }
  //
  //  @Test
  //  public void choosingDateInPastShowsToast() {
  //    String expectedWarning = getApplicationContext().getString(R.string.past_date_not_allowed);
  //    final int YEAR = 2010;
  //    final int MONTH_OF_YEAR = 10;
  //    final int DAY_OF_MONTH = 10;
  //
  //    onView(withId(R.id.start_date_edit_text)).perform(click());
  //    onView(withClassName(Matchers.equalTo(DatePicker.class.getName())))
  //        .perform(PickerActions.setDate(YEAR, MONTH_OF_YEAR, DAY_OF_MONTH));
  //    onView(withId(android.R.id.button1)).perform(click());
  //    onView(withText(expectedWarning))
  //        .inRoot(withDecorView(not(decorView)))
  //        .check(matches(isDisplayed()));
  //    onView(withId(R.id.start_date_edit_text))
  //
  // .check(matches(withHint(getApplicationContext().getString(R.string.start_date_required))));
  //  }
  //
  //  @Test
  //  public void startDateAndEndDateCanBothBeSameDay() {
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
  //  }
  //
  //  @Test
  //  public void startDateAfterEndDateShowsToast() {
  //    String expectedWarning =
  //        getApplicationContext().getString(R.string.start_date_after_end_date_not_allowed);
  //
  //    onView(withId(R.id.end_date_edit_text)).perform(click());
  //    onView(withClassName(Matchers.equalTo(DatePicker.class.getName())))
  //        .perform(PickerActions.setDate(YEAR, MONTH_OF_YEAR, DAY_OF_MONTH));
  //    onView(withId(android.R.id.button1)).perform(click());
  //    onView(withId(R.id.end_date_edit_text)).check(matches(withText(DATE)));
  //
  //    onView(withId(R.id.start_date_edit_text)).perform(click());
  //    onView(withClassName(Matchers.equalTo(DatePicker.class.getName())))
  //        .perform(PickerActions.setDate(YEAR, MONTH_OF_YEAR, DAY_OF_MONTH + 1));
  //    onView(withId(android.R.id.button1)).perform(click());
  //
  //    onView(withText(expectedWarning))
  //        .inRoot(withDecorView(not(decorView)))
  //        .check(matches(isDisplayed()));
  //    onView(withId(R.id.start_date_edit_text))
  //
  // .check(matches(withHint(getApplicationContext().getString(R.string.start_date_required))));
  //  }
  //
  //  @Test
  //  public void endDateBeforeStartDateShowsToast() {
  //    String expectedWarning =
  //        getApplicationContext().getString(R.string.end_date_after_start_date_not_allowed);
  //
  //    onView(withId(R.id.start_date_edit_text)).perform(click());
  //    onView(withClassName(Matchers.equalTo(DatePicker.class.getName())))
  //        .perform(PickerActions.setDate(YEAR, MONTH_OF_YEAR, DAY_OF_MONTH));
  //    onView(withId(android.R.id.button1)).perform(click());
  //    onView(withId(R.id.start_date_edit_text)).check(matches(withText(DATE)));
  //
  //    onView(withId(R.id.end_date_edit_text)).perform(click());
  //    onView(withClassName(Matchers.equalTo(DatePicker.class.getName())))
  //        .perform(PickerActions.setDate(YEAR, MONTH_OF_YEAR, DAY_OF_MONTH - 1));
  //    onView(withId(android.R.id.button1)).perform(click());
  //
  //    onView(withText(expectedWarning))
  //        .inRoot(withDecorView(not(decorView)))
  //        .check(matches(isDisplayed()));
  //    onView(withId(R.id.end_date_edit_text))
  //
  // .check(matches(withHint(getApplicationContext().getString(R.string.end_date_optional))));
  //  }
}
