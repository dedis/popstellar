package com.github.dedis.student20_pop.ui;

import androidx.test.core.app.ActivityScenario;
import com.github.dedis.student20_pop.AttendeeActivity;
import com.github.dedis.student20_pop.R;
import org.junit.Before;
import org.junit.Test;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;

public class AttendeeFragmentTest {

  @Before
  public void launchActivity() {
    ActivityScenario.launch(AttendeeActivity.class);
  }

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
}
