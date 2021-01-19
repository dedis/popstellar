package com.github.dedis.student20_pop.ui.event.creation;

import android.widget.DatePicker;
import android.widget.TimePicker;
import androidx.test.espresso.contrib.PickerActions;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import com.github.dedis.student20_pop.OrganizerActivity;
import com.github.dedis.student20_pop.PoPApplication;
import com.github.dedis.student20_pop.R;
import com.github.dedis.student20_pop.model.Lao;
import com.github.dedis.student20_pop.model.event.Event;
import org.hamcrest.Matchers;
import org.junit.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static org.hamcrest.Matchers.*;

public class MeetingEventCreationFragmentTest {
  private final int YEAR = 2022;
  private final int MONTH_OF_YEAR = 10;
  private final int DAY_OF_MONTH = 10;
  private final String DATE = "" + DAY_OF_MONTH + "/" + MONTH_OF_YEAR + "/" + YEAR;
  private final int HOURS = 12;
  private final int MINUTES = 15;
  private final String TIME = "" + HOURS + ":" + MINUTES;

  @Rule
  public ActivityScenarioRule<OrganizerActivity> activityScenarioRule =
      new ActivityScenarioRule<>(OrganizerActivity.class);

  @Before
  public void setUp() {
    onView(
            allOf(
                withId(R.id.add_future_event_button),
                withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
        .perform(click());
    onView(withText(getApplicationContext().getString(R.string.meeting_event))).perform(click());
  }

  @Test
  public void canLaunchEventMeetingFragment() {
    onView(withId(R.id.fragment_meeting_event_creation)).check(matches(isDisplayed()));
  }

  @Test
  public void confirmButtonIsDisabled() {
    onView(withId(R.id.meeting_event_creation_confirm)).check(matches(not(isEnabled())));
  }

  @Test
  public void cancelButtonWorks() {
    onView(withId(R.id.meeting_event_creation_cancel)).perform(click());
    onView(withId(R.id.fragment_organizer)).check(matches(isDisplayed()));
  }

  @Test
  @Ignore("TODO: solve issue with GithubActions Emulator")
  public void confirmButtonIsEnabledWhenRequiredFieldsFilled() {
    onView(withId(R.id.meeting_title_text)).perform(typeText("Random meeting title"));

    onView(withId(R.id.start_date_edit_text)).perform(click());
    onView(withClassName(Matchers.equalTo(DatePicker.class.getName())))
        .perform(PickerActions.setDate(YEAR, MONTH_OF_YEAR, DAY_OF_MONTH));
    onView(withId(android.R.id.button1)).perform(click());
    onView(withId(R.id.start_date_edit_text)).check(matches(withText(DATE)));

    onView(withId(R.id.start_time_edit_text)).perform(click());
    onView(withClassName(Matchers.equalTo(TimePicker.class.getName())))
        .perform(PickerActions.setTime(HOURS, MINUTES));
    onView(withId(android.R.id.button1)).perform(click());
    onView(withId(R.id.start_time_edit_text)).check(matches(withText(TIME)));

    onView(withId(R.id.meeting_event_creation_confirm)).check(matches(isEnabled()));
  }

  @Test
  @Ignore("TODO: solve issue with GithubActions Emulator")
  public void confirmAddsEventToEventList() {
    final String RANDOM_EVENT_TITLE = "Random meeting title";
    onView(withId(R.id.meeting_title_text)).perform(typeText(RANDOM_EVENT_TITLE));

    onView(withId(R.id.start_date_edit_text)).perform(click());
    onView(withClassName(Matchers.equalTo(DatePicker.class.getName())))
        .perform(PickerActions.setDate(YEAR, MONTH_OF_YEAR, DAY_OF_MONTH));
    onView(withId(android.R.id.button1)).perform(click());
    onView(withId(R.id.start_date_edit_text)).check(matches(withText(DATE)));

    onView(withId(R.id.start_time_edit_text)).perform(click());
    onView(withClassName(Matchers.equalTo(TimePicker.class.getName())))
        .perform(PickerActions.setTime(HOURS, MINUTES));
    onView(withId(android.R.id.button1)).perform(click());
    onView(withId(R.id.start_time_edit_text)).check(matches(withText(TIME)));

    onView(withId(R.id.meeting_event_creation_confirm)).check(matches(isEnabled()));
    onView(withId(R.id.meeting_event_creation_confirm)).perform(click());

    activityScenarioRule
        .getScenario()
        .onActivity(
            activity -> {
              PoPApplication app = (PoPApplication) activity.getApplication();
              List<Event> events =
                  app.getCurrentLao().map(Lao::getEvents).orElse(new ArrayList<>());
              List<String> eventsName =
                  events.stream().map(Event::getName).collect(Collectors.toList());
              Assert.assertThat(RANDOM_EVENT_TITLE, isIn(eventsName));
            });
  }
}
