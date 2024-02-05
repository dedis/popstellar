package com.github.dedis.popstellar.ui.home

import android.content.res.Configuration
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.dedis.popstellar.R
import com.github.dedis.popstellar.testutils.UITestUtils.dialogNegativeButton
import com.github.dedis.popstellar.testutils.UITestUtils.dialogNeutralButton
import com.github.dedis.popstellar.testutils.UITestUtils.dialogPositiveButton
import com.github.dedis.popstellar.testutils.pages.home.HomePageObject
import com.github.dedis.popstellar.testutils.pages.home.LaoCreatePageObject
import com.github.dedis.popstellar.testutils.pages.home.WalletPageObject
import com.github.dedis.popstellar.ui.home.HomeActivity.Companion.obtainViewModel
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.hamcrest.Matchers
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnit

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class HomeActivityTest {
  //  Hilt rule
  private val hiltAndroidRule = HiltAndroidRule(this)

  // Activity scenario rule that starts the activity.
  var activityScenarioRule = ActivityScenarioRule(HomeActivity::class.java)

  @JvmField
  @Rule
  val rule: RuleChain =
    RuleChain.outerRule(MockitoJUnit.testRule(this))
      .around(hiltAndroidRule)
      .around(activityScenarioRule)

  @Test
  fun createButtonBringsToCreateScreen() {
    initializeWallet(activityScenarioRule)
    HomePageObject.createButton().perform(ViewActions.click())
    HomePageObject.fragmentContainer()
      .check(
        ViewAssertions.matches(
          ViewMatchers.withChild(ViewMatchers.withId(LaoCreatePageObject.createFragmentId()))
        )
      )
  }

  @Test
  fun logOutMenuTest() {
    initializeWallet(activityScenarioRule)

    // Click on menu icon
    Espresso.onView(ViewMatchers.withContentDescription("More options"))
      .perform(ViewActions.click())
    HomePageObject.walletLogOutMenuItem().perform(ViewActions.click())
    ViewMatchers.assertThat(
      dialogPositiveButton(),
      Matchers.allOf(ViewMatchers.withText("CONFIRM"), ViewMatchers.isDisplayed())
    )
    ViewMatchers.assertThat(
      dialogNegativeButton(),
      Matchers.allOf(ViewMatchers.withText("CANCEL"), ViewMatchers.isDisplayed())
    )
    dialogPositiveButton().performClick()
    HomePageObject.fragmentContainer()
      .check(
        ViewAssertions.matches(
          ViewMatchers.withChild(ViewMatchers.withId(WalletPageObject.walletId()))
        )
      )
  }

  @Test
  fun clearDataTest() {
    initializeWallet(activityScenarioRule)

    // Click on menu icon
    Espresso.onView(ViewMatchers.withContentDescription("More options"))
      .perform(ViewActions.click())
    HomePageObject.clearDataMenuItem().perform(ViewActions.click())
    ViewMatchers.assertThat(
      dialogPositiveButton(),
      Matchers.allOf(ViewMatchers.withText("YES"), ViewMatchers.isDisplayed())
    )
    ViewMatchers.assertThat(
      dialogNegativeButton(),
      Matchers.allOf(ViewMatchers.withText("NO"), ViewMatchers.isDisplayed())
    )
  }

  @Test
  fun handleRotationTest() {
    activityScenarioRule.scenario.onActivity { activity: HomeActivity ->
      val before = activity.supportFragmentManager.findFragmentById(R.id.fragment_container_home)
      val config = Configuration(activity.resources.configuration)
      config.orientation = Configuration.ORIENTATION_LANDSCAPE
      activity.onConfigurationChanged(config)
      Assert.assertEquals(
        activity.supportFragmentManager.findFragmentById(R.id.fragment_container_home),
        before
      )
    }
  }

  companion object {
    fun initializeWallet(activityActivityScenarioRule: ActivityScenarioRule<HomeActivity>) {
      activityActivityScenarioRule.scenario.onActivity { activity: HomeActivity ->
        val viewModel = obtainViewModel(activity)
        if (!viewModel.isWalletSetUp) {
          dialogNeutralButton().performClick()
          WalletPageObject.confirmButton().perform(ViewActions.click())
          dialogPositiveButton().performClick()
        }
      }
    }
  }
}
