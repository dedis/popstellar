package com.github.dedis.popstellar.ui.pages.home;

import androidx.annotation.IdRes;
import androidx.test.espresso.ViewInteraction;

import com.github.dedis.popstellar.R;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

public class ConnectingPageObject {

  @IdRes
  public static int connectingFragmentContainerId() {
    return R.id.fragment_container_connecting;
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
