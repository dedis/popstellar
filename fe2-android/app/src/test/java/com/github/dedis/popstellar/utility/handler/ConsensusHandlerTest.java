package com.github.dedis.popstellar.utility.handler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

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
import com.github.dedis.popstellar.repository.LAORepository;
import com.github.dedis.popstellar.repository.LAOState;
import com.github.dedis.popstellar.repository.local.LAOLocalDataSource;
import com.github.dedis.popstellar.repository.remote.LAORemoteDataSource;
import com.github.dedis.popstellar.utility.error.DataHandlingException;
import com.github.dedis.popstellar.utility.error.InvalidMessageIdException;
import com.github.dedis.popstellar.utility.scheduler.SchedulerProvider;
import com.github.dedis.popstellar.utility.scheduler.TestSchedulerProvider;
import com.google.crypto.tink.KeysetHandle;
import com.google.crypto.tink.PublicKeySign;
import com.google.crypto.tink.integration.android.AndroidKeysetManager;
import com.google.crypto.tink.signature.Ed25519PrivateKeyManager;
import com.google.crypto.tink.signature.PublicKeySignWrapper;
import com.google.gson.Gson;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.internal.util.collections.Sets;
import org.mockito.junit.MockitoJUnitRunner;

import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.reactivex.Observable;

@RunWith(MockitoJUnitRunner.class)
public class ConsensusHandlerTest {

  private static final String ORGANIZER = "J9fBzJV70Jk5c-i3277Uq4CmeL4t53WDfUghaK0HpeM=";
  private static final String NODE_2_KEY = "Vf9kiBGdOPIutk9z055oQxpG9askyx5C_wnSznpOrHk=";
  private static final String NODE_3_KEY = "SYeTNtpM0XCbdrPTYSSMFJHtuHBiIEQyrq_auP4Hggw=";
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
  private static final String INVALID_MSG_ID = "Invalid messageId";

  private static final CreateLao CREATE_LAO =
      new CreateLao(
          LAO_ID, LAO_NAME, CREATION_TIME, ORGANIZER, Arrays.asList(NODE_2_KEY, NODE_3_KEY));
  private static final ConsensusElect elect =
      new ConsensusElect(CREATION_TIME, KEY_ID, TYPE, PROPERTY, VALUE);

  private static final Gson GSON = JsonModule.provideGson(DataRegistryModule.provideDataRegistry());
  private static final MessageHandler messageHandler =
      new MessageHandler(DataRegistryModule.provideDataRegistry());

  private LAORepository laoRepository;
  private MessageGeneral electMsg;
  private PublicKeySign signer;
  private String messageId;
  private Lao lao;

  @Mock LAORemoteDataSource remoteDataSource;
  @Mock LAOLocalDataSource localDataSource;
  @Mock AndroidKeysetManager androidKeysetManager;

  @Before
  public void setup() throws GeneralSecurityException, DataHandlingException {
    SchedulerProvider testSchedulerProvider = new TestSchedulerProvider();

    Mockito.when(remoteDataSource.observeMessage()).thenReturn(Observable.empty());
    Mockito.when(remoteDataSource.observeWebsocket()).thenReturn(Observable.empty());

    Ed25519PrivateKeyManager.registerPair(true);
    PublicKeySignWrapper.register();
    KeysetHandle keysetHandle =
        KeysetHandle.generateNew(Ed25519PrivateKeyManager.rawEd25519Template());
    signer = keysetHandle.getPrimitive(PublicKeySign.class);

    laoRepository =
        new LAORepository(
            remoteDataSource,
            localDataSource,
            androidKeysetManager,
            messageHandler,
            GSON,
            testSchedulerProvider);

    lao = new Lao(LAO_CHANNEL);
    laoRepository.getLaoById().put(LAO_CHANNEL, new LAOState(lao));
    MessageGeneral createLaoMessage = getMsg(ORGANIZER, CREATE_LAO);
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
  private MessageGeneral getMsg(String key, Data data) {
    return new MessageGeneral(Base64.getUrlDecoder().decode(key), data, signer, GSON);
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
    MessageGeneral failureMsg = getMsg(ORGANIZER, failure);

    messageHandler.handleMessage(laoRepository, CONSENSUS_CHANNEL, electMsg);
    messageHandler.handleMessage(laoRepository, CONSENSUS_CHANNEL, failureMsg);

    Optional<Consensus> consensusOpt = lao.getConsensus(electMsg.getMessageId());
    assertTrue(consensusOpt.isPresent());
    Consensus consensus = consensusOpt.get();

    assertTrue(consensus.isFailed());
    Optional<ConsensusNode> node2Opt =
        lao.getNodes().stream().filter(n -> n.getPublicKey().equals(NODE_2_KEY)).findAny();
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
    assertEquals(NODE_2_KEY, consensus.getProposer());
    assertEquals(CONSENSUS_CHANNEL, consensus.getChannel());
    assertEquals(CREATION_TIME, consensus.getCreation());
    assertEquals(VALUE, consensus.getValue());
    assertEquals(KEY, consensus.getKey());

    assertTrue(consensus.getAcceptorsToMessageId().isEmpty());
    assertEquals(Sets.newSet(ORGANIZER, NODE_2_KEY, NODE_3_KEY), consensus.getNodes());

    Map<String, Consensus> messageIdToConsensus = lao.getMessageIdToConsensus();
    assertEquals(1, messageIdToConsensus.size());
    assertEquals(consensus, messageIdToConsensus.get(consensus.getMessageId()));

    List<ConsensusNode> nodes = lao.getNodes();
    assertEquals(3, nodes.size());
    assertEquals(Optional.empty(), nodes.get(0).getLastConsensus(INSTANCE_ID));
    assertEquals(Optional.empty(), nodes.get(2).getLastConsensus(INSTANCE_ID));
    assertEquals(consensus, nodes.get(1).getLastConsensus(INSTANCE_ID).get());
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

    Map<String, String> acceptorsToMessageId = consensus.getAcceptorsToMessageId();
    assertEquals(1, acceptorsToMessageId.size());
    assertEquals(electAcceptMsg.getMessageId(), acceptorsToMessageId.get(NODE_3_KEY));

    // only the node3 has accepted the elect of node2
    List<ConsensusNode> nodes = lao.getNodes();
    ConsensusNode organizerNode =
        nodes.stream().filter(n -> n.getPublicKey().equals(ORGANIZER)).findAny().get();
    assertTrue(organizerNode.getAcceptedMessageIds().isEmpty());

    ConsensusNode node2 =
        nodes.stream().filter(n -> n.getPublicKey().equals(NODE_2_KEY)).findAny().get();
    assertTrue(node2.getAcceptedMessageIds().isEmpty());

    ConsensusNode node3 =
        nodes.stream().filter(n -> n.getPublicKey().equals(NODE_3_KEY)).findAny().get();
    assertEquals(Sets.newSet(electMsg.getMessageId()), node3.getAcceptedMessageIds());
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
    MessageGeneral electAcceptInvalidMsg = getMsg(ORGANIZER, electAcceptInvalid);
    MessageGeneral learnInvalidMsg = getMsg(ORGANIZER, learnInvalid);
    MessageGeneral failureMsg = getMsg(ORGANIZER, failureInvalid);

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
    LAORepository mockLAORepository = Mockito.mock(LAORepository.class);
    Map<String, MessageGeneral> messageById = new HashMap<>();
    Mockito.when(mockLAORepository.getMessageById()).thenReturn(messageById);

    ConsensusPrepare prepare = new ConsensusPrepare(INSTANCE_ID, messageId, CREATION_TIME, 3);
    ConsensusPromise promise =
        new ConsensusPromise(INSTANCE_ID, messageId, CREATION_TIME, 3, true, 2);
    ConsensusPropose propose =
        new ConsensusPropose(
            INSTANCE_ID, messageId, CREATION_TIME, 3, true, Collections.emptyList());
    ConsensusAccept accept = new ConsensusAccept(INSTANCE_ID, messageId, CREATION_TIME, 3, true);

    messageHandler.handleMessage(mockLAORepository, CONSENSUS_CHANNEL, getMsg(ORGANIZER, prepare));
    messageHandler.handleMessage(mockLAORepository, CONSENSUS_CHANNEL, getMsg(ORGANIZER, promise));
    messageHandler.handleMessage(mockLAORepository, CONSENSUS_CHANNEL, getMsg(ORGANIZER, propose));
    messageHandler.handleMessage(mockLAORepository, CONSENSUS_CHANNEL, getMsg(ORGANIZER, accept));

    // The handlers for prepare/promise/propose/accept should do nothing (call or update nothing)
    // because theses messages should only be handle in the backend server.

    Mockito.verify(mockLAORepository, Mockito.never()).getLaoByChannel(Mockito.anyString());
    Mockito.verify(mockLAORepository, Mockito.never()).updateNodes(Mockito.anyString());
  }
}
