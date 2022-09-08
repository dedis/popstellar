package com.github.dedis.popstellar.ui.detail.event.election;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.fragment.app.FragmentActivity;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.github.dedis.popstellar.model.network.method.message.data.election.*;
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
import com.github.dedis.popstellar.ui.detail.event.election.fragments.ElectionResultFragment;
import com.github.dedis.popstellar.utility.security.KeyManager;

import org.junit.*;
import org.junit.rules.ExternalResource;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoTestRule;

import java.util.Arrays;

import dagger.hilt.android.testing.*;
import io.reactivex.Completable;
import io.reactivex.subjects.BehaviorSubject;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static com.github.dedis.popstellar.model.objects.event.EventState.CREATED;
import static com.github.dedis.popstellar.testutils.Base64DataUtils.generateKeyPair;
import static com.github.dedis.popstellar.testutils.pages.detail.event.election.ElectionResultFragmentPageObject.electionResultElectionTitle;
import static com.github.dedis.popstellar.testutils.pages.detail.event.election.ElectionResultFragmentPageObject.electionResultLaoTitle;
import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@HiltAndroidTest
@RunWith(AndroidJUnit4.class)
public class ElectionResultFragmentTest {
  private static final String LAO_NAME = "LAO";
  private static final KeyPair SENDER_KEY = generateKeyPair();
  private static final PublicKey SENDER = SENDER_KEY.getPublicKey();
  private static final Lao LAO = new Lao(LAO_NAME, SENDER, 10223421);
  private static final String LAO_ID = LAO.getId();

  private static final String TITLE = "Election name";
  private static final long CREATION = 10323411;
  private static final long START = 10323421;
  private static final long END = 10323431;
  private static final String ELECTION_QUESTION_TEXT = "question";
  private static final String ELECTION_BALLOT_TEXT1 = "ballot option 1";
  private static final String ELECTION_BALLOT_TEXT2 = "ballot option 2";
  private static final String ELECTION_BALLOT_TEXT3 = "ballot option 3";

  private static final String PLURALITY = "Plurality";
  private static final int RESULT1 = 7;
  private static final int RESULT2 = 0;
  private static final int RESULT3 = 5;

  private static Election election;

  ElectionQuestion electionQuestion;

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

          initializeElection();
          when(keyManager.getMainPublicKey()).thenReturn(SENDER);

          when(networkManager.getMessageSender()).thenReturn(messageSender);
          when(messageSender.subscribe(any())).then(args -> Completable.complete());
          when(messageSender.publish(any(), any())).then(args -> Completable.complete());
          when(messageSender.publish(any(), any(), any())).then(args -> Completable.complete());
        }
      };

  @Rule(order = 3)
  public final FragmentScenarioRule<ElectionResultFragment> fragmentRule =
      FragmentScenarioRule.launch(
          ElectionResultFragment.class, ElectionResultFragment::newInstance);

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
    electionResultLaoTitle().check(matches(withText(LAO_NAME)));
  }

  @Test
  public void electionTitleMatches() {
    electionResultElectionTitle().check(matches(withText(TITLE)));
  }

  @Test
  public void question1ElementsAreDisplayed() {
    onView(withText(ELECTION_QUESTION_TEXT)).check(matches(isDisplayed()));
    onView(withText(ELECTION_BALLOT_TEXT1)).check(matches(isDisplayed()));
    onView(withText(ELECTION_BALLOT_TEXT2)).check(matches(isDisplayed()));
    onView(withText(ELECTION_BALLOT_TEXT3)).check(matches(isDisplayed()));
  }

  private void initializeElection() {
    election = new Election(LAO_ID, CREATION, TITLE, ElectionVersion.OPEN_BALLOT);

    electionQuestion =
        new ElectionQuestion(
            ELECTION_QUESTION_TEXT,
            PLURALITY,
            false,
            Arrays.asList(ELECTION_BALLOT_TEXT1, ELECTION_BALLOT_TEXT2, ELECTION_BALLOT_TEXT3),
            election.getId());

    QuestionResult result11 = new QuestionResult(ELECTION_BALLOT_TEXT1, RESULT1);
    QuestionResult result12 = new QuestionResult(ELECTION_BALLOT_TEXT2, RESULT2);
    QuestionResult result13 = new QuestionResult(ELECTION_BALLOT_TEXT3, RESULT3);

    ElectionResultQuestion questionResult1 =
        new ElectionResultQuestion(
            electionQuestion.getId(), Arrays.asList(result11, result12, result13));

    election.setElectionQuestions(singletonList(electionQuestion));
    election.setResults(singletonList(questionResult1));

    election.setStart(START);
    election.setEnd(END);
    election.setEventState(CREATED);
  }
}
