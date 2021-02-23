package com.github.dedis.student20_pop.ui.qrcode;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import com.github.dedis.student20_pop.MainActivity;
import com.github.dedis.student20_pop.R;
import org.junit.Rule;
import org.junit.Test;

/** Class handling camera permission fragment tests */
public class CameraPermissionFragmentTest {

  @Rule
  public ActivityScenarioRule<MainActivity> rule = new ActivityScenarioRule<>(MainActivity.class);

  @Test
  public void checkCorrectTabIsDisplayed() {
    // Check that the permission is not granted
    rule.getScenario()
        .onActivity(
            activity ->
                assertThat(
                    ActivityCompat.checkSelfPermission(activity, Manifest.permission.CAMERA),
                    not(is(PackageManager.PERMISSION_GRANTED))));

    onView(withId(R.id.tab_connect)).perform(click());
    onView(withId(R.id.fragment_camera_perm)).check(matches(isDisplayed()));
  }
}
