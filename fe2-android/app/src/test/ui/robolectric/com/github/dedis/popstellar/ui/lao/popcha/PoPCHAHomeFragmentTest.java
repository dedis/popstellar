package com.github.dedis.popstellar.ui.lao.popcha;

import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static com.github.dedis.popstellar.testutils.Base64DataUtils.*;
import static com.github.dedis.popstellar.testutils.UITestUtils.forceTypeText;
import static com.github.dedis.popstellar.testutils.pages.lao.popcha.PoPCHAHomePageObject.*;
import static com.github.dedis.popstellar.testutils.pages.scanning.QrScanningPageObject.*;
import static com.github.dedis.popstellar.ui.lao.popcha.PoPCHAViewModel.AUTHENTICATION;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.github.dedis.popstellar.model.network.method.message.data.popcha.PoPCHAAuthentication;
import com.github.dedis.popstellar.model.objects.*;
import com.github.dedis.popstellar.model.objects.event.EventState;
import com.github.dedis.popstellar.model.objects.security.*;
import com.github.dedis.popstellar.model.objects.view.LaoView;
import com.github.dedis.popstellar.repository.LAORepository;
import com.github.dedis.popstellar.repository.RollCallRepository;
import com.github.dedis.popstellar.repository.remote.GlobalNetworkManager;
import com.github.dedis.popstellar.testutils.BundleBuilder;
import com.github.dedis.popstellar.testutils.MessageSenderHelper;
import com.github.dedis.popstellar.testutils.fragment.ActivityFragmentScenarioRule;
import com.github.dedis.popstellar.testutils.pages.lao.LaoActivityPageObject;
import com.github.dedis.popstellar.ui.lao.LaoActivity;
import com.github.dedis.popstellar.utility.Constants;
import com.github.dedis.popstellar.utility.error.UnknownLaoException;
import com.github.dedis.popstellar.utility.error.keys.KeyException;
import com.github.dedis.popstellar.utility.security.KeyManager;
import dagger.hilt.android.testing.*;
import io.reactivex.subjects.BehaviorSubject;
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
  private static final Lao LAO = new Lao(LAO_NAME, SENDER_KEY.getPublicKey(), CREATION_TIME);
  private static final BehaviorSubject<LaoView> laoSubject =
      BehaviorSubject.createDefault(new LaoView(LAO));
  private static final String ROLLCALL_NAME = "rollcall#1";
  private static final String ROLLCALL_ID =
      RollCall.generateCreateRollCallId(LAO.getId(), CREATION_TIME, ROLLCALL_NAME);
  private static final long TIMESTAMP_1 = 1632204910;
  private static final long TIMESTAMP_2 = 1632204900;
  private static final PoPToken popToken = generatePoPToken();
  private static final AuthToken authToken = new AuthToken(popToken);
  private static final HashSet<PublicKey> attendees = new HashSet<>();

  static {
    attendees.add(popToken.getPublicKey());
  }

  private static final RollCall ROLL_CALL =
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

  private static final String ADDRESS = "localhost:9100";
  private static final String RESPONSE_MODE = "query";
  private static final String CLIENT_ID = "WAsabGuEe5m1KpqOZQKgmO7UShX84Jmd_eaenOZ32wU";
  private static final String NONCE =
      "frXgNl-IxJPzsNia07f_3yV0ECYlWOb2RXG_SGvATKcJ7-s0LthmboTrnMqlQS1RnzmV9hW0iumu_5NwAqXwGA";
  private static final String STATE =
      "m_9r5sPUD8NoRIdVVYFMyYCOb-8xh1d2q8l-pKDXO0sn9TWnR_2nmC8MfVj1COHZsh1rElqimOTLAp3CbhbYJQ";
  private static final String VALID_POPCHA_URL =
      "http://"
          + ADDRESS
          + "/authorize?response_mode="
          + RESPONSE_MODE
          + "&response_type=id_token&client_id="
          + CLIENT_ID
          + "&redirect_uri=http%3A%2F%2Flocalhost%3A8000%2Fcb&scope=openid+profile&login_hint="
          + LAO.getId()
          + "&nonce="
          + NONCE
          + "&state="
          + STATE;

  @Inject RollCallRepository rollCallRepository;
  @BindValue @Mock LAORepository laoRepo;
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
        protected void before() throws KeyException, UnknownLaoException {
          hiltRule.inject();
          when(laoRepo.getLaoObservable(anyString())).thenReturn(laoSubject);
          when(laoRepo.getLaoView(any())).thenAnswer(invocation -> new LaoView(LAO));

          rollCallRepository.updateRollCall(LAO.getId(), ROLL_CALL);

          when(networkManager.getMessageSender()).thenReturn(messageSenderHelper.getMockedSender());
          messageSenderHelper.setupMock();

          when(keyManager.getMainPublicKey()).thenReturn(SENDER_KEY.getPublicKey());
          when(keyManager.getValidPoPToken(anyString(), any(RollCall.class))).thenReturn(popToken);
          when(keyManager.getLongTermAuthToken(anyString(), anyString())).thenReturn(authToken);
        }
      };

  @Rule(order = 3)
  public ActivityFragmentScenarioRule<LaoActivity, PoPCHAHomeFragment> activityScenarioRule =
      ActivityFragmentScenarioRule.launchIn(
          LaoActivity.class,
          new BundleBuilder().putString(Constants.LAO_ID_EXTRA, LAO.getId()).build(),
          LaoActivityPageObject.containerId(),
          PoPCHAHomeFragment.class,
          PoPCHAHomeFragment::newInstance);

  @Test
  public void testPageHeaderText() {
    String expectedHeader =
        String.format("Welcome to the PoPCHA tab, you're currently in the LAO:\n%s", LAO.getId());
    getHeader().check(matches(withText(expectedHeader)));
  }

  @Test
  public void testOpenScanner() {
    getScanner().check(matches(isDisplayed()));
    getScanner().perform(click());
    getScannerFragment().check(matches(isDisplayed()));
  }

  @Test
  public void testScanValidPoPCHAUrlSendMessage() {
    getScanner().perform(click());

    openManualButton().perform(click());
    manualAddEditText().perform(forceTypeText(VALID_POPCHA_URL));
    manualAddConfirm().perform(click());

    InstrumentationRegistry.getInstrumentation().waitForIdleSync();

    verify(messageSenderHelper.getMockedSender())
        .publish(
            any(),
            eq(Channel.getLaoChannel(LAO.getId()).subChannel(AUTHENTICATION)),
            any(PoPCHAAuthentication.class));
  }

  @Test
  public void testBackButtonBehaviour() {
    getRootView().perform(pressBack());
    getEventListFragment().check(matches(isDisplayed()));
  }
}
