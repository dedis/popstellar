package com.github.dedis.popstellar.ui.home;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.github.dedis.popstellar.di.DataRegistryModuleHelper;
import com.github.dedis.popstellar.di.JsonModule;
import com.github.dedis.popstellar.model.objects.security.KeyPair;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.testutils.Base64DataUtils;
import com.github.dedis.popstellar.utility.security.KeyManager;
import com.google.gson.Gson;

import org.junit.*;
import org.junit.rules.ExternalResource;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoTestRule;

import dagger.hilt.android.testing.*;

import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static com.github.dedis.popstellar.testutils.pages.home.HomePageObject.witnessButton;
import static com.github.dedis.popstellar.testutils.pages.home.QrPageObject.privateKey;
import static com.github.dedis.popstellar.testutils.pages.home.QrPageObject.qrCode;
import static org.mockito.Mockito.when;

@HiltAndroidTest
@RunWith(AndroidJUnit4.class)
public class QrFragmentTest {

  private static final KeyPair KEY_PAIR = Base64DataUtils.generateKeyPair();
  private static final PublicKey PK = KEY_PAIR.getPublicKey();

  private static Gson gson;

  @BindValue @Mock KeyManager keyManager;

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
          when(keyManager.getMainPublicKey()).thenReturn(PK);
          when(keyManager.getMainKeyPair()).thenReturn(KEY_PAIR);
          gson = JsonModule.provideGson(DataRegistryModuleHelper.buildRegistry());
        }
      };

  @Rule(order = 3)
  public ActivityScenarioRule<HomeActivity> activityScenarioRule =
      new ActivityScenarioRule<>(HomeActivity.class);

  @Before
  public void setup() {
    // Open the launch tab
    HomeActivityTest.initializeWallet(activityScenarioRule);
    witnessButton().perform(click());
  }

  @Test
  public void elementsAreDisplayedTest() {
    qrCode().check(matches(isDisplayed()));
    privateKey().check(matches(isDisplayed()));
  }

  @Test
  public void publicKeyIsCorrectTest() {
    privateKey().check(matches(withText(keyManager.getMainPublicKey().getEncoded())));
  }
}
