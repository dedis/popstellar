package com.github.dedis.popstellar.utility.handler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.github.dedis.popstellar.Injection;
import com.github.dedis.popstellar.model.network.GenericMessage;
import com.github.dedis.popstellar.model.network.answer.Result;
import com.github.dedis.popstellar.model.network.method.message.MessageGeneral;
import com.github.dedis.popstellar.model.network.method.message.data.Data;
import com.github.dedis.popstellar.model.network.method.message.data.consensus.ConsensusElect;
import com.github.dedis.popstellar.model.network.method.message.data.consensus.ConsensusElectAccept;
import com.github.dedis.popstellar.model.network.method.message.data.consensus.ConsensusKey;
import com.github.dedis.popstellar.model.network.method.message.data.lao.CreateLao;
import com.github.dedis.popstellar.model.objects.Consensus;
import com.github.dedis.popstellar.model.objects.ConsensusNode;
import com.github.dedis.popstellar.model.objects.Lao;
import com.github.dedis.popstellar.repository.LAORepository;
import com.github.dedis.popstellar.repository.LAOState;
import com.github.dedis.popstellar.repository.local.LAOLocalDataSource;
import com.github.dedis.popstellar.repository.remote.LAORemoteDataSource;
import com.github.dedis.popstellar.utility.scheduler.SchedulerProvider;
import com.github.dedis.popstellar.utility.scheduler.TestSchedulerProvider;
import com.google.crypto.tink.PublicKeySign;
import com.google.crypto.tink.integration.android.AndroidKeysetManager;
import com.google.gson.Gson;

import org.junit.After;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.schedulers.TestScheduler;

@RunWith(MockitoJUnitRunner.class)
public class ConsensusHandlerTest {

  private static final String organizer = "J9fBzJV70Jk5c-i3277Uq4CmeL4t53WDfUghaK0HpeM=";
  private static final String node2Key = "Vf9kiBGdOPIutk9z055oQxpG9askyx5C_wnSznpOrHk=";
  private static final String node3Key = "SYeTNtpM0XCbdrPTYSSMFJHtuHBiIEQyrq_auP4Hggw=";
  private static final long creationTime = 946684800;
  private static final String laoName = "laoName";
  private static final String laoId = Lao.generateLaoId(organizer, creationTime, laoName);
  private static final String laoChannel = "/root/" + laoId;
  private static final String consensusChannel = laoChannel + "/consensus";
  private static final Lao lao = new Lao(laoChannel);

  private static final String type = "election";
  private static final String keyId = "-t0xoQZa-ryiW18JnTjJHCsCNehFxuXOFOsfgKHHkj0=";
  private static final String property = "state";
  private static final String value = "started";
  private static final ConsensusKey key = new ConsensusKey(type, keyId, property);

  private static final CreateLao CREATE_LAO =
      new CreateLao(laoId, laoName, creationTime, organizer, Arrays.asList(node2Key, node3Key));
  private static final ConsensusElect elect =
      new ConsensusElect(creationTime, keyId, type, property, value);

  private static final String instanceId = Consensus.generateConsensusId(type, keyId, property);

  private static final Gson gson = Injection.provideGson();
  private static final int REQUEST_ID = 42;
  private static final int RESPONSE_DELAY = 1000;

  private LAORepository laoRepository;
  private MessageGeneral electMsg;
  private ConsensusElectAccept electAccept;
  private MessageGeneral electAcceptMsg;

  @Mock LAORemoteDataSource remoteDataSource;
  @Mock LAOLocalDataSource localDataSource;
  @Mock AndroidKeysetManager androidKeysetManager;
  @Mock PublicKeySign signer;

  @Before
  public void setup() throws GeneralSecurityException {
    SchedulerProvider testSchedulerProvider = new TestSchedulerProvider();
    TestScheduler testScheduler = (TestScheduler) testSchedulerProvider.io();

    // Mock the signing of of any data for the MessageGeneral constructor
    byte[] dataBuf = Injection.provideGson().toJson(CREATE_LAO, Data.class).getBytes();
    Mockito.when(signer.sign(Mockito.any())).thenReturn(dataBuf);
    MessageGeneral createLaoMessage = getMsg(organizer, CREATE_LAO);

    // Simulate a network response from the server after the response delay
    Observable<GenericMessage> upstream =
        Observable.fromArray((GenericMessage) new Result(REQUEST_ID))
            .delay(RESPONSE_DELAY, TimeUnit.MILLISECONDS, testScheduler);

    Mockito.when(remoteDataSource.observeMessage()).thenReturn(upstream);
    Mockito.when(remoteDataSource.observeWebsocket()).thenReturn(Observable.empty());

    LAORepository.destroyInstance();
    laoRepository =
        LAORepository.getInstance(
            remoteDataSource,
            localDataSource,
            androidKeysetManager,
            Injection.provideGson(),
            testSchedulerProvider);

    laoRepository.getLaoById().put(laoChannel, new LAOState(lao));
    MessageHandler.handleMessage(laoRepository, laoChannel, createLaoMessage);
  }

  @After
  public void clean() {
    LAORepository.destroyInstance();
  }

  private MessageGeneral getMsg(String key, Data data) {
    return new MessageGeneral(Base64.getUrlDecoder().decode(key), data, signer, gson);
  }

  @Test
  public void handleConsensusTests() {
    // each test need to be run one after another
    handleConsensusElectTest();
    handleConsensusElectAcceptTest();
  }

  // handle an elect from node2
  private void handleConsensusElectTest() {
    electMsg = getMsg(node2Key, elect);
    MessageHandler.handleMessage(laoRepository, consensusChannel, electMsg);

    Optional<Consensus> consensusOpt = lao.getConsensus(electMsg.getMessageId());
    assertTrue(consensusOpt.isPresent());
    Consensus consensus = consensusOpt.get();

    assertEquals(electMsg.getMessageId(), consensus.getMessageId());
    assertEquals(node2Key, consensus.getProposer());
    assertEquals(consensusChannel, consensus.getChannel());
    assertEquals(creationTime, consensus.getCreation());
    assertEquals(value, consensus.getValue());
    assertEquals(key, consensus.getKey());

    assertTrue(consensus.getAcceptorsToMessageId().isEmpty());
    assertEquals(Sets.newSet(organizer, node2Key, node3Key), consensus.getNodes());

    Map<String, Consensus> messageIdToConsensus = lao.getMessageIdToConsensus();
    assertEquals(1, messageIdToConsensus.size());
    assertEquals(consensus, messageIdToConsensus.get(consensus.getMessageId()));

    List<ConsensusNode> nodes = lao.getNodes();
    assertEquals(3, nodes.size());
    assertEquals(Optional.empty(), nodes.get(0).getLastConsensus(instanceId));
    assertEquals(Optional.empty(), nodes.get(2).getLastConsensus(instanceId));
    assertEquals(consensus, nodes.get(1).getLastConsensus(instanceId).get());
  }

  // handle an electAccept from node3 for the elect of node2
  private void handleConsensusElectAcceptTest() {
    electAccept = new ConsensusElectAccept(instanceId, electMsg.getMessageId(), true);
    electAcceptMsg = getMsg(node3Key, electAccept);
    MessageHandler.handleMessage(laoRepository, consensusChannel, electAcceptMsg);

    Optional<Consensus> consensusOpt = lao.getConsensus(electMsg.getMessageId());
    assertTrue(consensusOpt.isPresent());
    Consensus consensus = consensusOpt.get();

    Map<String, String> acceptorsToMessageId = consensus.getAcceptorsToMessageId();
    assertEquals(1, acceptorsToMessageId.size());
    assertEquals(electAcceptMsg.getMessageId(), acceptorsToMessageId.get(node3Key));

    // only the node3 has accepted the elect of node2
    List<ConsensusNode> nodes = lao.getNodes();
    ConsensusNode organizerNode =
        nodes.stream().filter(n -> n.getPublicKey().equals(organizer)).findAny().get();
    assertTrue(organizerNode.getAcceptedMessageIds().isEmpty());

    ConsensusNode node2 =
        nodes.stream().filter(n -> n.getPublicKey().equals(node2Key)).findAny().get();
    assertTrue(node2.getAcceptedMessageIds().isEmpty());

    ConsensusNode node3 =
        nodes.stream().filter(n -> n.getPublicKey().equals(node3Key)).findAny().get();
    assertEquals(Sets.newSet(electMsg.getMessageId()), node3.getAcceptedMessageIds());
  }
}
