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
import com.github.dedis.popstellar.repository.remote.GlobalNetworkManager;
import com.github.dedis.popstellar.testutils.BundleBuilder;
import com.github.dedis.popstellar.testutils.MessageSenderHelper;
import com.github.dedis.popstellar.testutils.fragment.ActivityFragmentScenarioRule;
import com.github.dedis.popstellar.ui.detail.LaoDetailActivity;
import com.github.dedis.popstellar.ui.detail.LaoDetailViewModel;
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
import java.util.Date;
import java.util.Locale;

import dagger.hilt.android.testing.*;
import io.reactivex.subjects.BehaviorSubject;

import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static com.github.dedis.popstellar.testutils.Base64DataUtils.generateKeyPair;
import static com.github.dedis.popstellar.testutils.pages.detail.LaoDetailActivityPageObject.*;
import static com.github.dedis.popstellar.testutils.pages.detail.event.rollcall.RollCallFragmentPageObject.*;
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

  private final RollCall ROLL_CALL = new RollCall(LAO.getId(), CREATION, ROLL_CALL_TITLE);

  @BindValue @Mock LAORepository repository;
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
          when(repository.getLaoObservable(anyString())).thenReturn(laoSubject);
          when(repository.getLaoView(any())).thenAnswer(invocation -> new LaoView(LAO));

          when(keyManager.getMainPublicKey()).thenReturn(SENDER);

          when(networkManager.getMessageSender()).thenReturn(messageSenderHelper.getMockedSender());
          messageSenderHelper.setupMock();

          ROLL_CALL.setState(EventState.CLOSED);
          ROLL_CALL.setLocation(LOCATION);
          ROLL_CALL.setStart(ROLL_CALL_START);
          ROLL_CALL.setLocation(LOCATION);
          ROLL_CALL.setEnd(ROLL_CALL_END);
          ROLL_CALL.setDescription(ROLL_CALL_DESC);
          ROLL_CALL.setState(EventState.CREATED);

          LAO.updateRollCall(ROLL_CALL.getId(), ROLL_CALL);
        }
      };

  @Rule(order = 3)
  public ActivityFragmentScenarioRule<LaoDetailActivity, RollCallFragment> activityScenarioRule =
      ActivityFragmentScenarioRule.launchIn(
          LaoDetailActivity.class,
          new BundleBuilder()
              .putString(laoIdExtra(), LAO_ID)
              .putString(fragmentToOpenExtra(), laoDetailValue())
              .build(),
          containerId(),
          RollCallFragment.class,
          () -> RollCallFragment.newInstance(ROLL_CALL),
          new BundleBuilder().putString(Constants.RC_PK_EXTRA, SENDER.getEncoded()).build());

  @Test
  public void rollCallTitleMatches() {
    setupViewModel();
    rollCallTitle().check(matches(withText(ROLL_CALL_TITLE)));
  }

  @Test
  public void statusCreatedTest() {
    setupViewModel();
    rollCallStatusText().check(matches(withText("Closed")));
  }

  @Test
  public void datesDisplayedMatches() {
    Date startTime = new Date(ROLL_CALL.getStartTimestampInMillis());
    Date endTime = new Date(ROLL_CALL.getEndTimestampInMillis());
    String startTimeText = DATE_FORMAT.format(startTime);
    String endTimeText = DATE_FORMAT.format(endTime);

    setupViewModel();
    rollCallStartTime().check(matches(withText(startTimeText)));
    rollCallEndTime().check(matches(withText(endTimeText)));
  }

  @Test
  public void managementButtonIsDisplayed() {
    managementButton().check(matches(isDisplayed()));
  }

  @Test
  public void scanButtonIsNotDisplayedWhenCreatedTest() {
    setupViewModel();
    rollCallScanButton().check(matches(withEffectiveVisibility(Visibility.GONE)));
  }

  @Test
  public void managementButtonOpensRollCallWhenCreated() {
    setupViewModel();
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
    openRollCall();
    setupViewModel();
    rollCallStatusText().check(matches(withText("Open")));
  }

  @Test
  public void scanButtonIsDisplayedWhenOpenedTest() {
    openRollCall();
    setupViewModel();
    rollCallScanButton().check(matches(withEffectiveVisibility(Visibility.VISIBLE)));
  }

  @Test
  public void managementButtonCloseRollCallWhenOpened() {
    openRollCall();
    setupViewModel();

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
  public void scanButtonOPenScanningTest() {
    openRollCall();
    setupViewModel();

    rollCallScanButton().perform(click());
    fragmentContainer().check(matches(withChild(withId(cameraPermissionId()))));
  }

  @Test
  public void statusClosedTest() {
    closeRollCall();
    rollCallStatusText().check(matches(withText("Closed")));
  }

  @Test
  public void scanButtonIsNotDisplayedWhenClosedTest() {
    closeRollCall();
    setupViewModel();
    rollCallScanButton().check(matches(withEffectiveVisibility(Visibility.GONE)));
  }

  @Test
  public void managementButtonClosedTest() {
    closeRollCall();
    setupViewModel();
    managementButton().check(matches(withText("REOPEN")));
  }

  private void openRollCall() {
    ROLL_CALL.setState(EventState.OPENED);
  }

  private void closeRollCall() {
    ROLL_CALL.setState(EventState.CLOSED);
  }

  private void setupViewModel() {
    activityScenarioRule
        .getScenario()
        .onActivity(
            activity -> {
              LaoDetailViewModel laoDetailViewModel = LaoDetailActivity.obtainViewModel(activity);
              //    laoDetailViewModel.setCurrentLao(new LaoView(LAO));
              laoDetailViewModel.setCurrentRollCall(ROLL_CALL);
              laoDetailViewModel.setCurrentRollCallId(ROLL_CALL.getId());
            });
    // Recreate the fragment because the viewModel needed to be modified before start
    activityScenarioRule.getScenario().recreate();
  }
}
