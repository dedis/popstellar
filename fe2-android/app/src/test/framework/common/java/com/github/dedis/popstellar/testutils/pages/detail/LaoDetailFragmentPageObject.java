package com.github.dedis.popstellar.testutils.pages.detail;

import androidx.test.espresso.ViewInteraction;

import com.github.dedis.popstellar.R;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

public class LaoDetailFragmentPageObject {

  private LaoDetailFragmentPageObject() {
    throw new IllegalStateException("Page object");
  }

  public static ViewInteraction addEventButton() {
    return onView(withId(R.id.add_event));
  }

  public static ViewInteraction addElectionButton() {
    return onView(withId(R.id.add_election));
  }

  public static ViewInteraction addElectionText() {
    return onView(withId(R.id.add_election_text));
  }

  public static ViewInteraction addRollCallButton() {
    return onView(withId(R.id.add_roll_call));
  }

  public static ViewInteraction addRollCallText() {
    return onView(withId(R.id.add_roll_call_text));
  }

  public static ViewInteraction eventList() {
    return onView(withId(R.id.event_list));
  }
}
