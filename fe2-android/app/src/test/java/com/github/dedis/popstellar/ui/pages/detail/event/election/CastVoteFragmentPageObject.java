package com.github.dedis.popstellar.ui.pages.detail.event.election;

import androidx.test.espresso.ViewInteraction;

import com.github.dedis.popstellar.R;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

public class CastVoteFragmentPageObject {

  public static ViewInteraction castVoteLaoTitle() {
    return onView(withId(R.id.cast_vote_lao_name));
  }

  public static ViewInteraction castVoteElectionName() {
    return onView(withId(R.id.cast_vote_election_name));
  }

  public static ViewInteraction castVotePager() {
    return onView(withId(R.id.cast_vote_pager));
  }

  public static ViewInteraction castVoteButton() {
    return onView(withId(R.id.cast_vote_pager));
  }
}
