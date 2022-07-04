package com.github.dedis.popstellar.ui.home;

import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withChild;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static com.github.dedis.popstellar.ui.pages.home.HomePageObject.fragmentContainer;
import static com.github.dedis.popstellar.ui.pages.home.HomePageObject.homeFragmentContainerId;
import static com.github.dedis.popstellar.ui.pages.home.HomePageObject.homeFragmentId;
import static com.github.dedis.popstellar.ui.pages.home.LaunchPageObject.bodyText;
import static com.github.dedis.popstellar.ui.pages.home.LaunchPageObject.cancelButtonLaunch;
import static com.github.dedis.popstellar.ui.pages.home.LaunchPageObject.confirmButtonLaunch;
import static com.github.dedis.popstellar.ui.pages.home.LaunchPageObject.laoNameEntry;
import static com.github.dedis.popstellar.ui.pages.home.LaunchPageObject.titleText;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.test.espresso.action.ViewActions;
import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.github.dedis.popstellar.model.objects.Lao;
import com.github.dedis.popstellar.model.objects.security.KeyPair;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.repository.LAORepository;
import com.github.dedis.popstellar.repository.remote.GlobalNetworkManager;
import com.github.dedis.popstellar.repository.remote.MessageSender;
import com.github.dedis.popstellar.testutils.Base64DataUtils;
import com.github.dedis.popstellar.testutils.fragment.ActivityFragmentScenarioRule;
import com.github.dedis.popstellar.ui.detail.LaoDetailActivity;
import com.github.dedis.popstellar.utility.security.KeyManager;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoTestRule;

import java.util.Collections;

import dagger.hilt.android.testing.BindValue;
import dagger.hilt.android.testing.HiltAndroidRule;
import dagger.hilt.android.testing.HiltAndroidTest;
import io.reactivex.Completable;
import io.reactivex.subjects.BehaviorSubject;

@HiltAndroidTest
@RunWith(AndroidJUnit4.class)
public class LaunchFragmentTest {

  private static final String LAO_NAME = "LAO";
  private static final KeyPair KEY_PAIR = Base64DataUtils.generateKeyPair();
  private static final PublicKey PK = KEY_PAIR.getPublicKey();
  private static final Lao LAO = new Lao(LAO_NAME, PK, 10223421);

  @BindValue @Mock LAORepository repository;
  @BindValue @Mock KeyManager keyManager;
  @BindValue @Mock MessageSender messageSender;
  @BindValue @Mock GlobalNetworkManager networkManager;

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
          when(repository.getLaoObservable(anyString()))
              .thenReturn(BehaviorSubject.createDefault(LAO));
          when(repository.getAllLaos())
              .thenReturn(BehaviorSubject.createDefault(Collections.singletonList(LAO)));

          when(keyManager.getMainPublicKey()).thenReturn(PK);
          when(keyManager.getMainKeyPair()).thenReturn(KEY_PAIR);
          when(networkManager.getMessageSender()).thenReturn(messageSender);
          when(messageSender.subscribe(any())).then(args -> Completable.complete());
          when(messageSender.publish(any(), any())).then(args -> Completable.complete());
          when(messageSender.publish(any(), any(), any())).then(args -> Completable.complete());
        }
      };

  @Rule(order = 3)
  public ActivityFragmentScenarioRule<HomeActivity, LaunchFragment> activityScenarioRule =
      ActivityFragmentScenarioRule.launchIn(
          HomeActivity.class,
          homeFragmentContainerId(),
          LaunchFragment.class,
          LaunchFragment::newInstance);

  @Test
  public final void uiElementsAreDisplayed() {
    bodyText().check(matches(isDisplayed()));
    titleText().check(matches(isDisplayed()));
    laoNameEntry().check(matches(isDisplayed()));
    cancelButtonLaunch().check(matches(isDisplayed()));
    confirmButtonLaunch().check(matches(isDisplayed()));
  }

  @Test
  public void confirmButtonGoesToLaoDetail() {
    Intents.init();
    laoNameEntry().perform(ViewActions.replaceText(LAO_NAME));
    confirmButtonLaunch().check(matches(isEnabled()));
    confirmButtonLaunch().perform(click());
    intended(hasComponent(LaoDetailActivity.class.getName()));
    Intents.release();
  }

  @Test
  public void cancelButtonGoesToHome() {
    cancelButtonLaunch().perform(click());
    fragmentContainer().check(matches(withChild(withId(homeFragmentId()))));
  }
}
