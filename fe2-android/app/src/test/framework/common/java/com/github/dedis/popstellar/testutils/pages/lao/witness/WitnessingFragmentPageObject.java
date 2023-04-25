package com.github.dedis.popstellar.testutils.pages.lao.witness;

import androidx.test.espresso.ViewInteraction;

import com.github.dedis.popstellar.R;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

/**
 * This is the page object of WitnessingFragment
 *
 * <p>It makes writing test easier
 */
public class WitnessingFragmentPageObject {
  private WitnessingFragmentPageObject() {
    throw new IllegalStateException("Page object");
  }

  public static ViewInteraction getRootView() {
    return onView(isRoot());
  }

  public static ViewInteraction getEventListFragment() {
    return onView(withId(R.id.fragment_event_list));
  }
}
