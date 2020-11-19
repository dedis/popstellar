package com.github.dedis.student20_pop;

import android.Manifest;

import androidx.test.core.app.ActivityScenario;
import androidx.test.rule.GrantPermissionRule;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

public class MainActivityTest {

    @Rule public final GrantPermissionRule rule = GrantPermissionRule.grant(Manifest.permission.CAMERA);

    @Before
    public void launchActivity() {
        ActivityScenario.launch(MainActivity.class);
    }

    @Test
    public void onClickHomeTest() {
        onView(withId(R.id.tab_home)).perform(click());
        onView(withId(R.id.fragment_home)).check(matches(isDisplayed()));
    }

    @Test
    public void onClickConnectTest() {
        onView(withId(R.id.tab_connect)).perform(click());
        onView(withId(R.id.fragment_connect)).check(matches(isDisplayed()));
    }

    @Test
    public void onClickLaunchTest() {
        onView(withId(R.id.tab_launch)).perform(click());
        onView(withId(R.id.fragment_launch)).check(matches(isDisplayed()));
    }

    @Test
    public void onClickLaunchLAOTest() {
        onView(withId(R.id.tab_launch)).perform(click());
        onView(withId(R.id.button_launch)).perform(click());
        onView(withId(R.id.fragment_home)).check(matches(isDisplayed()));
    }

    @Test
    public void onClickCancelLAOTest() {
        onView(withId(R.id.tab_launch)).perform(click());
        onView(withId(R.id.button_cancel_launch)).perform(click());
        onView(withId(R.id.fragment_home)).check(matches(isDisplayed()));
    }

    @Ignore("Need to set testingAttendee to true in MainActivity")
    @Test
    public void onClickPropertiesTest(){
        onView(withId(R.id.tab_properties)).perform(click());
        onView(withId(R.id.properties_view)).check(matches(isDisplayed()));
    }

    @Ignore("Need to set testingAttendee to true in MainActivity")
    @Test
    public void onClickListEventsTest(){
        onView(withText("Past Events")).perform(click());
        onView(withText("Present Events")).perform(click());
        onView(withText("Future Events")).perform(click());
        onView(withId(R.id.event_layout)).check(matches(isDisplayed()));
    }
}
