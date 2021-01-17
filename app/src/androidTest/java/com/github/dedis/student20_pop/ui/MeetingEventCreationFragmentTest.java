package com.github.dedis.student20_pop.ui;

import android.view.View;
import android.widget.DatePicker;
import android.widget.TimePicker;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.contrib.PickerActions;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.rules.ActivityScenarioRule;

import com.github.dedis.student20_pop.OrganizerActivity;
import com.github.dedis.student20_pop.PoPApplication;
import com.github.dedis.student20_pop.R;
import com.github.dedis.student20_pop.model.event.Event;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.List;
import java.util.stream.Collectors;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withClassName;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.isIn;
import static org.hamcrest.Matchers.not;

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
    private View decorView;

    @Before
    public void setUp() {
        activityScenarioRule.getScenario().onActivity(new ActivityScenario.ActivityAction<OrganizerActivity>() {

            /**
             * This method is invoked on the main thread with the reference to the Activity.
             *
             * @param activity an Activity instrumented by the {@link ActivityScenario}. It never be null.
             */
            @Override
            public void perform(OrganizerActivity activity) {
                decorView = activity.getWindow().getDecorView();
            }
        });

        onView(allOf(withId(R.id.add_future_event_button), withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE))).perform(click());
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
    public void confirmButtonIsEnabledWhenRequiredFieldsFilled() {
        onView(withId(R.id.meeting_title_text)).perform(typeText("Random meeting title"));

        onView(withId(R.id.start_date_editText)).perform(click());
        onView(withClassName(Matchers.equalTo(DatePicker.class.getName()))).perform(PickerActions.setDate(YEAR, MONTH_OF_YEAR, DAY_OF_MONTH));
        onView(withId(android.R.id.button1)).perform(click());
        onView(withId(R.id.start_date_editText)).check(matches(withText(DATE)));

        onView(withId(R.id.start_time_editText)).perform(click());
        onView(withClassName(Matchers.equalTo(TimePicker.class.getName()))).perform(PickerActions.setTime(HOURS, MINUTES));
        onView(withId(android.R.id.button1)).perform(click());
        onView(withId(R.id.start_time_editText)).check(matches(withText(TIME)));

        onView(withId(R.id.meeting_event_creation_confirm)).check(matches(isEnabled()));
    }

    @Test
    public void confirmAddsEventToEventList() {
        final String RANDOM_EVENT_TITLE = "Random meeting title";
        onView(withId(R.id.meeting_title_text)).perform(typeText(RANDOM_EVENT_TITLE));

        onView(withId(R.id.start_date_editText)).perform(click());
        onView(withClassName(Matchers.equalTo(DatePicker.class.getName()))).perform(PickerActions.setDate(YEAR, MONTH_OF_YEAR, DAY_OF_MONTH));
        onView(withId(android.R.id.button1)).perform(click());
        onView(withId(R.id.start_date_editText)).check(matches(withText(DATE)));

        onView(withId(R.id.start_time_editText)).perform(click());
        onView(withClassName(Matchers.equalTo(TimePicker.class.getName()))).perform(PickerActions.setTime(HOURS, MINUTES));
        onView(withId(android.R.id.button1)).perform(click());
        onView(withId(R.id.start_time_editText)).check(matches(withText(TIME)));

        onView(withId(R.id.meeting_event_creation_confirm)).check(matches(isEnabled()));
        onView(withId(R.id.meeting_event_creation_confirm)).perform(click());

        activityScenarioRule.getScenario().onActivity(
                activity -> {
                    PoPApplication app = (PoPApplication) activity.getApplication();
                    List<Event> events = app.getCurrentLaoUnsafe().getEvents();
                    List<String> eventsName = events.stream().map(Event::getName).collect(Collectors.toList());
                    Assert.assertThat(RANDOM_EVENT_TITLE, isIn(eventsName));
                }
        );
    }
}
