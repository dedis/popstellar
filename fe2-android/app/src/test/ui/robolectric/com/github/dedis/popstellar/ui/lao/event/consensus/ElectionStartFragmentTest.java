package com.github.dedis.popstellar.ui.lao.event.consensus;

import androidx.test.annotation.UiThreadTest;
import androidx.test.espresso.DataInteraction;
import androidx.test.espresso.action.ViewActions;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.github.dedis.popstellar.model.network.method.message.MessageGeneral;
import com.github.dedis.popstellar.model.network.method.message.data.Data;
import com.github.dedis.popstellar.model.network.method.message.data.consensus.*;
import com.github.dedis.popstellar.model.network.method.message.data.election.ElectionVersion;
import com.github.dedis.popstellar.model.network.serializer.JsonUtils;
import com.github.dedis.popstellar.model.objects.*;
import com.github.dedis.popstellar.model.objects.event.EventState;
import com.github.dedis.popstellar.model.objects.security.KeyPair;
import com.github.dedis.popstellar.repository.ElectionRepository;
import com.github.dedis.popstellar.repository.LAORepository;
import com.github.dedis.popstellar.repository.remote.GlobalNetworkManager;
import com.github.dedis.popstellar.repository.remote.MessageSender;
import com.github.dedis.popstellar.testutils.BundleBuilder;
import com.github.dedis.popstellar.testutils.fragment.ActivityFragmentScenarioRule;
import com.github.dedis.popstellar.ui.lao.LaoActivity;
import com.github.dedis.popstellar.utility.error.*;
import com.github.dedis.popstellar.utility.error.keys.NoRollCallException;
import com.github.dedis.popstellar.utility.handler.MessageHandler;
import com.github.dedis.popstellar.utility.security.KeyManager;
import com.google.crypto.tink.KeyTemplates;
import com.google.crypto.tink.KeysetHandle;
import com.google.crypto.tink.signature.Ed25519PrivateKeyManager;
import com.google.crypto.tink.signature.PublicKeySignWrapper;
import com.google.gson.Gson;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.internal.util.collections.Sets;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoTestRule;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.testing.*;
import io.reactivex.Completable;

import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static com.github.dedis.popstellar.testutils.pages.lao.LaoActivityPageObject.containerId;
import static com.github.dedis.popstellar.testutils.pages.lao.LaoActivityPageObject.laoIdExtra;
import static com.github.dedis.popstellar.testutils.pages.lao.event.consensus.ElectionStartPageObject.*;
import static org.hamcrest.core.AllOf.allOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@HiltAndroidTest
@RunWith(AndroidJUnit4.class)
public class ElectionStartFragmentTest {

  @Inject KeyManager keyManager;
  @Inject MessageHandler messageHandler;
  @Inject Gson gson;
  @Inject ElectionRepository electionRepo;
  @Inject LAORepository laoRepo;

  @BindValue @Mock GlobalNetworkManager globalNetworkManager;
  @Mock MessageSender messageSender;

  private static final DateTimeFormatter DATE_TIME_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss z").withZone(ZoneId.systemDefault());
  private static final String LAO_ID = "laoId";
  private static final String ELECTION_NAME = "My Election !";
  private static final long PAST_TIME = 946684800;
  private static final long FUTURE_TIME = 2145916800;

  private static final Election ELECTION =
      new Election.ElectionBuilder(LAO_ID, PAST_TIME, ELECTION_NAME)
          .setElectionVersion(ElectionVersion.OPEN_BALLOT)
          .setState(EventState.CREATED)
          .build();
  private static final ConsensusKey KEY = new ConsensusKey("election", ELECTION.getId(), "state");
  private static final String INSTANCE_ID =
      ElectInstance.generateConsensusId(KEY.getType(), KEY.getId(), KEY.getProperty());

  @Rule(order = 0)
  public final MockitoTestRule mockitoRule = MockitoJUnit.testRule(this);

  @Rule(order = 1)
  public final HiltAndroidRule hiltRule = new HiltAndroidRule(this);

  @Rule(order = 2)
  public final TestRule setupRule =
      new ExternalResource() {
        @Override
        protected void before() {
          // Injection with hilt
          hiltRule.inject();
          // Preload the data schema before the test run
          JsonUtils.loadSchema(JsonUtils.DATA_SCHEMA);

          KeyPair node2KeyPair;
          try {
            Ed25519PrivateKeyManager.registerPair(true);
            PublicKeySignWrapper.register();
            KeysetHandle keysetHandle2 = KeysetHandle.generateNew(KeyTemplates.get("ED25519_RAW"));
            KeysetHandle keysetHandle3 = KeysetHandle.generateNew(KeyTemplates.get("ED25519_RAW"));

            mainKeyPair = keyManager.getMainKeyPair();
            node2KeyPair = keyManager.getKeyPair(keysetHandle2);
            node3KeyPair = keyManager.getKeyPair(keysetHandle3);

            publicKey = mainKeyPair.getPublicKey().getEncoded();
            node2 = node2KeyPair.getPublicKey().getEncoded();
            node3 = node3KeyPair.getPublicKey().getEncoded();

          } catch (Exception e) {
            throw new RuntimeException(e);
          }

          Lao lao = new Lao(LAO_ID);
          lao.setOrganizer(mainKeyPair.getPublicKey());
          lao.initKeyToNode(Sets.newSet(node2KeyPair.getPublicKey(), node3KeyPair.getPublicKey()));

          laoRepo.updateLao(lao);
          consensusChannel = lao.getChannel().subChannel("consensus");
          laoRepo.updateNodes(lao.getChannel());

          List<ConsensusNode> nodes = lao.getNodes();
          for (int i = 0; i < nodes.size(); ++i) {
            String key = nodes.get(i).getPublicKey().getEncoded();
            if (key.equals(publicKey)) {
              ownPos = i;
            } else if (key.equals(node2)) {
              node2Pos = i;
            } else {
              node3Pos = i;
            }
          }

          electionRepo.updateElection(ELECTION);

          when(globalNetworkManager.getMessageSender()).thenReturn(messageSender);

          when(messageSender.publish(any(), any(), any())).then(args -> Completable.complete());
          when(messageSender.publish(any(), any())).then(args -> Completable.complete());
          when(messageSender.subscribe(any())).then(args -> Completable.complete());
        }
      };

  @Rule(order = 3)
  public final ActivityFragmentScenarioRule<LaoActivity, ElectionStartFragment> fragmentRule =
      ActivityFragmentScenarioRule.launchIn(
          LaoActivity.class,
          new BundleBuilder().putString(laoIdExtra(), LAO_ID).build(),
          containerId(),
          ElectionStartFragment.class,
          () -> ElectionStartFragment.newInstance(ELECTION.getId()));

  private static final ConsensusElect elect =
      new ConsensusElect(PAST_TIME, KEY.getId(), KEY.getType(), KEY.getProperty(), "started");

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

  private Channel consensusChannel;

  private KeyPair mainKeyPair;
  private KeyPair node3KeyPair;

  private String publicKey;
  private String node2;
  private String node3;

  private int ownPos;
  private int node2Pos;
  private int node3Pos;

  @Test
  public void displayWithUpdatesIsCorrect()
      throws DataHandlingException, UnknownLaoException, UnknownRollCallException,
          UnknownElectionException, NoRollCallException, UnknownMeetingException {
    setElectionStart(PAST_TIME);

    // Election start time has passed, should display that it's ready and start button enabled
    displayAssertions(STATUS_READY, START_START, true);

    DataInteraction grid = nodesGrid();
    nodeAssertions(grid, ownPos, "Waiting\n" + publicKey, false);
    nodeAssertions(grid, node2Pos, "Waiting\n" + node2, false);
    nodeAssertions(grid, node3Pos, "Waiting\n" + node3, false);

    // Nodes 3 try to start
    MessageGeneral elect3Msg = createMsg(node3KeyPair, elect);
    messageHandler.handleMessage(messageSender, consensusChannel, elect3Msg);
    nodeAssertions(grid, node3Pos, "Approve Start by\n" + node3, true);

    // We try to start (it should disable the start button)
    MessageGeneral elect1Msg = createMsg(mainKeyPair, elect);
    messageHandler.handleMessage(messageSender, consensusChannel, elect1Msg);
    displayAssertions(STATUS_READY, START_START, false);
    nodeAssertions(grid, ownPos, "Approve Start by\n" + publicKey, true);

    // We accepted node 3 (it should disable button for node3)
    ConsensusElectAccept electAccept3 =
        new ConsensusElectAccept(INSTANCE_ID, elect3Msg.getMessageId(), true);
    MessageGeneral accept3Msg = createMsg(mainKeyPair, electAccept3);
    messageHandler.handleMessage(messageSender, consensusChannel, accept3Msg);
    nodeAssertions(grid, node3Pos, "Approve Start by\n" + node3, false);

    // Receive a learn message => node3 was accepted and has started the election
    ConsensusLearn learn3 =
        new ConsensusLearn(
            INSTANCE_ID, elect3Msg.getMessageId(), PAST_TIME, true, Collections.emptyList());
    MessageGeneral learn3Msg = createMsg(node3KeyPair, learn3);
    messageHandler.handleMessage(messageSender, consensusChannel, learn3Msg);
    displayAssertions(STATUS_STARTED, START_STARTED, false);
    nodeAssertions(grid, node3Pos, "Started by\n" + node3, false);
  }

  @Test
  public void startDisabledOnStartupIfInFutureTest() {
    setElectionStart(FUTURE_TIME);
    displayAssertions(STATUS_WAITING, START_SCHEDULED, false);
  }

  @Test
  public void startEnabledOnStartupIfStartTimeInPastTest() {
    setElectionStart(PAST_TIME);
    displayAssertions(STATUS_READY, START_START, true);
  }

  @Test
  @UiThreadTest
  public void updateTest() {
    setElectionStart(FUTURE_TIME);
    // Election start time has not passed yet, should display that it's waiting
    displayAssertions(STATUS_WAITING, START_SCHEDULED, false);

    // Update election start time
    electionRepo.updateElection(ELECTION.builder().setStart(PAST_TIME).build());

    // Election start time has passed, should display that it's ready and start button enabled
    displayAssertions(STATUS_READY, START_START, true);
  }

  @Test
  public void startButtonSendElectMessageTest() {
    setElectionStart(PAST_TIME);

    long minCreation = Instant.now().getEpochSecond();
    electionStartButton().perform(ViewActions.click());

    ArgumentCaptor<MessageGeneral> captor = ArgumentCaptor.forClass(MessageGeneral.class);
    Mockito.verify(messageSender).publish(eq(consensusChannel), captor.capture());
    MessageGeneral msgGeneral = captor.getValue();
    long maxCreation = Instant.now().getEpochSecond();
    assertEquals(mainKeyPair.getPublicKey(), msgGeneral.getSender());

    ConsensusElect elect = (ConsensusElect) msgGeneral.getData();
    assertEquals(KEY, elect.getKey());
    assertEquals(INSTANCE_ID, elect.getInstanceId());
    assertEquals("started", elect.getValue());
    assertTrue(minCreation <= elect.getCreation() && elect.getCreation() <= maxCreation);
  }

  @Test
  public void acceptButtonSendElectAcceptMessageTest()
      throws DataHandlingException, UnknownLaoException, UnknownRollCallException,
          UnknownElectionException, NoRollCallException, UnknownMeetingException {
    setElectionStart(PAST_TIME);

    // Nodes 3 try to start
    MessageGeneral elect3Msg = createMsg(node3KeyPair, elect);
    messageHandler.handleMessage(messageSender, consensusChannel, elect3Msg);

    // We try to accept node3
    nodesGrid().atPosition(node3Pos).perform(ViewActions.click());

    ArgumentCaptor<ConsensusElectAccept> captor =
        ArgumentCaptor.forClass(ConsensusElectAccept.class);
    Mockito.verify(messageSender).publish(eq(mainKeyPair), eq(consensusChannel), captor.capture());

    ConsensusElectAccept electAccept = captor.getValue();
    ConsensusElectAccept expectedElectAccept =
        new ConsensusElectAccept(INSTANCE_ID, elect3Msg.getMessageId(), true);
    assertEquals(expectedElectAccept, electAccept);
  }

  @Test
  public void failureTest()
      throws DataHandlingException, UnknownLaoException, UnknownRollCallException,
          UnknownElectionException, NoRollCallException, UnknownMeetingException {
    setElectionStart(PAST_TIME);

    // Nodes 3 try to start and failed
    MessageGeneral elect3Msg = createMsg(node3KeyPair, elect);
    messageHandler.handleMessage(messageSender, consensusChannel, elect3Msg);
    ConsensusFailure failure3 =
        new ConsensusFailure(INSTANCE_ID, elect3Msg.getMessageId(), PAST_TIME);
    MessageGeneral failure3Msg = createMsg(node3KeyPair, failure3);
    messageHandler.handleMessage(messageSender, consensusChannel, failure3Msg);

    nodeAssertions(nodesGrid(), node3Pos, "Start Failed\n" + node3, false);

    // We try to start and failed
    MessageGeneral elect1Msg = createMsg(mainKeyPair, elect);
    messageHandler.handleMessage(messageSender, consensusChannel, elect1Msg);
    ConsensusFailure failure1 =
        new ConsensusFailure(INSTANCE_ID, elect1Msg.getMessageId(), PAST_TIME);
    MessageGeneral failure1Msg = createMsg(mainKeyPair, failure1);
    messageHandler.handleMessage(messageSender, consensusChannel, failure1Msg);
    InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    displayAssertions(STATUS_READY, START_START, true);
    nodeAssertions(nodesGrid(), node3Pos, "Start Failed\n" + node3, false);
    nodeAssertions(nodesGrid(), node2Pos, "Waiting\n" + node2, false);
    nodeAssertions(nodesGrid(), ownPos, "Start Failed\n" + publicKey, false);
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

  private void setElectionStart(long electionStart) {
    electionRepo.updateElection(ELECTION.builder().setStart(electionStart).build());
  }

  private MessageGeneral createMsg(KeyPair nodeKey, Data data) {
    return new MessageGeneral(nodeKey, data, gson);
  }
}
