package com.github.dedis.popstellar.ui.detail.event.election;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.github.dedis.popstellar.model.network.method.message.data.election.*;
import com.github.dedis.popstellar.model.objects.Election;
import com.github.dedis.popstellar.model.objects.Lao;
import com.github.dedis.popstellar.model.objects.security.KeyPair;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.model.objects.view.LaoView;
import com.github.dedis.popstellar.repository.ElectionRepository;
import com.github.dedis.popstellar.repository.LAORepository;
import com.github.dedis.popstellar.repository.remote.GlobalNetworkManager;
import com.github.dedis.popstellar.repository.remote.MessageSender;
import com.github.dedis.popstellar.testutils.BundleBuilder;
import com.github.dedis.popstellar.testutils.fragment.ActivityFragmentScenarioRule;
import com.github.dedis.popstellar.ui.detail.event.election.fragments.ElectionResultFragment;
import com.github.dedis.popstellar.ui.lao.LaoActivity;
import com.github.dedis.popstellar.utility.error.UnknownLaoException;
import com.github.dedis.popstellar.utility.security.KeyManager;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoTestRule;

import java.util.*;

import javax.inject.Inject;

import dagger.hilt.android.testing.*;
import io.reactivex.Completable;
import io.reactivex.subjects.BehaviorSubject;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static com.github.dedis.popstellar.model.objects.Election.generateElectionSetupId;
import static com.github.dedis.popstellar.model.objects.event.EventState.CREATED;
import static com.github.dedis.popstellar.testutils.Base64DataUtils.generateKeyPair;
import static com.github.dedis.popstellar.testutils.pages.detail.event.election.ElectionResultFragmentPageObject.electionResultElectionTitle;
import static com.github.dedis.popstellar.testutils.pages.detail.event.election.ElectionResultFragmentPageObject.electionResultLaoTitle;
import static com.github.dedis.popstellar.testutils.pages.lao.LaoActivityPageObject.containerId;
import static com.github.dedis.popstellar.testutils.pages.lao.LaoActivityPageObject.laoIdExtra;
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
  private static final String ELECTION_BALLOT1 = "ballot option 1";
  private static final String ELECTION_BALLOT2 = "ballot option 2";
  private static final String ELECTION_BALLOT3 = "ballot option 3";

  private static final String PLURALITY = "Plurality";
  private static final int RESULT1 = 7;
  private static final int RESULT2 = 0;
  private static final int RESULT3 = 5;

  private static final String ELECTION_ID = generateElectionSetupId(LAO_ID, CREATION, TITLE);

  private static final ElectionQuestion QUESTION =
      new ElectionQuestion(
          ELECTION_ID,
          new ElectionQuestion.Question(
              ELECTION_QUESTION_TEXT,
              PLURALITY,
              Arrays.asList(ELECTION_BALLOT1, ELECTION_BALLOT2, ELECTION_BALLOT3),
              false));

  private static final Election ELECTION =
      new Election.ElectionBuilder(LAO_ID, CREATION, TITLE)
          .setElectionVersion(ElectionVersion.OPEN_BALLOT)
          .setElectionQuestions(Collections.singletonList(QUESTION))
          .setStart(START)
          .setEnd(END)
          .setState(CREATED)
          .setResults(
              buildResultsMap(
                  QUESTION.getId(),
                  new QuestionResult(ELECTION_BALLOT1, RESULT1),
                  new QuestionResult(ELECTION_BALLOT2, RESULT2),
                  new QuestionResult(ELECTION_BALLOT3, RESULT3)))
          .build();

  @Inject ElectionRepository electionRepository;

  @BindValue @Mock LAORepository laoRepository;
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
        protected void before() throws UnknownLaoException {
          hiltRule.inject();
          when(laoRepository.getLaoObservable(anyString()))
              .thenReturn(BehaviorSubject.createDefault(new LaoView(LAO)));
          when(laoRepository.getLaoView(any())).thenAnswer(invocation -> new LaoView(LAO));

          electionRepository.updateElection(ELECTION);
          when(keyManager.getMainPublicKey()).thenReturn(SENDER);

          when(networkManager.getMessageSender()).thenReturn(messageSender);
          when(messageSender.subscribe(any())).then(args -> Completable.complete());
          when(messageSender.publish(any(), any())).then(args -> Completable.complete());
          when(messageSender.publish(any(), any(), any())).then(args -> Completable.complete());
        }
      };

  @Rule(order = 3)
  public final ActivityFragmentScenarioRule<LaoActivity, ElectionResultFragment> fragmentRule =
      ActivityFragmentScenarioRule.launchIn(
          LaoActivity.class,
          new BundleBuilder().putString(laoIdExtra(), LAO_ID).build(),
          containerId(),
          ElectionResultFragment.class,
          () -> ElectionResultFragment.newInstance(ELECTION_ID));

  private static Map<String, Set<QuestionResult>> buildResultsMap(
      String id, QuestionResult... questionResults) {
    return Collections.singletonMap(id, new HashSet<>(Arrays.asList(questionResults)));
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
    onView(withText(ELECTION_BALLOT1)).check(matches(isDisplayed()));
    onView(withText(ELECTION_BALLOT2)).check(matches(isDisplayed()));
    onView(withText(ELECTION_BALLOT3)).check(matches(isDisplayed()));
  }
}
