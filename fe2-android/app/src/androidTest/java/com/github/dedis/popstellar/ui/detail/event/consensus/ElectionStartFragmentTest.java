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
import androidx.test.espresso.DataInteraction;
import androidx.test.espresso.action.ViewActions;

import com.github.dedis.popstellar.model.network.GenericMessage;
import com.github.dedis.popstellar.model.network.answer.Result;
import com.github.dedis.popstellar.model.network.method.Message;
import com.github.dedis.popstellar.model.network.method.Publish;
import com.github.dedis.popstellar.model.network.method.message.MessageGeneral;
import com.github.dedis.popstellar.model.network.method.message.data.Data;
import com.github.dedis.popstellar.model.network.method.message.data.consensus.ConsensusElect;
import com.github.dedis.popstellar.model.network.method.message.data.consensus.ConsensusElectAccept;
import com.github.dedis.popstellar.model.network.method.message.data.consensus.ConsensusFailure;
import com.github.dedis.popstellar.model.network.method.message.data.consensus.ConsensusKey;
import com.github.dedis.popstellar.model.network.method.message.data.consensus.ConsensusLearn;
import com.github.dedis.popstellar.model.network.serializer.JsonUtils;
import com.github.dedis.popstellar.model.objects.Consensus;
import com.github.dedis.popstellar.model.objects.Election;
import com.github.dedis.popstellar.model.objects.Lao;
import com.github.dedis.popstellar.repository.LAODataSource;
import com.github.dedis.popstellar.repository.LAORepository;
import com.github.dedis.popstellar.repository.LAOState;
import com.github.dedis.popstellar.testutils.fragment.FragmentScenarioRule;
import com.github.dedis.popstellar.ui.detail.LaoDetailActivity;
import com.github.dedis.popstellar.ui.detail.LaoDetailViewModel;
import com.github.dedis.popstellar.utility.error.DataHandlingException;
import com.github.dedis.popstellar.utility.handler.MessageHandler;
import com.github.dedis.popstellar.utility.scheduler.ProdSchedulerProvider;
import com.google.crypto.tink.KeysetHandle;
import com.google.crypto.tink.PublicKeySign;
import com.google.crypto.tink.integration.android.AndroidKeysetManager;
import com.google.crypto.tink.signature.Ed25519PrivateKeyManager;
import com.google.crypto.tink.signature.PublicKeySignWrapper;
import com.google.gson.Gson;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.internal.util.collections.Sets;
import org.mockito.junit.MockitoJUnit;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import dagger.hilt.android.testing.BindValue;
import dagger.hilt.android.testing.HiltAndroidRule;
import dagger.hilt.android.testing.HiltAndroidTest;
import io.reactivex.Observable;

@HiltAndroidTest
public class ElectionStartFragmentTest {

  @Inject AndroidKeysetManager keysetManager;
  @Inject MessageHandler messageHandler;
  @Inject Gson gson;

  @BindValue LAORepository laoRepository;

  @Mock LAODataSource.Remote remoteDataSource;
  @Mock LAODataSource.Local localDataSource;

  // A custom rule to call setup and teardown before the fragment rule and after the mockito rule
  private final TestRule setupRule =
      new ExternalResource() {
        @Override
        protected void before() {
          // Injection with hilt
          hiltRule.inject();
          // Preload the data schema before the test run
          JsonUtils.loadSchema(JsonUtils.DATA_SCHEMA);

          when(remoteDataSource.incrementAndGetRequestId()).thenReturn(42);
          when(remoteDataSource.observeWebsocket()).thenReturn(Observable.empty());
          Observable<GenericMessage> upstream = Observable.fromArray(new Result(42));

          // Mock the remote data source to receive a response
          when(remoteDataSource.observeMessage()).thenReturn(upstream);

          try {
            laoRepository =
                new LAORepository(
                    remoteDataSource,
                    localDataSource,
                    keysetManager,
                    messageHandler,
                    gson,
                    new ProdSchedulerProvider());

            Ed25519PrivateKeyManager.registerPair(true);
            PublicKeySignWrapper.register();
            KeysetHandle keysetHandle1 = keysetManager.getKeysetHandle();
            KeysetHandle keysetHandle2 =
                KeysetHandle.generateNew(Ed25519PrivateKeyManager.rawEd25519Template());
            KeysetHandle keysetHandle3 =
                KeysetHandle.generateNew(Ed25519PrivateKeyManager.rawEd25519Template());

            signer1 = keysetHandle1.getPrimitive(PublicKeySign.class);
            signer3 = keysetHandle2.getPrimitive(PublicKeySign.class);

            publicKey = Keys.getEncodedKey(keysetHandle1.getPublicKeysetHandle());
            node2Key = Keys.getEncodedKey(keysetHandle2.getPublicKeysetHandle());
            node3Key = Keys.getEncodedKey(keysetHandle3.getPublicKeysetHandle());

          } catch (Exception e) {
            throw new RuntimeException(e);
          }

          LAO.setChannel(LAO_CHANNEL);
          LAO.setOrganizer(publicKey);
          LAO.setWitnesses(Sets.newSet(node2Key, node3Key));

          laoRepository.getLaoById().put(LAO_CHANNEL, new LAOState(LAO));
          laoRepository.updateNodes(LAO_CHANNEL);
        }
      };

  private final FragmentScenarioRule<ElectionStartFragment> fragmentRule =
      FragmentScenarioRule.launch(ElectionStartFragment.class, ElectionStartFragment::newInstance);

  private final HiltAndroidRule hiltRule = new HiltAndroidRule(this);

  @Rule
  public final RuleChain chain =
      RuleChain.outerRule(MockitoJUnit.testRule(this))
          .around(hiltRule)
          .around(setupRule)
          .around(fragmentRule);

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
  private static final ConsensusKey KEY = new ConsensusKey("election", election.getId(), "state");
  private static final String INSTANCE_ID =
      Consensus.generateConsensusId(KEY.getType(), KEY.getId(), KEY.getProperty());

  private static final ConsensusElect elect =
      new ConsensusElect(PAST_TIME, KEY.getId(), KEY.getType(), KEY.getProperty(), "started");

  private static final ArgumentCaptor<Message> CAPTOR = ArgumentCaptor.forClass(Message.class);

  private static final String STATUS_WAITING = "Waiting scheduled time";
  private static final String STATUS_READY = "Ready to start";
  private static final String STATUS_STARTED = "Started";
  private static final String DATE_FUTURE =
      DATE_TIME_FORMATTER.format(Instant.ofEpochSecond(FUTURE_TIME));
  private static final String DATE_PAST =
      DATE_TIME_FORMATTER.format(Instant.ofEpochSecond(PAST_TIME));
  private static final String START_SCHEDULED = "Election scheduled to start at\n" + DATE_FUTURE;
  private static final String START_START = "Start Election";
  private static final String START_STARTED = "Election started successfully at\n" + DATE_PAST;

  private String publicKey;
  private String node2Key;
  private String node3Key;
  private PublicKeySign signer1;
  private PublicKeySign signer3;

  @Test
  public void displayWithUpdatesIsCorrect() throws DataHandlingException {
    setupViewModel(PAST_TIME);

    // Election start time has passed, should display that it's ready and start button enabled
    displayAssertions(STATUS_READY, START_START, true);

    // Order of nodes are not guaranteed in general, but in theses tests it's ownNode(0), node2(1),
    // node3(2)
    DataInteraction grid = nodesGrid();
    nodeAssertions(grid, 0, "Waiting\n" + publicKey, false);
    nodeAssertions(grid, 1, "Waiting\n" + node2Key, false);
    nodeAssertions(grid, 2, "Waiting\n" + node3Key, false);

    // Nodes 3 try to start
    MessageGeneral elect3Msg = createMsg(node3Key, signer3, elect);
    messageHandler.handleMessage(laoRepository, CONSENSUS_CHANNEL, elect3Msg);
    nodeAssertions(grid, 2, "Approve Start by\n" + node3Key, true);

    // We try to start (it should disable the start button)
    MessageGeneral elect1Msg = createMsg(publicKey, signer1, elect);
    messageHandler.handleMessage(laoRepository, CONSENSUS_CHANNEL, elect1Msg);
    displayAssertions(STATUS_READY, START_START, false);
    nodeAssertions(grid, 0, "Approve Start by\n" + publicKey, true);

    // We accepted node 3 (it should disable button for node3)
    ConsensusElectAccept electAccept3 =
        new ConsensusElectAccept(INSTANCE_ID, elect3Msg.getMessageId(), true);
    MessageGeneral accept3Msg = createMsg(publicKey, signer1, electAccept3);
    messageHandler.handleMessage(laoRepository, CONSENSUS_CHANNEL, accept3Msg);
    nodeAssertions(grid, 2, "Approve Start by\n" + node3Key, false);

    // Receive a learn message => node3 was accepted and has started the election
    ConsensusLearn learn3 =
        new ConsensusLearn(
            INSTANCE_ID, elect3Msg.getMessageId(), PAST_TIME, true, Collections.emptyList());
    MessageGeneral learn3Msg = createMsg(node3Key, signer3, learn3);
    messageHandler.handleMessage(laoRepository, CONSENSUS_CHANNEL, learn3Msg);
    displayAssertions(STATUS_STARTED, START_STARTED, false);
    nodeAssertions(grid, 2, "Started by\n" + node3Key, false);
  }

  @Test
  public void startDisabledOnStartupIfInFutureTest() {
    setupViewModel(FUTURE_TIME);
    displayAssertions(STATUS_WAITING, START_SCHEDULED, false);
  }

  @Test
  public void startEnabledOnStartupIfStartTimeInPastTest() {
    setupViewModel(PAST_TIME);
    displayAssertions(STATUS_READY, START_START, true);
  }

  @Test
  public void updateTest() throws InterruptedException {
    setupViewModel(FUTURE_TIME);
    // Election start time has not passed yet, should display that it's waiting
    displayAssertions(STATUS_WAITING, START_SCHEDULED, false);

    // Wait for the timer update
    election.setStart(PAST_TIME);
    TimeUnit.SECONDS.sleep(2);

    // Election start time has passed, should display that it's ready and start button enabled
    displayAssertions(STATUS_READY, START_START, true);
  }

  @Test
  public void startButtonSendElectMessageTest() {
    setupViewModel(PAST_TIME);

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
  }

  @Test
  public void acceptButtonSendElectAcceptMessageTest() throws DataHandlingException {
    setupViewModel(PAST_TIME);

    // Nodes 3 try to start
    MessageGeneral elect3Msg = createMsg(node3Key, signer3, elect);
    messageHandler.handleMessage(laoRepository, CONSENSUS_CHANNEL, elect3Msg);

    // We try to accept node3
    nodesGrid().atPosition(2).perform(ViewActions.click());
    Mockito.verify(remoteDataSource).sendMessage(CAPTOR.capture());
    Publish publish = (Publish) CAPTOR.getValue();
    MessageGeneral msgGeneral = publish.getMessage();

    assertEquals(CONSENSUS_CHANNEL, publish.getChannel());
    assertEquals(publicKey, msgGeneral.getSender());
    ConsensusElectAccept electAccept = (ConsensusElectAccept) msgGeneral.getData();
    ConsensusElectAccept expectedElectAccept =
        new ConsensusElectAccept(INSTANCE_ID, elect3Msg.getMessageId(), true);
    assertEquals(expectedElectAccept, electAccept);
  }

  @Test
  public void failureTest() throws DataHandlingException {
    setupViewModel(PAST_TIME);

    // Nodes 3 try to start and failed
    MessageGeneral elect3Msg = createMsg(node3Key, signer3, elect);
    messageHandler.handleMessage(laoRepository, CONSENSUS_CHANNEL, elect3Msg);
    ConsensusFailure failure3 =
        new ConsensusFailure(INSTANCE_ID, elect3Msg.getMessageId(), PAST_TIME);
    MessageGeneral failure3Msg = createMsg(node3Key, signer3, failure3);
    messageHandler.handleMessage(laoRepository, CONSENSUS_CHANNEL, failure3Msg);

    nodeAssertions(nodesGrid(), 2, "Start Failed\n" + node3Key, false);

    // We try to start and failed
    MessageGeneral elect1Msg = createMsg(publicKey, signer1, elect);
    messageHandler.handleMessage(laoRepository, CONSENSUS_CHANNEL, elect1Msg);
    ConsensusFailure failure1 =
        new ConsensusFailure(INSTANCE_ID, elect1Msg.getMessageId(), PAST_TIME);
    MessageGeneral failure1Msg = createMsg(publicKey, signer1, failure1);
    messageHandler.handleMessage(laoRepository, CONSENSUS_CHANNEL, failure1Msg);

    displayAssertions(STATUS_READY, START_START, true);
    nodeAssertions(nodesGrid(), 0, "Start Failed\n" + publicKey, false);
  }

  private void displayAssertions(String expectedStatus, String expectedStart, boolean enabled) {
    String expectedTitle = "Election " + '"' + ELECTION_NAME + '"';
    electionTitle().check(matches(withText(expectedTitle))).check(matches(isDisplayed()));
    electionStatus().check(matches(withText(expectedStatus))).check(matches(isDisplayed()));
    electionStartButton()
        .check(matches(withText(expectedStart)))
        .check(matches(isDisplayed()))
        .check(matches(enabled ? isEnabled() : isNotEnabled()));
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

  private void setupViewModel(long electionStart) {
    election.setStart(electionStart);
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
  }

  private MessageGeneral createMsg(String nodeKey, PublicKeySign nodeSigner, Data data) {
    return new MessageGeneral(Base64.getUrlDecoder().decode(nodeKey), data, nodeSigner, gson);
  }
}
