package com.github.dedis.popstellar.utility.handler;

import static com.github.dedis.popstellar.model.objects.ElectInstance.State.ACCEPTED;
import static com.github.dedis.popstellar.model.objects.ElectInstance.State.FAILED;
import static com.github.dedis.popstellar.testutils.Base64DataUtils.generateKeyPair;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.dedis.popstellar.di.DataRegistryModule;
import com.github.dedis.popstellar.di.JsonModule;
import com.github.dedis.popstellar.model.network.method.message.MessageGeneral;
import com.github.dedis.popstellar.model.network.method.message.data.Data;
import com.github.dedis.popstellar.model.network.method.message.data.consensus.ConsensusAccept;
import com.github.dedis.popstellar.model.network.method.message.data.consensus.ConsensusElect;
import com.github.dedis.popstellar.model.network.method.message.data.consensus.ConsensusElectAccept;
import com.github.dedis.popstellar.model.network.method.message.data.consensus.ConsensusFailure;
import com.github.dedis.popstellar.model.network.method.message.data.consensus.ConsensusKey;
import com.github.dedis.popstellar.model.network.method.message.data.consensus.ConsensusLearn;
import com.github.dedis.popstellar.model.network.method.message.data.consensus.ConsensusPrepare;
import com.github.dedis.popstellar.model.network.method.message.data.consensus.ConsensusPromise;
import com.github.dedis.popstellar.model.network.method.message.data.consensus.ConsensusPropose;
import com.github.dedis.popstellar.model.network.method.message.data.lao.CreateLao;
import com.github.dedis.popstellar.model.objects.Channel;
import com.github.dedis.popstellar.model.objects.ConsensusNode;
import com.github.dedis.popstellar.model.objects.ElectInstance;
import com.github.dedis.popstellar.model.objects.Lao;
import com.github.dedis.popstellar.model.objects.security.KeyPair;
import com.github.dedis.popstellar.model.objects.security.MessageID;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.repository.LAORepository;
import com.github.dedis.popstellar.repository.remote.MessageSender;
import com.github.dedis.popstellar.utility.error.DataHandlingException;
import com.github.dedis.popstellar.utility.error.InvalidMessageIdException;
import com.github.dedis.popstellar.utility.security.KeyManager;
import com.google.gson.Gson;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.internal.util.collections.Sets;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import io.reactivex.Completable;

@RunWith(MockitoJUnitRunner.class)
public class ConsensusHandlerTest {

  private static final KeyPair ORGANIZER_KEY = generateKeyPair();
  private static final KeyPair NODE_2_KEY = generateKeyPair();
  private static final KeyPair NODE_3_KEY = generateKeyPair();

  private static final PublicKey ORGANIZER = ORGANIZER_KEY.getPublicKey();
  private static final PublicKey NODE_2 = NODE_2_KEY.getPublicKey();
  private static final PublicKey NODE_3 = NODE_3_KEY.getPublicKey();

  private static final long CREATION_TIME = 946684800;
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

  private static final Gson GSON = JsonModule.provideGson(DataRegistryModule.provideDataRegistry());

  private LAORepository laoRepository;
  private MessageHandler messageHandler;

  private MessageGeneral electMsg;
  private MessageID messageId;
  private Lao lao;

  @Mock MessageSender messageSender;
  @Mock KeyManager keyManager;

  @Before
  public void setup() throws GeneralSecurityException, DataHandlingException, IOException {
    lenient().when(keyManager.getMainKeyPair()).thenReturn(ORGANIZER_KEY);
    lenient().when(keyManager.getMainPublicKey()).thenReturn(ORGANIZER);

    when(messageSender.subscribe(any())).then(args -> Completable.complete());

    laoRepository = new LAORepository();
    messageHandler = new MessageHandler(DataRegistryModule.provideDataRegistry(), keyManager);

    Channel channel = Channel.getLaoChannel(LAO_ID);
    MessageGeneral createLaoMessage = getMsg(ORGANIZER_KEY, CREATE_LAO);
    messageHandler.handleMessage(laoRepository, messageSender, channel, createLaoMessage);

    electMsg = getMsg(NODE_2_KEY, elect);
    messageId = electMsg.getMessageId();
    lao = laoRepository.getLaoById().get(LAO_ID).getLao();
  }

  /**
   * Create a MessageGeneral containing the given data, with the given public key sender
   *
   * @param key public key of sender
   * @param data the data to encapsulated
   * @return a MessageGeneral
   */
  private MessageGeneral getMsg(KeyPair key, Data data) {
    return new MessageGeneral(key, data, GSON);
  }

  @Test
  public void handleConsensusTests() throws DataHandlingException {
    // each test need to be run one after another
    handleConsensusElectTest();
    handleConsensusElectAcceptTest();
    handleConsensusLearnTest();
  }

  @Test
  public void handleConsensusFailure() throws DataHandlingException {
    // handle an elect from node2 then handle a failure for this elect
    // the state of the node2 for this instanceId should be FAILED

    ConsensusFailure failure = new ConsensusFailure(INSTANCE_ID, messageId, CREATION_TIME);
    MessageGeneral failureMsg = getMsg(ORGANIZER_KEY, failure);

    messageHandler.handleMessage(laoRepository, messageSender, CONSENSUS_CHANNEL, electMsg);
    messageHandler.handleMessage(laoRepository, messageSender, CONSENSUS_CHANNEL, failureMsg);

    Optional<ElectInstance> electInstanceOpt = lao.getElectInstance(electMsg.getMessageId());
    assertTrue(electInstanceOpt.isPresent());
    ElectInstance electInstance = electInstanceOpt.get();

    assertEquals(FAILED, electInstance.getState());
    ConsensusNode node2 = lao.getNode(NODE_2);
    assertNotNull(node2);
    assertEquals(FAILED, node2.getState(INSTANCE_ID));
  }

  // handle an elect from node2
  // This should add an attempt from node2 to start a consensus (in this case for starting an
  // election)
  private void handleConsensusElectTest() throws DataHandlingException {
    messageHandler.handleMessage(laoRepository, messageSender, CONSENSUS_CHANNEL, electMsg);

    Optional<ElectInstance> electInstanceOpt = lao.getElectInstance(electMsg.getMessageId());
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

    Map<MessageID, ElectInstance> messageIdToElectInstance = lao.getMessageIdToElectInstance();
    assertEquals(1, messageIdToElectInstance.size());
    assertEquals(electInstance, messageIdToElectInstance.get(electInstance.getMessageId()));

    assertEquals(3, lao.getNodes().size());
    ConsensusNode organizer = lao.getNode(ORGANIZER);
    ConsensusNode node2 = lao.getNode(NODE_2);
    ConsensusNode node3 = lao.getNode(NODE_3);

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
  private void handleConsensusElectAcceptTest() throws DataHandlingException {
    ConsensusElectAccept electAccept = new ConsensusElectAccept(INSTANCE_ID, messageId, true);
    MessageGeneral electAcceptMsg = getMsg(NODE_3_KEY, electAccept);
    messageHandler.handleMessage(laoRepository, messageSender, CONSENSUS_CHANNEL, electAcceptMsg);

    Optional<ElectInstance> electInstanceOpt = lao.getElectInstance(electMsg.getMessageId());
    assertTrue(electInstanceOpt.isPresent());
    ElectInstance electInstance = electInstanceOpt.get();

    Map<PublicKey, MessageID> acceptorsToMessageId = electInstance.getAcceptorsToMessageId();
    assertEquals(1, acceptorsToMessageId.size());
    assertEquals(electAcceptMsg.getMessageId(), acceptorsToMessageId.get(NODE_3));

    assertEquals(3, lao.getNodes().size());
    ConsensusNode organizer = lao.getNode(ORGANIZER);
    ConsensusNode node2 = lao.getNode(NODE_2);
    ConsensusNode node3 = lao.getNode(NODE_3);

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
  private void handleConsensusLearnTest() throws DataHandlingException {
    ConsensusLearn learn =
        new ConsensusLearn(INSTANCE_ID, messageId, CREATION_TIME, true, Collections.emptyList());
    MessageGeneral learnMsg = getMsg(NODE_3_KEY, learn);
    messageHandler.handleMessage(laoRepository, messageSender, CONSENSUS_CHANNEL, learnMsg);

    Optional<ElectInstance> electInstanceOpt = lao.getElectInstance(electMsg.getMessageId());
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
            messageHandler.handleMessage(
                laoRepository, messageSender, CONSENSUS_CHANNEL, electAcceptInvalidMsg));
    assertThrows(
        InvalidMessageIdException.class,
        () ->
            messageHandler.handleMessage(
                laoRepository, messageSender, CONSENSUS_CHANNEL, learnInvalidMsg));
    assertThrows(
        InvalidMessageIdException.class,
        () ->
            messageHandler.handleMessage(
                laoRepository, messageSender, CONSENSUS_CHANNEL, failureMsg));
  }

  @Test
  public void handleConsensusDoNothingOnBackendMessageTest() throws DataHandlingException {
    LAORepository mockLAORepository = mock(LAORepository.class);
    Map<MessageID, MessageGeneral> messageById = new HashMap<>();
    when(mockLAORepository.getMessageById()).thenReturn(messageById);

    ConsensusPrepare prepare = new ConsensusPrepare(INSTANCE_ID, messageId, CREATION_TIME, 3);
    ConsensusPromise promise =
        new ConsensusPromise(INSTANCE_ID, messageId, CREATION_TIME, 3, true, 2);
    ConsensusPropose propose =
        new ConsensusPropose(
            INSTANCE_ID, messageId, CREATION_TIME, 3, true, Collections.emptyList());
    ConsensusAccept accept = new ConsensusAccept(INSTANCE_ID, messageId, CREATION_TIME, 3, true);

    messageHandler.handleMessage(
        mockLAORepository, messageSender, CONSENSUS_CHANNEL, getMsg(ORGANIZER_KEY, prepare));
    messageHandler.handleMessage(
        mockLAORepository, messageSender, CONSENSUS_CHANNEL, getMsg(ORGANIZER_KEY, promise));
    messageHandler.handleMessage(
        mockLAORepository, messageSender, CONSENSUS_CHANNEL, getMsg(ORGANIZER_KEY, propose));
    messageHandler.handleMessage(
        mockLAORepository, messageSender, CONSENSUS_CHANNEL, getMsg(ORGANIZER_KEY, accept));

    // The handlers for prepare/promise/propose/accept should do nothing (call or update nothing)
    // because theses messages should only be handle in the backend server.
    verify(mockLAORepository, never()).getLaoByChannel(any(Channel.class));
    verify(mockLAORepository, never()).updateNodes(any(Channel.class));
  }
}
