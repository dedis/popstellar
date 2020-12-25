
package com.github.dedis.student20_pop.ui;

import android.Manifest;

import androidx.fragment.app.Fragment;
import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.rule.GrantPermissionRule;

import com.github.dedis.student20_pop.MainActivity;
import com.github.dedis.student20_pop.R;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static com.github.dedis.student20_pop.ui.QRCodeScanningFragment.QRCodeScanningType.ADD_WITNESS;
import static com.github.dedis.student20_pop.ui.QRCodeScanningFragment.QRCodeScanningType.CONNECT_LAO;

/**
 * Class handling connect fragment tests
 */
public class QRCodeScanningFragmentTest {

    private static final String TEST_URL = "Test url";

    @Rule public final GrantPermissionRule rule = GrantPermissionRule.grant(Manifest.permission.CAMERA);

    @Test
    public void testSimpleBarcodeReaction() {
        ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class);

        // Set good fragment
        onView(ViewMatchers.withId(R.id.tab_connect)).perform(click());
        onView(withId(R.id.fragment_connect)).check(matches(isDisplayed()));

        // Simulate a detected url
        scenario.onActivity(a -> {
            Fragment fragment = a.getSupportFragmentManager().findFragmentByTag(QRCodeScanningFragment.TAG);
            Assert.assertNotNull(fragment);
            Assert.assertTrue(fragment instanceof QRCodeScanningFragment);
            ((QRCodeScanningFragment) fragment).onQRCodeDetected(TEST_URL, CONNECT_LAO);
        });

        // Check everything
        onView(withId(R.id.fragment_connecting)).check(matches(isDisplayed()));
        onView(withId(R.id.connecting_url)).check(matches(withText(TEST_URL)));
    }
}