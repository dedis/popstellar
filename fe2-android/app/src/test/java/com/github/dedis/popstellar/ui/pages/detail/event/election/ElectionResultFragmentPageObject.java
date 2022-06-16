package com.github.dedis.popstellar.ui.pages.detail.event.election;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import androidx.test.espresso.ViewInteraction;

import com.github.dedis.popstellar.R;

public class ElectionResultFragmentPageObject {

  public static ViewInteraction electionResultLaoTitle() {
    return onView(withId(R.id.election_result_lao_name));
  }

  public static ViewInteraction electionResultElectionTitle() {
    return onView(withId(R.id.election_result_election_title));
  }

  public static ViewInteraction electionResultPage() {
    return onView(withId(R.id.election_result_pager));
  }
}
