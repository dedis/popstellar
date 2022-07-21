package com.github.dedis.popstellar.ui.home;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.github.dedis.popstellar.testutils.fragment.ActivityFragmentScenarioRule;
import com.github.dedis.popstellar.ui.wallet.ContentWalletFragment;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoTestRule;

import dagger.hilt.android.testing.HiltAndroidRule;
import dagger.hilt.android.testing.HiltAndroidTest;

import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static com.github.dedis.popstellar.ui.pages.home.HomePageObject.homeFragmentContainerId;
import static com.github.dedis.popstellar.ui.pages.home.WalletPageObject.tokenTitle;
import static com.github.dedis.popstellar.ui.pages.home.WalletPageObject.walletContentText1;
import static com.github.dedis.popstellar.ui.pages.home.WalletPageObject.walletContentText2;

@HiltAndroidTest
@RunWith(AndroidJUnit4.class)
public class WalletContentFragmentTest {

  @Rule(order = 0)
  public final MockitoTestRule mockitoRule = MockitoJUnit.testRule(this);

  @Rule(order = 1)
  public final HiltAndroidRule hiltRule = new HiltAndroidRule(this);

  @Rule(order = 3)
  public final ActivityFragmentScenarioRule<HomeActivity, ContentWalletFragment> fragmentRule =
      ActivityFragmentScenarioRule.launchIn(
          HomeActivity.class,
          homeFragmentContainerId(),
          ContentWalletFragment.class,
          ContentWalletFragment::newInstance);

  @Before
  public void setup() {
    hiltRule.inject();
  }

  @Test
  public void tokenTitleIsDisplayed() {
    tokenTitle().check(matches(isDisplayed()));
  }

  @Test
  public void walletContentTextsAreDisplayed() {
    walletContentText1().check(matches(isDisplayed()));
    walletContentText2().check(matches(isDisplayed()));
  }
}
