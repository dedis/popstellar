package com.github.dedis.popstellar.ui.home;

import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.assertThat;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withChild;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static com.github.dedis.popstellar.testutils.UITestUtils.dialogNegativeButton;
import static com.github.dedis.popstellar.testutils.UITestUtils.dialogPositiveButton;
import static com.github.dedis.popstellar.ui.pages.home.HomePageObject.fragmentContainer;
import static com.github.dedis.popstellar.ui.pages.home.HomePageObject.homeFragmentContainerId;
import static com.github.dedis.popstellar.ui.pages.home.HomePageObject.navBar;
import static com.github.dedis.popstellar.ui.pages.home.WalletPageObject.confirmButton;
import static com.github.dedis.popstellar.ui.pages.home.WalletPageObject.seedWalletText;
import static com.github.dedis.popstellar.ui.pages.home.WalletPageObject.walletContentFragmentId;
import static com.github.dedis.popstellar.ui.pages.home.WalletPageObject.walletSeedWarningText;
import static org.hamcrest.Matchers.allOf;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.github.dedis.popstellar.testutils.fragment.ActivityFragmentScenarioRule;
import com.github.dedis.popstellar.ui.wallet.SeedWalletFragment;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoTestRule;

import dagger.hilt.android.testing.HiltAndroidRule;
import dagger.hilt.android.testing.HiltAndroidTest;

@HiltAndroidTest
@RunWith(AndroidJUnit4.class)
public class WalletSeedFragmentTest {

  @Rule(order = 0)
  public final MockitoTestRule mockitoRule = MockitoJUnit.testRule(this);

  @Rule(order = 1)
  public final HiltAndroidRule hiltRule = new HiltAndroidRule(this);

  @Rule(order = 3)
  public final ActivityFragmentScenarioRule<HomeActivity, SeedWalletFragment> fragmentRule =
      ActivityFragmentScenarioRule.launchIn(
          HomeActivity.class,
          homeFragmentContainerId(),
          SeedWalletFragment.class,
          SeedWalletFragment::newInstance);

  @Before
  public void setup() {
    hiltRule.inject();
  }

  @Test
  public void navBarIsDisplayed() {
    navBar().check(matches(isDisplayed()));
  }

  @Test
  public void warningTextIsDisplayed() {
    walletSeedWarningText().check(matches(isDisplayed()));
  }

  @Test
  public void seedWalletTextIsDisplayed() {
    seedWalletText().check(matches(isDisplayed()));
  }

  @Test
  public void confirmButtonIsDisplayed() {
    confirmButton().check(matches(isDisplayed()));
  }

  @Test
  public void confirmButtonDisplaysDialogOnClick() {
    confirmButton().perform(click());
    assertThat(dialogPositiveButton(), allOf(withText("YES"), isDisplayed()));
    assertThat(dialogNegativeButton(), allOf(withText("Cancel"), isDisplayed()));
  }

  @Test
  public void acceptingDialogConfirmButtonsOpensWalletContentFragment() {
    confirmButton().perform(click());
    dialogPositiveButton().performClick();
    fragmentContainer().check(matches(withChild(withId(walletContentFragmentId()))));
  }
}
