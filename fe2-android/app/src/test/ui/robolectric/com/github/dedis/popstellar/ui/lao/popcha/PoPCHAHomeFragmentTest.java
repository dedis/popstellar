package com.github.dedis.popstellar.ui.lao.popcha;

import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static com.github.dedis.popstellar.testutils.Base64DataUtils.*;
import static com.github.dedis.popstellar.testutils.pages.lao.popcha.PoPCHAHomePageObject.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.github.dedis.popstellar.model.objects.*;
import com.github.dedis.popstellar.model.objects.event.EventState;
import com.github.dedis.popstellar.model.objects.security.*;
import com.github.dedis.popstellar.repository.RollCallRepository;
import com.github.dedis.popstellar.repository.remote.GlobalNetworkManager;
import com.github.dedis.popstellar.testutils.BundleBuilder;
import com.github.dedis.popstellar.testutils.MessageSenderHelper;
import com.github.dedis.popstellar.testutils.fragment.ActivityFragmentScenarioRule;
import com.github.dedis.popstellar.testutils.pages.lao.LaoActivityPageObject;
import com.github.dedis.popstellar.ui.lao.LaoActivity;
import com.github.dedis.popstellar.utility.Constants;
import com.github.dedis.popstellar.utility.error.keys.KeyException;
import com.github.dedis.popstellar.utility.security.KeyManager;
import dagger.hilt.android.testing.*;
import java.util.HashSet;
import javax.inject.Inject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoTestRule;

@HiltAndroidTest
@RunWith(AndroidJUnit4.class)
public class PoPCHAHomeFragmentTest {
  private static final long CREATION_TIME = 1612204910;
  private static final String LAO_NAME = "laoName";
  private static final KeyPair SENDER_KEY = generateKeyPair();
  private static final String LAO_ID =
      Lao.generateLaoId(SENDER_KEY.getPublicKey(), CREATION_TIME, LAO_NAME);
  private static final String ROLLCALL_NAME = "rollcall#1";
  private static final String ROLLCALL_ID =
      RollCall.generateCreateRollCallId(LAO_ID, CREATION_TIME, ROLLCALL_NAME);
  private static final long TIMESTAMP_1 = 1632204910;
  private static final long TIMESTAMP_2 = 1632204900;
  private static final PoPToken popToken = generatePoPToken();
  private static final HashSet<PublicKey> attendees = new HashSet<>();

  static {
    attendees.add(popToken.getPublicKey());
  }

  private final RollCall ROLL_CALL =
      new RollCall(
          ROLLCALL_ID,
          ROLLCALL_ID,
          ROLLCALL_NAME,
          CREATION_TIME,
          TIMESTAMP_1,
          TIMESTAMP_2,
          EventState.CLOSED,
          attendees,
          "bc",
          "");

  @Inject RollCallRepository rollCallRepository;
  @BindValue @Mock GlobalNetworkManager networkManager;
  @BindValue @Mock KeyManager keyManager;
  MessageSenderHelper messageSenderHelper = new MessageSenderHelper();

  @Rule public InstantTaskExecutorRule rule = new InstantTaskExecutorRule();

  @Rule(order = 0)
  public final MockitoTestRule mockitoRule = MockitoJUnit.testRule(this);

  @Rule(order = 1)
  public final HiltAndroidRule hiltRule = new HiltAndroidRule(this);

  @Rule(order = 2)
  public final ExternalResource setupRule =
      new ExternalResource() {
        @Override
        protected void before() throws KeyException {
          hiltRule.inject();

          rollCallRepository.updateRollCall(LAO_ID, ROLL_CALL);

          when(networkManager.getMessageSender()).thenReturn(messageSenderHelper.getMockedSender());
          messageSenderHelper.setupMock();

          when(keyManager.getMainPublicKey()).thenReturn(SENDER_KEY.getPublicKey());
          when(keyManager.getValidPoPToken(anyString(), any(RollCall.class))).thenReturn(popToken);
        }
      };

  @Rule(order = 3)
  public ActivityFragmentScenarioRule<LaoActivity, PoPCHAHomeFragment> activityScenarioRule =
      ActivityFragmentScenarioRule.launchIn(
          LaoActivity.class,
          new BundleBuilder().putString(Constants.LAO_ID_EXTRA, LAO_ID).build(),
          LaoActivityPageObject.containerId(),
          PoPCHAHomeFragment.class,
          PoPCHAHomeFragment::newInstance);

  @Test
  public void testPageHeaderText() {
    String expectedHeader =
        String.format("Welcome to the PoPCHA tab, you're currently in the LAO:\n%s", LAO_ID);
    getHeader().check(matches(withText(expectedHeader)));
  }

  @Test
  public void testOpenScanner() {
    getScanner().check(matches(isDisplayed()));
    getScanner().perform(click());
    getScannerFragment().check(matches(isDisplayed()));
  }

  @Test
  public void testBackButtonBehaviour() {
    getRootView().perform(pressBack());
    getEventListFragment().check(matches(isDisplayed()));
  }
}
