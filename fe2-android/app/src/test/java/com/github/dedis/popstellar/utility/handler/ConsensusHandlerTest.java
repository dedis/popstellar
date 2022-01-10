package com.github.dedis.popstellar.utility.handler;

import static com.github.dedis.popstellar.Base64DataUtils.generateKeyPair;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
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
import com.github.dedis.popstellar.model.objects.Consensus;
import com.github.dedis.popstellar.model.objects.ConsensusNode;
import com.github.dedis.popstellar.model.objects.Lao;
import com.github.dedis.popstellar.model.objects.security.KeyPair;
import com.github.dedis.popstellar.model.objects.security.MessageID;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.repository.LAORepository;
import com.github.dedis.popstellar.repository.LAOState;
import com.github.dedis.popstellar.repository.local.LAOLocalDataSource;
import com.github.dedis.popstellar.repository.remote.LAORemoteDataSource;
import com.github.dedis.popstellar.utility.error.DataHandlingException;
import com.github.dedis.popstellar.utility.error.InvalidMessageIdException;
import com.github.dedis.popstellar.utility.scheduler.SchedulerProvider;
import com.github.dedis.popstellar.utility.scheduler.TestSchedulerProvider;
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
import java.util.stream.Collectors;

import io.reactivex.Observable;

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
  private static final String LAO_CHANNEL = "/root/" + LAO_ID;
  private static final String CONSENSUS_CHANNEL = LAO_CHANNEL + "/consensus";

  private static final String TYPE = "election";
  private static final String KEY_ID = "-t0xoQZa-ryiW18JnTjJHCsCNehFxuXOFOsfgKHHkj0=";
  private static final String PROPERTY = "state";
  private static final String VALUE = "started";
  private static final ConsensusKey KEY = new ConsensusKey(TYPE, KEY_ID, PROPERTY);
  private static final String INSTANCE_ID = Consensus.generateConsensusId(TYPE, KEY_ID, PROPERTY);
  private static final MessageID INVALID_MSG_ID = new MessageID("SU5BVkxJRF9NU0c=");

  private static final CreateLao CREATE_LAO =
      new CreateLao(LAO_ID, LAO_NAME, CREATION_TIME, ORGANIZER, Arrays.asList(NODE_2, NODE_3));
  private static final ConsensusElect elect =
      new ConsensusElect(CREATION_TIME, KEY_ID, TYPE, PROPERTY, VALUE);

  private static final Gson GSON = JsonModule.provideGson(DataRegistryModule.provideDataRegistry());
  private static final MessageHandler messageHandler =
      new MessageHandler(DataRegistryModule.provideDataRegistry());

  private LAORepository laoRepository;
  private MessageGeneral electMsg;
  private MessageID messageId;
  private Lao lao;

  @Mock LAORemoteDataSource remoteDataSource;
  @Mock LAOLocalDataSource localDataSource;
  @Mock KeyManager keyManager;

  @Before
  public void setup() throws GeneralSecurityException, DataHandlingException, IOException {
    SchedulerProvider testSchedulerProvider = new TestSchedulerProvider();

    when(remoteDataSource.observeMessage()).thenReturn(Observable.empty());
    when(remoteDataSource.observeWebsocket()).thenReturn(Observable.empty());

    lenient().when(keyManager.getMainKeyPair()).thenReturn(ORGANIZER_KEY);
    lenient().when(keyManager.getMainPublicKey()).thenReturn(ORGANIZER);

    laoRepository =
        new LAORepository(
            remoteDataSource,
            localDataSource,
            keyManager,
            messageHandler,
            GSON,
            testSchedulerProvider);

    lao = new Lao(LAO_CHANNEL);
    laoRepository.getLaoById().put(LAO_CHANNEL, new LAOState(lao));
    MessageGeneral createLaoMessage = getMsg(ORGANIZER_KEY, CREATE_LAO);
    messageHandler.handleMessage(laoRepository, LAO_CHANNEL, createLaoMessage);

    electMsg = getMsg(NODE_2_KEY, elect);
    messageId = electMsg.getMessageId();
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

    messageHandler.handleMessage(laoRepository, CONSENSUS_CHANNEL, electMsg);
    messageHandler.handleMessage(laoRepository, CONSENSUS_CHANNEL, failureMsg);

    Optional<Consensus> consensusOpt = lao.getConsensus(electMsg.getMessageId());
    assertTrue(consensusOpt.isPresent());
    Consensus consensus = consensusOpt.get();

    assertTrue(consensus.isFailed());
    Optional<ConsensusNode> node2Opt =
        lao.getNodes().stream().filter(n -> n.getPublicKey().equals(NODE_2)).findAny();
    assertTrue(node2Opt.isPresent());
    assertEquals(ConsensusNode.State.FAILED, node2Opt.get().getState(INSTANCE_ID));
  }

  // handle an elect from node2
  // This should add an attempt from node2 to start a consensus (in this case for starting an
  // election)
  private void handleConsensusElectTest() throws DataHandlingException {
    messageHandler.handleMessage(laoRepository, CONSENSUS_CHANNEL, electMsg);

    Optional<Consensus> consensusOpt = lao.getConsensus(electMsg.getMessageId());
    assertTrue(consensusOpt.isPresent());
    Consensus consensus = consensusOpt.get();

    assertEquals(electMsg.getMessageId(), consensus.getMessageId());
    assertEquals(NODE_2, consensus.getProposer());
    assertEquals(CONSENSUS_CHANNEL, consensus.getChannel());
    assertEquals(CREATION_TIME, consensus.getCreation());
    assertEquals(VALUE, consensus.getValue());
    assertEquals(KEY, consensus.getKey());

    assertTrue(consensus.getAcceptorsToMessageId().isEmpty());
    assertEquals(Sets.newSet(ORGANIZER, NODE_2, NODE_3), consensus.getNodes());

    Map<MessageID, Consensus> messageIdToConsensus = lao.getMessageIdToConsensus();
    assertEquals(1, messageIdToConsensus.size());
    assertEquals(consensus, messageIdToConsensus.get(consensus.getMessageId()));

    // Create a map from id to consensus
    Map<PublicKey, Optional<Consensus>> consensuses =
        lao.getNodes().stream()
            .collect(
                Collectors.toMap(
                    ConsensusNode::getPublicKey, n -> n.getLastConsensus(INSTANCE_ID)));
    assertEquals(3, consensuses.size());

    Optional<Consensus> organizerConsensus = consensuses.get(ORGANIZER);
    Optional<Consensus> node2Consensus = consensuses.get(NODE_2);
    Optional<Consensus> node3Consensus = consensuses.get(NODE_3);

    assertNotNull(organizerConsensus);
    assertNotNull(node2Consensus);
    assertNotNull(node3Consensus);

    assertEquals(Optional.empty(), organizerConsensus);
    assertEquals(consensus, node2Consensus.orElse(null));
    assertEquals(Optional.empty(), node3Consensus);
  }

  // handle an electAccept from node3 for the elect of node2
  // This test need be run after the elect message was handled, else the messageId would be invalid
  private void handleConsensusElectAcceptTest() throws DataHandlingException {
    ConsensusElectAccept electAccept = new ConsensusElectAccept(INSTANCE_ID, messageId, true);
    MessageGeneral electAcceptMsg = getMsg(NODE_3_KEY, electAccept);
    messageHandler.handleMessage(laoRepository, CONSENSUS_CHANNEL, electAcceptMsg);

    Optional<Consensus> consensusOpt = lao.getConsensus(electMsg.getMessageId());
    assertTrue(consensusOpt.isPresent());
    Consensus consensus = consensusOpt.get();

    Map<PublicKey, MessageID> acceptorsToMessageId = consensus.getAcceptorsToMessageId();
    assertEquals(1, acceptorsToMessageId.size());
    assertEquals(electAcceptMsg.getMessageId(), acceptorsToMessageId.get(NODE_3));

    // only the node3 has accepted the elect of node2
    Map<PublicKey, Set<MessageID>> consensuses =
        lao.getNodes().stream()
            .collect(
                Collectors.toMap(
                    ConsensusNode::getPublicKey, ConsensusNode::getAcceptedMessageIds));

    assertEquals(3, consensuses.size());

    Set<MessageID> organizerAcceptedMsg = consensuses.get(ORGANIZER);
    Set<MessageID> node2AcceptedMsg = consensuses.get(NODE_2);
    Set<MessageID> node3AcceptedMsg = consensuses.get(NODE_3);

    assertNotNull(organizerAcceptedMsg);
    assertNotNull(node2AcceptedMsg);
    assertNotNull(node3AcceptedMsg);

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
    messageHandler.handleMessage(laoRepository, CONSENSUS_CHANNEL, learnMsg);

    Optional<Consensus> consensusOpt = lao.getConsensus(electMsg.getMessageId());
    assertTrue(consensusOpt.isPresent());
    Consensus consensus = consensusOpt.get();
    assertTrue(consensus.isAccepted());
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
            messageHandler.handleMessage(laoRepository, CONSENSUS_CHANNEL, electAcceptInvalidMsg));
    assertThrows(
        InvalidMessageIdException.class,
        () -> messageHandler.handleMessage(laoRepository, CONSENSUS_CHANNEL, learnInvalidMsg));
    assertThrows(
        InvalidMessageIdException.class,
        () -> messageHandler.handleMessage(laoRepository, CONSENSUS_CHANNEL, failureMsg));
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
        mockLAORepository, CONSENSUS_CHANNEL, getMsg(ORGANIZER_KEY, prepare));
    messageHandler.handleMessage(
        mockLAORepository, CONSENSUS_CHANNEL, getMsg(ORGANIZER_KEY, promise));
    messageHandler.handleMessage(
        mockLAORepository, CONSENSUS_CHANNEL, getMsg(ORGANIZER_KEY, propose));
    messageHandler.handleMessage(
        mockLAORepository, CONSENSUS_CHANNEL, getMsg(ORGANIZER_KEY, accept));

    // The handlers for prepare/promise/propose/accept should do nothing (call or update nothing)
    // because theses messages should only be handle in the backend server.
    verify(mockLAORepository, never()).getLaoByChannel(anyString());
    verify(mockLAORepository, never()).updateNodes(anyString());
  }
}
