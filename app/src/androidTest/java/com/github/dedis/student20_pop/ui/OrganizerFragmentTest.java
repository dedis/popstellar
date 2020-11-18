package com.github.dedis.student20_pop.ui;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.matcher.ViewMatchers;

import com.github.dedis.student20_pop.MainActivity;
import com.github.dedis.student20_pop.R;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;

public class OrganizerFragmentTest {

    @Before
    public void launchActivity() {
        ActivityScenario.launch(MainActivity.class);
    }

    //These tests only pass when testingOrganizer is true in MainActivity
    @Test
    public void onClickPropertiesTest() {
        onView(withId(R.id.tab_properties)).perform(click());
        onView(withId(R.id.properties_view)).check(matches(isDisplayed()));
    }

    @Test
    public void onClickListEventsTest() {
        onView(withText(getApplicationContext().getString(R.string.past_events))).perform(click());
        onView(withText(getApplicationContext().getString(R.string.present_events))).perform(click());
        onView(withText(getApplicationContext().getString(R.string.future_events))).perform(click());
        onView(withId(R.id.event_layout)).check(matches(isDisplayed()));
    }

    @Test
    public void clickOnAddEventButtonOpensDialog() {
        onView(allOf(withId(R.id.add_future_event_button),
                withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
                .perform(click());
        onView(withText(getApplicationContext().getString(R.string.meeting_event))).check(matches(isDisplayed()));
    }

    @Test
    public void canCancelAddEvent() {
        onView(allOf(withId(R.id.add_future_event_button),
                withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
                .perform(click());
        onView(withText(getApplicationContext().getString(R.string.meeting_event))).check(matches(isDisplayed()));
        onView(withId(android.R.id.button2)).perform(click());
    }

    @Test
    @Ignore("TODO : Check that the corresponding Fragment has been launched")
    public void canLaunchCreateMeetingEventFragment() {
        onView(allOf(withId(R.id.add_future_event_button),
                withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
                .perform(click());
        onView(withText(getApplicationContext().getString(R.string.meeting_event))).check(matches(isDisplayed()));
        onView(withText(getApplicationContext().getString(R.string.meeting_event))).perform(click());
    }

    @Test
    @Ignore("TODO : Check that the corresponding Fragment has been launched")
    public void canLaunchCreateRollCallEventFragment() {
        onView(allOf(withId(R.id.add_future_event_button),
                withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
                .perform(click());
        onView(withText(getApplicationContext().getString(R.string.roll_call_event))).check(matches(isDisplayed()));
        onView(withText(getApplicationContext().getString(R.string.roll_call_event))).perform(click());
    }

    @Test
    @Ignore("TODO : Check that the corresponding Fragment has been launched")
    public void canLaunchCreatePollEventFragment() {
        onView(allOf(withId(R.id.add_future_event_button),
                withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
                .perform(click());
        onView(withText(getApplicationContext().getString(R.string.poll_event))).check(matches(isDisplayed()));
        onView(withText(getApplicationContext().getString(R.string.roll_call_event))).perform(click());
    }
}
