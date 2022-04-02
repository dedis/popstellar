package com.github.dedis.popstellar.ui.home;

import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withChild;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static com.github.dedis.popstellar.ui.pages.home.HomePageObject.fragmentContainer;
import static com.github.dedis.popstellar.ui.pages.home.HomePageObject.homeButton;
import static com.github.dedis.popstellar.ui.pages.home.HomePageObject.connectButton;
import static com.github.dedis.popstellar.ui.pages.home.HomePageObject.walletButton;
import static com.github.dedis.popstellar.ui.pages.home.HomePageObject.socialMediaButton;
import static com.github.dedis.popstellar.ui.pages.home.HomePageObject.launchButton;
import static com.github.dedis.popstellar.ui.pages.home.HomePageObject.homeFragmentId;
import static com.github.dedis.popstellar.ui.pages.home.WalletPageObject.walletFragmentId;
import static com.github.dedis.popstellar.ui.pages.home.HomePageObject.navBar;


import androidx.test.ext.junit.rules.ActivityScenarioRule;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import dagger.hilt.android.testing.HiltAndroidRule;
import dagger.hilt.android.testing.HiltAndroidTest;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnit;


@HiltAndroidTest
@RunWith(AndroidJUnit4.class)
public class HomeActivityTest {

  //  Hilt rule
  private final HiltAndroidRule hiltAndroidRule = new HiltAndroidRule(this);

  // Activity scenario rule that starts the activity.
  public ActivityScenarioRule<HomeActivity> activityScenarioRule =
      new ActivityScenarioRule<HomeActivity>(HomeActivity.class);

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
  public void walletButtonIsDisplayed(){
    walletButton().check(matches(isDisplayed()));
  }

  @Test
  public void navBarIsDisplayed(){
    navBar().check(matches(isDisplayed()));
  }
}
