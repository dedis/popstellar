package com.github.dedis.popstellar.ui.home;

import android.app.ActivityManager;
import android.content.Context;
import android.os.SystemClock;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnit;

import java.util.List;

import dagger.hilt.android.testing.HiltAndroidRule;
import dagger.hilt.android.testing.HiltAndroidTest;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.pressBack;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static com.github.dedis.popstellar.testutils.UITestUtils.*;
import static com.github.dedis.popstellar.testutils.pages.home.HomePageObject.*;
import static com.github.dedis.popstellar.testutils.pages.home.LaoCreatePageObject.createFragmentId;
import static com.github.dedis.popstellar.testutils.pages.home.WalletPageObject.confirmButton;
import static com.github.dedis.popstellar.testutils.pages.home.WalletPageObject.walletId;
import static org.hamcrest.Matchers.allOf;
import static org.junit.Assert.assertTrue;

@HiltAndroidTest
@RunWith(AndroidJUnit4.class)
public class HomeActivityTest {

  //  Hilt rule
  private final HiltAndroidRule hiltAndroidRule = new HiltAndroidRule(this);

  // Activity scenario rule that starts the activity.
  public ActivityScenarioRule<HomeActivity> activityScenarioRule =
      new ActivityScenarioRule<>(HomeActivity.class);

  @Rule
  public final RuleChain rule =
      RuleChain.outerRule(MockitoJUnit.testRule(this))
          .around(hiltAndroidRule)
          .around(activityScenarioRule);

  @Test
  public void createButtonBringsToCreateScreen() {
    initializeWallet(activityScenarioRule);
    createButton().perform(click());
    fragmentContainer().check(matches(withChild(withId(createFragmentId()))));
  }

  @Test
  public void logOutMenuTest() {
    initializeWallet(activityScenarioRule);

    // Click on menu icon
    onView(withContentDescription("More options")).perform(click());
    walletLogOutMenuItem().perform(click());
    assertThat(dialogPositiveButton(), allOf(withText("CONFIRM"), isDisplayed()));
    assertThat(dialogNegativeButton(), allOf(withText("CANCEL"), isDisplayed()));
    dialogPositiveButton().performClick();
    fragmentContainer().check(matches(withChild(withId(walletId()))));
  }

  @Test
  public void clearDataTest() {
    initializeWallet(activityScenarioRule);

    // Click on menu icon
    onView(withContentDescription("More options")).perform(click());
    clearDataMenuItem().perform(click());
    assertThat(dialogPositiveButton(), allOf(withText("YES"), isDisplayed()));
    assertThat(dialogNegativeButton(), allOf(withText("NO"), isDisplayed()));
  }

  @Test
  public void testMoveAppToBackground() {
    // Launch HomeActivity
    ActivityScenario<HomeActivity> scenario = activityScenarioRule.getScenario();

    // Press back button
    onView(isRoot()).perform(pressBack());

    // Wait for 1 second
    SystemClock.sleep(1000);

    // Get the list of running tasks
    ActivityManager activityManager =
        (ActivityManager)
            InstrumentationRegistry.getInstrumentation()
                .getTargetContext()
                .getSystemService(Context.ACTIVITY_SERVICE);
    List<ActivityManager.AppTask> appTasks = activityManager.getAppTasks();

    // Check that the task is moved to the back
    assertTrue(appTasks.isEmpty());
  }

  public static void initializeWallet(
      ActivityScenarioRule<HomeActivity> activityActivityScenarioRule) {
    activityActivityScenarioRule
        .getScenario()
        .onActivity(
            activity -> {
              HomeViewModel viewModel = HomeActivity.obtainViewModel(activity);
              if (!viewModel.isWalletSetUp()) {
                dialogNeutralButton().performClick();
                confirmButton().perform(click());
                dialogPositiveButton().performClick();
              }
            });
  }
}
