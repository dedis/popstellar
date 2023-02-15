package com.github.dedis.popstellar.ui.detail.event.rollcall;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.github.dedis.popstellar.model.network.method.message.data.rollcall.CloseRollCall;
import com.github.dedis.popstellar.model.network.method.message.data.rollcall.OpenRollCall;
import com.github.dedis.popstellar.model.objects.Lao;
import com.github.dedis.popstellar.model.objects.RollCall;
import com.github.dedis.popstellar.model.objects.event.EventState;
import com.github.dedis.popstellar.model.objects.security.KeyPair;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.model.objects.view.LaoView;
import com.github.dedis.popstellar.repository.LAORepository;
import com.github.dedis.popstellar.repository.RollCallRepository;
import com.github.dedis.popstellar.repository.remote.GlobalNetworkManager;
import com.github.dedis.popstellar.testutils.BundleBuilder;
import com.github.dedis.popstellar.testutils.MessageSenderHelper;
import com.github.dedis.popstellar.testutils.fragment.ActivityFragmentScenarioRule;
import com.github.dedis.popstellar.ui.lao.LaoActivity;
import com.github.dedis.popstellar.ui.lao.event.rollcall.RollCallFragment;
import com.github.dedis.popstellar.utility.Constants;
import com.github.dedis.popstellar.utility.error.UnknownLaoException;
import com.github.dedis.popstellar.utility.security.KeyManager;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoTestRule;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import javax.inject.Inject;

import dagger.hilt.android.testing.*;
import io.reactivex.subjects.BehaviorSubject;

import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static com.github.dedis.popstellar.testutils.Base64DataUtils.generateKeyPair;
import static com.github.dedis.popstellar.testutils.pages.detail.event.rollcall.RollCallFragmentPageObject.*;
import static com.github.dedis.popstellar.testutils.pages.lao.LaoActivityPageObject.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@HiltAndroidTest
@RunWith(AndroidJUnit4.class)
public class RollCallFragmentTest {

  private static final String LAO_NAME = "lao";
  private static final KeyPair SENDER_KEY = generateKeyPair();
  private static final PublicKey SENDER = SENDER_KEY.getPublicKey();
  private static final Lao LAO = new Lao(LAO_NAME, SENDER, 10223421);
  private static final String LAO_ID = LAO.getId();
  private static final String ROLL_CALL_TITLE = "RC title";
  private static final long CREATION = 10323411;
  private static final long ROLL_CALL_START = 10323421;
  private static final long ROLL_CALL_END = 10323431;
  private static final String ROLL_CALL_DESC = "";
  private static final String LOCATION = "EPFL";
  private static final BehaviorSubject<LaoView> laoSubject =
      BehaviorSubject.createDefault(new LaoView(LAO));

  private static final DateFormat DATE_FORMAT =
      new SimpleDateFormat("dd/MM/yyyy HH:mm z", Locale.ENGLISH);

  private final RollCall ROLL_CALL =
      new RollCall(
          LAO.getId(),
          LAO.getId(),
          ROLL_CALL_TITLE,
          CREATION,
          ROLL_CALL_START,
          ROLL_CALL_END,
          EventState.CREATED,
          new HashSet<>(),
          LOCATION,
          ROLL_CALL_DESC);

  @Inject RollCallRepository rollCallRepo;

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
        protected void before() throws UnknownLaoException {
          hiltRule.inject();
          when(laoRepo.getLaoObservable(anyString())).thenReturn(laoSubject);
          when(laoRepo.getLaoView(any())).thenAnswer(invocation -> new LaoView(LAO));

          rollCallRepo.updateRollCall(LAO_ID, ROLL_CALL);

          when(keyManager.getMainPublicKey()).thenReturn(SENDER);

          when(networkManager.getMessageSender()).thenReturn(messageSenderHelper.getMockedSender());
          messageSenderHelper.setupMock();
        }
      };

  @Rule(order = 3)
  public ActivityFragmentScenarioRule<LaoActivity, RollCallFragment> activityScenarioRule =
      ActivityFragmentScenarioRule.launchIn(
          LaoActivity.class,
          new BundleBuilder().putString(laoIdExtra(), LAO_ID).build(),
          containerId(),
          RollCallFragment.class,
          () -> RollCallFragment.newInstance(ROLL_CALL),
          new BundleBuilder()
              .putString(Constants.ROLL_CALL_ID, ROLL_CALL.getPersistentId())
              .build());

  @Test
  public void rollCallTitleMatches() {
    rollCallTitle().check(matches(withText(ROLL_CALL_TITLE)));
  }

  @Test
  public void statusCreatedTest() {
    rollCallStatusText().check(matches(withText("Not yet opened")));
  }

  @Test
  public void datesDisplayedMatches() {
    Date startTime = new Date(ROLL_CALL.getStartTimestampInMillis());
    Date endTime = new Date(ROLL_CALL.getEndTimestampInMillis());
    String startTimeText = DATE_FORMAT.format(startTime);
    String endTimeText = DATE_FORMAT.format(endTime);

    rollCallStartTime().check(matches(withText(startTimeText)));
    rollCallEndTime().check(matches(withText(endTimeText)));
  }

  @Test
  public void managementButtonIsDisplayed() {
    managementButton().check(matches(isDisplayed()));
  }

  @Test
  public void scanButtonIsNotDisplayedWhenCreatedTest() {
    rollCallScanButton().check(matches(withEffectiveVisibility(Visibility.GONE)));
  }

  @Test
  public void managementButtonOpensRollCallWhenCreated() {
    managementButton().check(matches(withText("OPEN")));
    managementButton().perform(click());
    // Wait for the main thread to finish executing the calls made above
    // before asserting their effect
    InstrumentationRegistry.getInstrumentation().waitForIdleSync();

    verify(messageSenderHelper.getMockedSender())
        .publish(any(), eq(LAO.getChannel()), any(OpenRollCall.class));
    messageSenderHelper.assertSubscriptions();
  }

  @Test
  public void statusOpenedTest() {
    rollCallRepo.updateRollCall(LAO_ID, RollCall.openRollCall(ROLL_CALL));

    rollCallStatusText().check(matches(withText("Open")));
  }

  @Test
  public void scanButtonIsDisplayedWhenOpenedTest() {
    rollCallRepo.updateRollCall(LAO_ID, RollCall.openRollCall(ROLL_CALL));

    rollCallScanButton().check(matches(withEffectiveVisibility(Visibility.VISIBLE)));
  }

  @Test
  public void managementButtonCloseRollCallWhenOpened() {
    rollCallRepo.updateRollCall(LAO_ID, RollCall.openRollCall(ROLL_CALL));

    // Mock the fact that the rollcall was successfully opened
    managementButton().check(matches(withText("CLOSE")));
    managementButton().perform(click());
    // Wait for the main thread to finish executing the calls made above
    // before asserting their effect
    InstrumentationRegistry.getInstrumentation().waitForIdleSync();

    verify(messageSenderHelper.getMockedSender())
        .publish(any(), eq(LAO.getChannel()), any(CloseRollCall.class));
    messageSenderHelper.assertSubscriptions();
  }

  @Test
  public void scanButtonOpenScanningTest() {
    rollCallRepo.updateRollCall(LAO_ID, RollCall.openRollCall(ROLL_CALL));

    rollCallScanButton().perform(click());
    fragmentContainer().check(matches(withChild(withId(qrCodeFragmentId()))));
  }

  @Test
  public void statusClosedTest() {
    rollCallRepo.updateRollCall(LAO_ID, RollCall.closeRollCall(ROLL_CALL));
    rollCallStatusText().check(matches(withText("Closed")));
  }

  @Test
  public void scanButtonIsNotDisplayedWhenClosedTest() {
    rollCallRepo.updateRollCall(LAO_ID, RollCall.closeRollCall(ROLL_CALL));
    rollCallScanButton().check(matches(withEffectiveVisibility(Visibility.GONE)));
  }

  @Test
  public void managementButtonClosedTest() {
    rollCallRepo.updateRollCall(LAO_ID, RollCall.closeRollCall(ROLL_CALL));
    managementButton().check(matches(withText("REOPEN")));
  }
}
