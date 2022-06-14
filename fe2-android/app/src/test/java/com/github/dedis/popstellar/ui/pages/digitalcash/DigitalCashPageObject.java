package com.github.dedis.popstellar.ui.pages.digitalcash;


import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import androidx.annotation.IdRes;
import androidx.test.espresso.ViewInteraction;

import com.github.dedis.popstellar.R;

/**
 * Page object of {@Link DigitalCashMainActivity}
 *
 * <p>Creation : 20.05.2022
 */
public class DigitalCashPageObject {

    public static ViewInteraction fragmentContainer() {
        return onView(withId(R.id.fragment_container_digital_cash));
    }

    public static ViewInteraction sendButton() {
        return onView(withId(R.id.send_coin));
    }

    public static ViewInteraction receiveButton() {
        return onView(withId(R.id.receive_coin));
    }

    public static ViewInteraction historyButton() {
        return onView(withId(R.id.history_coin));
    }

    public static ViewInteraction homeButton() {
        return onView(withId(R.id.home_coin));
    }

    public static ViewInteraction issueButton() {
        return onView(withId(R.id.issue_coin));
    }

    public static ViewInteraction navBar(){
        return onView(withId(R.id.digital_cash_nav_bar));
    }

    @IdRes
    public static int digitalCashFragmentId() {
        return R.id.fragment_digital_cash_home;
    }

    @IdRes
    public static int digitalCashFragmentContainerId() {
        return R.id.fragment_container_digital_cash;
    }
}
