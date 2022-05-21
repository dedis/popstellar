package com.github.dedis.popstellar.ui.digitalcash;

import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withChild;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static com.github.dedis.popstellar.ui.pages.digitalcash.DigitalCashPageObject.digitalCashFragmentId;
import static com.github.dedis.popstellar.ui.pages.digitalcash.DigitalCashPageObject.fragmentContainer;
import static com.github.dedis.popstellar.ui.pages.digitalcash.DigitalCashPageObject.homeButton;
import static com.github.dedis.popstellar.ui.pages.digitalcash.DigitalCashPageObject.sendButton;
import static com.github.dedis.popstellar.ui.pages.digitalcash.SendPageObject.fragmentDigitalCashSendId;
import static com.github.dedis.popstellar.ui.pages.home.HomePageObject.connectButton;


import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.github.dedis.popstellar.ui.home.HomeActivity;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnit;

import dagger.hilt.android.testing.HiltAndroidRule;
import dagger.hilt.android.testing.HiltAndroidTest;
import dagger.hilt.android.AndroidEntryPoint;


@HiltAndroidTest
@RunWith(AndroidJUnit4.class)
public class DigitalCashMainTest {
    //  Hilt rule
    private final HiltAndroidRule hiltAndroidRule = new HiltAndroidRule(this);

    // Activity scenario rule that starts the activity.
    public ActivityScenarioRule<DigitalCashMain> activityScenarioRule =
            new ActivityScenarioRule<DigitalCashMain>(DigitalCashMain.class);

    @Rule
    public final RuleChain rule =
            RuleChain.outerRule(MockitoJUnit.testRule(this))
                    .around(hiltAndroidRule)
                    .around(activityScenarioRule);

    @Test
    public void homeButtonStaysHome() {
        homeButton().perform(click());
        fragmentContainer().check(matches(withChild(withId(digitalCashFragmentId()))));
    }

    @Test
    public void connectButtonStaysHomeWithoutInitializedWallet() {
        sendButton().perform(click());
        fragmentContainer().check(matches(withChild(withId(fragmentDigitalCashSendId()))));
    }
/*
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

 */


}
