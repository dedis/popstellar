package com.github.dedis.student20_pop;

import android.Manifest;
import android.view.View;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.rule.GrantPermissionRule;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.RootMatchers.withDecorView;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static org.hamcrest.Matchers.not;

public class MainActivityTest {

  @Rule
  public final GrantPermissionRule rule = GrantPermissionRule.grant(Manifest.permission.CAMERA);

  @Rule
  public ActivityScenarioRule<MainActivity> activityScenarioRule =
      new ActivityScenarioRule<>(MainActivity.class);

  private View decorView;

  @Before
  public void setUp() {
    activityScenarioRule
        .getScenario()
        .onActivity(
            new ActivityScenario.ActivityAction<MainActivity>() {

              /**
               * This method is invoked on the main thread with the reference to the Activity.
               *
               * @param activity an Activity instrumented by the {@link ActivityScenario}. It never
               *     be null.
               */
              @Override
              public void perform(MainActivity activity) {
                decorView = activity.getWindow().getDecorView();
              }
            });
  }

  @Test
  public void onClickHomeTest() {
    onView(withId(R.id.tab_home)).perform(click());
    onView(withId(R.id.fragment_home)).check(matches(isDisplayed()));
  }

  @Test
  @Ignore("TODO: solve issue with emulator")
  public void onClickConnectTest() {
    onView(withId(R.id.tab_connect)).perform(click());
    onView(withId(R.id.fragment_qrcode)).check(matches(isDisplayed()));
  }

  @Test
  public void onClickLaunchTest() {
    onView(withId(R.id.tab_launch)).perform(click());
    onView(withId(R.id.fragment_launch)).check(matches(isDisplayed()));
  }

  @Test
  @Ignore("TODO : Modify test because it requires connection to backend")
  public void onClickLaunchLAOTest() {
    onView(withId(R.id.tab_launch)).perform(click());
    onView(withId(R.id.entry_box_launch)).perform(typeText("Random Name"), closeSoftKeyboard());
    onView(withId(R.id.button_launch)).perform(click());
    onView(withId(R.id.fragment_organizer)).check(matches(isDisplayed()));
  }

  @Test
  public void clickOnLaunchWithEmptyNameShowsToast() {
    String expectedWarning =
        getApplicationContext().getString(R.string.exception_message_empty_lao_name);
    onView(withId(R.id.tab_launch)).perform(click());
    onView(withId(R.id.button_launch)).perform(click());
    onView(withText(expectedWarning))
        .inRoot(withDecorView(not(decorView)))
        .check(matches(isDisplayed()));
  }

  @Test
  public void onClickCancelLAOTest() {
    onView(withId(R.id.tab_launch)).perform(click());
    onView(withId(R.id.button_cancel_launch)).perform(click());
    onView(withId(R.id.fragment_home)).check(matches(isDisplayed()));
  }
}
