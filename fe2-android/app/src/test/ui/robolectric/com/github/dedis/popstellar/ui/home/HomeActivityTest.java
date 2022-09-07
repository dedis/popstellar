package com.github.dedis.popstellar.ui.home;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnit;

import dagger.hilt.android.testing.HiltAndroidRule;
import dagger.hilt.android.testing.HiltAndroidTest;

import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static com.github.dedis.popstellar.testutils.UITestUtils.dialogPositiveButton;
import static com.github.dedis.popstellar.testutils.pages.home.HomePageObject.*;
import static com.github.dedis.popstellar.testutils.pages.home.LaunchPageObject.launchFragmentId;
import static com.github.dedis.popstellar.testutils.pages.home.WalletPageObject.*;

@HiltAndroidTest
@RunWith(AndroidJUnit4.class)
public class HomeActivityTest {

  //  Hilt rule
  private final HiltAndroidRule hiltAndroidRule = new HiltAndroidRule(this);

  // Activity scenario rule that starts the activity.
  public ActivityScenarioRule<HomeActivity> activityScenarioRule =
      new ActivityScenarioRule<>(HomeActivity.class);

  @Rule
  public final RuleChain rule =
      RuleChain.outerRule(MockitoJUnit.testRule(this))
          .around(hiltAndroidRule)
          .around(activityScenarioRule);

  @Test
  public void homeButtonStaysHome() {
    homeButton().perform(click());
    fragmentContainer().check(matches(withChild(withId(homeFragmentId()))));
  }

  @Test
  public void connectButtonStaysHomeWithoutInitializedWallet() {
    connectButton().perform(click());
    fragmentContainer().check(matches(withChild(withId(homeFragmentId()))));
  }

  @Test
  public void launchButtonStaysHomeWithoutInitializedWallet() {
    launchButton().perform(click());
    fragmentContainer().check(matches(withChild(withId(homeFragmentId()))));
  }

  @Test
  public void socialMediaButtonStaysHomeWithoutInitializedWallet() {
    socialMediaButton().perform(click());
    fragmentContainer().check(matches(withChild(withId(homeFragmentId()))));
  }

  @Test
  public void walletButtonOpensWalletFragment() {
    walletButton().perform(click());
    fragmentContainer().check(matches(withChild(withId(walletFragmentId()))));
  }

  @Test
  public void walletButtonIsDisplayed() {
    walletButton().check(matches(isDisplayed()));
  }

  @Test
  public void navBarIsDisplayed() {
    navBar().check(matches(isDisplayed()));
  }

  @Test
  public void launchButtonBringsToLaunchScreenWithInitializedWallet() {
    initializeWallet();
    launchButton().perform(click());
    fragmentContainer().check(matches(withChild(withId(launchFragmentId()))));
  }

  public static void initializeWallet() {
    walletButton().perform(click());
    newWalletButton().perform(click());
    confirmButton().perform(click());
    dialogPositiveButton().performClick();
  }
}
