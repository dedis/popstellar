package com.github.dedis.popstellar.ui.home;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.test.espresso.action.ViewActions;
import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.github.dedis.popstellar.model.objects.Lao;
import com.github.dedis.popstellar.model.objects.security.KeyPair;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.model.objects.view.LaoView;
import com.github.dedis.popstellar.repository.LAORepository;
import com.github.dedis.popstellar.repository.remote.GlobalNetworkManager;
import com.github.dedis.popstellar.repository.remote.MessageSender;
import com.github.dedis.popstellar.testutils.Base64DataUtils;
import com.github.dedis.popstellar.utility.error.UnknownLaoException;
import com.github.dedis.popstellar.utility.security.KeyManager;

import org.junit.*;
import org.junit.rules.ExternalResource;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoTestRule;

import java.util.Collections;

import dagger.hilt.android.testing.*;
import io.reactivex.subjects.BehaviorSubject;

import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static com.github.dedis.popstellar.testutils.pages.home.HomePageObject.*;
import static com.github.dedis.popstellar.testutils.pages.home.LaoCreatePageObject.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@HiltAndroidTest
@RunWith(AndroidJUnit4.class)
public class LaoCreateFragmentTest {

  private static final String LAO_NAME = "LAO";
  private static final String SERVER_URL = "localhost";
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
        protected void before() throws UnknownLaoException {
          hiltRule.inject();

          when(repository.getAllLaoIds())
              .thenReturn(BehaviorSubject.createDefault(Collections.singletonList(LAO.getId())));
          when(repository.getLaoView(anyString())).thenReturn(new LaoView(LAO));
        }
      };

  @Rule(order = 3)
  public ActivityScenarioRule<HomeActivity> activityScenarioRule =
      new ActivityScenarioRule<>(HomeActivity.class);

  @Before
  public void setup() {
    // Open the launch tab
    HomeActivityTest.initializeWallet(activityScenarioRule);
    createButton().perform(click());
  }

  @Test
  public final void uiElementsAreDisplayed() {
    laoNameEntry().check(matches(isDisplayed()));
    cancelButtonLaunch().check(matches(isDisplayed()));
    confirmButtonLaunch().check(matches(isDisplayed()));
    addWitnessButton().check(matches(isDisplayed()));
    witnessTitle().check(matches(withEffectiveVisibility(Visibility.GONE)));
  }

  @Test
  public void confirmButtonGoesToLaoDetail() {
    Intents.init();
    laoNameEntry().perform(ViewActions.replaceText(LAO_NAME));
    serverNameEntry().perform(ViewActions.replaceText(SERVER_URL));
    confirmButtonLaunch().check(matches(isDisplayed()));
    confirmButtonLaunch().check(matches(isEnabled()));
    confirmButtonLaunch().perform(click());
    intended(hasComponent(ConnectingActivity.class.getName()));
    Intents.release();
  }

  @Test
  public void cancelButtonGoesToHome() {
    cancelButtonLaunch().perform(click());
    fragmentContainer().check(matches(withChild(withId(homeFragmentId()))));
  }

  @Test
  public void addWitnessButtonGoesToScanner() {
    addWitnessButton().perform(click());
    fragmentContainer().check(matches(withChild(withId(qrScannerFragmentId()))));
  }
}
