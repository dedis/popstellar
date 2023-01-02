package com.github.dedis.popstellar.utility.handler;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.github.dedis.popstellar.di.DataRegistryModuleHelper;
import com.github.dedis.popstellar.di.JsonModule;
import com.github.dedis.popstellar.model.network.method.message.MessageGeneral;
import com.github.dedis.popstellar.model.network.method.message.data.DataRegistry;
import com.github.dedis.popstellar.model.network.method.message.data.election.*;
import com.github.dedis.popstellar.model.network.method.message.data.election.ElectionQuestion.Question;
import com.github.dedis.popstellar.model.network.method.message.data.lao.CreateLao;
import com.github.dedis.popstellar.model.objects.*;
import com.github.dedis.popstellar.model.objects.event.EventState;
import com.github.dedis.popstellar.model.objects.security.*;
import com.github.dedis.popstellar.model.objects.security.elGamal.ElectionKeyPair;
import com.github.dedis.popstellar.model.objects.security.elGamal.ElectionPublicKey;
import com.github.dedis.popstellar.repository.*;
import com.github.dedis.popstellar.repository.remote.MessageSender;
import com.github.dedis.popstellar.utility.error.*;
import com.github.dedis.popstellar.utility.security.Hash;
import com.github.dedis.popstellar.utility.security.KeyManager;
import com.google.gson.Gson;

import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.*;
import java.util.stream.Collectors;

import io.reactivex.Completable;

import static com.github.dedis.popstellar.testutils.Base64DataUtils.generateKeyPair;
import static com.github.dedis.popstellar.utility.handler.data.ElectionHandler.electionSetupWitnessMessage;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ElectionHandlerTest {

  private static final KeyPair SENDER_KEY = generateKeyPair();
  private static final PublicKey SENDER = SENDER_KEY.getPublicKey();

  private static final KeyPair ATTENDEE_KEY = generateKeyPair();

  private static final CreateLao CREATE_LAO = new CreateLao("Lao", SENDER);
  private static final Lao LAO =
      new Lao(CREATE_LAO.getName(), CREATE_LAO.getOrganizer(), CREATE_LAO.getCreation());
  private static final Channel LAO_CHANNEL = Channel.ROOT.subChannel(LAO.getId());

  private static final long CREATED_AT = CREATE_LAO.getCreation() + 10 * 1000; // 10 seconds later
  private static final long STARTED_AT = CREATE_LAO.getCreation() + 20 * 1000; // 20 seconds later
  private static final long OPENED_AT = CREATE_LAO.getCreation() + 30 * 1000; // 30 seconds later
  private static final long END_AT = CREATE_LAO.getCreation() + 600 * 1000; // 60 seconds later

  private static final String ELECTION_NAME = "Election Name";
  private static final String ELECTION_ID =
      Election.generateElectionSetupId(LAO.getId(), CREATED_AT, ELECTION_NAME);
  private static final String OPTION_1 = "Yes";
  private static final String OPTION_2 = "No";
  private static final ElectionQuestion QUESTION =
      new ElectionQuestion(
          ELECTION_ID,
          new Question("Does this work ?", "Plurality", Arrays.asList(OPTION_1, OPTION_2), false));
  private static final String ELECTION_KEY = "JsS0bXJU8yMT9jvIeTfoS6RJPZ8YopuAUPkxssHaoTQ";
  private static final Election OPEN_BALLOT_ELECTION =
      new Election.ElectionBuilder(LAO.getId(), CREATED_AT, ELECTION_NAME)
          .setElectionVersion(ElectionVersion.OPEN_BALLOT)
          .setElectionQuestions(Collections.singletonList(QUESTION))
          .setStart(STARTED_AT)
          .setEnd(END_AT)
          .build();

  private static final Election SECRET_BALLOT_ELECTION =
      new Election.ElectionBuilder(OPEN_BALLOT_ELECTION)
          .setElectionVersion(ElectionVersion.SECRET_BALLOT)
          .build();

  private LAORepository laoRepo;
  private ElectionRepository electionRepo;
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
    electionRepo = new ElectionRepository();

    DataRegistry dataRegistry =
        DataRegistryModuleHelper.buildRegistry(laoRepo, electionRepo, keyManager);
    MessageRepository messageRepo = new MessageRepository();
    gson = JsonModule.provideGson(dataRegistry);
    messageHandler = new MessageHandler(messageRepo, dataRegistry);

    laoRepo.updateLao(LAO);

    // Add the CreateLao message to the LAORepository
    MessageGeneral createLaoMessage = new MessageGeneral(SENDER_KEY, CREATE_LAO, gson);
    messageRepo.addMessage(createLaoMessage);
  }

  private MessageID handleElectionSetup(Election election)
      throws UnknownElectionException, UnknownRollCallException, UnknownLaoException,
          DataHandlingException {
    List<Question> questions =
        election.getElectionQuestions().stream()
            .map(
                elecQuestion ->
                    new Question(
                        elecQuestion.getQuestion(),
                        elecQuestion.getVotingMethod(),
                        elecQuestion.getBallotOptions(),
                        elecQuestion.getWriteIn()))
            .collect(Collectors.toList());

    // Create the setup Election message
    ElectionSetup electionSetupOpenBallot =
        new ElectionSetup(
            election.getName(),
            election.getCreation(),
            election.getStartTimestamp(),
            election.getEndTimestamp(),
            LAO.getId(),
            election.getElectionVersion(),
            questions);

    MessageGeneral message = new MessageGeneral(SENDER_KEY, electionSetupOpenBallot, gson);
    messageHandler.handleMessage(messageSender, LAO_CHANNEL, message);
    return message.getMessageId();
  }

  private void handleElectionKey(Election election, String key)
      throws UnknownElectionException, UnknownRollCallException, UnknownLaoException,
          DataHandlingException {
    // Create the election key message
    ElectionKey electionKey = new ElectionKey(election.getId(), key);

    MessageGeneral message = new MessageGeneral(SENDER_KEY, electionKey, gson);
    messageHandler.handleMessage(messageSender, election.getChannel(), message);
  }

  private void handleElectionOpen(Election election)
      throws UnknownElectionException, UnknownRollCallException, UnknownLaoException,
          DataHandlingException {
    OpenElection openElection = new OpenElection(LAO.getId(), election.getId(), OPENED_AT);

    MessageGeneral message = new MessageGeneral(SENDER_KEY, openElection, gson);
    messageHandler.handleMessage(messageSender, election.getChannel(), message);
  }

  private MessageID handleCastVote(ElectionVote vote, KeyPair senderKey)
      throws UnknownElectionException, UnknownRollCallException, UnknownLaoException,
          DataHandlingException {
    CastVote<ElectionVote> electionVote =
        new CastVote<>(
            Collections.singletonList(vote), OPEN_BALLOT_ELECTION.getId(), CREATE_LAO.getId());

    MessageGeneral message = new MessageGeneral(senderKey, electionVote, gson);
    messageHandler.handleMessage(messageSender, OPEN_BALLOT_ELECTION.getChannel(), message);
    return message.getMessageId();
  }

  // Once ElectionVote and EncryptedVote have the same parent, merge the two functions and use the
  // parent
  private MessageID handleCastVote(ElectionEncryptedVote vote, KeyPair senderKey)
      throws UnknownElectionException, UnknownRollCallException, UnknownLaoException,
          DataHandlingException {
    CastVote<ElectionEncryptedVote> electionVote =
        new CastVote<>(
            Collections.singletonList(vote), SECRET_BALLOT_ELECTION.getId(), CREATE_LAO.getId());

    MessageGeneral message = new MessageGeneral(senderKey, electionVote, gson);
    messageHandler.handleMessage(messageSender, SECRET_BALLOT_ELECTION.getChannel(), message);

    return message.getMessageId();
  }

  private void handleElectionEnd()
      throws UnknownElectionException, UnknownRollCallException, UnknownLaoException,
          DataHandlingException {
    // Retrieve current election to use the correct vote hash
    Election current = electionRepo.getElection(LAO.getId(), ELECTION_ID);
    ElectionEnd endElection =
        new ElectionEnd(LAO.getId(), ELECTION_ID, current.computerRegisteredVotesHash());

    MessageGeneral message = new MessageGeneral(SENDER_KEY, endElection, gson);
    messageHandler.handleMessage(messageSender, OPEN_BALLOT_ELECTION.getChannel(), message);
  }

  private void handleElectionResults(Set<QuestionResult> results, Channel electionChannel)
      throws UnknownElectionException, UnknownRollCallException, UnknownLaoException,
          DataHandlingException {
    ElectionResultQuestion electionResultQuestion =
        new ElectionResultQuestion(QUESTION.getId(), results);
    ElectionResult electionResult =
        new ElectionResult(Collections.singletonList(electionResultQuestion));
    MessageGeneral message = new MessageGeneral(SENDER_KEY, electionResult, gson);

    // Call the message handler
    messageHandler.handleMessage(messageSender, electionChannel, message);
  }

  @Test
  public void testHandleElectionSetup()
      throws DataHandlingException, UnknownLaoException, UnknownRollCallException,
          UnknownElectionException {
    MessageID messageID = handleElectionSetup(OPEN_BALLOT_ELECTION);

    // Check the Election is present and has correct values
    Election election = electionRepo.getElectionByChannel(OPEN_BALLOT_ELECTION.getChannel());
    assertEquals(EventState.CREATED, election.getState());
    assertEquals(OPEN_BALLOT_ELECTION.getId(), election.getId());
    assertEquals(OPEN_BALLOT_ELECTION.getElectionQuestions(), election.getElectionQuestions());
    assertEquals(OPEN_BALLOT_ELECTION.getElectionVersion(), election.getElectionVersion());
    assertEquals(OPEN_BALLOT_ELECTION.getStartTimestamp(), election.getStartTimestamp());
    assertEquals(OPEN_BALLOT_ELECTION.getCreation(), election.getCreation());
    assertEquals(OPEN_BALLOT_ELECTION.getEndTimestamp(), election.getEndTimestamp());

    // Check the WitnessMessage has been created
    Optional<WitnessMessage> witnessMessage =
        laoRepo.getLaoByChannel(LAO_CHANNEL).getWitnessMessage(messageID);
    assertTrue(witnessMessage.isPresent());

    // Check the Witness message contains the expected title and description
    WitnessMessage expectedMessage = electionSetupWitnessMessage(messageID, election);
    assertEquals(expectedMessage.getTitle(), witnessMessage.get().getTitle());
    assertEquals(expectedMessage.getDescription(), witnessMessage.get().getDescription());
  }

  @Test
  public void testElectionKey()
      throws DataHandlingException, UnknownLaoException, UnknownRollCallException,
          UnknownElectionException {

    handleElectionSetup(OPEN_BALLOT_ELECTION);
    handleElectionKey(OPEN_BALLOT_ELECTION, ELECTION_KEY);

    Election election = electionRepo.getElectionByChannel(OPEN_BALLOT_ELECTION.getChannel());
    assertEquals(ELECTION_KEY, election.getElectionKey());
  }

  @Test
  public void testHandleElectionResult()
      throws DataHandlingException, UnknownLaoException, UnknownRollCallException,
          UnknownElectionException {
    Set<QuestionResult> results = Collections.singleton(new QuestionResult(OPTION_1, 1));

    handleElectionSetup(OPEN_BALLOT_ELECTION);
    handleElectionKey(OPEN_BALLOT_ELECTION, ELECTION_KEY);
    handleElectionOpen(OPEN_BALLOT_ELECTION);
    handleCastVote(new ElectionVote(QUESTION.getId(), 0, false, null, ELECTION_ID), SENDER_KEY);
    handleElectionEnd();
    handleElectionResults(results, OPEN_BALLOT_ELECTION.getChannel());

    Election election = electionRepo.getElectionByChannel(OPEN_BALLOT_ELECTION.getChannel());
    assertEquals(EventState.RESULTS_READY, election.getState());
    assertEquals(results, election.getResultsForQuestionId(QUESTION.getId()));
  }

  @Test
  public void testHandleElectionOpen()
      throws DataHandlingException, UnknownLaoException, UnknownRollCallException,
          UnknownElectionException {
    handleElectionSetup(OPEN_BALLOT_ELECTION);
    handleElectionKey(OPEN_BALLOT_ELECTION, ELECTION_KEY);
    handleElectionOpen(OPEN_BALLOT_ELECTION);

    Election election = electionRepo.getElectionByChannel(OPEN_BALLOT_ELECTION.getChannel());
    assertEquals(OPENED_AT, election.getStartTimestamp());
    assertEquals(EventState.OPENED, election.getState());
  }

  @Test
  public void testHandleElectionEnd()
      throws DataHandlingException, UnknownLaoException, UnknownRollCallException,
          UnknownElectionException {
    handleElectionSetup(OPEN_BALLOT_ELECTION);
    handleElectionKey(OPEN_BALLOT_ELECTION, ELECTION_KEY);
    handleElectionOpen(OPEN_BALLOT_ELECTION);
    handleElectionEnd();

    Election election = electionRepo.getElectionByChannel(OPEN_BALLOT_ELECTION.getChannel());
    assertEquals(EventState.CLOSED, election.getState());
  }

  @Test
  public void castVoteWithOpenBallotScenario()
      throws UnknownElectionException, UnknownRollCallException, UnknownLaoException,
          DataHandlingException {
    ElectionVote vote1 = new ElectionVote(QUESTION.getId(), 0, false, null, ELECTION_ID);
    ElectionVote vote2 = new ElectionVote(QUESTION.getId(), 1, false, null, ELECTION_ID);

    handleElectionSetup(OPEN_BALLOT_ELECTION);
    handleElectionKey(OPEN_BALLOT_ELECTION, ELECTION_KEY);
    handleElectionOpen(OPEN_BALLOT_ELECTION);
    MessageID vote1Id = handleCastVote(vote1, SENDER_KEY);
    MessageID vote2Id = handleCastVote(vote2, ATTENDEE_KEY);

    Election election = electionRepo.getElectionByChannel(OPEN_BALLOT_ELECTION.getChannel());
    // Sort the vote ids based on the message id
    String[] voteIds =
        vote1Id.getEncoded().compareTo(vote2Id.getEncoded()) < 0
            ? new String[] {vote1.getId(), vote2.getId()}
            : new String[] {vote2.getId(), vote1.getId()};

    assertEquals(Hash.hash(voteIds), election.computerRegisteredVotesHash());
  }

  @Test
  public void castVoteWithSecretBallotScenario()
      throws UnknownElectionException, UnknownRollCallException, UnknownLaoException,
          DataHandlingException {
    ElectionKeyPair keys = ElectionKeyPair.generateKeyPair();
    ElectionPublicKey pubKey = keys.getEncryptionScheme();
    Base64URLData encodedKey = new Base64URLData(pubKey.getPublicKey().toBytes());

    ElectionEncryptedVote vote1 =
        new ElectionEncryptedVote(QUESTION.getId(), "0", false, null, ELECTION_ID);
    ElectionEncryptedVote vote2 =
        new ElectionEncryptedVote(QUESTION.getId(), "1", false, null, ELECTION_ID);

    handleElectionSetup(SECRET_BALLOT_ELECTION);
    handleElectionKey(SECRET_BALLOT_ELECTION, encodedKey.getEncoded());
    handleElectionOpen(SECRET_BALLOT_ELECTION);

    MessageID vote1Id = handleCastVote(vote1, SENDER_KEY);
    MessageID vote2Id = handleCastVote(vote2, ATTENDEE_KEY);

    Election election = electionRepo.getElectionByChannel(OPEN_BALLOT_ELECTION.getChannel());
    // Sort the vote ids based on the message id
    String[] voteIds =
        vote1Id.getEncoded().compareTo(vote2Id.getEncoded()) < 0
            ? new String[] {vote1.getId(), vote2.getId()}
            : new String[] {vote2.getId(), vote1.getId()};

    assertEquals(Hash.hash(voteIds), election.computerRegisteredVotesHash());
  }
}
