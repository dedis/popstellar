package com.github.dedis.popstellar.utility.handler;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.github.dedis.popstellar.di.*;
import com.github.dedis.popstellar.model.network.method.message.MessageGeneral;
import com.github.dedis.popstellar.model.network.method.message.data.Data;
import com.github.dedis.popstellar.model.network.method.message.data.DataRegistry;
import com.github.dedis.popstellar.model.network.method.message.data.consensus.*;
import com.github.dedis.popstellar.model.network.method.message.data.lao.CreateLao;
import com.github.dedis.popstellar.model.objects.*;
import com.github.dedis.popstellar.model.objects.security.*;
import com.github.dedis.popstellar.repository.LAORepository;
import com.github.dedis.popstellar.repository.MessageRepository;
import com.github.dedis.popstellar.repository.database.AppDatabase;
import com.github.dedis.popstellar.repository.remote.MessageSender;
import com.github.dedis.popstellar.utility.error.*;
import com.github.dedis.popstellar.utility.error.keys.NoRollCallException;
import com.github.dedis.popstellar.utility.security.KeyManager;
import com.google.gson.Gson;

import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.internal.util.collections.Sets;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.*;

import io.reactivex.Completable;

import static com.github.dedis.popstellar.model.objects.ElectInstance.State.ACCEPTED;
import static com.github.dedis.popstellar.model.objects.ElectInstance.State.FAILED;
import static com.github.dedis.popstellar.testutils.Base64DataUtils.generateKeyPair;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(AndroidJUnit4.class)
public class ConsensusHandlerTest {

  private static final KeyPair ORGANIZER_KEY = generateKeyPair();
  private static final KeyPair NODE_2_KEY = generateKeyPair();
  private static final KeyPair NODE_3_KEY = generateKeyPair();

  private static final PublicKey ORGANIZER = ORGANIZER_KEY.getPublicKey();
  private static final PublicKey NODE_2 = NODE_2_KEY.getPublicKey();
  private static final PublicKey NODE_3 = NODE_3_KEY.getPublicKey();

  private static final long CREATION_TIME = Instant.now().getEpochSecond();
  private static final String LAO_NAME = "laoName";
  private static final String LAO_ID = Lao.generateLaoId(ORGANIZER, CREATION_TIME, LAO_NAME);
  private static final Channel CONSENSUS_CHANNEL =
      Channel.getLaoChannel(LAO_ID).subChannel("consensus");

  private static final String TYPE = "election";
  private static final String KEY_ID = "-t0xoQZa-ryiW18JnTjJHCsCNehFxuXOFOsfgKHHkj0=";
  private static final String PROPERTY = "state";
  private static final String VALUE = "started";
  private static final ConsensusKey KEY = new ConsensusKey(TYPE, KEY_ID, PROPERTY);
  private static final String INSTANCE_ID =
      ElectInstance.generateConsensusId(TYPE, KEY_ID, PROPERTY);
  private static final MessageID INVALID_MSG_ID = new MessageID("SU5BVkxJRF9NU0c=");

  private static final CreateLao CREATE_LAO =
      new CreateLao(LAO_ID, LAO_NAME, CREATION_TIME, ORGANIZER, Arrays.asList(NODE_2, NODE_3));
  private static final ConsensusElect elect =
      new ConsensusElect(CREATION_TIME, KEY_ID, TYPE, PROPERTY, VALUE);

  private LAORepository laoRepo;
  private MessageHandler messageHandler;
  private Gson gson;
  private AppDatabase appDatabase;

  private MessageGeneral electMsg;
  private MessageID messageId;

  @Mock MessageSender messageSender;
  @Mock KeyManager keyManager;

  @Before
  public void setup()
      throws GeneralSecurityException, DataHandlingException, IOException, UnknownLaoException,
          UnknownRollCallException, UnknownElectionException, NoRollCallException {
    MockitoAnnotations.openMocks(this);
    Context context = ApplicationProvider.getApplicationContext();
    appDatabase = AppDatabaseModuleHelper.getAppDatabase(context);

    lenient().when(keyManager.getMainKeyPair()).thenReturn(ORGANIZER_KEY);
    lenient().when(keyManager.getMainPublicKey()).thenReturn(ORGANIZER);

    when(messageSender.subscribe(any())).then(args -> Completable.complete());

    laoRepo = new LAORepository(appDatabase, ApplicationProvider.getApplicationContext());
    DataRegistry dataRegistry = DataRegistryModuleHelper.buildRegistry(laoRepo, keyManager);
    MessageRepository messageRepo =
        new MessageRepository(appDatabase, ApplicationProvider.getApplicationContext());
    gson = JsonModule.provideGson(dataRegistry);
    messageHandler = new MessageHandler(messageRepo, dataRegistry);

    Channel channel = Channel.getLaoChannel(LAO_ID);
    MessageGeneral createLaoMessage = getMsg(ORGANIZER_KEY, CREATE_LAO);
    messageHandler.handleMessage(messageSender, channel, createLaoMessage);

    electMsg = getMsg(NODE_2_KEY, elect);
    messageId = electMsg.getMessageId();
  }

  @After
  public void tearDown() {
    appDatabase.clearAllTables();
    appDatabase.close();
  }

  /**
   * Create a MessageGeneral containing the given data, with the given public key sender
   *
   * @param key public key of sender
   * @param data the data to encapsulated
   * @return a MessageGeneral
   */
  private MessageGeneral getMsg(KeyPair key, Data data) {
    return new MessageGeneral(key, data, gson);
  }

  @Test
  public void handleConsensusTests()
      throws DataHandlingException, UnknownLaoException, UnknownRollCallException,
          UnknownElectionException, NoRollCallException {
    // each test need to be run one after another
    handleConsensusElectTest();
    handleConsensusElectAcceptTest();
    handleConsensusLearnTest();
  }

  @Test
  public void handleConsensusFailure()
      throws DataHandlingException, UnknownLaoException, UnknownRollCallException,
          UnknownElectionException, NoRollCallException {
    // handle an elect from node2 then handle a failure for this elect
    // the state of the node2 for this instanceId should be FAILED

    ConsensusFailure failure = new ConsensusFailure(INSTANCE_ID, messageId, CREATION_TIME);
    MessageGeneral failureMsg = getMsg(ORGANIZER_KEY, failure);

    messageHandler.handleMessage(messageSender, CONSENSUS_CHANNEL, electMsg);
    messageHandler.handleMessage(messageSender, CONSENSUS_CHANNEL, failureMsg);

    Lao updatedLao = laoRepo.getLaoViewByChannel(Channel.getLaoChannel(LAO_ID)).createLaoCopy();

    Optional<ElectInstance> electInstanceOpt = updatedLao.getElectInstance(electMsg.getMessageId());
    assertTrue(electInstanceOpt.isPresent());
    ElectInstance electInstance = electInstanceOpt.get();
    assertEquals(FAILED, electInstance.getState());
    ConsensusNode node2 = updatedLao.getNode(NODE_2);
    assertNotNull(node2);
    assertEquals(FAILED, node2.getState(INSTANCE_ID));
  }

  // handle an elect from node2
  // This should add an attempt from node2 to start a consensus (in this case for starting an
  // election)
  private void handleConsensusElectTest()
      throws DataHandlingException, UnknownLaoException, UnknownRollCallException,
          UnknownElectionException, NoRollCallException {
    messageHandler.handleMessage(messageSender, CONSENSUS_CHANNEL, electMsg);

    Lao updatedLao = laoRepo.getLaoViewByChannel(Channel.getLaoChannel(LAO_ID)).createLaoCopy();

    Optional<ElectInstance> electInstanceOpt = updatedLao.getElectInstance(electMsg.getMessageId());
    assertTrue(electInstanceOpt.isPresent());
    ElectInstance electInstance = electInstanceOpt.get();

    assertEquals(electMsg.getMessageId(), electInstance.getMessageId());
    assertEquals(NODE_2, electInstance.getProposer());
    assertEquals(CONSENSUS_CHANNEL, electInstance.getChannel());
    assertEquals(CREATION_TIME, electInstance.getCreation());
    assertEquals(VALUE, electInstance.getValue());
    assertEquals(KEY, electInstance.getKey());

    assertTrue(electInstance.getAcceptorsToMessageId().isEmpty());
    assertEquals(Sets.newSet(ORGANIZER, NODE_2, NODE_3), electInstance.getNodes());

    Map<MessageID, ElectInstance> messageIdToElectInstance =
        updatedLao.getMessageIdToElectInstance();
    assertEquals(1, messageIdToElectInstance.size());
    assertEquals(electInstance, messageIdToElectInstance.get(electInstance.getMessageId()));

    assertEquals(3, updatedLao.getNodes().size());
    ConsensusNode organizer = updatedLao.getNode(ORGANIZER);
    ConsensusNode node2 = updatedLao.getNode(NODE_2);
    ConsensusNode node3 = updatedLao.getNode(NODE_3);

    assertNotNull(organizer);
    assertNotNull(node2);
    assertNotNull(node3);

    Optional<ElectInstance> organizerElectInstance = organizer.getLastElectInstance(INSTANCE_ID);
    Optional<ElectInstance> node2ElectInstance = node2.getLastElectInstance(INSTANCE_ID);
    Optional<ElectInstance> node3ElectInstance = node3.getLastElectInstance(INSTANCE_ID);

    assertEquals(Optional.empty(), organizerElectInstance);
    assertTrue(node2ElectInstance.isPresent());
    assertEquals(electInstance, node2ElectInstance.get());
    assertEquals(Optional.empty(), node3ElectInstance);
  }

  // handle an electAccept from node3 for the elect of node2
  // This test need be run after the elect message was handled, else the messageId would be invalid
  private void handleConsensusElectAcceptTest()
      throws DataHandlingException, UnknownLaoException, UnknownRollCallException,
          UnknownElectionException, NoRollCallException {
    ConsensusElectAccept electAccept = new ConsensusElectAccept(INSTANCE_ID, messageId, true);
    MessageGeneral electAcceptMsg = getMsg(NODE_3_KEY, electAccept);
    messageHandler.handleMessage(messageSender, CONSENSUS_CHANNEL, electAcceptMsg);

    Lao updatedLao = laoRepo.getLaoViewByChannel(Channel.getLaoChannel(LAO_ID)).createLaoCopy();

    Optional<ElectInstance> electInstanceOpt = updatedLao.getElectInstance(electMsg.getMessageId());
    assertTrue(electInstanceOpt.isPresent());
    ElectInstance electInstance = electInstanceOpt.get();

    Map<PublicKey, MessageID> acceptorsToMessageId = electInstance.getAcceptorsToMessageId();
    assertEquals(1, acceptorsToMessageId.size());
    assertEquals(electAcceptMsg.getMessageId(), acceptorsToMessageId.get(NODE_3));

    assertEquals(3, updatedLao.getNodes().size());
    ConsensusNode organizer = updatedLao.getNode(ORGANIZER);
    ConsensusNode node2 = updatedLao.getNode(NODE_2);
    ConsensusNode node3 = updatedLao.getNode(NODE_3);

    assertNotNull(organizer);
    assertNotNull(node2);
    assertNotNull(node3);

    Set<MessageID> organizerAcceptedMsg = organizer.getAcceptedMessageIds();
    Set<MessageID> node2AcceptedMsg = node2.getAcceptedMessageIds();
    Set<MessageID> node3AcceptedMsg = node3.getAcceptedMessageIds();

    assertTrue(organizerAcceptedMsg.isEmpty());
    assertTrue(node2AcceptedMsg.isEmpty());
    assertEquals(Sets.newSet(electMsg.getMessageId()), node3AcceptedMsg);
  }

  // handle a learn from node3 for the elect of node2
  // This test need be run after the elect message was handled, else the messageId would be invalid
  private void handleConsensusLearnTest()
      throws DataHandlingException, UnknownLaoException, UnknownRollCallException,
          UnknownElectionException, NoRollCallException {
    ConsensusLearn learn =
        new ConsensusLearn(INSTANCE_ID, messageId, CREATION_TIME, true, Collections.emptyList());
    MessageGeneral learnMsg = getMsg(NODE_3_KEY, learn);
    messageHandler.handleMessage(messageSender, CONSENSUS_CHANNEL, learnMsg);

    Lao updatedLao = laoRepo.getLaoViewByChannel(Channel.getLaoChannel(LAO_ID)).createLaoCopy();

    Optional<ElectInstance> electInstanceOpt = updatedLao.getElectInstance(electMsg.getMessageId());
    assertTrue(electInstanceOpt.isPresent());
    ElectInstance electInstance = electInstanceOpt.get();
    assertEquals(ACCEPTED, electInstance.getState());
  }

  @Test
  public void handleConsensusWithInvalidMessageIdTest() {
    // When an invalid instance id is used in handler for elect_accept and learn,
    // it should throw an InvalidMessageIdException

    ConsensusElectAccept electAcceptInvalid =
        new ConsensusElectAccept(INSTANCE_ID, INVALID_MSG_ID, true);
    ConsensusLearn learnInvalid =
        new ConsensusLearn(
            INSTANCE_ID, INVALID_MSG_ID, CREATION_TIME, true, Collections.emptyList());
    ConsensusFailure failureInvalid =
        new ConsensusFailure(INSTANCE_ID, INVALID_MSG_ID, CREATION_TIME);
    MessageGeneral electAcceptInvalidMsg = getMsg(ORGANIZER_KEY, electAcceptInvalid);
    MessageGeneral learnInvalidMsg = getMsg(ORGANIZER_KEY, learnInvalid);
    MessageGeneral failureMsg = getMsg(ORGANIZER_KEY, failureInvalid);

    assertThrows(
        InvalidMessageIdException.class,
        () ->
            messageHandler.handleMessage(messageSender, CONSENSUS_CHANNEL, electAcceptInvalidMsg));
    assertThrows(
        InvalidMessageIdException.class,
        () -> messageHandler.handleMessage(messageSender, CONSENSUS_CHANNEL, learnInvalidMsg));
    assertThrows(
        InvalidMessageIdException.class,
        () -> messageHandler.handleMessage(messageSender, CONSENSUS_CHANNEL, failureMsg));
  }

  @Test
  public void handleConsensusDoNothingOnBackendMessageTest()
      throws DataHandlingException, UnknownLaoException, UnknownRollCallException,
          UnknownElectionException, NoRollCallException {
    LAORepository mockLAORepository = mock(LAORepository.class);

    ConsensusPrepare prepare = new ConsensusPrepare(INSTANCE_ID, messageId, CREATION_TIME, 3);
    ConsensusPromise promise =
        new ConsensusPromise(INSTANCE_ID, messageId, CREATION_TIME, 3, true, 2);
    ConsensusPropose propose =
        new ConsensusPropose(
            INSTANCE_ID, messageId, CREATION_TIME, 3, true, Collections.emptyList());
    ConsensusAccept accept = new ConsensusAccept(INSTANCE_ID, messageId, CREATION_TIME, 3, true);

    messageHandler.handleMessage(messageSender, CONSENSUS_CHANNEL, getMsg(ORGANIZER_KEY, prepare));
    messageHandler.handleMessage(messageSender, CONSENSUS_CHANNEL, getMsg(ORGANIZER_KEY, promise));
    messageHandler.handleMessage(messageSender, CONSENSUS_CHANNEL, getMsg(ORGANIZER_KEY, propose));
    messageHandler.handleMessage(messageSender, CONSENSUS_CHANNEL, getMsg(ORGANIZER_KEY, accept));

    // The handlers for prepare/promise/propose/accept should do nothing (call or update nothing)
    // because theses messages should only be handle in the backend server.
    verify(mockLAORepository, never()).getLaoByChannel(any(Channel.class));
    verify(mockLAORepository, never()).updateNodes(any(Channel.class));
  }
}
