package com.github.dedis.popstellar.ui.home;

import android.widget.Button;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.github.dedis.popstellar.testutils.fragment.ActivityFragmentScenarioRule;
import com.github.dedis.popstellar.ui.wallet.WalletFragment;

import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoTestRule;

import dagger.hilt.android.testing.HiltAndroidRule;
import dagger.hilt.android.testing.HiltAndroidTest;

import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static com.github.dedis.popstellar.testutils.UITestUtils.dialogNegativeButton;
import static com.github.dedis.popstellar.testutils.UITestUtils.dialogPositiveButton;
import static com.github.dedis.popstellar.ui.pages.home.HomePageObject.*;
import static com.github.dedis.popstellar.ui.pages.home.WalletPageObject.*;
import static org.hamcrest.Matchers.allOf;

@HiltAndroidTest
@RunWith(AndroidJUnit4.class)
public class WalletFragmentTest {

  @Rule(order = 0)
  public final MockitoTestRule mockitoRule = MockitoJUnit.testRule(this);

  @Rule(order = 1)
  public final HiltAndroidRule hiltRule = new HiltAndroidRule(this);

  @Rule(order = 3)
  public final ActivityFragmentScenarioRule<HomeActivity, WalletFragment> fragmentRule =
      ActivityFragmentScenarioRule.launchIn(
          HomeActivity.class,
          homeFragmentContainerId(),
          WalletFragment.class,
          WalletFragment::newInstance);

  @Before
  public void setup() {
    hiltRule.inject();
  }

  @Test
  public void welcomeWalletTextsIsDisplayed() {
    welcomeText1().check(matches(isDisplayed()));
    welcomeText2().check(matches(isDisplayed()));
    welcomeText3().check(matches(isDisplayed()));
  }

  @Test
  public void navBarIsDisplayed() {
    navBar().check(matches(isDisplayed()));
  }

  @Test
  public void actionButtonsAreDisplayed() {
    newWalletButton().check(matches(isDisplayed()));
    iOwnASeedButton().check(matches(isDisplayed()));
  }

  @Test
  public void newWalletOpensWalletSeedFragment() {
    newWalletButton().perform(click());
    fragmentContainer().check(matches(withChild(withId(walletSeedFragmentId()))));
  }

  @Test
  public void iOwnASeedButtonDisplaysDialogOnClick() {
    iOwnASeedButton().perform(click());
    Button setupWallet = dialogPositiveButton();
    assertThat(setupWallet, allOf(withText("SET UP WALLET"), isDisplayed()));
    assertThat(dialogNegativeButton(), allOf(withText("CANCEL"), isDisplayed()));
  }

  @Test
  public void logoutWalletUITest() {
    newWalletButton().perform(click());
    confirmButton().perform(click());
    dialogPositiveButton().performClick();
    logoutButton().check(matches(isDisplayed()));
    logoutButton().perform(click());
    assertThat(dialogPositiveButton(), allOf(withText("CONFIRM"), isDisplayed()));
    assertThat(dialogNegativeButton(), allOf(withText("CANCEL"), isDisplayed()));
    dialogPositiveButton().performClick();
    fragmentContainer().check(matches(withChild(withId(walletFragmentId()))));
  }
}
