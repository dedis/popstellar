package com.github.dedis.popstellar.ui.home;

import androidx.test.core.app.ActivityScenario.ActivityAction;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnit;

import dagger.hilt.android.testing.HiltAndroidRule;
import dagger.hilt.android.testing.HiltAndroidTest;

import static androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static com.github.dedis.popstellar.testutils.UITestUtils.dialogNegativeButton;
import static com.github.dedis.popstellar.testutils.UITestUtils.dialogPositiveButton;
import static com.github.dedis.popstellar.testutils.pages.home.HomePageObject.*;
import static com.github.dedis.popstellar.testutils.pages.home.LaoCreatePageObject.createFragmentId;
import static com.github.dedis.popstellar.testutils.pages.home.WalletPageObject.*;
import static org.hamcrest.Matchers.allOf;

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
    ActivityAction<HomeActivity> action =
        activity -> {
          HomeViewModel viewModel = HomeActivity.obtainViewModel(activity);
          if (!viewModel.isWalletSetUp()) {
            initializeWallet();
          }
          createButton().perform(click());
          fragmentContainer().check(matches(withChild(withId(createFragmentId()))));
        };
    executeAction(action);
  }

  @Test
  public void logOutMenuTest(){
      ActivityAction<HomeActivity> action =
              activity -> {
                  HomeViewModel viewModel = HomeActivity.obtainViewModel(activity);
                  if (!viewModel.isWalletSetUp()) {
                      initializeWallet();
                  }
                  openActionBarOverflowOrOptionsMenu(
                          InstrumentationRegistry.getInstrumentation().getTargetContext());
                  walletLogOutMenuItem().perform(click());
                  assertThat(dialogPositiveButton(), allOf(withText("CONFIRM"), isDisplayed()));
                  assertThat(dialogNegativeButton(), allOf(withText("CANCEL"), isDisplayed()));
              };
      executeAction(action);
  }


  public static void initializeWallet() {
    openActionBarOverflowOrOptionsMenu(
        InstrumentationRegistry.getInstrumentation().getTargetContext());
    walletSetupMenuItem().perform(click());
    confirmButton().perform(click());
    dialogPositiveButton().performClick();
  }

  private void executeAction(ActivityAction<HomeActivity> action) {
    activityScenarioRule.getScenario().onActivity(action);
  }
}
