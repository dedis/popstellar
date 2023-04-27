package com.github.dedis.popstellar.ui.home;

import androidx.preference.EditTextPreference;
import androidx.preference.SwitchPreference;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.github.dedis.popstellar.testutils.BundleBuilder;
import com.github.dedis.popstellar.testutils.fragment.ActivityFragmentScenarioRule;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoTestRule;

import dagger.hilt.android.testing.HiltAndroidRule;
import dagger.hilt.android.testing.HiltAndroidTest;

import static com.github.dedis.popstellar.testutils.pages.home.HomePageObject.homeFragmentContainerId;
import static com.github.dedis.popstellar.testutils.pages.home.SettingsPageObject.serverUrlKey;
import static com.github.dedis.popstellar.testutils.pages.home.SettingsPageObject.switchLogging;
import static org.junit.Assert.*;

@HiltAndroidTest
@RunWith(AndroidJUnit4.class)
public class SettingsFragmentTest {

  private static final String CORRECT_URL = "wss://localhost:9000";

  @Rule(order = 0)
  public final MockitoTestRule mockitoRule = MockitoJUnit.testRule(this);

  @Rule(order = 1)
  public final HiltAndroidRule hiltRule = new HiltAndroidRule(this);

  @Rule(order = 2)
  public final ExternalResource setupRule =
      new ExternalResource() {
        @Override
        protected void before() {
          hiltRule.inject();
        }
      };

  @Rule(order = 3)
  public ActivityFragmentScenarioRule<HomeActivity, SettingsFragment> activityScenarioRule =
      ActivityFragmentScenarioRule.launchIn(
          HomeActivity.class,
          new BundleBuilder().build(),
          homeFragmentContainerId(),
          SettingsFragment.class,
          SettingsFragment::newInstance,
          new BundleBuilder().build());

  @Test
  public void testCorrectServerUrlEnableLogging() {
    activityScenarioRule
        .getScenario()
        .onFragment(
            fragment -> {
              EditTextPreference serverUrlPreference =
                  fragment
                      .getPreferenceManager()
                      .findPreference(fragment.getString(serverUrlKey()));
              SwitchPreference switchPreference =
                  fragment
                      .getPreferenceManager()
                      .findPreference(fragment.getString(switchLogging()));

              assertNotNull(switchPreference);
              assertNotNull(serverUrlPreference);

              // Check that the logging is disabled
              assertFalse(switchPreference.isEnabled());

              serverUrlPreference.callChangeListener(CORRECT_URL);

              // Check that after a correct url set, switch is enabled
              assertTrue(switchPreference.isEnabled());
            });
  }

  @Test
  public void testIncorrectServerUrl() {
    String wrongUrl = "w://example";
    activityScenarioRule
        .getScenario()
        .onFragment(
            fragment -> {
              EditTextPreference serverUrlPreference =
                  fragment
                      .getPreferenceManager()
                      .findPreference(fragment.getString(serverUrlKey()));
              SwitchPreference switchPreference =
                  fragment
                      .getPreferenceManager()
                      .findPreference(fragment.getString(switchLogging()));

              assertNotNull(switchPreference);
              assertNotNull(serverUrlPreference);

              // Check that the logging is disabled
              assertFalse(switchPreference.isEnabled());

              serverUrlPreference.callChangeListener(wrongUrl);

              // Check that after a not correct url set, switch is still disabled
              assertFalse(switchPreference.isEnabled());
            });
  }

  @Test
  public void testEnableLogging() {
    activityScenarioRule
        .getScenario()
        .onFragment(
            fragment -> {
              EditTextPreference serverUrlPreference =
                  fragment
                      .getPreferenceManager()
                      .findPreference(fragment.getString(serverUrlKey()));
              SwitchPreference switchPreference =
                  fragment
                      .getPreferenceManager()
                      .findPreference(fragment.getString(switchLogging()));

              assertNotNull(serverUrlPreference);
              assertNotNull(switchPreference);

              serverUrlPreference.callChangeListener(CORRECT_URL);

              // Turn on the switch
              switchPreference.callChangeListener(true);

              // Ensures that the server url edit text is disabled now
              assertFalse(serverUrlPreference.isEnabled());
            });
  }

  @Test
  public void testDisableLogging() {
    activityScenarioRule
        .getScenario()
        .onFragment(
            fragment -> {
              EditTextPreference serverUrlPreference =
                  fragment
                      .getPreferenceManager()
                      .findPreference(fragment.getString(serverUrlKey()));
              SwitchPreference switchPreference =
                  fragment
                      .getPreferenceManager()
                      .findPreference(fragment.getString(switchLogging()));

              assertNotNull(serverUrlPreference);
              assertNotNull(switchPreference);

              serverUrlPreference.setText(CORRECT_URL);

              // Turn off the switch
              switchPreference.callChangeListener(false);

              // Ensures that the server url edit text is re-enabled
              assertTrue(serverUrlPreference.isEnabled());
            });
  }
}
