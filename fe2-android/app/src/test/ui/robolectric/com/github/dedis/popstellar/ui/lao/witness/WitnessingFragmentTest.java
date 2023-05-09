package com.github.dedis.popstellar.ui.lao.witness;

import android.view.View;
import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.action.ViewActions;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.github.dedis.popstellar.model.objects.Lao;
import com.github.dedis.popstellar.model.objects.security.KeyPair;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.testutils.BundleBuilder;
import com.github.dedis.popstellar.testutils.fragment.ActivityFragmentScenarioRule;
import com.github.dedis.popstellar.testutils.pages.lao.LaoActivityPageObject;
import com.github.dedis.popstellar.ui.lao.LaoActivity;
import com.github.dedis.popstellar.utility.Constants;

import com.google.android.material.tabs.TabLayout;
import java.time.Instant;
import org.hamcrest.Matcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoTestRule;

import dagger.hilt.android.testing.HiltAndroidRule;
import dagger.hilt.android.testing.HiltAndroidTest;

import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static com.github.dedis.popstellar.testutils.Base64DataUtils.generatePoPToken;
import static com.github.dedis.popstellar.testutils.pages.lao.witness.WitnessingFragmentPageObject.getEventListFragment;
import static com.github.dedis.popstellar.testutils.pages.lao.witness.WitnessingFragmentPageObject.getRootView;
import static com.github.dedis.popstellar.testutils.pages.lao.witness.WitnessingFragmentPageObject.witnessMessageFragment;
import static com.github.dedis.popstellar.testutils.pages.lao.witness.WitnessingFragmentPageObject.witnessesFragment;
import static com.github.dedis.popstellar.testutils.pages.lao.witness.WitnessingFragmentPageObject.witnessingTabs;

@HiltAndroidTest
@RunWith(AndroidJUnit4.class)
public class WitnessingFragmentTest {

  private static final long CREATION_TIME = Instant.now().getEpochSecond();
  private static final String LAO_NAME = "laoName";

  private static final KeyPair SENDER_KEY_1 = generatePoPToken();

  private static final PublicKey SENDER_1 = SENDER_KEY_1.getPublicKey();
  private static final String LAO_ID = Lao.generateLaoId(SENDER_1, CREATION_TIME, LAO_NAME);

  @Rule public InstantTaskExecutorRule rule = new InstantTaskExecutorRule();

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
  public ActivityFragmentScenarioRule<LaoActivity, WitnessingFragment> activityScenarioRule =
      ActivityFragmentScenarioRule.launchIn(
          LaoActivity.class,
          new BundleBuilder().putString(Constants.LAO_ID_EXTRA, LAO_ID).build(),
          LaoActivityPageObject.containerId(),
          WitnessingFragment.class,
          WitnessingFragment::new);

  @Test
  public void testBackButtonBehaviour() {
    getRootView().perform(ViewActions.pressBack());
    // Check current fragment displayed is event list
    getEventListFragment().check(matches(isDisplayed()));
  }

  @Test
  public void testTabMenu() {
    witnessingTabs().check(matches(isDisplayed()));

    // Check that by default the witnesses are displayed
    witnessMessageFragment().check(doesNotExist());
    witnessesFragment().check(matches(isDisplayed()));

    // Select the messages tab
    witnessingTabs().perform(selectTabAtPosition(1));

    // Check that the witness messages are displayed and the witnesses are not displayed
    witnessMessageFragment().check(matches(isDisplayed()));
    witnessesFragment().check(doesNotExist());

    // Select the witnesses tab
    witnessingTabs().perform(selectTabAtPosition(0));

    // Check that the witnesses are displayed and the witnesses messages are not displayed
    witnessMessageFragment().check(doesNotExist());
    witnessesFragment().check(matches(isDisplayed()));
  }

  private static ViewAction selectTabAtPosition(final int tabIndex) {
    return new ViewAction() {
      @Override
      public Matcher<View> getConstraints() {
        return isAssignableFrom(TabLayout.class);
      }

      @Override
      public String getDescription() {
        return "select tab at index " + tabIndex;
      }

      @Override
      public void perform(UiController uiController, View view) {
        TabLayout tabLayout = (TabLayout) view;
        TabLayout.Tab tab = tabLayout.getTabAt(tabIndex);
        if (tab != null) {
          tab.select();
        }
      }
    };
  }

}
