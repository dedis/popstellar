package com.github.dedis.popstellar.ui.detail.event.election;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.github.dedis.popstellar.model.network.method.message.data.election.*;
import com.github.dedis.popstellar.model.objects.Election;
import com.github.dedis.popstellar.model.objects.Lao;
import com.github.dedis.popstellar.model.objects.security.KeyPair;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.model.objects.view.LaoView;
import com.github.dedis.popstellar.repository.ElectionRepository;
import com.github.dedis.popstellar.repository.LAORepository;
import com.github.dedis.popstellar.repository.remote.GlobalNetworkManager;
import com.github.dedis.popstellar.testutils.BundleBuilder;
import com.github.dedis.popstellar.testutils.MessageSenderHelper;
import com.github.dedis.popstellar.testutils.fragment.ActivityFragmentScenarioRule;
import com.github.dedis.popstellar.ui.lao.LaoActivity;
import com.github.dedis.popstellar.ui.lao.event.election.fragments.ElectionFragment;
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
import static com.github.dedis.popstellar.model.objects.Election.generateElectionSetupId;
import static com.github.dedis.popstellar.model.objects.event.EventState.*;
import static com.github.dedis.popstellar.testutils.Base64DataUtils.generateKeyPair;
import static com.github.dedis.popstellar.testutils.UITestUtils.dialogNegativeButton;
import static com.github.dedis.popstellar.testutils.UITestUtils.dialogPositiveButton;
import static com.github.dedis.popstellar.testutils.pages.detail.event.election.ElectionFragmentPageObject.*;
import static com.github.dedis.popstellar.testutils.pages.lao.LaoActivityPageObject.containerId;
import static com.github.dedis.popstellar.testutils.pages.lao.LaoActivityPageObject.laoIdExtra;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@HiltAndroidTest
@RunWith(AndroidJUnit4.class)
public class ElectionFragmentTest {

  private static final String LAO_NAME = "lao";
  private static final KeyPair SENDER_KEY = generateKeyPair();
  private static final PublicKey SENDER = SENDER_KEY.getPublicKey();
  private static final Lao LAO = new Lao(LAO_NAME, SENDER, 10223421);
  private static final String LAO_ID = LAO.getId();
  private static final String TITLE = "Election name";
  private static final long CREATION = 10323411;
  private static final long START = 10323421;
  private static final long END = 10323431;

  private static final String ELECTION_ID = generateElectionSetupId(LAO_ID, CREATION, TITLE);
  private static final ElectionQuestion ELECTION_QUESTION_1 =
      new ElectionQuestion(
          ELECTION_ID,
          new ElectionQuestion.Question(
              "ElectionQuestion", "Plurality", Arrays.asList("1", "2"), false));
  private static final ElectionQuestion ELECTION_QUESTION_2 =
      new ElectionQuestion(
          ELECTION_ID,
          new ElectionQuestion.Question(
              "ElectionQuestion2", "Plurality", Arrays.asList("a", "b"), false));

  private static final Election ELECTION =
      new Election.ElectionBuilder(LAO_ID, CREATION, TITLE)
          .setElectionVersion(ElectionVersion.OPEN_BALLOT)
          .setElectionQuestions(Arrays.asList(ELECTION_QUESTION_1, ELECTION_QUESTION_2))
          .setStart(START)
          .setEnd(END)
          .setState(CREATED)
          .build();

  private static final BehaviorSubject<LaoView> laoSubject =
      BehaviorSubject.createDefault(new LaoView(LAO));

  private static final DateFormat DATE_FORMAT =
      new SimpleDateFormat("dd/MM/yyyy HH:mm z", Locale.ENGLISH);

  @Inject ElectionRepository electionRepository;

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

          electionRepository.updateElection(ELECTION);

          when(repository.getLaoObservable(anyString())).thenReturn(laoSubject);
          when(repository.getLaoView(any())).thenAnswer(invocation -> new LaoView(LAO));

          when(keyManager.getMainPublicKey()).thenReturn(SENDER);
          when(networkManager.getMessageSender()).thenReturn(messageSenderHelper.getMockedSender());
          messageSenderHelper.setupMock();
        }
      };

  @Rule(order = 3)
  public ActivityFragmentScenarioRule<LaoActivity, ElectionFragment> activityScenarioRule =
      ActivityFragmentScenarioRule.launchIn(
          LaoActivity.class,
          new BundleBuilder().putString(laoIdExtra(), LAO_ID).build(),
          containerId(),
          ElectionFragment.class,
          () -> ElectionFragment.newInstance(ELECTION.getId()));

  @Test
  public void electionTitleMatches() {
    electionFragmentTitle().check(matches(withText(TITLE)));
  }

  @Test
  public void statusCreatedTests() {
    electionFragmentStatus().check(matches(withText("Not yet opened")));
  }

  @Test
  public void datesDisplayedMatches() {
    Date startTime = new Date(ELECTION.getStartTimestampInMillis());
    Date endTime = new Date(ELECTION.getEndTimestampInMillis());
    String startTimeText = DATE_FORMAT.format(startTime);
    String endTimeText = DATE_FORMAT.format(endTime);

    electionFragmentStartTime().check(matches(withText(startTimeText)));
    electionFragmentEndTime().check(matches(withText(endTimeText)));
  }

  @Test
  public void managementButtonIsDisplayed() {
    electionManagementButton().check(matches(isDisplayed()));
  }

  @Test
  public void managementButtonOpensElectionWhenCreated() {
    electionManagementButton().check(matches(withText("OPEN")));
    electionManagementButton().perform(click());
    dialogPositiveButton().performClick();
    // Wait for the main thread to finish executing the calls made above
    // before asserting their effect
    InstrumentationRegistry.getInstrumentation().waitForIdleSync();

    verify(messageSenderHelper.getMockedSender())
        .publish(any(), eq(ELECTION.getChannel()), any(OpenElection.class));
    messageSenderHelper.assertSubscriptions();
  }

  @Test
  public void actionButtonCreatedTest() {
    electionActionButton().check(matches(withText("VOTE")));
    electionActionButton().check(matches(not(isEnabled())));
  }

  @Test
  public void statusOpenTest() {
    openElection();
    electionFragmentStatus().check(matches(withText("Open")));
  }

  @Test
  public void managementButtonEndElectionWhenOpened() {
    openElection();
    electionManagementButton().check(matches(withText("CLOSE")));
    electionManagementButton().perform(click());
    dialogPositiveButton().performClick();
    // Wait for the main thread to finish executing the calls made above
    // before asserting their effect
    InstrumentationRegistry.getInstrumentation().waitForIdleSync();

    verify(messageSenderHelper.getMockedSender())
        .publish(any(), eq(ELECTION.getChannel()), any(ElectionEnd.class));
    messageSenderHelper.assertSubscriptions();
  }

  @Test
  public void actionButtonOpenTest() {
    openElection();

    electionActionButton().check(matches(withText("VOTE")));
    electionActionButton().check(matches(isEnabled()));
  }

  @Test
  public void statusClosedTest() {
    closeElection();
    electionFragmentStatus().check(matches(withText("Waiting for results")));
  }

  @Test
  public void managementButtonClosedTest() {
    closeElection();
    electionManagementButton().check(matches(not(isDisplayed())));
  }

  @Test
  public void actionButtonClosedTest() {
    closeElection();
    electionActionButton().check(matches(withText("Results")));
    electionActionButton().check(matches(not(isEnabled())));
  }

  @Test
  public void statusResultsTest() {
    receiveResults();
    electionFragmentStatus().check(matches(withText("Finished")));
  }

  @Test
  public void managementButtonResultsTest() {
    receiveResults();
    electionManagementButton().check(matches(not(isDisplayed())));
  }

  @Test
  public void actionButtonResultsTest() {
    receiveResults();
    electionActionButton().check(matches(withText("Results")));
    electionActionButton().check(matches(isEnabled()));
  }

  @Test
  public void openButtonDisplaysDialogOnclick() {
    electionManagementButton().perform(click());
    assertThat(dialogPositiveButton(), allOf(withText("Yes"), isDisplayed()));
    assertThat(dialogNegativeButton(), allOf(withText("No"), isDisplayed()));
  }

  @Test
  public void closeButtonDisplaysDialogOnclick() {
    openElection();
    electionManagementButton().perform(click());
    assertThat(dialogPositiveButton(), allOf(withText("Yes"), isDisplayed()));
    assertThat(dialogNegativeButton(), allOf(withText("No"), isDisplayed()));
  }

  private void openElection() {
    electionRepository.updateElection(ELECTION.builder().setState(OPENED).build());
  }

  private void closeElection() {
    electionRepository.updateElection(ELECTION.builder().setState(CLOSED).build());
  }

  private void receiveResults() {
    electionRepository.updateElection(ELECTION.builder().setState(RESULTS_READY).build());
  }
}
