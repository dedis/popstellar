package com.github.dedis.popstellar.ui.detail.event.election;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.fragment.app.FragmentActivity;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.github.dedis.popstellar.model.network.method.message.data.election.ElectionQuestion;
import com.github.dedis.popstellar.model.network.method.message.data.election.ElectionVersion;
import com.github.dedis.popstellar.model.objects.Election;
import com.github.dedis.popstellar.model.objects.Lao;
import com.github.dedis.popstellar.model.objects.security.KeyPair;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.repository.LAORepository;
import com.github.dedis.popstellar.repository.remote.GlobalNetworkManager;
import com.github.dedis.popstellar.repository.remote.MessageSender;
import com.github.dedis.popstellar.testutils.fragment.FragmentScenarioRule;
import com.github.dedis.popstellar.ui.detail.LaoDetailActivity;
import com.github.dedis.popstellar.ui.detail.LaoDetailViewModel;
import com.github.dedis.popstellar.ui.detail.event.election.fragments.CastVoteFragment;
import com.github.dedis.popstellar.utility.security.KeyManager;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoTestRule;

import java.util.Arrays;
import java.util.Collections;

import dagger.hilt.android.testing.BindValue;
import dagger.hilt.android.testing.HiltAndroidRule;
import dagger.hilt.android.testing.HiltAndroidTest;
import io.reactivex.Completable;
import io.reactivex.subjects.BehaviorSubject;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.swipeLeft;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static com.github.dedis.popstellar.model.objects.event.EventState.CREATED;
import static com.github.dedis.popstellar.testutils.Base64DataUtils.generateKeyPair;
import static com.github.dedis.popstellar.ui.pages.detail.event.election.CastVoteFragmentPageObject.castVoteButton;
import static com.github.dedis.popstellar.ui.pages.detail.event.election.CastVoteFragmentPageObject.castVoteElectionName;
import static com.github.dedis.popstellar.ui.pages.detail.event.election.CastVoteFragmentPageObject.castVoteLaoTitle;
import static com.github.dedis.popstellar.ui.pages.detail.event.election.CastVoteFragmentPageObject.castVotePager;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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
  private static final String ELECTION_QUESTION_TEXT2 = "question 1";
  private static final String ELECTION_BALLOT_TEXT11 = "ballot option 1";
  private static final String ELECTION_BALLOT_TEXT12 = "ballot option 2";
  private static final String ELECTION_BALLOT_TEXT13 = "ballot option 3";
  private static final String ELECTION_BALLOT_TEXT21 = "random 1";
  private static final String ELECTION_BALLOT_TEXT22 = "random 2";
  private static final String PLURALITY = "Plurality";

  private static Election election;

  ElectionQuestion electionQuestion1;

  private ElectionQuestion electionQuestion2;

  @BindValue @Mock LAORepository repository;
  @BindValue @Mock KeyManager keyManager;
  @BindValue @Mock MessageSender messageSender;
  @BindValue @Mock GlobalNetworkManager networkManager;

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
          when(repository.getAllLaos())
              .thenReturn(BehaviorSubject.createDefault(Collections.singletonList(LAO)));
          initializeElection();
          when(keyManager.getMainPublicKey()).thenReturn(SENDER);

          when(networkManager.getMessageSender()).thenReturn(messageSender);
          when(messageSender.subscribe(any())).then(args -> Completable.complete());
          when(messageSender.publish(any(), any())).then(args -> Completable.complete());
          when(messageSender.publish(any(), any(), any())).then(args -> Completable.complete());
        }
      };

  @Rule(order = 3)
  public final FragmentScenarioRule<CastVoteFragment> fragmentRule =
      FragmentScenarioRule.launch(CastVoteFragment.class, CastVoteFragment::newInstance);

  @Before
  public void setUpViewModel() {
    fragmentRule
        .getScenario()
        .onFragment(
            fragment -> {
              FragmentActivity fragmentActivity = fragment.requireActivity();
              LaoDetailViewModel viewModel = LaoDetailActivity.obtainViewModel(fragmentActivity);
              viewModel.setCurrentLao(LAO);
              viewModel.setCurrentElection(election);
            });
    fragmentRule.getScenario().recreate();
  }

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
    castVoteButton().check(matches(isEnabled()));
  }

  private void initializeElection() {
    election = new Election(LAO_ID, CREATION, TITLE, ElectionVersion.OPEN_BALLOT);
    electionQuestion1 =
        new ElectionQuestion(
            ELECTION_QUESTION_TEXT1,
            PLURALITY,
            false,
            Arrays.asList(ELECTION_BALLOT_TEXT11, ELECTION_BALLOT_TEXT12, ELECTION_BALLOT_TEXT13),
            election.getId());
    electionQuestion2 =
        new ElectionQuestion(
            ELECTION_QUESTION_TEXT2,
            PLURALITY,
            false,
            Arrays.asList(ELECTION_BALLOT_TEXT21, ELECTION_BALLOT_TEXT22),
            election.getId());
    election.setElectionQuestions(Arrays.asList(electionQuestion1, electionQuestion2));
    election.setStart(START);
    election.setEnd(END);
    election.setEventState(CREATED);
  }
}
