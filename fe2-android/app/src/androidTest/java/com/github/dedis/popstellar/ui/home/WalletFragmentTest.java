package com.github.dedis.popstellar.ui.home;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withClassName;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withParent;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anything;
import static org.hamcrest.Matchers.is;

import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import androidx.test.espresso.DataInteraction;
import androidx.test.espresso.ViewInteraction;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.filters.LargeTest;
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;

import com.github.dedis.popstellar.R;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.hamcrest.core.IsInstanceOf;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@LargeTest
@RunWith(AndroidJUnit4ClassRunner.class)
public class WalletFragmentTest {

  @Rule
  public ActivityScenarioRule<HomeActivity> mActivityTestRule =
      new ActivityScenarioRule<>(HomeActivity.class);

  @Test
  public void HomeWalletUITest() {
    ViewInteraction button =
        onView(
            allOf(
                withId(R.id.tab_wallet),
                withText("WALLET"),
                withParent(
                    allOf(
                        withId(R.id.tab_wallet_only),
                        withParent(withId(R.id.fragment_container_home)))),
                isDisplayed()));
    button.check(matches(isDisplayed()));

    ViewInteraction appCompatButton =
        onView(
            allOf(
                withId(R.id.tab_wallet),
                withText("Wallet"),
                childAtPosition(
                    allOf(
                        withId(R.id.tab_wallet_only),
                        childAtPosition(withId(R.id.fragment_container_home), 2)),
                    2),
                isDisplayed()));
    appCompatButton.perform(click());

    ViewInteraction textView =
        onView(
            allOf(
                withText("Welcome to your wallet !"),
                withParent(withParent(withId(R.id.fragment_wallet))),
                isDisplayed()));
    textView.check(matches(withText("Welcome to your wallet !")));

    ViewInteraction textView2 =
        onView(
            allOf(
                withText("Welcome to your wallet !"),
                withParent(withParent(withId(R.id.fragment_wallet))),
                isDisplayed()));
    textView2.check(matches(isDisplayed()));

    ViewInteraction textView3 =
        onView(
            allOf(
                withText(
                    "ATTENTION: if you create a new wallet remember to write down the given seed and store it secure place, this is the only backup to your PoP tokens."),
                withParent(withParent(withId(R.id.fragment_wallet))),
                isDisplayed()));
    textView3.check(
        matches(
            withText(
                "ATTENTION: if you create a new wallet remember to write down the given seed and store it secure place, this is the only backup to your PoP tokens.")));

    ViewInteraction textView4 =
        onView(
            allOf(
                withText(
                    "ATTENTION: if you create a new wallet remember to write down the given seed and store it secure place, this is the only backup to your PoP tokens."),
                withParent(withParent(withId(R.id.fragment_wallet))),
                isDisplayed()));
    textView4.check(matches(isDisplayed()));

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
  public void SetUpWalletUITest() {
    ViewInteraction appCompatButton =
        onView(
            allOf(
                withId(R.id.tab_wallet),
                withText("Wallet"),
                childAtPosition(
                    allOf(
                        withId(R.id.tab_wallet_only),
                        childAtPosition(withId(R.id.fragment_container_home), 2)),
                    2),
                isDisplayed()));
    appCompatButton.perform(click());

    ViewInteraction appCompatButton2 =
        onView(
            allOf(
                withId(R.id.button_new_wallet),
                withText("New wallet"),
                childAtPosition(
                    childAtPosition(withClassName(is("android.widget.LinearLayout")), 3), 0),
                isDisplayed()));
    appCompatButton2.perform(click());

    ViewInteraction button =
        onView(
            allOf(
                withId(R.id.tab_wallet),
                withText("WALLET"),
                withParent(
                    allOf(
                        withId(R.id.tab_wallet_only),
                        withParent(withId(R.id.fragment_container_home)))),
                isDisplayed()));
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

    ViewInteraction textView5 =
        onView(
            allOf(
                withId(R.id.alertTitle),
                withText("You are sure you have saved the words somewhere?"),
                withParent(allOf(withId(R.id.title_template), withParent(withId(R.id.topPanel)))),
                isDisplayed()));
    textView5.check(matches(withText("You are sure you have saved the words somewhere?")));

    ViewInteraction textView6 =
        onView(
            allOf(
                withId(R.id.alertTitle),
                withText("You are sure you have saved the words somewhere?"),
                withParent(allOf(withId(R.id.title_template), withParent(withId(R.id.topPanel)))),
                isDisplayed()));
    textView6.check(matches(isDisplayed()));

    ViewInteraction button4 =
        onView(
            allOf(
                withId(android.R.id.button2),
                withText("CANCEL"),
                withParent(withParent(withId(R.id.buttonPanel))),
                isDisplayed()));
    button4.check(matches(isDisplayed()));

    ViewInteraction button5 =
        onView(
            allOf(
                withId(android.R.id.button1),
                withText("YES"),
                withParent(withParent(withId(R.id.buttonPanel))),
                isDisplayed()));
    button5.check(matches(isDisplayed()));

    ViewInteraction appCompatButton4 =
        onView(
            allOf(
                withId(android.R.id.button2),
                withText("Cancel"),
                childAtPosition(childAtPosition(withId(R.id.buttonPanel), 0), 2)));
    appCompatButton4.perform(scrollTo(), click());
  }

  @Test
  public void ContentOfNewWalletUITest() {
    ViewInteraction appCompatButton =
        onView(
            allOf(
                withId(R.id.tab_wallet),
                withText("Wallet"),
                childAtPosition(
                    allOf(
                        withId(R.id.tab_wallet_only),
                        childAtPosition(withId(R.id.fragment_container_home), 2)),
                    2),
                isDisplayed()));
    appCompatButton.perform(click());

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

    ViewInteraction appCompatButton4 =
        onView(
            allOf(
                withId(android.R.id.button1),
                withText("Yes"),
                childAtPosition(childAtPosition(withId(R.id.buttonPanel), 0), 3)));
    appCompatButton4.perform(scrollTo(), click());

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

    ViewInteraction button =
        onView(
            allOf(
                withId(R.id.logout_button),
                withText("LOGOUT"),
                withParent(
                    allOf(
                        withId(R.id.fragment_content_wallet),
                        withParent(withId(R.id.fragment_container_home)))),
                isDisplayed()));
    button.check(matches(isDisplayed()));

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
  public void SetUpWithSeedWalletUITest() {
    ViewInteraction appCompatButton =
        onView(
            allOf(
                withId(R.id.tab_wallet),
                withText("Wallet"),
                childAtPosition(
                    allOf(
                        withId(R.id.tab_wallet_only),
                        childAtPosition(withId(R.id.fragment_container_home), 2)),
                    2),
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

    DataInteraction appCompatCheckedTextView =
        onData(anything())
            .inAdapterView(
                allOf(
                    withId(R.id.select_dialog_listview),
                    childAtPosition(withId(R.id.contentPanel), 1)))
            .atPosition(0);
    appCompatCheckedTextView.perform(click());

    ViewInteraction textView =
        onView(
            allOf(
                withId(R.id.alertTitle),
                withText("Type the 12 word seed:"),
                withParent(allOf(withId(R.id.title_template), withParent(withId(R.id.topPanel)))),
                isDisplayed()));
    textView.check(matches(withText("Type the 12 word seed:")));

    ViewInteraction textView2 =
        onView(
            allOf(
                withId(R.id.alertTitle),
                withText("Type the 12 word seed:"),
                withParent(allOf(withId(R.id.title_template), withParent(withId(R.id.topPanel)))),
                isDisplayed()));
    textView2.check(matches(isDisplayed()));

    ViewInteraction checkedTextView =
        onView(
            allOf(
                withId(android.R.id.text1),
                withText("show password"),
                withParent(
                    allOf(
                        withId(R.id.select_dialog_listview),
                        withParent(withId(R.id.contentPanel)))),
                isDisplayed()));
    checkedTextView.check(matches(isDisplayed()));

    ViewInteraction editText =
        onView(
            allOf(
                withText(
                    "elbow six card empty next sight turn quality capital please vocal indoor"),
                withParent(allOf(withId(R.id.custom), withParent(withId(R.id.customPanel)))),
                isDisplayed()));
    editText.check(
        matches(
            withText("elbow six card empty next sight turn quality capital please vocal indoor")));

    ViewInteraction editText2 =
        onView(
            allOf(
                withText(
                    "elbow six card empty next sight turn quality capital please vocal indoor"),
                withParent(allOf(withId(R.id.custom), withParent(withId(R.id.customPanel)))),
                isDisplayed()));
    editText2.check(matches(isDisplayed()));

    ViewInteraction button =
        onView(
            allOf(
                withId(android.R.id.button2),
                withText("CANCEL"),
                withParent(withParent(withId(R.id.buttonPanel))),
                isDisplayed()));
    button.check(matches(isDisplayed()));

    ViewInteraction button2 =
        onView(
            allOf(
                withId(android.R.id.button1),
                withText("SET UP WALLET"),
                withParent(withParent(withId(R.id.buttonPanel))),
                isDisplayed()));
    button2.check(matches(isDisplayed()));

    ViewInteraction appCompatButton3 =
        onView(
            allOf(
                withId(android.R.id.button1),
                withText("Set up wallet"),
                childAtPosition(childAtPosition(withId(R.id.buttonPanel), 0), 3)));
    appCompatButton3.perform(scrollTo(), click());
  }

  @Test
  public void LogoutWalletUITest() {
    ViewInteraction appCompatButton =
        onView(
            allOf(
                withId(R.id.tab_wallet),
                withText("Wallet"),
                childAtPosition(
                    allOf(
                        withId(R.id.tab_wallet_only),
                        childAtPosition(withId(R.id.fragment_container_home), 2)),
                    2),
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

    ViewInteraction appCompatButton3 =
        onView(
            allOf(
                withId(android.R.id.button1),
                withText("Set up wallet"),
                childAtPosition(childAtPosition(withId(R.id.buttonPanel), 0), 3)));
    appCompatButton3.perform(scrollTo(), click());

    ViewInteraction appCompatButton4 =
        onView(
            allOf(
                withId(R.id.logout_button),
                withText("LOGOUT"),
                childAtPosition(
                    allOf(
                        withId(R.id.fragment_content_wallet),
                        childAtPosition(withId(R.id.fragment_container_home), 3)),
                    4),
                isDisplayed()));
    appCompatButton4.perform(click());

    ViewInteraction textView =
        onView(
            allOf(
                withId(R.id.alertTitle),
                withText("Log out"),
                withParent(allOf(withId(R.id.title_template), withParent(withId(R.id.topPanel)))),
                isDisplayed()));
    textView.check(matches(withText("Log out")));

    ViewInteraction textView2 =
        onView(
            allOf(
                withId(R.id.alertTitle),
                withText("Log out"),
                withParent(allOf(withId(R.id.title_template), withParent(withId(R.id.topPanel)))),
                isDisplayed()));
    textView2.check(matches(isDisplayed()));

    ViewInteraction textView3 =
        onView(
            allOf(
                withId(android.R.id.message),
                withText(
                    "This action will delete the current seed of your wallet and your tokens will be lost. They can be recovered later once you import the current seed."),
                withParent(withParent(withId(R.id.scrollView))),
                isDisplayed()));
    textView3.check(
        matches(
            withText(
                "This action will delete the current seed of your wallet and your tokens will be lost. They can be recovered later once you import the current seed.")));

    ViewInteraction textView4 =
        onView(
            allOf(
                withId(android.R.id.message),
                withText(
                    "This action will delete the current seed of your wallet and your tokens will be lost. They can be recovered later once you import the current seed."),
                withParent(withParent(withId(R.id.scrollView))),
                isDisplayed()));
    textView4.check(matches(isDisplayed()));

    ViewInteraction button =
        onView(
            allOf(
                withId(android.R.id.button2),
                withText("CANCEL"),
                withParent(withParent(withId(R.id.buttonPanel))),
                isDisplayed()));
    button.check(matches(isDisplayed()));

    ViewInteraction button2 =
        onView(
            allOf(
                withId(android.R.id.button1),
                withText("CONFIRM"),
                withParent(withParent(withId(R.id.buttonPanel))),
                isDisplayed()));
    button2.check(matches(isDisplayed()));

    ViewInteraction appCompatButton5 =
        onView(
            allOf(
                withId(android.R.id.button1),
                withText("Confirm"),
                childAtPosition(childAtPosition(withId(R.id.buttonPanel), 0), 3)));
    appCompatButton5.perform(scrollTo(), click());
  }

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
}
