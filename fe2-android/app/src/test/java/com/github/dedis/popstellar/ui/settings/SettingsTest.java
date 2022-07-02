package com.github.dedis.popstellar.ui.settings;

import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static com.github.dedis.popstellar.ui.pages.settings.SettingsPageObject.applyButton;
import static com.github.dedis.popstellar.ui.pages.settings.SettingsPageObject.pageTitle;
import static com.github.dedis.popstellar.ui.pages.settings.SettingsPageObject.serverUrlEditText;

import androidx.test.espresso.action.ViewActions;
import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.github.dedis.popstellar.testutils.fragment.FragmentScenarioRule;
import com.github.dedis.popstellar.ui.home.HomeActivity;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;

import dagger.hilt.android.testing.HiltAndroidRule;
import dagger.hilt.android.testing.HiltAndroidTest;

@HiltAndroidTest
@RunWith(AndroidJUnit4.class)
public class SettingsTest {

  @Rule(order = 0)
  public final HiltAndroidRule hiltRule = new HiltAndroidRule(this);
  
  @Rule(order = 1)
  public final FragmentScenarioRule<SettingsFragment> fragmentRule =
      FragmentScenarioRule.launch(SettingsFragment.class);

  @Test
  public void uiElementsAreDisplayed() {
    pageTitle().check(matches(isDisplayed()));
    serverUrlEditText().check(matches(isDisplayed()));
    applyButton().check(matches(isDisplayed()));
  }

  @Test
  public void applyBringsToHome() {
    Intents.init();
    serverUrlEditText().perform(ViewActions.replaceText("NEW TEXT"));
    applyButton().check(matches(isEnabled()));
    applyButton().perform(click());
    intended(hasComponent(HomeActivity.class.getName()));
    Intents.release();
  }
}
