//TODO: adapt this test to current version
/*package com.github.dedis.student20_pop.home;


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
import static org.hamcrest.Matchers.is;

import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import androidx.test.espresso.ViewInteraction;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.filters.LargeTest;
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;
import com.github.dedis.student20_pop.R;
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
  public ActivityScenarioRule<HomeActivity> mActivityTestRule = new ActivityScenarioRule<>(HomeActivity.class);

  @Test
  public void walletFragmentTest() {
    ViewInteraction button = onView(
        allOf(withId(R.id.tab_wallet), withText("WALLET"),
            withParent(allOf(withId(R.id.tab_wallet_only),
                withParent(withId(R.id.fragment_container_home)))),
            isDisplayed()));
    button.check(matches(isDisplayed()));

    ViewInteraction appCompatButton = onView(
        allOf(withId(R.id.tab_connect), withText("Connect"),
            childAtPosition(
                allOf(withId(R.id.tab_connect_launch),
                    childAtPosition(
                        withId(R.id.fragment_container_home),
                        1)),
                4),
            isDisplayed()));
    appCompatButton.perform(click());

    ViewInteraction textView = onView(
        allOf(withId(R.id.alertTitle),
            withText("You have to setup up your wallet before connecting."),
            withParent(allOf(withId(R.id.title_template),
                withParent(withId(R.id.topPanel)))),
            isDisplayed()));
    textView.check(matches(withText("You have to setup up your wallet before connecting.")));

    ViewInteraction button2 = onView(
        allOf(withId(android.R.id.button1), withText("GO TO WALLET"),
            withParent(withParent(withId(R.id.buttonPanel))),
            isDisplayed()));
    button2.check(matches(isDisplayed()));

    ViewInteraction appCompatButton2 = onView(
        allOf(withId(android.R.id.button1), withText("Go to wallet"),
            childAtPosition(
                childAtPosition(
                    withId(R.id.buttonPanel),
                    0),
                3)));
    appCompatButton2.perform(scrollTo(), click());

    ViewInteraction textView2 = onView(
        allOf(withText("Welcome to your wallet !"),
            withParent(withParent(withId(R.id.fragment_wallet))),
            isDisplayed()));
    textView2.check(matches(withText("Welcome to your wallet !")));

    ViewInteraction textView3 = onView(
        allOf(withText("You may import your seed if you own one or create a new wallet."),
            withParent(withParent(withId(R.id.fragment_wallet))),
            isDisplayed()));
    textView3.check(
        matches(withText("You may import your seed if you own one or create a new wallet.")));

    ViewInteraction textView4 = onView(
        allOf(withText(
            "ATTENTION: if you create a new wallet remember to write down the given seed and store it secure place, this is the only backup to your PoP tokens."),
            withParent(withParent(withId(R.id.fragment_wallet))),
            isDisplayed()));
    textView4.check(matches(withText(
        "ATTENTION: if you create a new wallet remember to write down the given seed and store it secure place, this is the only backup to your PoP tokens.")));

    ViewInteraction button3 = onView(
        allOf(withId(R.id.button_new_wallet), withText("New wallet"),
            withParent(
                withParent(IsInstanceOf.<View>instanceOf(android.widget.LinearLayout.class))),
            isDisplayed()));
    button3.check(matches(isDisplayed()));

    ViewInteraction button4 = onView(
        allOf(withId(R.id.button_own_seed), withText("I own a seed"),
            withParent(
                withParent(IsInstanceOf.<View>instanceOf(android.widget.LinearLayout.class))),
            isDisplayed()));
    button4.check(matches(isDisplayed()));

    ViewInteraction appCompatButton3 = onView(
        allOf(withId(R.id.button_new_wallet), withText("New wallet"),
            childAtPosition(
                childAtPosition(
                    withClassName(is("android.widget.LinearLayout")),
                    3),
                0),
            isDisplayed()));
    appCompatButton3.perform(click());

    ViewInteraction textView5 = onView(
        allOf(withText("This is the only backup for your PoP tokens - store it securely"),
            withParent(withParent(withId(R.id.fragment_seed_wallet))),
            isDisplayed()));
    textView5.check(
        matches(withText("This is the only backup for your PoP tokens - store it securely")));

    ViewInteraction textView6 = onView(
        allOf(withId(R.id.seed_wallet),
            withParent(withParent(withId(R.id.fragment_seed_wallet))),
            isDisplayed()));
    textView6.check(matches(isDisplayed()));

    ViewInteraction button5 = onView(
        allOf(withId(R.id.button_confirm_seed), withText("Confirm"),
            withParent(
                withParent(IsInstanceOf.<View>instanceOf(android.widget.LinearLayout.class))),
            isDisplayed()));
    button5.check(matches(isDisplayed()));

    ViewInteraction appCompatButton4 = onView(
        allOf(withId(R.id.button_confirm_seed), withText("Confirm"),
            childAtPosition(
                childAtPosition(
                    withClassName(is("android.widget.LinearLayout")),
                    2),
                0),
            isDisplayed()));
    appCompatButton4.perform(click());

    ViewInteraction textView7 = onView(
        allOf(withId(R.id.alertTitle), withText("You are sure you have saved the words somewhere?"),
            withParent(allOf(withId(R.id.title_template),
                withParent(withId(R.id.topPanel)))),
            isDisplayed()));
    textView7.check(matches(withText("You are sure you have saved the words somewhere?")));

    ViewInteraction button6 = onView(
        allOf(withId(android.R.id.button2), withText("CANCEL"),
            withParent(withParent(withId(R.id.buttonPanel))),
            isDisplayed()));
    button6.check(matches(isDisplayed()));

    ViewInteraction button7 = onView(
        allOf(withId(android.R.id.button1), withText("YES"),
            withParent(withParent(withId(R.id.buttonPanel))),
            isDisplayed()));
    button7.check(matches(isDisplayed()));

    ViewInteraction appCompatButton5 = onView(
        allOf(withId(android.R.id.button2), withText("Cancel"),
            childAtPosition(
                childAtPosition(
                    withId(R.id.buttonPanel),
                    0),
                2)));
    appCompatButton5.perform(scrollTo(), click());

    ViewInteraction appCompatButton10 = onView(
        allOf(withId(R.id.tab_home), withText("Home"),
            childAtPosition(
                allOf(withId(R.id.tab_home_only),
                    childAtPosition(
                        withId(R.id.fragment_container_home),
                        0)),
                2),
            isDisplayed()));
    appCompatButton10.perform(click());

    ViewInteraction appCompatButton11 = onView(
        allOf(withId(R.id.tab_wallet), withText("Wallet"),
            childAtPosition(
                allOf(withId(R.id.tab_wallet_only),
                    childAtPosition(
                        withId(R.id.fragment_container_home),
                        2)),
                2),
            isDisplayed()));
    appCompatButton11.perform(click());

    ViewInteraction appCompatButton12 = onView(
        allOf(withId(R.id.button_own_seed), withText("I own a seed"),
            childAtPosition(
                childAtPosition(
                    withClassName(is("android.widget.LinearLayout")),
                    3),
                1),
            isDisplayed()));
    appCompatButton12.perform(click());

    ViewInteraction textView8 = onView(
        allOf(withId(R.id.alertTitle), withText("Type the 12 word seed:"),
            withParent(allOf(withId(R.id.title_template),
                withParent(withId(R.id.topPanel)))),
            isDisplayed()));
    textView8.check(matches(withText("Type the 12 word seed:")));

    ViewInteraction checkedTextView = onView(
        allOf(withId(android.R.id.text1), withText("show password"),
            withParent(allOf(withId(R.id.select_dialog_listview),
                withParent(withId(R.id.contentPanel)))),
            isDisplayed()));
    checkedTextView.check(matches(isDisplayed()));

    ViewInteraction button8 = onView(
        allOf(withId(android.R.id.button2), withText("CANCEL"),
            withParent(withParent(withId(R.id.buttonPanel))),
            isDisplayed()));
    button8.check(matches(isDisplayed()));

    ViewInteraction button9 = onView(
        allOf(withId(android.R.id.button1), withText("SET UP WALLET"),
            withParent(withParent(withId(R.id.buttonPanel))),
            isDisplayed()));
    button9.check(matches(isDisplayed()));
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
        return parent instanceof ViewGroup && parentMatcher.matches(parent)
            && view.equals(((ViewGroup) parent).getChildAt(position));
      }
    };
  }
}*/
