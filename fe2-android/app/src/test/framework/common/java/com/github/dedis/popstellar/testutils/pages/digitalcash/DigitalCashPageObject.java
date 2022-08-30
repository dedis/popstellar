package com.github.dedis.popstellar.testutils.pages.digitalcash;

import androidx.annotation.IdRes;
import androidx.test.espresso.ViewInteraction;

import com.github.dedis.popstellar.R;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

/**
 * Page object of {@Link DigitalCashMainActivity}
 *
 * <p>Creation : 20.05.2022
 */
public class DigitalCashPageObject {

  public static ViewInteraction fragmentContainer() {
    return onView(withId(R.id.fragment_container_digital_cash));
  }

  public static ViewInteraction sendButton() {
    return onView(withId(R.id.digital_cash_send_menu));
  }

  public static ViewInteraction receiveButton() {
    return onView(withId(R.id.digital_cash_receive_menu));
  }

  public static ViewInteraction historyButton() {
    return onView(withId(R.id.digital_cash_history_menu));
  }

  public static ViewInteraction homeButton() {
    return onView(withId(R.id.digital_cash_home_menu));
  }

  public static ViewInteraction issueButton() {
    return onView(withId(R.id.digital_cash_issue_menu));
  }

  public static ViewInteraction navBar() {
    return onView(withId(R.id.digital_cash_nav_bar));
  }

  @IdRes
  public static int digitalCashFragmentId() {
    return R.id.fragment_digital_cash_home;
  }

  @IdRes
  public static int digitalCashFragmentContainerId() {
    return R.id.fragment_container_digital_cash;
  }
}
