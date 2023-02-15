package com.github.dedis.popstellar.ui.lao.event.election;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.github.dedis.popstellar.model.network.method.message.data.election.*;
import com.github.dedis.popstellar.model.objects.*;
import com.github.dedis.popstellar.model.objects.event.EventState;
import com.github.dedis.popstellar.model.objects.security.KeyPair;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.model.objects.view.LaoView;
import com.github.dedis.popstellar.repository.*;
import com.github.dedis.popstellar.repository.remote.GlobalNetworkManager;
import com.github.dedis.popstellar.testutils.BundleBuilder;
import com.github.dedis.popstellar.testutils.MessageSenderHelper;
import com.github.dedis.popstellar.testutils.fragment.ActivityFragmentScenarioRule;
import com.github.dedis.popstellar.ui.lao.LaoActivity;
import com.github.dedis.popstellar.ui.lao.event.election.fragments.CastVoteFragment;
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

import java.util.Arrays;
import java.util.HashSet;

import javax.inject.Inject;

import dagger.hilt.android.testing.*;
import io.reactivex.subjects.BehaviorSubject;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.swipeLeft;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static com.github.dedis.popstellar.model.objects.Election.generateElectionSetupId;
import static com.github.dedis.popstellar.model.objects.event.EventState.CREATED;
import static com.github.dedis.popstellar.testutils.Base64DataUtils.generateKeyPair;
import static com.github.dedis.popstellar.testutils.Base64DataUtils.generatePoPToken;
import static com.github.dedis.popstellar.testutils.pages.lao.LaoActivityPageObject.containerId;
import static com.github.dedis.popstellar.testutils.pages.lao.LaoActivityPageObject.laoIdExtra;
import static com.github.dedis.popstellar.testutils.pages.lao.event.election.CastVoteFragmentPageObject.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@HiltAndroidTest
@RunWith(AndroidJUnit4.class)
public class CastVoteFragmentTest {
  private static final String LAO_NAME = "LAO";
  private static final KeyPair SENDER_KEY = generateKeyPair();
  private static final PublicKey SENDER = SENDER_KEY.getPublicKey();
  private static final Lao LAO = new Lao(LAO_NAME, SENDER, 10223421);
  private static final String LAO_ID = LAO.getId();

  private static final String TITLE = "Election name";
  private static final long CREATION = 10323411;
  private static final long START = 10323421;
  private static final long END = 10323431;
  private static final String ELECTION_QUESTION_TEXT1 = "question 1";
  private static final String ELECTION_QUESTION_TEXT2 = "question 2";
  private static final String ELECTION_BALLOT_TEXT11 = "ballot option 1";
  private static final String ELECTION_BALLOT_TEXT12 = "ballot option 2";
  private static final String ELECTION_BALLOT_TEXT13 = "ballot option 3";
  private static final String ELECTION_BALLOT_TEXT21 = "random 1";
  private static final String ELECTION_BALLOT_TEXT22 = "random 2";
  private static final String PLURALITY = "Plurality";
  private static final BehaviorSubject<LaoView> laoSubject =
      BehaviorSubject.createDefault(new LaoView(LAO));
  private static final RollCall ROLL_CALL =
      new RollCall(
          "id", "id", "rc", 0L, 1L, 2L, EventState.CLOSED, new HashSet<>(), "nowhere", "none");

  private static final String ELECTION_ID = generateElectionSetupId(LAO_ID, CREATION, TITLE);
  private static final ElectionQuestion ELECTION_QUESTION_1 =
      new ElectionQuestion(
          ELECTION_ID,
          new ElectionQuestion.Question(
              ELECTION_QUESTION_TEXT1,
              PLURALITY,
              Arrays.asList(ELECTION_BALLOT_TEXT11, ELECTION_BALLOT_TEXT12, ELECTION_BALLOT_TEXT13),
              false));
  private static final ElectionQuestion ELECTION_QUESTION_2 =
      new ElectionQuestion(
          ELECTION_ID,
          new ElectionQuestion.Question(
              ELECTION_QUESTION_TEXT2,
              PLURALITY,
              Arrays.asList(ELECTION_BALLOT_TEXT21, ELECTION_BALLOT_TEXT22),
              false));

  private static final Election ELECTION =
      new Election.ElectionBuilder(LAO_ID, CREATION, TITLE)
          .setElectionVersion(ElectionVersion.OPEN_BALLOT)
          .setElectionQuestions(Arrays.asList(ELECTION_QUESTION_1, ELECTION_QUESTION_2))
          .setStart(START)
          .setEnd(END)
          .setState(CREATED)
          .build();

  @Inject ElectionRepository electionRepo;
  @Inject RollCallRepository rollCallRepo;

  @BindValue @Mock LAORepository laoRepo;
  @BindValue @Mock KeyManager keyManager;
  @BindValue @Mock GlobalNetworkManager networkManager;

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

          rollCallRepo.updateRollCall(LAO_ID, ROLL_CALL);
          electionRepo.updateElection(ELECTION);

          when(keyManager.getMainPublicKey()).thenReturn(SENDER);
          when(keyManager.getValidPoPToken(any(), any())).thenReturn(generatePoPToken());

          when(networkManager.getMessageSender()).thenReturn(messageSenderHelper.getMockedSender());
          messageSenderHelper.setupMock();
        }
      };

  @Rule(order = 3)
  public final ActivityFragmentScenarioRule<LaoActivity, CastVoteFragment> fragmentRule =
      ActivityFragmentScenarioRule.launchIn(
          LaoActivity.class,
          new BundleBuilder().putString(laoIdExtra(), LAO_ID).build(),
          containerId(),
          CastVoteFragment.class,
          () -> CastVoteFragment.newInstance(ELECTION_ID));

  @Test
  public void laoTitleMatches() {
    castVoteLaoTitle().check(matches(withText(LAO_NAME)));
  }

  @Test
  public void electionTitleMatches() {
    castVoteElectionName().check(matches(withText(TITLE)));
  }

  @Test
  public void question1ElementsAreDisplayed() {
    onView(withText(ELECTION_QUESTION_TEXT1)).check(matches(isDisplayed()));
    onView(withText(ELECTION_BALLOT_TEXT11)).check(matches(isDisplayed()));
    onView(withText(ELECTION_BALLOT_TEXT12)).check(matches(isDisplayed()));
    onView(withText(ELECTION_BALLOT_TEXT13)).check(matches(isDisplayed()));
  }

  @Test
  public void question2ElementsAreDisplayed() {
    castVotePager().perform(swipeLeft());
    onView(withText(ELECTION_QUESTION_TEXT2)).check(matches(isDisplayed()));
    onView(withText(ELECTION_BALLOT_TEXT21)).check(matches(isDisplayed()));
    onView(withText(ELECTION_BALLOT_TEXT22)).check(matches(isDisplayed()));
  }

  @Test
  public void castVoteButtonIsEnabledWhenAnElementIsClicked() {
    onView(withText(ELECTION_BALLOT_TEXT11)).perform(click());
    castVotePager().perform(swipeLeft());
    onView(withText(ELECTION_BALLOT_TEXT22)).perform(click());
    castVoteButton().check(matches(isEnabled()));
  }

  @Test
  public void castVoteSendsACastVoteMessage() {
    onView(withText(ELECTION_BALLOT_TEXT11)).perform(click());
    castVotePager().perform(swipeLeft());
    onView(withText(ELECTION_BALLOT_TEXT22)).perform(click());
    castVoteButton().perform(click());
    // Wait for the operations performed above to complete
    InstrumentationRegistry.getInstrumentation().waitForIdleSync();

    verify(messageSenderHelper.getMockedSender())
        .publish(any(), eq(ELECTION.getChannel()), any(CastVote.class));
  }
}
