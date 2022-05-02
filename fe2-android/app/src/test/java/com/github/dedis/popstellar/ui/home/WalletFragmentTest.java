package com.github.dedis.popstellar.ui.home;

import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.assertThat;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
<<<<<<< HEAD
import static androidx.test.espresso.matcher.ViewMatchers.withClassName;
=======
import static androidx.test.espresso.matcher.ViewMatchers.withChild;
>>>>>>> master
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static com.github.dedis.popstellar.testutils.UITestUtils.dialogNegativeButton;
import static com.github.dedis.popstellar.testutils.UITestUtils.dialogPositiveButton;
<<<<<<< HEAD
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
=======
import static com.github.dedis.popstellar.ui.pages.home.HomePageObject.fragmentContainer;
import static com.github.dedis.popstellar.ui.pages.home.HomePageObject.homeFragmentContainerId;
import static com.github.dedis.popstellar.ui.pages.home.HomePageObject.navBar;
import static com.github.dedis.popstellar.ui.pages.home.WalletPageObject.confirmButton;
import static com.github.dedis.popstellar.ui.pages.home.WalletPageObject.iOwnASeedButton;
import static com.github.dedis.popstellar.ui.pages.home.WalletPageObject.logoutButton;
import static com.github.dedis.popstellar.ui.pages.home.WalletPageObject.newWalletButton;
import static com.github.dedis.popstellar.ui.pages.home.WalletPageObject.walletFragmentId;
import static com.github.dedis.popstellar.ui.pages.home.WalletPageObject.walletSeedFragmentId;
import static com.github.dedis.popstellar.ui.pages.home.WalletPageObject.welcomeText1;
import static com.github.dedis.popstellar.ui.pages.home.WalletPageObject.welcomeText2;
import static com.github.dedis.popstellar.ui.pages.home.WalletPageObject.welcomeText3;
import static org.hamcrest.Matchers.allOf;
>>>>>>> master

import android.widget.Button;

import androidx.test.ext.junit.runners.AndroidJUnit4;

<<<<<<< HEAD
import com.github.dedis.popstellar.R;
=======
import com.github.dedis.popstellar.testutils.fragment.ActivityFragmentScenarioRule;
import com.github.dedis.popstellar.ui.wallet.WalletFragment;
>>>>>>> master

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import dagger.hilt.android.testing.HiltAndroidRule;
import dagger.hilt.android.testing.HiltAndroidTest;

@HiltAndroidTest
@RunWith(AndroidJUnit4.class)
public class WalletFragmentTest {

<<<<<<< HEAD
  // TODO: update those tests: needs to be simplified and readable
  public ActivityScenarioRule<HomeActivity> mActivityScenarioRule =
      new ActivityScenarioRule<>(HomeActivity.class);

  @Rule
  public final RuleChain rule =
      RuleChain.outerRule(new HiltAndroidRule(this)).around(mActivityScenarioRule);

  private View decorView;

  @Before
  public void setUp() {
    mActivityScenarioRule
        .getScenario()
        .onActivity(activity -> decorView = activity.getWindow().getDecorView());
  }

  @Test
  public void homeWalletUITest() {
    ViewInteraction button =
        onView(
            allOf(
                withId(R.id.tab_wallet),
                withText("WALLET"),
                withParent(
                    allOf(
                        withId(R.id.tab_wallet_only),
                        withParent(withId(R.id.fragment_container_home))))));
    button.check(matches(isDisplayed()));
    button.perform(click());

    ViewInteraction mainText =
        onView(
            allOf(
                withText("Welcome to your wallet !"),
                withParent(withParent(withId(R.id.fragment_wallet)))));
    mainText.check(matches(isDisplayed()));

    ViewInteraction tooltip =
        onView(
            allOf(
                withText(
                    "ATTENTION: if you create a new wallet remember to write down the given seed and store it secure place, this is the only backup to your PoP tokens."),
                withParent(withParent(withId(R.id.fragment_wallet)))));
    tooltip.check(matches(isDisplayed()));

    ViewInteraction button2 =
        onView(
            allOf(
                withId(R.id.tab_wallet),
                withText("WALLET"),
                withParent(
                    allOf(
                        withId(R.id.tab_wallet_only),
                        withParent(withId(R.id.fragment_container_home)))),
                isDisplayed()));
    button2.check(matches(isDisplayed()));

    ViewInteraction button3 =
        onView(
            allOf(
                withId(R.id.button_new_wallet),
                withText("New wallet"),
                withParent(withParent(IsInstanceOf.instanceOf(android.widget.LinearLayout.class))),
                isDisplayed()));
    button3.check(matches(isDisplayed()));

    ViewInteraction button4 =
        onView(
            allOf(
                withId(R.id.button_own_seed),
                withText("I own a seed"),
                withParent(withParent(IsInstanceOf.instanceOf(android.widget.LinearLayout.class))),
                isDisplayed()));
    button4.check(matches(isDisplayed()));
  }

  @Test
  public void setUpWalletUITest() {
    ViewInteraction button =
        onView(
            allOf(
                withId(R.id.tab_wallet),
                withText("WALLET"),
                withParent(
                    allOf(
                        withId(R.id.tab_wallet_only),
                        withParent(withId(R.id.fragment_container_home))))));
    button.check(matches(isDisplayed()));
    button.perform(click());

    ViewInteraction appCompatButton2 =
        onView(
            allOf(
                withId(R.id.button_new_wallet),
                withText("New wallet"),
                childAtPosition(
                    childAtPosition(withClassName(is("android.widget.LinearLayout")), 3), 0),
                isDisplayed()));
    appCompatButton2.perform(click());

    button.check(matches(isDisplayed()));

    ViewInteraction textView =
        onView(
            allOf(
                withText("This is the only backup for your PoP tokens - store it securely"),
                withParent(withParent(withId(R.id.fragment_seed_wallet))),
                isDisplayed()));
    textView.check(
        matches(withText("This is the only backup for your PoP tokens - store it securely")));

    ViewInteraction textView2 =
        onView(
            allOf(
                withText("This is the only backup for your PoP tokens - store it securely"),
                withParent(withParent(withId(R.id.fragment_seed_wallet))),
                isDisplayed()));
    textView2.check(matches(isDisplayed()));

    ViewInteraction textView3 =
        onView(
            allOf(
                withId(R.id.seed_wallet),
                withParent(withParent(withId(R.id.fragment_seed_wallet))),
                isDisplayed()));
    textView3.check(matches(isDisplayed()));

    ViewInteraction textView4 =
        onView(
            allOf(
                withId(R.id.seed_wallet),
                withParent(withParent(withId(R.id.fragment_seed_wallet))),
                isDisplayed()));
    textView4.perform(click());

    assertToastIsDisplayedWithText(R.string.copied_to_clipboard);

    ViewInteraction button2 =
        onView(
            allOf(
                withId(R.id.button_confirm_seed),
                withText("Confirm"),
                withParent(withParent(IsInstanceOf.instanceOf(android.widget.LinearLayout.class))),
                isDisplayed()));
    button2.check(matches(isDisplayed()));

    ViewInteraction appCompatButton3 =
        onView(
            allOf(
                withId(R.id.button_confirm_seed),
                withText("Confirm"),
                childAtPosition(
                    childAtPosition(withClassName(is("android.widget.LinearLayout")), 2), 0),
                isDisplayed()));
    appCompatButton3.perform(click());

    assertThat(dialogPositiveButton(), allOf(withText("YES"), isDisplayed()));
    assertThat(dialogNegativeButton(), allOf(withText("Cancel"), isDisplayed()));
=======
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
>>>>>>> master
  }

  @Test
  public void contentOfNewWalletUITest() {
    ViewInteraction button =
        onView(
            allOf(
                withId(R.id.tab_wallet),
                withText("WALLET"),
                withParent(
                    allOf(
                        withId(R.id.tab_wallet_only),
                        withParent(withId(R.id.fragment_container_home))))));
    button.check(matches(isDisplayed()));
    button.perform(click());

    ViewInteraction appCompatButton2 =
        onView(
            allOf(
                withId(R.id.button_new_wallet),
                withText("New wallet"),
                childAtPosition(
                    childAtPosition(withClassName(is("android.widget.LinearLayout")), 3), 0),
                isDisplayed()));
    appCompatButton2.perform(click());

    ViewInteraction appCompatButton3 =
        onView(
            allOf(
                withId(R.id.button_confirm_seed),
                withText("Confirm"),
                childAtPosition(
                    childAtPosition(withClassName(is("android.widget.LinearLayout")), 2), 0),
                isDisplayed()));
    appCompatButton3.perform(click());

    assertThat(dialogPositiveButton(), allOf(withText("Yes"), isDisplayed()));
    dialogPositiveButton().performClick();

    ViewInteraction textView =
        onView(
            allOf(
                withId(R.id.title_wallet),
                withText("Tokens"),
                withParent(
                    allOf(
                        withId(R.id.fragment_content_wallet),
                        withParent(withId(R.id.fragment_container_home)))),
                isDisplayed()));
    textView.check(matches(withText("Tokens")));

    ViewInteraction textView2 =
        onView(
            allOf(
                withId(R.id.title_wallet),
                withText("Tokens"),
                withParent(
                    allOf(
                        withId(R.id.fragment_content_wallet),
                        withParent(withId(R.id.fragment_container_home)))),
                isDisplayed()));
    textView2.check(matches(isDisplayed()));

    ViewInteraction textView3 =
        onView(
            allOf(
                withText("Your wallet is empty"),
                withParent(
                    allOf(
                        withId(R.id.welcome_screen),
                        withParent(withId(R.id.fragment_content_wallet)))),
                isDisplayed()));
    textView3.check(matches(withText("Your wallet is empty")));

    ViewInteraction textView4 =
        onView(
            allOf(
                withText("Your wallet is empty"),
                withParent(
                    allOf(
                        withId(R.id.welcome_screen),
                        withParent(withId(R.id.fragment_content_wallet)))),
                isDisplayed()));
    textView4.check(matches(isDisplayed()));

    ViewInteraction textView5 =
        onView(
            allOf(
                withText(
                    "If you would like to receive tokens, please connect to a LAO and participate in a roll call.\n If you are an organizer, you can look at the attendees' public tokens of the roll calls you organize."),
                withParent(
                    allOf(
                        withId(R.id.welcome_screen),
                        withParent(withId(R.id.fragment_content_wallet)))),
                isDisplayed()));
    textView5.check(
        matches(
            withText(
                "If you would like to receive tokens, please connect to a LAO and participate in a roll call.\n If you are an organizer, you can look at the attendees' public tokens of the roll calls you organize.")));

    ViewInteraction textView6 =
        onView(
            allOf(
                withText(
                    "If you would like to receive tokens, please connect to a LAO and participate in a roll call.\n If you are an organizer, you can look at the attendees' public tokens of the roll calls you organize."),
                withParent(
                    allOf(
                        withId(R.id.welcome_screen),
                        withParent(withId(R.id.fragment_content_wallet)))),
                isDisplayed()));
    textView6.check(matches(isDisplayed()));

    ViewInteraction button1 =
        onView(
            allOf(
                withId(R.id.logout_button),
                withText("LOGOUT"),
                withParent(
                    allOf(
                        withId(R.id.fragment_content_wallet),
                        withParent(withId(R.id.fragment_container_home)))),
                isDisplayed()));
    button1.check(matches(isDisplayed()));

    ViewInteraction button2 =
        onView(
            allOf(
                withId(R.id.logout_button),
                withText("LOGOUT"),
                withParent(
                    allOf(
                        withId(R.id.fragment_content_wallet),
                        withParent(withId(R.id.fragment_container_home)))),
                isDisplayed()));
    button2.check(matches(isDisplayed()));
  }

  @Test
<<<<<<< HEAD
  public void setUpWithSeedWalletUITest() {
    ViewInteraction appCompatButton =
        onView(
            allOf(
                withId(R.id.tab_wallet),
                withText("Wallet"),
                childAtPosition(
                    allOf(
                        withId(R.id.tab_wallet_only),
                        childAtPosition(withId(R.id.fragment_container_home), 2)),
                    3),
                isDisplayed()));
    appCompatButton.perform(click());

    ViewInteraction appCompatButton2 =
        onView(
            allOf(
                withId(R.id.button_own_seed),
                withText("I own a seed"),
                childAtPosition(
                    childAtPosition(withClassName(is("android.widget.LinearLayout")), 3), 1),
                isDisplayed()));
    appCompatButton2.perform(click());

=======
  public void iOwnASeedButtonDisplaysDialogOnClick() {
    iOwnASeedButton().perform(click());
>>>>>>> master
    Button setupWallet = dialogPositiveButton();

    assertThat(setupWallet, allOf(withText("SET UP WALLET"), isDisplayed()));
    assertThat(dialogNegativeButton(), allOf(withText("CANCEL"), isDisplayed()));

    setupWallet.performClick();
  }

  @Test
  public void logoutWalletUITest() {
    ViewInteraction appCompatButton =
        onView(allOf(withId(R.id.tab_wallet), withText("Wallet"), isDisplayed()));
    appCompatButton.perform(click());

    ViewInteraction appCompatButton2 =
        onView(
            allOf(
                withId(R.id.button_own_seed),
                withText("I own a seed"),
                childAtPosition(
                    childAtPosition(withClassName(is("android.widget.LinearLayout")), 3), 1),
                isDisplayed()));
    appCompatButton2.perform(click());

    Button setupWallet = dialogPositiveButton();
    assertThat(setupWallet, allOf(withText("SET UP WALLET"), isDisplayed()));
    setupWallet.performClick();

    ViewInteraction appCompatButton4 =
        onView(
            allOf(
                withId(R.id.logout_button),
                withText("LOGOUT"),
                childAtPosition(
                    allOf(
                        withId(R.id.fragment_content_wallet),
                        childAtPosition(withId(R.id.fragment_container_home), 4)),
                    4),
                isDisplayed()));
    appCompatButton4.perform(click());

    assertThat(dialogNegativeButton(), allOf(withText("CANCEL"), isDisplayed()));

    Button confirm = dialogPositiveButton();
    assertThat(confirm, allOf(withText("Confirm"), isDisplayed()));
    confirm.performClick();
  }
<<<<<<< HEAD

  private static Matcher<View> childAtPosition(
      final Matcher<View> parentMatcher, final int position) {

    return new TypeSafeMatcher<View>() {
      @Override
      public void describeTo(Description description) {
        description.appendText("Child at position " + position + " in parent ");
        parentMatcher.describeTo(description);
      }

      @Override
      public boolean matchesSafely(View view) {
        ViewParent parent = view.getParent();
        return parent instanceof ViewGroup
            && parentMatcher.matches(parent)
            && view.equals(((ViewGroup) parent).getChildAt(position));
      }
    };
  }
=======
>>>>>>> master
}
