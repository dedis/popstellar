package com.github.dedis.student20_pop;

import android.Manifest;
import android.app.Activity;

import androidx.test.core.app.ActivityScenario;
import androidx.test.rule.GrantPermissionRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.Collections;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.assertThat;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;

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
        onView(withId(R.id.entry_box_launch)).perform(typeText("LAO"));
        onView(withId(R.id.entry_box_launch)).perform(closeSoftKeyboard());
        onView(withId(R.id.button_launch)).perform(click());
        onView(withId(R.id.fragment_organizer)).check(matches(isDisplayed()));
    }

    @Test
    public void onClickCancelLAOTest() {
        onView(withId(R.id.tab_launch)).perform(click());
        onView(withId(R.id.button_cancel_launch)).perform(click());
        onView(withId(R.id.fragment_home)).check(matches(isDisplayed()));
    }
}
