package com.github.dedis.popstellar.testutils.pages.digitalcash;

import androidx.test.espresso.ViewInteraction;

import com.github.dedis.popstellar.R;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

/**
 * Page object of {@link com.github.dedis.popstellar.ui.digitalcash.DigitalCashHomeFragment}
 *
 * <p>Creation : 20.05.2022
 */
public class DigitalCashPageObject {

  private DigitalCashPageObject() {
    throw new IllegalStateException("Page object");
  }

  public static ViewInteraction sendButton() {
    return onView(withId(R.id.digital_cash_send_button));
  }

  public static ViewInteraction receiveButton() {
    return onView(withId(R.id.digital_cash_receive_button));
  }

  public static ViewInteraction historyButton() {
    return onView(withId(R.id.history_menu_toolbar));
  }

  public static ViewInteraction issueButton() {
    return onView(withId(R.id.issue_button));
  }
}
