package com.github.dedis.popstellar.testutils.pages.lao.popcha;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import androidx.test.espresso.ViewInteraction;
import com.github.dedis.popstellar.R;

/**
 * This is the page object of PoPCHAHomeFragment
 *
 * <p>It makes writing test easier
 */
public class PoPCHAHomePageObject {

  private PoPCHAHomePageObject() {
    throw new IllegalStateException("Page object");
  }

  public static ViewInteraction getRootView() {
    return onView(isRoot());
  }

  public static ViewInteraction getEventListFragment() {
    return onView(withId(R.id.fragment_event_list));
  }

  public static ViewInteraction getHeader() {
    return onView(withId(R.id.popcha_header));
  }

  public static ViewInteraction getScanner() {
    return onView(withId(R.id.popcha_scanner));
  }

  public static ViewInteraction getScannerFragment() {
    return onView(withId(R.id.fragment_qr_scanner));
  }
}
