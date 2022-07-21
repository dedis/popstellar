package com.github.dedis.popstellar.ui.detail.event.rollcall;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.github.dedis.popstellar.model.objects.Lao;
import com.github.dedis.popstellar.model.objects.RollCall;
import com.github.dedis.popstellar.model.objects.event.EventState;
import com.github.dedis.popstellar.model.objects.security.KeyPair;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.repository.LAORepository;
import com.github.dedis.popstellar.repository.remote.GlobalNetworkManager;
import com.github.dedis.popstellar.testutils.BundleBuilder;
import com.github.dedis.popstellar.testutils.fragment.ActivityFragmentScenarioRule;
import com.github.dedis.popstellar.ui.detail.LaoDetailActivity;
import com.github.dedis.popstellar.utility.Constants;
import com.github.dedis.popstellar.utility.handler.MessageHandler;
import com.github.dedis.popstellar.utility.security.KeyManager;
import com.google.gson.Gson;

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

import javax.inject.Inject;

import dagger.hilt.android.testing.BindValue;
import dagger.hilt.android.testing.HiltAndroidRule;
import dagger.hilt.android.testing.HiltAndroidTest;
import io.reactivex.subjects.BehaviorSubject;

import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static com.github.dedis.popstellar.testutils.Base64DataUtils.generateKeyPair;
import static com.github.dedis.popstellar.ui.pages.detail.LaoDetailActivityPageObject.containerId;
import static com.github.dedis.popstellar.ui.pages.detail.LaoDetailActivityPageObject.fragmentToOpenExtra;
import static com.github.dedis.popstellar.ui.pages.detail.LaoDetailActivityPageObject.laoDetailValue;
import static com.github.dedis.popstellar.ui.pages.detail.LaoDetailActivityPageObject.laoIdExtra;
import static com.github.dedis.popstellar.ui.pages.detail.event.rollcall.RollCallFragmentPageObject.managementButton;
import static com.github.dedis.popstellar.ui.pages.detail.event.rollcall.RollCallFragmentPageObject.rollCallEndTime;
import static com.github.dedis.popstellar.ui.pages.detail.event.rollcall.RollCallFragmentPageObject.rollCallStartTime;
import static com.github.dedis.popstellar.ui.pages.detail.event.rollcall.RollCallFragmentPageObject.rollCallStatusText;
import static com.github.dedis.popstellar.ui.pages.detail.event.rollcall.RollCallFragmentPageObject.rollCallTitle;
import static org.mockito.ArgumentMatchers.anyString;
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

  private static final DateFormat DATE_FORMAT =
      new SimpleDateFormat("dd/MM/yyyy HH:mm z", Locale.ENGLISH);

  private final RollCall ROLL_CALL = new RollCall(LAO.getId(), CREATION, ROLL_CALL_TITLE);

  @Inject GlobalNetworkManager networkManager;
  @Inject Gson gson;
  @Inject MessageHandler messageHandler;

  @BindValue @Mock LAORepository repository;
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
          when(repository.getLaoObservable(anyString()))
              .thenReturn(BehaviorSubject.createDefault(LAO));

          when(keyManager.getMainPublicKey()).thenReturn(SENDER);

          ROLL_CALL.setState(EventState.CLOSED);
          ROLL_CALL.setLocation(LOCATION);
          ROLL_CALL.setStart(ROLL_CALL_START);
          ROLL_CALL.setLocation(LOCATION);
          ROLL_CALL.setEnd(ROLL_CALL_END);
          ROLL_CALL.setDescription(ROLL_CALL_DESC);
          ROLL_CALL.setState(EventState.CREATED);
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
    rollCallTitle().check(matches(withText(ROLL_CALL_TITLE)));
  }

  @Test
  public void statusCreatedTest() {
    rollCallStatusText().check(matches(withText("Closed")));
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
  public void managementButtonCreatedTest() {
    managementButton().check(matches(withText("OPEN")));
  }

  @Test
  public void statusOpenedTest() {
    openRollCall();
    rollCallStatusText().check(matches(withText("Open")));
  }

  @Test
  public void managementButtonOpenedTest() {
    openRollCall();
    managementButton().check(matches(withText("CLOSE")));
  }

  @Test
  public void statusClosedTest() {
    closeRollCall();
    rollCallStatusText().check(matches(withText("Closed")));
  }

  @Test
  public void managementButtonClosedTest() {
    closeRollCall();
    managementButton().check(matches(withText("REOPEN")));
  }

  private void openRollCall() {
    ROLL_CALL.setState(EventState.OPENED);
  }

  private void closeRollCall() {
    ROLL_CALL.setState(EventState.CLOSED);
  }
}
