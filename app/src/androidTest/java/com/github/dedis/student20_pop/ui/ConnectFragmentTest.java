package com.github.dedis.student20_pop.ui;

import androidx.fragment.app.Fragment;
import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.matcher.ViewMatchers;

import com.github.dedis.student20_pop.MainActivity;
import com.github.dedis.student20_pop.R;

import org.junit.Assert;
import org.junit.Test;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

/**
 * Class handling connect fragment tests
 */
public class ConnectFragmentTest {

    private static final String TEST_URL = "Test url";

    @Test
    public void testSimpleBarcodeReaction() {
        ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class);

        // Set good fragment
        onView(ViewMatchers.withId(R.id.tab_connect)).perform(click());
        onView(withId(R.id.fragment_connect)).check(matches(isDisplayed()));

        // Simulate a detected url
        scenario.onActivity(a -> {
            Fragment fragment = a.getSupportFragmentManager().findFragmentByTag(ConnectFragment.TAG);
            Assert.assertNotNull(fragment);
            Assert.assertTrue(fragment instanceof ConnectFragment);
            ((ConnectFragment) fragment).onQRCodeDetected(TEST_URL);
        });

        // Check everything
        onView(withId(R.id.fragment_connecting)).check(matches(isDisplayed()));
        onView(withId(R.id.connecting_url)).check(matches(withText(TEST_URL)));
    }
}