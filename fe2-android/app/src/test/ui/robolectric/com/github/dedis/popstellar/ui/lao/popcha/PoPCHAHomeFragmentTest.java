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
import static org.mockito.Mockito.*;

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
  private static final KeyPair SENDER_KEY = generateKeyPair();
  private static final Lao LAO = new Lao("laoName", SENDER_KEY.publicKey, 1612204910);

  @Rule(order = 0)
  public final MockitoTestRule mockitoRule = MockitoJUnit.testRule(this);

  @Rule(order = 1)
  public final HiltAndroidRule hiltRule = new HiltAndroidRule(this);

  @Rule public InstantTaskExecutorRule rule = new InstantTaskExecutorRule();

  @Rule(order = 3)
  public ActivityFragmentScenarioRule<LaoActivity, PoPCHAHomeFragment> activityScenarioRule =
      ActivityFragmentScenarioRule.launchIn(
          LaoActivity.class,
          new BundleBuilder().putString(Constants.LAO_ID_EXTRA, LAO.getId()).build(),
          LaoActivityPageObject.containerId(),
          PoPCHAHomeFragment.class,
          PoPCHAHomeFragment::newInstance);

  @Inject RollCallRepository rollCallRepository;
  @BindValue @Mock LAORepository laoRepo;
  @BindValue @Mock GlobalNetworkManager networkManager;
  @BindValue @Mock KeyManager keyManager;
  MessageSenderHelper messageSenderHelper = new MessageSenderHelper();

  @Rule(order = 2)
  public final ExternalResource setupRule =
      new ExternalResource() {
        @Override
        protected void before() throws KeyException, UnknownLaoException {
          hiltRule.inject();
          when(laoRepo.getLaoObservable(anyString()))
              .thenReturn(BehaviorSubject.createDefault(new LaoView(LAO)));
          when(laoRepo.getLaoView(any())).thenAnswer(invocation -> new LaoView(LAO));

          String rollcallName = "rollcall#1";
          String rollCallId =
              RollCall.generateCreateRollCallId(LAO.getId(), 1612204910, rollcallName);
          PoPToken popToken = generatePoPToken();

          HashSet<PublicKey> attendees = new HashSet<>();
          attendees.add(popToken.publicKey);

          rollCallRepository.updateRollCall(
              LAO.getId(),
              new RollCall(
                  rollCallId,
                  rollCallId,
                  rollcallName,
                  1612204910,
                  1632204910,
                  1632204900,
                  EventState.CLOSED,
                  attendees,
                  "bc",
                  ""));

          when(networkManager.getMessageSender()).thenReturn(messageSenderHelper.getMockedSender());
          messageSenderHelper.setupMock();

          when(keyManager.getMainPublicKey()).thenReturn(SENDER_KEY.publicKey);
          when(keyManager.getValidPoPToken(anyString(), any(RollCall.class))).thenReturn(popToken);
          when(keyManager.getLongTermAuthToken(anyString(), anyString()))
              .thenReturn(new AuthToken(popToken));
        }
      };

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
    final String validPopchaUrl =
        "http://localhost:9100/authorize?response_mode=query&response_type=id_token&client_id="
            + "WAsabGuEe5m1KpqOZQKgmO7UShX84Jmd_eaenOZ32wU&redirect_uri="
            + "http%3A%2F%2Flocalhost%3A8000%2Fcb&scope=openid+profile&login_hint="
            + LAO.getId()
            + "&nonce=frXgNl-IxJPzsNia07f_3yV0ECYlWOb2RXG_SGvATKcJ7-s0LthmboTrnMqlQS1RnzmV9hW0ium"
            + "u_5NwAqXwGA&state=m_9r5sPUD8NoRIdVVYFMyYCOb-8xh1d2q8l-pKDXO0sn9TWnR_2nmC8MfVj1CO"
            + "HZsh1rElqimOTLAp3CbhbYJQ";
    getScanner().perform(click());

    openManualButton().perform(click());
    manualAddEditText().perform(forceTypeText(validPopchaUrl));
    manualAddConfirm().perform(click());

    InstrumentationRegistry.getInstrumentation().waitForIdleSync();

    verify(messageSenderHelper.getMockedSender())
        .publish(
            any(),
            eq(Channel.getLaoChannel(LAO.getId()).subChannel(AUTHENTICATION)),
            any(PoPCHAAuthentication.class));
  }

  @Test
  public void testScanInvalidPoPCHAUrlFails() {
    final String validPopchaUrl =
        "http://localhost:9100/authorize?response_mode=query&response_type=id_token&client_id="
            + "WAsabGuEe5m1KpqOZQKgmO7UShX84Jmd_eaenOZ32wU&redirect_uri="
            + "http%3A%2F%2Flocalhost%3A8000%2Fcb&scope=openid+profile&login_hint="
            + "random_invalid_lao_id"
            + "&nonce=frXgNl-IxJPzsNia07f_3yV0ECYlWOb2RXG_SGvATKcJ7-s0LthmboTrnMqlQS1RnzmV9hW0ium"
            + "u_5NwAqXwGA&state=m_9r5sPUD8NoRIdVVYFMyYCOb-8xh1d2q8l-pKDXO0sn9TWnR_2nmC8MfVj1CO"
            + "HZsh1rElqimOTLAp3CbhbYJQ";
    getScanner().perform(click());

    openManualButton().perform(click());
    manualAddEditText().perform(forceTypeText(validPopchaUrl));
    manualAddConfirm().perform(click());

    InstrumentationRegistry.getInstrumentation().waitForIdleSync();

    verifyNoInteractions(messageSenderHelper.getMockedSender());
  }

  @Test
  public void testBackButtonBehaviour() {
    getRootView().perform(pressBack());
    getEventListFragment().check(matches(isDisplayed()));
  }
}
