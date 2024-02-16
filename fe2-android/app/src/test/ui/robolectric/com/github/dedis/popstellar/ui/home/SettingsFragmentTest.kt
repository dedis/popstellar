package com.github.dedis.popstellar.ui.home

import androidx.preference.EditTextPreference
import androidx.preference.SwitchPreference
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.dedis.popstellar.testutils.BundleBuilder
import com.github.dedis.popstellar.testutils.fragment.ActivityFragmentScenarioRule
import com.github.dedis.popstellar.testutils.pages.home.HomePageObject
import com.github.dedis.popstellar.testutils.pages.home.SettingsPageObject
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoTestRule

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class SettingsFragmentTest {
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
  var activityScenarioRule: ActivityFragmentScenarioRule<HomeActivity, SettingsFragment> =
    ActivityFragmentScenarioRule.launchIn(
      HomeActivity::class.java,
      BundleBuilder().build(),
      HomePageObject.homeFragmentContainerId(),
      SettingsFragment::class.java,
      { SettingsFragment.newInstance() },
      BundleBuilder().build()
    )

  @Test
  fun testCorrectServerUrlEnableLogging() {
    activityScenarioRule.scenario.onFragment { fragment: SettingsFragment ->
      val serverUrlPreference =
        fragment.preferenceManager.findPreference<EditTextPreference>(
          fragment.getString(SettingsPageObject.serverUrlKey())
        )
      val switchPreference =
        fragment.preferenceManager.findPreference<SwitchPreference>(
          fragment.getString(SettingsPageObject.switchLogging())
        )
      Assert.assertNotNull(switchPreference)
      Assert.assertNotNull(serverUrlPreference)

      // Check that the logging is disabled
      Assert.assertFalse(switchPreference!!.isEnabled)
      serverUrlPreference!!.callChangeListener(CORRECT_URL)

      // Check that after a correct url set, switch is enabled
      Assert.assertTrue(switchPreference.isEnabled)
    }
  }

  @Test
  fun testIncorrectServerUrl() {
    val wrongUrl = "w://example"
    activityScenarioRule.scenario.onFragment { fragment: SettingsFragment ->
      val serverUrlPreference =
        fragment.preferenceManager.findPreference<EditTextPreference>(
          fragment.getString(SettingsPageObject.serverUrlKey())
        )
      val switchPreference =
        fragment.preferenceManager.findPreference<SwitchPreference>(
          fragment.getString(SettingsPageObject.switchLogging())
        )
      Assert.assertNotNull(switchPreference)
      Assert.assertNotNull(serverUrlPreference)

      // Check that the logging is disabled
      Assert.assertFalse(switchPreference!!.isEnabled)
      serverUrlPreference!!.callChangeListener(wrongUrl)

      // Check that after a not correct url set, switch is still disabled
      Assert.assertFalse(switchPreference.isEnabled)
    }
  }

  @Test
  fun testEnableLogging() {
    activityScenarioRule.scenario.onFragment { fragment: SettingsFragment ->
      val serverUrlPreference =
        fragment.preferenceManager.findPreference<EditTextPreference>(
          fragment.getString(SettingsPageObject.serverUrlKey())
        )
      val switchPreference =
        fragment.preferenceManager.findPreference<SwitchPreference>(
          fragment.getString(SettingsPageObject.switchLogging())
        )
      Assert.assertNotNull(serverUrlPreference)
      Assert.assertNotNull(switchPreference)
      serverUrlPreference!!.callChangeListener(CORRECT_URL)

      // Turn on the switch
      switchPreference!!.callChangeListener(true)

      // Ensures that the server url edit text is disabled now
      Assert.assertFalse(serverUrlPreference.isEnabled)
    }
  }

  @Test
  fun testDisableLogging() {
    activityScenarioRule.scenario.onFragment { fragment: SettingsFragment ->
      val serverUrlPreference =
        fragment.preferenceManager.findPreference<EditTextPreference>(
          fragment.getString(SettingsPageObject.serverUrlKey())
        )
      val switchPreference =
        fragment.preferenceManager.findPreference<SwitchPreference>(
          fragment.getString(SettingsPageObject.switchLogging())
        )
      Assert.assertNotNull(serverUrlPreference)
      Assert.assertNotNull(switchPreference)
      serverUrlPreference!!.text = CORRECT_URL

      // Turn off the switch
      switchPreference!!.callChangeListener(false)

      // Ensures that the server url edit text is re-enabled
      Assert.assertTrue(serverUrlPreference.isEnabled)
    }
  }

  companion object {
    private const val CORRECT_URL = "wss://localhost:9000"
  }
}
