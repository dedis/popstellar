package com.github.dedis.popstellar.ui.home;

import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static com.github.dedis.popstellar.ui.pages.home.ConnectingPageObject.cancelButton;
import static com.github.dedis.popstellar.ui.pages.home.ConnectingPageObject.connectingText;
import static com.github.dedis.popstellar.ui.pages.home.ConnectingPageObject.laoConnectingText;
import static com.github.dedis.popstellar.ui.pages.home.ConnectingPageObject.progressBar;

import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.github.dedis.popstellar.model.objects.Lao;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.testutils.Base64DataUtils;
import com.github.dedis.popstellar.testutils.BundleBuilder;
import com.github.dedis.popstellar.testutils.IntentUtils;
import com.github.dedis.popstellar.ui.home.connecting.ConnectingActivity;
import com.github.dedis.popstellar.utility.Constants;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoTestRule;

import dagger.hilt.android.testing.HiltAndroidRule;
import dagger.hilt.android.testing.HiltAndroidTest;

@HiltAndroidTest
@RunWith(AndroidJUnit4.class)
public class ConnectingFragmentTest {
  private static final String LAO_NAME = "Lao Name";
  private static final PublicKey PK = Base64DataUtils.generatePublicKey();
  private static final Lao LAO = new Lao(LAO_NAME, PK, 10223421);
  private static final String LAO_ID = LAO.getId();

  @Rule(order = 0)
  public final MockitoTestRule mockitoRule = MockitoJUnit.testRule(this);

  @Rule(order = 1)
  public final HiltAndroidRule hiltRule = new HiltAndroidRule(this);

  @Rule(order = 3)
  public final ActivityScenarioRule<ConnectingActivity> activityScenarioRule =
      new ActivityScenarioRule<>(
          IntentUtils.createIntent(
              ConnectingActivity.class,
              new BundleBuilder().putString(Constants.LAO_ID_EXTRA, LAO_ID).build()));

  @Before
  public void setup() {
    hiltRule.inject();
  }

  @Test
  public void basicElementsAreDisplayed() {
    connectingText().check(matches(isDisplayed()));
    laoConnectingText().check(matches(isDisplayed()));
    progressBar().check(matches(isDisplayed()));
    cancelButton().check(matches(isDisplayed()));
  }

  @Test
  public void staticTextTest() {
    connectingText().check(matches(withText("Connecting to")));
    cancelButton().check(matches(withText("Cancel")));
  }

  @Test
  public void dynamicTextTest() {
    laoConnectingText().check(matches(withText(LAO_ID)));
  }

  @Test
  public void cancelButtonLaunchesHomeTest() {
    Intents.init();
    cancelButton().perform(click());
    intended(hasComponent(HomeActivity.class.getName()));
    Intents.release();
  }
}
