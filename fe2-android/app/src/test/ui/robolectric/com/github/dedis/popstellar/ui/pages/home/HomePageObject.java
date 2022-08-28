package com.github.dedis.popstellar.ui.pages.home;

import androidx.annotation.IdRes;
import androidx.test.espresso.ViewInteraction;

import com.github.dedis.popstellar.R;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static com.github.dedis.popstellar.testutils.UITestUtils.dialogPositiveButton;
import static com.github.dedis.popstellar.ui.pages.home.WalletPageObject.confirmButton;
import static com.github.dedis.popstellar.ui.pages.home.WalletPageObject.newWalletButton;

/**
 * Page object of {@Link HomeActivity}
 *
 * <p>Creation : 26.03.2022
 */
public class HomePageObject {

  public static ViewInteraction fragmentContainer() {
    return onView(withId(R.id.fragment_container_home));
  }

  public static ViewInteraction homeButton() {
    return onView(withId(R.id.home_home_menu));
  }

  public static ViewInteraction connectButton() {
    return onView(withId(R.id.home_connect_menu));
  }

  public static ViewInteraction launchButton() {
    return onView(withId(R.id.home_launch_menu));
  }

  public static ViewInteraction walletButton() {
    return onView(withId(R.id.home_wallet_menu));
  }

  public static ViewInteraction socialMediaButton() {
    return onView(withId(R.id.home_social_media_menu));
  }

  public static ViewInteraction navBar() {
    return onView(withId(R.id.home_nav_bar));
  }

  @IdRes
  public static int homeFragmentId() {
    return R.id.fragment_home;
  }

  @IdRes
  public static int homeFragmentContainerId() {
    return R.id.fragment_container_home;
  }

  public static void initializeWallet() {
    walletButton().perform(click());
    newWalletButton().perform(click());
    confirmButton().perform(click());
    dialogPositiveButton().performClick();
  }
}
