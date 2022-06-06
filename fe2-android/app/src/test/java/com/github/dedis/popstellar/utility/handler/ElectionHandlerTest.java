package com.github.dedis.popstellar.utility.handler;

import static com.github.dedis.popstellar.testutils.Base64DataUtils.generateKeyPair;
import static com.github.dedis.popstellar.utility.handler.data.ElectionHandler.electionSetupWitnessMessage;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.github.dedis.popstellar.di.DataRegistryModule;
import com.github.dedis.popstellar.di.JsonModule;
import com.github.dedis.popstellar.model.network.method.message.MessageGeneral;
import com.github.dedis.popstellar.model.network.method.message.data.election.CastVote;
import com.github.dedis.popstellar.model.network.method.message.data.election.ElectionEncryptedVote;
import com.github.dedis.popstellar.model.network.method.message.data.election.ElectionEnd;
import com.github.dedis.popstellar.model.network.method.message.data.election.ElectionKey;
import com.github.dedis.popstellar.model.network.method.message.data.election.ElectionQuestion;
import com.github.dedis.popstellar.model.network.method.message.data.election.ElectionResult;
import com.github.dedis.popstellar.model.network.method.message.data.election.ElectionResultQuestion;
import com.github.dedis.popstellar.model.network.method.message.data.election.ElectionSetup;
import com.github.dedis.popstellar.model.network.method.message.data.election.ElectionVersion;
import com.github.dedis.popstellar.model.network.method.message.data.election.ElectionVote;
import com.github.dedis.popstellar.model.network.method.message.data.election.OpenElection;
import com.github.dedis.popstellar.model.network.method.message.data.election.QuestionResult;
import com.github.dedis.popstellar.model.network.method.message.data.lao.CreateLao;
import com.github.dedis.popstellar.model.objects.Channel;
import com.github.dedis.popstellar.model.objects.Election;
import com.github.dedis.popstellar.model.objects.Lao;
import com.github.dedis.popstellar.model.objects.RollCall;
import com.github.dedis.popstellar.model.objects.WitnessMessage;
import com.github.dedis.popstellar.model.objects.event.EventState;
import com.github.dedis.popstellar.model.objects.security.Base64URLData;
import com.github.dedis.popstellar.model.objects.security.Ed25519.ElectionKeyPair;
import com.github.dedis.popstellar.model.objects.security.Ed25519.ElectionPublicKey;
import com.github.dedis.popstellar.model.objects.security.KeyPair;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.repository.LAORepository;
import com.github.dedis.popstellar.repository.LAOState;
import com.github.dedis.popstellar.repository.ServerRepository;
import com.github.dedis.popstellar.repository.remote.MessageSender;
import com.github.dedis.popstellar.utility.error.DataHandlingException;
import com.github.dedis.popstellar.utility.security.Hash;
import com.github.dedis.popstellar.utility.security.KeyManager;
import com.google.gson.Gson;

import junit.framework.TestCase;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import io.reactivex.Completable;

@RunWith(MockitoJUnitRunner.class)
public class ElectionHandlerTest extends TestCase {

  private static final KeyPair SENDER_KEY = generateKeyPair();
  private static final PublicKey SENDER = SENDER_KEY.getPublicKey();

  private static final CreateLao CREATE_LAO = new CreateLao("lao", SENDER);
  private static final Channel LAO_CHANNEL = Channel.ROOT.subChannel(CREATE_LAO.getId());

  private static final Gson GSON = JsonModule.provideGson(DataRegistryModule.provideDataRegistry());

  private static final long openedAt = 1633099883;
  private Lao lao;
  private RollCall rollCall;
  private Election election;
  private Election electionEncrypted;
  private ElectionQuestion electionQuestion;

  private LAORepository laoRepository;
  private MessageHandler messageHandler;
  private ServerRepository serverRepository;

  @Mock MessageSender messageSender;
  @Mock KeyManager keyManager;

  @Rule public InstantTaskExecutorRule rule = new InstantTaskExecutorRule();

  @Before
  public void setup() throws GeneralSecurityException, IOException {
    lenient().when(keyManager.getMainKeyPair()).thenReturn(SENDER_KEY);
    lenient().when(keyManager.getMainPublicKey()).thenReturn(SENDER);

    laoRepository = new LAORepository();

    when(messageSender.subscribe(any())).then(args -> Completable.complete());

    laoRepository = new LAORepository();
    messageHandler =
        new MessageHandler(DataRegistryModule.provideDataRegistry(), keyManager, serverRepository);

    // Create one LAO
    lao = new Lao(CREATE_LAO.getName(), CREATE_LAO.getOrganizer(), CREATE_LAO.getCreation());
    lao.setLastModified(lao.getCreation());

    // Create one Roll Call and add it to the LAO
    rollCall = new RollCall(lao.getId(), Instant.now().getEpochSecond(), "roll call 1");
    lao.setRollCalls(
        new HashMap<String, RollCall>() {
          {
            put(rollCall.getId(), rollCall);
          }
        });

    // Create one Election and add it to the LAO
    election =
        new Election(
            lao.getId(), Instant.now().getEpochSecond(), "election 1", ElectionVersion.OPEN_BALLOT);
    electionEncrypted =
        new Election(
            lao.getId(),
            Instant.now().getEpochSecond(),
            "election 2",
            ElectionVersion.SECRET_BALLOT);

    election.setStart(Instant.now().getEpochSecond());
    election.setEnd(Instant.now().getEpochSecond() + 20L);
    election.setChannel(lao.getChannel().subChannel(election.getId()));
    electionEncrypted.setStart(Instant.now().getEpochSecond());
    electionEncrypted.setEnd(Instant.now().getEpochSecond() + 21L);
    electionEncrypted.setId("election2");
    electionEncrypted.setChannel(lao.getChannel().subChannel(electionEncrypted.getId()));

    electionQuestion =
        new ElectionQuestion(
            "question", "Plurality", false, Arrays.asList("a", "b"), election.getId());
    election.setElectionQuestions(Collections.singletonList(electionQuestion));
    lao.setElections(
        new HashMap<String, Election>() {
          {
            put(election.getId(), election);
            put(electionEncrypted.getId(), electionEncrypted);
          }
        });

    // Add the LAO to the LAORepository
    laoRepository.getLaoById().put(lao.getId(), new LAOState(lao));
    laoRepository.setAllLaoSubject();

    // Add the CreateLao message to the LAORepository
    MessageGeneral createLaoMessage = new MessageGeneral(SENDER_KEY, CREATE_LAO, GSON);
    laoRepository.getMessageById().put(createLaoMessage.getMessageId(), createLaoMessage);
  }

  @Test
  public void testHandleElectionSetup() throws DataHandlingException {
    // Create the setup Election message
    ElectionSetup electionSetupOpenBallot =
        new ElectionSetup(
            Collections.singletonList(electionQuestion.getWriteIn()),
            "election 2",
            election.getCreation(),
            election.getStartTimestamp(),
            election.getEndTimestamp(),
            Collections.singletonList(electionQuestion.getVotingMethod()),
            lao.getId(),
            Collections.singletonList(electionQuestion.getBallotOptions()),
            Collections.singletonList(electionQuestion.getQuestion()),
            ElectionVersion.OPEN_BALLOT);
    MessageGeneral message = new MessageGeneral(SENDER_KEY, electionSetupOpenBallot, GSON);

    // Call the message handler
    messageHandler.handleMessage(laoRepository, messageSender, LAO_CHANNEL, message);

    // Check the Election is present with state OPENED and the correct ID
    Optional<Election> electionOpt =
        laoRepository.getLaoByChannel(LAO_CHANNEL).getElection(electionSetupOpenBallot.getId());
    assertTrue(electionOpt.isPresent());

    assertEquals(EventState.CREATED, electionOpt.get().getState().getValue());
    assertEquals(electionSetupOpenBallot.getId(), electionOpt.get().getId());

    // Check that the election version has been successfully set
    assertEquals(ElectionVersion.OPEN_BALLOT, electionOpt.get().getElectionVersion());

    // Check the WitnessMessage has been created
    Optional<WitnessMessage> witnessMessage =
        laoRepository.getLaoByChannel(LAO_CHANNEL).getWitnessMessage(message.getMessageId());
    assertTrue(witnessMessage.isPresent());

    // Check the Witness message contains the expected title and description
    WitnessMessage expectedMessage =
        electionSetupWitnessMessage(message.getMessageId(), electionOpt.get());
    assertEquals(expectedMessage.getTitle(), witnessMessage.get().getTitle());
    assertEquals(expectedMessage.getDescription(), witnessMessage.get().getDescription());
  }

  @Test
  public void testHandleElectionResult() throws DataHandlingException {
    // Create the result Election message
    QuestionResult questionResult =
        new QuestionResult(electionQuestion.getBallotOptions().get(0), 2);
    ElectionResultQuestion electionResultQuestion =
        new ElectionResultQuestion("id", Collections.singletonList(questionResult));
    ElectionResult electionResult =
        new ElectionResult(Collections.singletonList(electionResultQuestion));
    MessageGeneral message = new MessageGeneral(SENDER_KEY, electionResult, GSON);

    // Call the message handler
    messageHandler.handleMessage(
        laoRepository, messageSender, LAO_CHANNEL.subChannel(election.getId()), message);

    // Check the Election is present with state RESULTS_READY and the results
    Optional<Election> electionOpt =
        laoRepository.getLaoByChannel(LAO_CHANNEL).getElection(election.getId());
    assertTrue(electionOpt.isPresent());
    assertEquals(EventState.RESULTS_READY, electionOpt.get().getState().getValue());
    assertEquals(
        Collections.singletonList(questionResult), electionOpt.get().getResultsForQuestionId("id"));
  }

  @Test
  public void testHandleElectionEnd() throws DataHandlingException {
    // Create the end Election message
    ElectionEnd electionEnd = new ElectionEnd(election.getId(), lao.getId(), "");
    MessageGeneral message = new MessageGeneral(SENDER_KEY, electionEnd, GSON);

    // Call the message handler
    messageHandler.handleMessage(
        laoRepository, messageSender, LAO_CHANNEL.subChannel(election.getId()), message);

    // Check the Election is present with state CLOSED and the results
    Optional<Election> electionOpt =
        laoRepository.getLaoByChannel(LAO_CHANNEL).getElection(election.getId());
    assertTrue(electionOpt.isPresent());
    assertEquals(EventState.CLOSED, electionOpt.get().getState().getValue());
  }

  @Test
  public void testHandleElectionOpen() throws DataHandlingException {
    OpenElection openElection = new OpenElection(lao.getId(), election.getId(), openedAt);
    MessageGeneral message = new MessageGeneral(SENDER_KEY, openElection, GSON);

    for (EventState state : EventState.values()) {
      election.setEventState(state);
      messageHandler.handleMessage(laoRepository, messageSender, election.getChannel(), message);
      if (state == EventState.CREATED) {
        // Test for current TimeStamp
        assertEquals(EventState.OPENED, election.getState().getValue());
        assertEquals(Instant.now().getEpochSecond(), election.getStartTimestamp());
      } else {
        assertEquals(state, election.getState().getValue());
      }
    }
  }

  @Test
  public void testElectionKey() throws DataHandlingException {
    // Create the election key message
    String key = "JsS0bXJU8yMT9jvIeTfoS6RJPZ8YopuAUPkxssHaoTQ";
    ElectionKey electionKey = new ElectionKey(election.getId(), key);
    MessageGeneral message = new MessageGeneral(SENDER_KEY, electionKey, GSON);

    // Call the message handler
    messageHandler.handleMessage(
        laoRepository, messageSender, LAO_CHANNEL.subChannel(election.getId()), message);

    assertEquals(key, election.getElectionKey());
  }

  @Test
  public void testHandleCastVote() throws DataHandlingException {
    // Here we test if the handling can both an Open Ballot cast vote and
    // an Secret Ballot vote

    // First test the Open Ballot version
    // Set up a open ballot election
    ElectionVote electionVote1 = new ElectionVote("1", 1, false, null, election.getId());
    List<ElectionVote> electionVotes = Arrays.asList(electionVote1);
    CastVote<ElectionVote> electionVote =
        new CastVote<>(electionVotes, election.getId(), CREATE_LAO.getId());

    MessageGeneral message1 = new MessageGeneral(SENDER_KEY, electionVote, GSON);
    // Test the whole process
    messageHandler.handleMessage(
        laoRepository, messageSender, LAO_CHANNEL.subChannel(election.getId()), message1);
    List<String> listOfVoteIds = new ArrayList<>();
    // Since messageMap is a TreeMap, votes will already be sorted in the alphabetical order of
    // messageIds
    listOfVoteIds.add(electionVote1.getId());
    String expectedHash = Hash.hash(listOfVoteIds.toArray(new String[0]));
    assertEquals(expectedHash, election.computerRegisteredVotes());

    // Now test with a SECRET BALLOT election

    // Generate some keys for encryption
    ElectionKeyPair keys = ElectionKeyPair.generateKeyPair();
    ElectionPublicKey pubKey = keys.getEncryptionScheme();
    Base64URLData encodedKey = new Base64URLData(pubKey.getPublicKey().toBytes());
    electionEncrypted.setElectionKey(encodedKey.getEncoded());

    ElectionEncryptedVote electionEncryptedVote1 =
        new ElectionEncryptedVote("2", "1", false, null, electionEncrypted.getId());
    List<ElectionEncryptedVote> electionEncryptedVote = Arrays.asList(electionEncryptedVote1);
    CastVote<ElectionEncryptedVote> encryptedCastVote =
        new CastVote<>(electionEncryptedVote, electionEncrypted.getId(), CREATE_LAO.getId());

    MessageGeneral message2 = new MessageGeneral(SENDER_KEY, encryptedCastVote, GSON);
    // Test the handling, it no error are thrown it means that the validation happened without
    // problems
    messageHandler.handleMessage(
        laoRepository, messageSender, LAO_CHANNEL.subChannel(electionEncrypted.getId()), message2);
  }
}
