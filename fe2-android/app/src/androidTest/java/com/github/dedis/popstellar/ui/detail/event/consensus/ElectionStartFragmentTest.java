package com.github.dedis.popstellar.ui.detail.event.consensus;

import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.isNotEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static com.github.dedis.popstellar.pages.detail.event.consensus.ElectionStartPageObject.electionStartButton;
import static com.github.dedis.popstellar.pages.detail.event.consensus.ElectionStartPageObject.electionStatus;
import static com.github.dedis.popstellar.pages.detail.event.consensus.ElectionStartPageObject.electionTitle;
import static com.github.dedis.popstellar.pages.detail.event.consensus.ElectionStartPageObject.nodesGrid;
import static org.hamcrest.core.AllOf.allOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import androidx.fragment.app.FragmentActivity;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.DataInteraction;
import androidx.test.espresso.action.ViewActions;
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;

import com.github.dedis.popstellar.Injection;
import com.github.dedis.popstellar.model.network.GenericMessage;
import com.github.dedis.popstellar.model.network.answer.Result;
import com.github.dedis.popstellar.model.network.method.Message;
import com.github.dedis.popstellar.model.network.method.Publish;
import com.github.dedis.popstellar.model.network.method.message.MessageGeneral;
import com.github.dedis.popstellar.model.network.method.message.data.consensus.ConsensusElect;
import com.github.dedis.popstellar.model.network.method.message.data.consensus.ConsensusElectAccept;
import com.github.dedis.popstellar.model.network.method.message.data.consensus.ConsensusKey;
import com.github.dedis.popstellar.model.network.method.message.data.consensus.ConsensusLearn;
import com.github.dedis.popstellar.model.network.serializer.JsonUtils;
import com.github.dedis.popstellar.model.objects.Consensus;
import com.github.dedis.popstellar.model.objects.Election;
import com.github.dedis.popstellar.model.objects.Lao;
import com.github.dedis.popstellar.repository.LAORepository;
import com.github.dedis.popstellar.repository.LAOState;
import com.github.dedis.popstellar.repository.local.LAOLocalDataSource;
import com.github.dedis.popstellar.repository.remote.LAORemoteDataSource;
import com.github.dedis.popstellar.testutils.FragmentScenarioRule;
import com.github.dedis.popstellar.ui.detail.LaoDetailActivity;
import com.github.dedis.popstellar.ui.detail.LaoDetailViewModel;
import com.github.dedis.popstellar.utility.error.DataHandlingException;
import com.github.dedis.popstellar.utility.handler.ConsensusHandler;
import com.github.dedis.popstellar.utility.scheduler.ProdSchedulerProvider;
import com.github.dedis.popstellar.utility.security.Keys;
import com.google.crypto.tink.KeysetHandle;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.internal.util.collections.Sets;
import org.mockito.junit.MockitoJUnit;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;

@RunWith(AndroidJUnit4ClassRunner.class)
public class ElectionStartFragmentTest {

  // A custom rule to call setup and teardown before the fragment rule and after the mockito rule
  private final TestRule setupRule =
      new ExternalResource() {
        @Override
        protected void before() {
          // Preload the data schema before the test run
          JsonUtils.loadSchema(JsonUtils.DATA_SCHEMA);

          when(remoteDataSource.incrementAndGetRequestId()).thenReturn(42);
          when(remoteDataSource.observeWebsocket()).thenReturn(Observable.empty());
          Observable<GenericMessage> upstream =
              Observable.fromArray((GenericMessage) new Result(42));

          // Mock the remote data source to receive a response
          when(remoteDataSource.observeMessage()).thenReturn(upstream);

          try {
            LAORepository.destroyInstance();
            laoRepository =
                LAORepository.getInstance(
                    remoteDataSource,
                    localDataSource,
                    Injection.provideAndroidKeysetManager(
                        ApplicationProvider.getApplicationContext()),
                    Injection.provideGson(),
                    new ProdSchedulerProvider());
            publicKey = getPublicKey();
          } catch (Exception e) {
            throw new RuntimeException(e);
          }

          election.setStart(FUTURE_TIME);
          LAO.setChannel(LAO_CHANNEL);
          LAO.setOrganizer(publicKey);
          LAO.setWitnesses(Sets.newSet(NODE_2_KEY, NODE_3_KEY));

          laoRepository.getLaoById().put(LAO_CHANNEL, new LAOState(LAO));
          laoRepository.updateNodes(LAO_CHANNEL);
        }

        @Override
        protected void after() {
          LAORepository.destroyInstance();
        }
      };

  private final FragmentScenarioRule<ElectionStartFragment> fragmentRule =
      FragmentScenarioRule.launchInContainer(
          ElectionStartFragment.class, ElectionStartFragment::newInstance);

  @Rule
  public final RuleChain chain =
      RuleChain.outerRule(MockitoJUnit.testRule(this)).around(setupRule).around(fragmentRule);

  private static final DateTimeFormatter DATE_TIME_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss z").withZone(ZoneId.systemDefault());
  private static final String LAO_ID = "laoId";
  private static final String LAO_CHANNEL = "/root/" + LAO_ID;
  private static final String CONSENSUS_CHANNEL = LAO_CHANNEL + "/consensus";
  private static final String ELECTION_NAME = "My Election !";
  private static final long PAST_TIME = 946684800;
  private static final long FUTURE_TIME = 2145916800;

  private static final Lao LAO = new Lao(LAO_ID);
  private static final Election election = new Election(LAO_ID, PAST_TIME, ELECTION_NAME);
  private static final String NODE_2_KEY = "12RDUW4s9bZrdqJB7zoVag";
  private static final String NODE_3_KEY = "dRCXFdXYfY5OVAzh3oQ3pA";
  private static final ConsensusKey KEY = new ConsensusKey("election", election.getId(), "state");
  private static final String INSTANCE_ID =
      Consensus.generateConsensusId(KEY.getType(), KEY.getId(), KEY.getProperty());

  private static final ConsensusElect elect =
      new ConsensusElect(PAST_TIME, KEY.getId(), KEY.getType(), KEY.getProperty(), "started");
  private static final ConsensusElectAccept accept3 =
      new ConsensusElectAccept(INSTANCE_ID, "m3", true);
  private static final ConsensusLearn learn3 =
      new ConsensusLearn(INSTANCE_ID, "m3", Collections.emptyList());

  private static final ArgumentCaptor<Message> CAPTOR = ArgumentCaptor.forClass(Message.class);

  private String publicKey;
  private LAORepository laoRepository;

  @Mock private LAORemoteDataSource remoteDataSource;
  @Mock private LAOLocalDataSource localDataSource;

  @Test
  public void displayWithUpdatesIsCorrectAndButtonsProduceCorrectMessages()
      throws InterruptedException, DataHandlingException {
    fragmentRule
        .getScenario()
        .onFragment(
            electionStartFragment -> {
              FragmentActivity fragmentActivity = electionStartFragment.requireActivity();
              LaoDetailViewModel laoDetailViewModel =
                  LaoDetailActivity.obtainViewModel(fragmentActivity);
              laoDetailViewModel.setCurrentElection(election);
              laoDetailViewModel.setCurrentLao(LAO);
            });

    // Recreate the fragment because the viewModel needed to be modified before start
    fragmentRule.getScenario().recreate();

    String expectedTitle = "Election " + '"' + ELECTION_NAME + '"';
    String expectedStatusBefore = "Waiting scheduled time";
    String expectedStatusAfter = "Ready to start";
    String futureDate = DATE_TIME_FORMATTER.format(Instant.ofEpochSecond(FUTURE_TIME));
    String pastDate = DATE_TIME_FORMATTER.format(Instant.ofEpochSecond(PAST_TIME));
    String expectedStartBefore = "Election scheduled to start at\n" + futureDate;
    String expectedStartAfter = "Start Election";
    String expectedStatusStarted = "Started";
    String expectedStartStarted = "Election started successfully at\n" + pastDate;

    // Election start time has not passed yet, should display that it's waiting
    electionTitle().check(matches(withText(expectedTitle))).check(matches(isDisplayed()));
    electionStatus().check(matches(withText(expectedStatusBefore))).check(matches(isDisplayed()));
    electionStartButton()
        .check(matches(withText(expectedStartBefore)))
        .check(matches(isDisplayed()))
        .check(matches(isNotEnabled()));

    // Wait for the timer update
    election.setStart(PAST_TIME);
    TimeUnit.SECONDS.sleep(2);

    // Election start time has passed, should display that it's ready and start button enabled
    electionStatus().check(matches(withText(expectedStatusAfter))).check(matches(isDisplayed()));
    electionStartButton()
        .check(matches(withText(expectedStartAfter)))
        .check(matches(isDisplayed()))
        .check(matches(isEnabled()));

    // Order of nodes are not guaranteed in general, but in this this it's ownNode(0), node2, node3
    DataInteraction grid = nodesGrid();
    nodeAssertions(grid, 0, "Waiting\n" + publicKey, false);
    nodeAssertions(grid, 1, "Waiting\n" + NODE_2_KEY, false);
    nodeAssertions(grid, 2, "Waiting\n" + NODE_3_KEY, false);

    // Nodes 3 try to start
    ConsensusHandler.handleConsensusMessage(
        laoRepository, CONSENSUS_CHANNEL, elect, "m3", NODE_3_KEY);
    laoRepository.updateNodes(LAO_CHANNEL);

    nodeAssertions(grid, 2, "Approve Start by\n" + NODE_3_KEY, true);

    // We try to start
    long minCreation = Instant.now().getEpochSecond();
    electionStartButton().perform(ViewActions.click());
    Mockito.verify(remoteDataSource).sendMessage(CAPTOR.capture());
    Publish publish = (Publish) CAPTOR.getValue();
    MessageGeneral msgGeneral = publish.getMessage();
    long maxCreation = Instant.now().getEpochSecond();
    assertEquals(CONSENSUS_CHANNEL, publish.getChannel());
    assertEquals(publicKey, msgGeneral.getSender());

    ConsensusElect elect = (ConsensusElect) msgGeneral.getData();
    assertEquals(KEY, elect.getKey());
    assertEquals(INSTANCE_ID, elect.getInstanceId());
    assertEquals("started", elect.getValue());
    assertTrue(minCreation <= elect.getCreation() && elect.getCreation() <= maxCreation);

    ConsensusHandler.handleConsensusMessage(
        laoRepository, CONSENSUS_CHANNEL, elect, "m1", publicKey);
    laoRepository.updateNodes(LAO_CHANNEL);

    nodeAssertions(grid, 0, "Approve Start by\n" + publicKey, true);

    // We try to accept node3
    grid.atPosition(2).perform(ViewActions.click());
    Mockito.verify(remoteDataSource, Mockito.times(2)).sendMessage(CAPTOR.capture());
    publish = (Publish) CAPTOR.getValue();
    msgGeneral = publish.getMessage();
    assertEquals(CONSENSUS_CHANNEL, publish.getChannel());
    assertEquals(publicKey, msgGeneral.getSender());
    ConsensusElectAccept electAccept = (ConsensusElectAccept) msgGeneral.getData();
    assertEquals(new ConsensusElectAccept(INSTANCE_ID, "m3", true), electAccept);

    // We accepted node 3 (it should disable button for node3)
    ConsensusHandler.handleConsensusMessage(
        laoRepository, CONSENSUS_CHANNEL, accept3, "a3", publicKey);
    laoRepository.updateNodes(LAO_CHANNEL);

    nodeAssertions(grid, 2, "Approve Start by\n" + NODE_3_KEY, false);

    // Receive a learn message => node3 was accepted and has started the election
    ConsensusHandler.handleConsensusMessage(laoRepository, CONSENSUS_CHANNEL, learn3, "l3", null);
    laoRepository.updateNodes(LAO_CHANNEL);

    electionStatus().check(matches(withText(expectedStatusStarted))).check(matches(isDisplayed()));
    electionStartButton()
        .check(matches(withText(expectedStartStarted)))
        .check(matches(isDisplayed()))
        .check(matches(isNotEnabled()));

    nodeAssertions(grid, 2, "Started by\n" + NODE_3_KEY, false);
  }

  private void nodeAssertions(
      DataInteraction grid, int position, String expectedText, boolean enabled) {
    grid.atPosition(position)
        .check(
            matches(
                allOf(
                    isDisplayed(),
                    withText(expectedText),
                    enabled ? isEnabled() : isNotEnabled())));
  }

  private String getPublicKey() throws IOException, GeneralSecurityException {
    KeysetHandle publicKeysetHandle =
        Injection.provideAndroidKeysetManager(ApplicationProvider.getApplicationContext())
            .getKeysetHandle()
            .getPublicKeysetHandle();
    return Keys.getEncodedKey(publicKeysetHandle);
  }
}
