package com.github.dedis.popstellar.ui.lao.witness

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.dedis.popstellar.model.objects.Lao.Companion.generateLaoId
import com.github.dedis.popstellar.model.objects.security.KeyPair
import com.github.dedis.popstellar.testutils.Base64DataUtils
import com.github.dedis.popstellar.testutils.BundleBuilder
import com.github.dedis.popstellar.testutils.MatcherUtils
import com.github.dedis.popstellar.testutils.fragment.ActivityFragmentScenarioRule
import com.github.dedis.popstellar.testutils.pages.lao.LaoActivityPageObject
import com.github.dedis.popstellar.testutils.pages.lao.witness.WitnessingFragmentPageObject
import com.github.dedis.popstellar.ui.lao.LaoActivity
import com.github.dedis.popstellar.utility.Constants
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import java.time.Instant
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoTestRule

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class WitnessingFragmentTest {
  @JvmField @Rule var rule = InstantTaskExecutorRule()

  @JvmField @Rule(order = 0) val mockitoRule: MockitoTestRule = MockitoJUnit.testRule(this)

  @JvmField @Rule(order = 1) val hiltRule = HiltAndroidRule(this)

  @JvmField
  @Rule(order = 2)
  val setupRule: ExternalResource =
    object : ExternalResource() {
      override fun before() {
        hiltRule.inject()
      }
    }

  @JvmField
  @Rule(order = 3)
  var activityScenarioRule: ActivityFragmentScenarioRule<LaoActivity, WitnessingFragment> =
    ActivityFragmentScenarioRule.launchIn(
      LaoActivity::class.java,
      BundleBuilder().putString(Constants.LAO_ID_EXTRA, LAO_ID).build(),
      LaoActivityPageObject.containerId(),
      WitnessingFragment::class.java
    ) {
      WitnessingFragment()
    }

  @Test
  fun testBackButtonBehaviour() {
    WitnessingFragmentPageObject.getRootView().perform(ViewActions.pressBack())
    // Check current fragment displayed is event list
    WitnessingFragmentPageObject.getEventListFragment()
      .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
  }

  @Test
  fun testTabMenu() {
    WitnessingFragmentPageObject.witnessingTabs()
      .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))

    // Check that by default the witness messages are displayed
    WitnessingFragmentPageObject.witnessMessageFragment()
      .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    WitnessingFragmentPageObject.witnessesFragment().check(ViewAssertions.doesNotExist())

    // Select the messages tab
    WitnessingFragmentPageObject.witnessingTabs().perform(MatcherUtils.selectTabAtPosition(0))

    // Check that the witness messages are displayed and the witnesses are not displayed
    WitnessingFragmentPageObject.witnessMessageFragment()
      .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    WitnessingFragmentPageObject.witnessesFragment().check(ViewAssertions.doesNotExist())

    // Select the witnesses tab
    WitnessingFragmentPageObject.witnessingTabs().perform(MatcherUtils.selectTabAtPosition(1))

    // Check that the witnesses are displayed and the witnesses messages are not displayed
    WitnessingFragmentPageObject.witnessMessageFragment().check(ViewAssertions.doesNotExist())
    WitnessingFragmentPageObject.witnessesFragment()
      .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
  }

  companion object {
    private val CREATION_TIME = Instant.now().epochSecond
    private const val LAO_NAME = "laoName"
    private val SENDER_KEY_1: KeyPair = Base64DataUtils.generatePoPToken()
    private val SENDER_1 = SENDER_KEY_1.publicKey
    private val LAO_ID = generateLaoId(SENDER_1, CREATION_TIME, LAO_NAME)
  }
}
