package com.github.dedis.popstellar.ui.lao.socialmedia;

import android.content.res.Configuration;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.fragment.app.Fragment;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.model.objects.Lao;
import com.github.dedis.popstellar.model.objects.security.KeyPair;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.testutils.BundleBuilder;
import com.github.dedis.popstellar.testutils.fragment.ActivityFragmentScenarioRule;
import com.github.dedis.popstellar.testutils.pages.lao.LaoActivityPageObject;
import com.github.dedis.popstellar.ui.lao.LaoActivity;
import com.github.dedis.popstellar.utility.Constants;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoTestRule;

import dagger.hilt.android.testing.HiltAndroidRule;
import dagger.hilt.android.testing.HiltAndroidTest;

import static androidx.test.espresso.action.ViewActions.pressBack;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static com.github.dedis.popstellar.testutils.Base64DataUtils.generatePoPToken;
import static com.github.dedis.popstellar.testutils.pages.lao.socialmedia.SocialMediaHomePageObject.getEventListFragment;
import static com.github.dedis.popstellar.testutils.pages.lao.socialmedia.SocialMediaHomePageObject.getRootView;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@HiltAndroidTest
@RunWith(AndroidJUnit4.class)
public class SocialMediaHomeFragmentTest {

  private static final long CREATION_TIME = 1631280815;
  private static final String LAO_NAME = "laoName";

  private static final KeyPair SENDER_KEY_1 = generatePoPToken();
  private static final KeyPair SENDER_KEY_2 = generatePoPToken();

  private static final PublicKey SENDER_1 = SENDER_KEY_1.publicKey;
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
  public ActivityFragmentScenarioRule<LaoActivity, SocialMediaHomeFragment> activityScenarioRule =
      ActivityFragmentScenarioRule.launchIn(
          LaoActivity.class,
          new BundleBuilder().putString(Constants.LAO_ID_EXTRA, LAO_ID).build(),
          LaoActivityPageObject.containerId(),
          SocialMediaHomeFragment.class,
          SocialMediaHomeFragment::new);

  @Test
  public void testBackButtonBehaviour() {
    getRootView().perform(pressBack());
    // Check current fragment displayed is event list
    getEventListFragment().check(matches(isDisplayed()));
  }

  @Test
  public void handleRotationTest() {
    activityScenarioRule
        .getScenario()
        .onActivity(
            activity -> {
              Fragment before =
                  activity
                      .getSupportFragmentManager()
                      .findFragmentById(R.id.fragment_container_lao);
              Configuration config = new Configuration(activity.getResources().getConfiguration());
              config.orientation = Configuration.ORIENTATION_LANDSCAPE;
              activity.onConfigurationChanged(config);
              Fragment after =
                  activity
                      .getSupportFragmentManager()
                      .findFragmentById(R.id.fragment_container_lao);
              assertEquals(after, before);
              assertTrue(after instanceof SocialMediaHomeFragment);
            });
  }
}
