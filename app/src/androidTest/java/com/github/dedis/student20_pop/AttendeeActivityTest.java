package com.github.dedis.student20_pop;

import androidx.test.core.app.ActivityScenario;

import org.junit.Before;
import org.junit.Test;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

public class AttendeeActivityTest {

    @Before
    public void launchActivity() {
        ActivityScenario.launch(AttendeeActivity.class);
    }

    @Test
    public void onClickHomeTest() {
        onView(withId(R.id.tab_home)).perform(click());
        onView(withId(R.id.fragment_home)).check(matches(isDisplayed()));
    }

    @Test
    public void onClickPropertiesTest(){
        onView(withId(R.id.tab_properties)).perform(click());
        onView(withId(R.id.properties_view)).check(matches(isDisplayed()));
    }

    @Test
    public void onClickListEventsTest(){
        onView(withText("Past Events")).perform(click());
        onView(withText("Present Events")).perform(click());
        onView(withText("Future Events")).perform(click());
        onView(withId(R.id.event_layout)).check(matches(isDisplayed()));
    }
}
