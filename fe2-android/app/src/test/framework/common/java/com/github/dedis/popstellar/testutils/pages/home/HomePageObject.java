package com.github.dedis.popstellar.testutils.pages.home;

import androidx.annotation.IdRes;
import androidx.test.espresso.ViewInteraction;

import com.github.dedis.popstellar.R;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import android.view.View;

/**
 * Page object of {@link com.github.dedis.popstellar.ui.home.HomeActivity}
 *
 * <p>Creation : 26.03.2022
 */
public class HomePageObject {

  private HomePageObject() {
    throw new IllegalStateException("Page object");
  }

  public static ViewInteraction fragmentContainer() {
    return onView(withId(R.id.fragment_container_home));
  }

  public static ViewInteraction homeButton() {
    return onView(withId(R.id.home_home_menu));
  }

  public static ViewInteraction joinButton() {
    return onView(withId(R.id.home_join_button));
  }

  public static ViewInteraction createButton() {
    return onView(withId(R.id.home_create_button));
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

  public static ViewInteraction walletSetupMenuItem(){
      return onView(withText(R.string.wallet_setup));
  }

  public static ViewInteraction walletLogOutMenuItem(){
      return onView(withText(R.string.logout_title));
  }

  @IdRes
  public static int homeFragmentId() {
    return R.id.fragment_home;
  }

  @IdRes
  public static int homeFragmentContainerId() {
    return R.id.fragment_container_home;
  }
}
