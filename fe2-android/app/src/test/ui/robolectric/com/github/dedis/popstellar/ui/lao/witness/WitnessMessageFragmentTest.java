package com.github.dedis.popstellar.ui.lao.witness;

import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static com.github.dedis.popstellar.testutils.Base64DataUtils.generateKeyPair;
import static com.github.dedis.popstellar.testutils.pages.lao.witness.WitnessMessageFragmentPageObject.witnessMessageList;
import static com.github.dedis.popstellar.testutils.pages.lao.witness.WitnessingFragmentPageObject.getEventListFragment;
import static com.github.dedis.popstellar.testutils.pages.lao.witness.WitnessingFragmentPageObject.getRootView;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.test.espresso.action.ViewActions;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.github.dedis.popstellar.model.network.method.message.data.lao.CreateLao;
import com.github.dedis.popstellar.model.objects.security.KeyPair;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.testutils.BundleBuilder;
import com.github.dedis.popstellar.testutils.fragment.ActivityFragmentScenarioRule;
import com.github.dedis.popstellar.testutils.pages.lao.LaoActivityPageObject;
import com.github.dedis.popstellar.ui.lao.LaoActivity;
import com.github.dedis.popstellar.utility.Constants;
import dagger.hilt.android.testing.HiltAndroidRule;
import dagger.hilt.android.testing.HiltAndroidTest;
import java.util.ArrayList;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoTestRule;

@HiltAndroidTest
@RunWith(AndroidJUnit4.class)
public class WitnessMessageFragmentTest {

  private static final KeyPair SENDER_KEY = generateKeyPair();
  private static final PublicKey SENDER = SENDER_KEY.getPublicKey();

  private static final CreateLao CREATE_LAO = new CreateLao("lao", SENDER, new ArrayList<>());

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
  public ActivityFragmentScenarioRule<LaoActivity, WitnessMessageFragment> activityScenarioRule =
      ActivityFragmentScenarioRule.launchIn(
          LaoActivity.class,
          new BundleBuilder().putString(Constants.LAO_ID_EXTRA, CREATE_LAO.getId()).build(),
          LaoActivityPageObject.containerId(),
          WitnessMessageFragment.class,
          WitnessMessageFragment::new);

  @Test
  public void testWitnessMessageListIsDisplayed() {
    witnessMessageList().check(matches(isDisplayed()));
  }
}
