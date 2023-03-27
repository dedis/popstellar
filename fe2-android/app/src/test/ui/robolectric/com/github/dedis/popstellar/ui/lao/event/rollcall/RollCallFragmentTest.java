package com.github.dedis.popstellar.ui.lao.event.rollcall;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.github.dedis.popstellar.model.network.method.message.data.rollcall.CloseRollCall;
import com.github.dedis.popstellar.model.network.method.message.data.rollcall.OpenRollCall;
import com.github.dedis.popstellar.model.objects.Lao;
import com.github.dedis.popstellar.model.objects.RollCall;
import com.github.dedis.popstellar.model.objects.event.EventState;
import com.github.dedis.popstellar.model.objects.security.*;
import com.github.dedis.popstellar.model.objects.view.LaoView;
import com.github.dedis.popstellar.repository.LAORepository;
import com.github.dedis.popstellar.repository.RollCallRepository;
import com.github.dedis.popstellar.repository.remote.GlobalNetworkManager;
import com.github.dedis.popstellar.testutils.*;
import com.github.dedis.popstellar.testutils.fragment.ActivityFragmentScenarioRule;
import com.github.dedis.popstellar.ui.lao.LaoActivity;
import com.github.dedis.popstellar.utility.Constants;
import com.github.dedis.popstellar.utility.error.UnknownLaoException;
import com.github.dedis.popstellar.utility.error.keys.KeyException;
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
import static com.github.dedis.popstellar.testutils.pages.lao.LaoActivityPageObject.*;
import static com.github.dedis.popstellar.testutils.pages.lao.event.rollcall.RollCallFragmentPageObject.*;
import static org.junit.Assert.assertNotEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@HiltAndroidTest
@RunWith(AndroidJUnit4.class)
public class RollCallFragmentTest {

  private static final String LAO_NAME = "lao";
  private static final KeyPair SENDER_KEY = generateKeyPair();
  private static final PublicKey SENDER = SENDER_KEY.getPublicKey();
  private static final PublicKey SENDER_2 = generateKeyPair().getPublicKey();
  private static final Lao LAO = new Lao(LAO_NAME, SENDER, 10223421);
  private static final Lao LAO_2 = new Lao(LAO_NAME + "2", SENDER_2, 10223422);
  private static final String LAO_ID = LAO.getId();
  private static final String LAO_ID2 = LAO_2.getId();
  private static final String ROLL_CALL_TITLE = "RC title";
  private static final long CREATION = 10323411;
  private static final long ROLL_CALL_START = 10323421;
  private static final long ROLL_CALL_END = 10323431;
  private static final String ROLL_CALL_DESC = "";
  private static final String LOCATION = "EPFL";
  private static final BehaviorSubject<LaoView> laoSubject =
      BehaviorSubject.createDefault(new LaoView(LAO));
  private static final BehaviorSubject<LaoView> laoSubject2 =
      BehaviorSubject.createDefault(new LaoView(LAO_2));

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

  private final RollCall ROLL_CALL_2 =
      new RollCall(
          LAO.getId() + "2",
          LAO.getId() + "2",
          ROLL_CALL_TITLE,
          CREATION,
          ROLL_CALL_START,
          ROLL_CALL_END,
          EventState.CREATED,
          new HashSet<>(),
          LOCATION,
          ROLL_CALL_DESC);

  private static final PoPToken POP_TOKEN = Base64DataUtils.generatePoPToken();

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
        protected void before() throws UnknownLaoException, KeyException {
          hiltRule.inject();
          when(laoRepo.getLaoObservable(anyString())).thenReturn(laoSubject);
          when(laoRepo.getLaoView(any())).thenAnswer(invocation -> new LaoView(LAO));

          rollCallRepo.updateRollCall(LAO_ID, ROLL_CALL);
          rollCallRepo.updateRollCall(LAO_ID, ROLL_CALL_2);

          when(keyManager.getMainPublicKey()).thenReturn(SENDER);

          when(networkManager.getMessageSender()).thenReturn(messageSenderHelper.getMockedSender());
          messageSenderHelper.setupMock();

          when(keyManager.getPoPToken(any(), any())).thenReturn(POP_TOKEN);
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

  @Test
  public void blockOpenRollCallTest() {
    rollCallRepo.updateRollCall(LAO_ID, RollCall.openRollCall(ROLL_CALL_2));

    managementButton().check(matches(withText("OPEN")));
    managementButton().perform(click());
    // Wait for the main thread to finish executing the calls made above
    // before asserting their effect
    InstrumentationRegistry.getInstrumentation().waitForIdleSync();

    // Assert state of roll call is unchanged
    assertNotEquals(ROLL_CALL.getState(), EventState.OPENED);
    managementButton().check(matches(withText("OPEN")));
  }

  @Test
  public void attendeesTextTest() {
    // Assert that when the roll call is not opened, the organizer has no attendees view
    rollCallAttendeesText().check(matches(withEffectiveVisibility(Visibility.INVISIBLE)));

    // Open the roll call
    rollCallRepo.updateRollCall(LAO_ID, RollCall.openRollCall(ROLL_CALL));

    rollCallAttendeesText().check(matches(withEffectiveVisibility(Visibility.VISIBLE)));
    rollCallAttendeesText().check(matches(withText("Scanned tokens : 0")));

    // Close the roll call
    rollCallRepo.updateRollCall(LAO_ID, RollCall.closeRollCall(ROLL_CALL));

    // Check that it has switched from scanned tokens to attendees
    rollCallAttendeesText().check(matches(withEffectiveVisibility(Visibility.VISIBLE)));
    rollCallAttendeesText().check(matches(withText("Attendees : 0")));
  }

  @Test
  public void attendeesListTest() {
    // Assert that when the roll call is not opened, the organizer has no attendees view
    rollCallListAttendees().check(matches(withEffectiveVisibility(Visibility.INVISIBLE)));

    // Open the roll call
    rollCallRepo.updateRollCall(LAO_ID, RollCall.openRollCall(ROLL_CALL));

    rollCallListAttendees().check(matches(withEffectiveVisibility(Visibility.VISIBLE)));
    // Assert that no scanned participant is present
    rollCallListAttendees().check(matches(hasChildCount(0)));
  }

  private void fakeClientLao() throws UnknownLaoException {
    when(laoRepo.getLaoObservable(anyString())).thenReturn(laoSubject2);
    when(laoRepo.getLaoView(any())).thenAnswer(invocation -> new LaoView(LAO_2));
    rollCallRepo.updateRollCall(LAO_ID2, ROLL_CALL);
    rollCallRepo.updateRollCall(LAO_ID2, ROLL_CALL_2);
    when(keyManager.getMainPublicKey()).thenReturn(SENDER_2);
  }

  @Test
  public void qrCodeVisibilityTest() throws UnknownLaoException {
    // Fake to be a client
    fakeClientLao();
    // Check visibility as client
    rollCallRepo.updateRollCall(LAO_ID, RollCall.openRollCall(ROLL_CALL));
    rollCallQRCode().check(matches(withEffectiveVisibility(Visibility.VISIBLE)));
  }
}
