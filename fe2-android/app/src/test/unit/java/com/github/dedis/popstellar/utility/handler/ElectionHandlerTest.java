package com.github.dedis.popstellar.utility.handler;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.github.dedis.popstellar.di.DataRegistryModuleHelper;
import com.github.dedis.popstellar.di.JsonModule;
import com.github.dedis.popstellar.model.network.method.message.MessageGeneral;
import com.github.dedis.popstellar.model.network.method.message.data.DataRegistry;
import com.github.dedis.popstellar.model.network.method.message.data.election.*;
import com.github.dedis.popstellar.model.network.method.message.data.lao.CreateLao;
import com.github.dedis.popstellar.model.objects.*;
import com.github.dedis.popstellar.model.objects.event.EventState;
import com.github.dedis.popstellar.model.objects.security.*;
import com.github.dedis.popstellar.model.objects.security.elGamal.ElectionKeyPair;
import com.github.dedis.popstellar.model.objects.security.elGamal.ElectionPublicKey;
import com.github.dedis.popstellar.repository.LAORepository;
import com.github.dedis.popstellar.repository.MessageRepository;
import com.github.dedis.popstellar.repository.remote.MessageSender;
import com.github.dedis.popstellar.utility.error.*;
import com.github.dedis.popstellar.utility.security.Hash;
import com.github.dedis.popstellar.utility.security.KeyManager;
import com.google.gson.Gson;

import junit.framework.TestCase;

import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.*;

import io.reactivex.Completable;

import static com.github.dedis.popstellar.testutils.Base64DataUtils.generateKeyPair;
import static com.github.dedis.popstellar.utility.handler.data.ElectionHandler.electionSetupWitnessMessage;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ElectionHandlerTest extends TestCase {

  private static final KeyPair SENDER_KEY = generateKeyPair();
  private static final PublicKey SENDER = SENDER_KEY.getPublicKey();

  private static final CreateLao CREATE_LAO = new CreateLao("lao", SENDER);
  private static final Channel LAO_CHANNEL = Channel.ROOT.subChannel(CREATE_LAO.getId());

  private static final long openedAt = 1633099883;
  private Lao lao;
  private Election election;
  private Election electionEncrypted;
  private ElectionQuestion electionQuestion;

  private LAORepository laoRepo;
  private MessageHandler messageHandler;
  private Gson gson;

  @Mock MessageSender messageSender;
  @Mock KeyManager keyManager;

  @Rule public InstantTaskExecutorRule rule = new InstantTaskExecutorRule();

  @Before
  public void setup() throws GeneralSecurityException, IOException {
    lenient().when(keyManager.getMainKeyPair()).thenReturn(SENDER_KEY);
    lenient().when(keyManager.getMainPublicKey()).thenReturn(SENDER);

    when(messageSender.subscribe(any())).then(args -> Completable.complete());

    laoRepo = new LAORepository();
    DataRegistry dataRegistry = DataRegistryModuleHelper.buildRegistry(laoRepo, keyManager);
    MessageRepository messageRepo = new MessageRepository();
    gson = JsonModule.provideGson(dataRegistry);
    messageHandler = new MessageHandler(messageRepo, dataRegistry);

    // Create one LAO
    lao = new Lao(CREATE_LAO.getName(), CREATE_LAO.getOrganizer(), CREATE_LAO.getCreation());
    lao.setLastModified(lao.getCreation());

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

    // Add the Lao to the repository
    laoRepo.updateLao(lao);

    // Add the CreateLao message to the LAORepository
    MessageGeneral createLaoMessage = new MessageGeneral(SENDER_KEY, CREATE_LAO, gson);
    messageRepo.addMessage(createLaoMessage);
  }

  @Test
  public void testHandleElectionSetup()
      throws DataHandlingException, UnknownLaoException, UnknownRollCallException {
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
    MessageGeneral message = new MessageGeneral(SENDER_KEY, electionSetupOpenBallot, gson);

    // Call the message handler
    messageHandler.handleMessage(messageSender, LAO_CHANNEL, message);

    // Check the Election is present with state OPENED and the correct ID
    Optional<Election> electionOpt =
        laoRepo.getLaoByChannel(LAO_CHANNEL).getElection(electionSetupOpenBallot.getId());
    assertTrue(electionOpt.isPresent());

    assertEquals(EventState.CREATED, electionOpt.get().getState());
    assertEquals(electionSetupOpenBallot.getId(), electionOpt.get().getId());

    // Check that the election version has been successfully set
    assertEquals(ElectionVersion.OPEN_BALLOT, electionOpt.get().getElectionVersion());

    // Check the WitnessMessage has been created
    Optional<WitnessMessage> witnessMessage =
        laoRepo.getLaoByChannel(LAO_CHANNEL).getWitnessMessage(message.getMessageId());
    assertTrue(witnessMessage.isPresent());

    // Check the Witness message contains the expected title and description
    WitnessMessage expectedMessage =
        electionSetupWitnessMessage(message.getMessageId(), electionOpt.get());
    assertEquals(expectedMessage.getTitle(), witnessMessage.get().getTitle());
    assertEquals(expectedMessage.getDescription(), witnessMessage.get().getDescription());
  }

  @Test
  public void testHandleElectionResult()
      throws DataHandlingException, UnknownLaoException, UnknownRollCallException {
    // Create the result Election message
    QuestionResult questionResult =
        new QuestionResult(electionQuestion.getBallotOptions().get(0), 2);
    ElectionResultQuestion electionResultQuestion =
        new ElectionResultQuestion("id", Collections.singletonList(questionResult));
    ElectionResult electionResult =
        new ElectionResult(Collections.singletonList(electionResultQuestion));
    MessageGeneral message = new MessageGeneral(SENDER_KEY, electionResult, gson);

    // Call the message handler
    messageHandler.handleMessage(messageSender, LAO_CHANNEL.subChannel(election.getId()), message);

    // Check the Election is present with state RESULTS_READY and the results
    Optional<Election> electionOpt =
        laoRepo.getLaoByChannel(LAO_CHANNEL).getElection(election.getId());
    assertTrue(electionOpt.isPresent());
    assertEquals(EventState.RESULTS_READY, electionOpt.get().getState());
    assertEquals(
        Collections.singletonList(questionResult), electionOpt.get().getResultsForQuestionId("id"));
  }

  @Test
  public void testHandleElectionEnd()
      throws DataHandlingException, UnknownLaoException, UnknownRollCallException {
    // Create the end Election message
    ElectionEnd electionEnd = new ElectionEnd(election.getId(), lao.getId(), "");
    MessageGeneral message = new MessageGeneral(SENDER_KEY, electionEnd, gson);

    // Call the message handler
    messageHandler.handleMessage(messageSender, LAO_CHANNEL.subChannel(election.getId()), message);

    // Check the Election is present with state CLOSED and the results
    Optional<Election> electionOpt =
        laoRepo.getLaoByChannel(LAO_CHANNEL).getElection(election.getId());
    assertTrue(electionOpt.isPresent());
    assertEquals(EventState.CLOSED, electionOpt.get().getState());
  }

  @Test
  public void testHandleElectionOpen()
      throws DataHandlingException, UnknownLaoException, UnknownRollCallException {
    OpenElection openElection = new OpenElection(lao.getId(), election.getId(), openedAt);
    MessageGeneral message = new MessageGeneral(SENDER_KEY, openElection, gson);

    for (EventState state : EventState.values()) {
      election.setEventState(state);
      messageHandler.handleMessage(messageSender, election.getChannel(), message);
      if (state == EventState.CREATED) {
        // Test for current TimeStamp
        assertEquals(EventState.OPENED, election.getState());
        assertEquals(Instant.now().getEpochSecond(), election.getStartTimestamp());
      } else {
        assertEquals(state, election.getState());
      }
    }
  }

  @Test
  public void testElectionKey()
      throws DataHandlingException, UnknownLaoException, UnknownRollCallException {
    // Create the election key message
    String key = "JsS0bXJU8yMT9jvIeTfoS6RJPZ8YopuAUPkxssHaoTQ";
    ElectionKey electionKey = new ElectionKey(election.getId(), key);
    MessageGeneral message = new MessageGeneral(SENDER_KEY, electionKey, gson);

    // Call the message handler
    messageHandler.handleMessage(messageSender, LAO_CHANNEL.subChannel(election.getId()), message);

    assertEquals(key, election.getElectionKey());
  }

  @Test
  public void testHandleCastVote()
      throws DataHandlingException, UnknownLaoException, UnknownRollCallException {
    // Here we test if the handling can both an Open Ballot cast vote and
    // an Secret Ballot vote

    // First test the Open Ballot version
    // Set up a open ballot election
    ElectionVote electionVote1 = new ElectionVote("1", 1, false, null, election.getId());
    List<ElectionVote> electionVotes = Collections.singletonList(electionVote1);
    CastVote<ElectionVote> electionVote =
        new CastVote<>(electionVotes, election.getId(), CREATE_LAO.getId());

    MessageGeneral message1 = new MessageGeneral(SENDER_KEY, electionVote, gson);
    // Test the whole process
    messageHandler.handleMessage(messageSender, LAO_CHANNEL.subChannel(election.getId()), message1);
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
    ElectionEncryptedVote electionEncryptedVote2 =
        new ElectionEncryptedVote("3", "1", false, null, electionEncrypted.getId());
    List<ElectionEncryptedVote> electionEncryptedVote =
        Arrays.asList(electionEncryptedVote1, electionEncryptedVote2);
    CastVote<ElectionEncryptedVote> encryptedCastVote =
        new CastVote<>(electionEncryptedVote, electionEncrypted.getId(), CREATE_LAO.getId());
    MessageGeneral message2 = new MessageGeneral(SENDER_KEY, encryptedCastVote, gson);
    // Test the handling, it no error are thrown it means that the validation happened without
    // problems
    messageHandler.handleMessage(
        messageSender, LAO_CHANNEL.subChannel(electionEncrypted.getId()), message2);
    List<String> listOfVoteIds2 = new ArrayList<>();
    listOfVoteIds2.add(electionEncryptedVote2.getId());
    listOfVoteIds2.add(electionEncryptedVote1.getId());
    String expectedHash2 = Hash.hash(listOfVoteIds2.toArray(new String[0]));
    assertEquals(
        expectedHash2,
        laoRepo.getElectionByChannel(electionEncrypted.getChannel()).computerRegisteredVotes());
  }
}
