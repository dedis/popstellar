package com.github.dedis.popstellar.ui.digitalcash;

import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withChild;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static com.github.dedis.popstellar.ui.pages.digitalcash.DigitalCashPageObject.digitalCashFragmentId;
import static com.github.dedis.popstellar.ui.pages.digitalcash.DigitalCashPageObject.fragmentContainer;
import static com.github.dedis.popstellar.ui.pages.digitalcash.DigitalCashPageObject.historyButton;
import static com.github.dedis.popstellar.ui.pages.digitalcash.DigitalCashPageObject.homeButton;
import static com.github.dedis.popstellar.ui.pages.digitalcash.DigitalCashPageObject.issueButton;
import static com.github.dedis.popstellar.ui.pages.digitalcash.DigitalCashPageObject.receiveButton;
import static com.github.dedis.popstellar.ui.pages.digitalcash.DigitalCashPageObject.sendButton;
import static com.github.dedis.popstellar.ui.pages.digitalcash.HistoryPageObject.fragmentDigitalCashHistoryId;
import static com.github.dedis.popstellar.ui.pages.digitalcash.IssuePageObject.fragmentDigitalCashIssueId;
import static com.github.dedis.popstellar.ui.pages.digitalcash.ReceiptPageObject.fragmentDigitalCashReceiptId;
import static com.github.dedis.popstellar.ui.pages.digitalcash.ReceivePageObject.fragmentDigitalCashReceiveId;
import static com.github.dedis.popstellar.ui.pages.digitalcash.SendPageObject.fragmentDigitalCashSendId;
import static com.github.dedis.popstellar.ui.pages.digitalcash.SendPageObject.sendButtonToReceipt;


import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnit;

import dagger.hilt.android.testing.HiltAndroidRule;
import dagger.hilt.android.testing.HiltAndroidTest;


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
    public void sendButtonGoesToSendThenToReceipt() {
        sendButton().perform(click());
        fragmentContainer().check(matches(withChild(withId(fragmentDigitalCashSendId()))));
        sendButtonToReceipt().perform(click());
        fragmentContainer().check(matches(withChild(withId(fragmentDigitalCashReceiptId()))));
    }

    @Test
    public void historyButtonGoesToHistory() {
        historyButton().perform(click());
        fragmentContainer().check(matches(withChild(withId(fragmentDigitalCashHistoryId()))));
    }

    @Test
    public void issueButtonGoesToIssue() {
        issueButton().perform(click());
        fragmentContainer().check(matches(withChild(withId(fragmentDigitalCashIssueId()))));
    }

    @Test
    public void receiveButtonGoesToReceive() {
        receiveButton().perform(click());
        fragmentContainer().check(matches(withChild(withId(fragmentDigitalCashReceiveId()))));
    }
}
