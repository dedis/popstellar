package com.github.dedis.popstellar.testutils.pages.home;

import androidx.test.espresso.ViewInteraction;

import com.github.dedis.popstellar.R;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

public class ConnectingPageObject {

  private ConnectingPageObject() {
    throw new IllegalStateException("Page object");
  }

  public static ViewInteraction connectingText() {
    return onView(withId(R.id.connecting_text));
  }

  public static ViewInteraction laoConnectingText() {
    return onView(withId(R.id.connecting_lao));
  }

  public static ViewInteraction progressBar() {
    return onView(withId(R.id.connecting_progress_bar));
  }

  public static ViewInteraction cancelButton() {
    return onView(withId(R.id.button_cancel_connecting));
  }
}
