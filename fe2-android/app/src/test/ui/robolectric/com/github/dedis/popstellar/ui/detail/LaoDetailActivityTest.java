package com.github.dedis.popstellar.ui.detail;

import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.github.dedis.popstellar.model.objects.Lao;
import com.github.dedis.popstellar.repository.LAORepository;
import com.github.dedis.popstellar.repository.remote.GlobalNetworkManager;
import com.github.dedis.popstellar.testutils.*;
import com.github.dedis.popstellar.ui.digitalcash.DigitalCashActivity;
import com.github.dedis.popstellar.ui.socialmedia.SocialMediaActivity;
import com.google.gson.Gson;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.*;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;

import javax.inject.Inject;

import dagger.hilt.android.testing.*;
import io.reactivex.subjects.BehaviorSubject;

import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.matcher.ViewMatchers.withChild;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static com.github.dedis.popstellar.testutils.pages.detail.LaoDetailActivityPageObject.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@HiltAndroidTest
@RunWith(AndroidJUnit4.class)
public class LaoDetailActivityTest {

  private static final Lao LAO = new Lao("LAO", Base64DataUtils.generatePublicKey(), 10223421);
  private static final String LAO_ID = LAO.getId();

  @Inject GlobalNetworkManager networkManager;
  @Inject Gson gson;

  @BindValue @Mock LAORepository laoRepository;

  // Hilt rule
  private final HiltAndroidRule hiltAndroidRule = new HiltAndroidRule(this);
  // Setup rule, used to setup things before the activity is started
  private final TestRule setupRule =
      new ExternalResource() {
        @Override
        protected void before() {
          hiltAndroidRule.inject();

          when(laoRepository.getLaoObservable(anyString()))
              .thenReturn(BehaviorSubject.createDefault(LAO));
        }
      };
  // Activity scenario rule that starts the activity.
  // It creates a LaoDetailActivity with set extras such that the LAO is used
  public ActivityScenarioRule<LaoDetailActivity> activityScenarioRule =
      new ActivityScenarioRule<>(
          IntentUtils.createIntent(
              LaoDetailActivity.class,
              new BundleBuilder()
                  .putString(laoIdExtra(), LAO_ID)
                  .putString(fragmentToOpenExtra(), laoDetailValue())
                  .build()));

  @Rule
  public final RuleChain rule =
      RuleChain.outerRule(MockitoJUnit.testRule(this))
          .around(hiltAndroidRule)
          .around(setupRule)
          .around(activityScenarioRule);

  @Test
  public void identityTabOpensIdentityTab() {
    identityButton().perform(click());
    fragmentContainer().check(matches(withChild(withId(identityFragmentId()))));
  }

  @Test
  public void witnessingTabShowsWitnessTab() {
    witnessButton().perform(click());
    fragmentContainer().check(matches(withChild(withId(witnessingFragmentId()))));
  }

  @Test
  public void cancelGoesBackToEventList() {
    identityButton().perform(click());
    toolBarBackButton().perform(click());
    fragmentContainer().check(matches(withChild(withId(laoDetailFragmentId()))));
  }

  @Test
  public void socialMediaNavOpensSocialMediaActivity() {
    Intents.init();
    socialMediaButton().perform(click());
    intended(hasComponent(SocialMediaActivity.class.getName()));
    Intents.release();
  }

  @Test
  public void digitalCashNavOpensActivity() {
    Intents.init();
    digitalCashButton().perform(click());
    intended(hasComponent(DigitalCashActivity.class.getName()));
    Intents.release();
  }
}
