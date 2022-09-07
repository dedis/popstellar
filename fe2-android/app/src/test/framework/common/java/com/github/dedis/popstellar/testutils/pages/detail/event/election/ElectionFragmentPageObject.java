package com.github.dedis.popstellar.testutils.pages.detail.event.election;

import androidx.test.espresso.ViewInteraction;

import com.github.dedis.popstellar.R;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

public class ElectionFragmentPageObject {

  private ElectionFragmentPageObject() {
    throw new IllegalStateException("Page object");
  }

  public static ViewInteraction electionFragmentTitle() {
    return onView(withId(R.id.election_fragment_title));
  }

  public static ViewInteraction electionFragmentStatus() {
    return onView(withId(R.id.election_fragment_status));
  }

  public static ViewInteraction electionFragmentStartTime() {
    return onView(withId(R.id.election_fragment_start_time));
  }

  public static ViewInteraction electionFragmentEndTime() {
    return onView(withId(R.id.election_fragment_end_time));
  }

  public static ViewInteraction electionActionButton() {
    return onView(withId(R.id.election_action_button));
  }

  public static ViewInteraction electionManagementButton() {
    return onView(withId(R.id.election_management_button));
  }
}
