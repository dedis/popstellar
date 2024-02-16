package com.github.dedis.popstellar.ui.home

import android.content.res.Configuration
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.dedis.popstellar.R
import com.github.dedis.popstellar.model.objects.Lao
import com.github.dedis.popstellar.testutils.Base64DataUtils
import com.github.dedis.popstellar.testutils.BundleBuilder
import com.github.dedis.popstellar.testutils.IntentUtils
import com.github.dedis.popstellar.testutils.pages.home.ConnectingPageObject
import com.github.dedis.popstellar.utility.Constants
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoTestRule

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class ConnectingActivityTest {
  @JvmField @Rule(order = 0) val mockitoRule: MockitoTestRule = MockitoJUnit.testRule(this)

  @JvmField @Rule(order = 1) val hiltRule = HiltAndroidRule(this)

  @JvmField
  @Rule(order = 3)
  val activityScenarioRule =
    ActivityScenarioRule<ConnectingActivity>(
      IntentUtils.createIntent(
        ConnectingActivity::class.java,
        BundleBuilder()
          .putString(Constants.LAO_ID_EXTRA, LAO_ID)
          .putString(Constants.ACTIVITY_TO_OPEN_EXTRA, Constants.LAO_DETAIL_EXTRA)
          .putString(Constants.CONNECTION_PURPOSE_EXTRA, Constants.JOINING_EXTRA)
          .build()
      )
    )

  @Before
  fun setup() {
    hiltRule.inject()
  }

  @Test
  fun basicElementsAreDisplayed() {
    ConnectingPageObject.connectingText().check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    ConnectingPageObject.laoConnectingText()
      .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    ConnectingPageObject.progressBar().check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    ConnectingPageObject.cancelButton().check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
  }

  @Test
  fun staticTextTest() {
    ConnectingPageObject.connectingText()
      .check(ViewAssertions.matches(ViewMatchers.withText("Connecting to")))
    ConnectingPageObject.cancelButton()
      .check(ViewAssertions.matches(ViewMatchers.withText("Cancel")))
  }

  @Test
  fun dynamicTextTest() {
    ConnectingPageObject.laoConnectingText()
      .check(ViewAssertions.matches(ViewMatchers.withText(LAO_ID)))
  }

  @Test
  fun cancelButtonLaunchesHomeTest() {
    Intents.init()
    ConnectingPageObject.cancelButton().perform(ViewActions.click())
    Intents.intended(IntentMatchers.hasComponent(HomeActivity::class.java.name))
    Intents.release()
  }

  @Test
  fun handleRotationTest() {
    activityScenarioRule.scenario.onActivity { activity: ConnectingActivity ->
      val before =
        activity.supportFragmentManager.findFragmentById(R.id.fragment_container_connecting)
      val config = Configuration(activity.resources.configuration)
      config.orientation = Configuration.ORIENTATION_LANDSCAPE
      activity.onConfigurationChanged(config)
      Assert.assertEquals(
        activity.supportFragmentManager.findFragmentById(R.id.fragment_container_connecting),
        before
      )
    }
  }

  companion object {
    private const val LAO_NAME = "Lao Name"
    private val PK = Base64DataUtils.generatePublicKey()
    private val LAO = Lao(LAO_NAME, PK, 10223421)
    private val LAO_ID = LAO.id
  }
}
