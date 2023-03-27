package com.github.dedis.popstellar.testutils.pages.lao.token;

import androidx.test.espresso.ViewInteraction;

import com.github.dedis.popstellar.R;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

public class TokenPageObject {
  public static ViewInteraction tokenTextView() {
    return onView(withId(R.id.token_text_view));
  }

  public static ViewInteraction getRootView() {
    return onView(isRoot());
  }

  public static ViewInteraction getEventListFragment() {
    return onView(withId(R.id.fragment_event_list));
  }
}
