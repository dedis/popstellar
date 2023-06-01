package com.github.dedis.popstellar.utility.handler;

import android.app.Application;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

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
import com.github.dedis.popstellar.repository.database.AppDatabase;
import com.github.dedis.popstellar.repository.database.event.election.ElectionDao;
import com.github.dedis.popstellar.repository.database.event.election.ElectionEntity;
import com.github.dedis.popstellar.repository.database.lao.LAODao;
import com.github.dedis.popstellar.repository.database.lao.LAOEntity;
import com.github.dedis.popstellar.repository.database.message.MessageDao;
import com.github.dedis.popstellar.repository.database.message.MessageEntity;
import com.github.dedis.popstellar.repository.database.witnessing.*;
import com.github.dedis.popstellar.repository.remote.MessageSender;
import com.github.dedis.popstellar.utility.error.*;
import com.github.dedis.popstellar.utility.error.keys.NoRollCallException;
import com.github.dedis.popstellar.utility.security.Hash;
import com.github.dedis.popstellar.utility.security.KeyManager;
import com.google.gson.Gson;

import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.reactivex.Completable;
import io.reactivex.Single;

import static com.github.dedis.popstellar.testutils.Base64DataUtils.generateKeyPair;
import static com.github.dedis.popstellar.testutils.Base64DataUtils.generateRandomBase64String;
import static com.github.dedis.popstellar.utility.handler.data.ElectionHandler.electionSetupWitnessMessage;
import static java.util.Collections.singletonList;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@RunWith(AndroidJUnit4.class)
public class ElectionHandlerTest {

  private static final KeyPair SENDER_KEY = generateKeyPair();
  private static final PublicKey SENDER = SENDER_KEY.getPublicKey();
  private static final KeyPair ATTENDEE_KEY = generateKeyPair();
  private static final String LAO_NAME = "lao name";
  private static final long CREATION = Instant.now().getEpochSecond() - 100;
  private static final String LAO_ID = Lao.generateLaoId(SENDER, CREATION, LAO_NAME);
  private static final CreateLao CREATE_LAO =
      new CreateLao(LAO_ID, LAO_NAME, CREATION, SENDER, new ArrayList<>());
  private static final Lao LAO = new Lao(LAO_NAME, SENDER, CREATION);
  private static final Channel LAO_CHANNEL = Channel.ROOT.subChannel(LAO.getId());

  private static final long CREATED_AT = CREATION + 10; // 10 seconds later
  private static final long STARTED_AT = CREATION + 20; // 20 seconds later
  private static final long OPENED_AT = CREATION + 30; // 30 seconds later
  private static final long END_AT = CREATION + 60; // 60 seconds later

  private static final String ELECTION_NAME = "Election Name";
  private static final String ELECTION_ID =
      Election.generateElectionSetupId(LAO.getId(), CREATED_AT, ELECTION_NAME);
  private static final String OPTION_1 = "Yes";
  private static final String OPTION_2 = "No";
  private static final ElectionQuestion QUESTION =
      new ElectionQuestion(
          ELECTION_ID,
          new Question("Does this work ?", "Plurality", Arrays.asList(OPTION_1, OPTION_2), false));
  private static final PlainVote VOTE1 =
      new PlainVote(QUESTION.getId(), 0, false, null, ELECTION_ID);
  private static final PlainVote VOTE2 =
      new PlainVote(QUESTION.getId(), 1, false, null, ELECTION_ID);
  private static final PlainVote VOTE3 =
      new PlainVote(QUESTION.getId(), 0, false, null, ELECTION_ID);
  private static final String ELECTION_KEY = generateRandomBase64String();
  private static final Election OPEN_BALLOT_ELECTION =
      new Election.ElectionBuilder(LAO.getId(), CREATED_AT, ELECTION_NAME)
          .setElectionVersion(ElectionVersion.OPEN_BALLOT)
          .setElectionQuestions(singletonList(QUESTION))
          .setStart(STARTED_AT)
          .setEnd(END_AT)
          .build();

  private static final Election SECRET_BALLOT_ELECTION =
      OPEN_BALLOT_ELECTION.builder().setElectionVersion(ElectionVersion.SECRET_BALLOT).build();

  private ElectionRepository electionRepo;
  private WitnessingRepository witnessingRepository;
  private MessageHandler messageHandler;
  private MessageRepository messageRepo;
  private Gson gson;

  @Mock AppDatabase appDatabase;
  @Mock LAODao laoDao;
  @Mock MessageDao messageDao;
  @Mock ElectionDao electionDao;
  @Mock WitnessingDao witnessingDao;
  @Mock WitnessDao witnessDao;
  @Mock MessageSender messageSender;
  @Mock KeyManager keyManager;

  @Rule public InstantTaskExecutorRule rule = new InstantTaskExecutorRule();

  @Before
  public void setup() throws GeneralSecurityException, IOException {
    MockitoAnnotations.openMocks(this);
    Application application = ApplicationProvider.getApplicationContext();

    lenient().when(keyManager.getMainKeyPair()).thenReturn(SENDER_KEY);
    lenient().when(keyManager.getMainPublicKey()).thenReturn(SENDER);

    when(messageSender.subscribe(any())).then(args -> Completable.complete());

    when(appDatabase.laoDao()).thenReturn(laoDao);
    when(laoDao.getAllLaos()).thenReturn(Single.just(new ArrayList<>()));
    when(laoDao.insert(any(LAOEntity.class))).thenReturn(Completable.complete());

    when(appDatabase.messageDao()).thenReturn(messageDao);
    when(messageDao.takeFirstNMessages(anyInt())).thenReturn(Single.just(new ArrayList<>()));
    when(messageDao.insert(any(MessageEntity.class))).thenReturn(Completable.complete());
    when(messageDao.getMessageById(any(MessageID.class))).thenReturn(null);

    when(appDatabase.electionDao()).thenReturn(electionDao);
    when(electionDao.insert(any(ElectionEntity.class))).thenReturn(Completable.complete());
    when(electionDao.getElectionsByLaoId(anyString())).thenReturn(Single.just(new ArrayList<>()));

    when(appDatabase.witnessDao()).thenReturn(witnessDao);
    when(witnessDao.getWitnessesByLao(anyString())).thenReturn(Single.just(new ArrayList<>()));
    when(witnessDao.insertAll(any())).thenReturn(Completable.complete());
    when(witnessDao.isWitness(anyString(), any(PublicKey.class))).thenReturn(0);

    when(appDatabase.witnessingDao()).thenReturn(witnessingDao);
    when(witnessingDao.getWitnessMessagesByLao(anyString()))
        .thenReturn(Single.just(new ArrayList<>()));
    when(witnessingDao.insert(any(WitnessingEntity.class))).thenReturn(Completable.complete());
    when(witnessingDao.deleteMessagesByIds(anyString(), any())).thenReturn(Completable.complete());

    LAORepository laoRepo = new LAORepository(appDatabase, application);
    electionRepo = new ElectionRepository(appDatabase, application);
    witnessingRepository =
        new WitnessingRepository(
            appDatabase,
            application,
            rollCallRepository,
            electionRepository,
            meetingRepository,
            digitalCashRepository);
    messageRepo = new MessageRepository(appDatabase, application);

    DataRegistry dataRegistry =
        DataRegistryModuleHelper.buildRegistry(
            laoRepo, electionRepo, witnessingRepository, keyManager, messageRepo);

    gson = JsonModule.provideGson(dataRegistry);
    messageHandler = new MessageHandler(messageRepo, dataRegistry);

    laoRepo.updateLao(LAO);

    // Add the CreateLao message to the LAORepository
    MessageGeneral createLaoMessage = new MessageGeneral(SENDER_KEY, CREATE_LAO, gson);
    messageRepo.addMessage(createLaoMessage, true, true);
  }

  private MessageID handleElectionSetup(Election election, Channel channel)
      throws UnknownElectionException, UnknownRollCallException, UnknownLaoException,
          DataHandlingException, NoRollCallException, UnknownWitnessMessageException {
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
    messageHandler.handleMessage(messageSender, channel, message);
    return message.getMessageId();
  }

  private void handleElectionKey(Election election, String key)
      throws UnknownElectionException, UnknownRollCallException, UnknownLaoException,
          DataHandlingException, NoRollCallException, UnknownWitnessMessageException {
    // Create the election key message
    ElectionKey electionKey = new ElectionKey(election.getId(), key);

    MessageGeneral message = new MessageGeneral(SENDER_KEY, electionKey, gson);
    messageHandler.handleMessage(messageSender, election.getChannel(), message);
  }

  private void handleElectionOpen(Election election)
      throws UnknownElectionException, UnknownRollCallException, UnknownLaoException,
          DataHandlingException, NoRollCallException, UnknownWitnessMessageException {
    ElectionOpen electionOpen = new ElectionOpen(LAO.getId(), election.getId(), OPENED_AT);

    MessageGeneral message = new MessageGeneral(SENDER_KEY, electionOpen, gson);
    messageHandler.handleMessage(messageSender, election.getChannel(), message);
  }

  private MessageID handleCastVote(Vote vote, KeyPair senderKey, Long creation)
      throws UnknownElectionException, UnknownRollCallException, UnknownLaoException,
          DataHandlingException, NoRollCallException, UnknownWitnessMessageException {

    CastVote castVote =
        new CastVote(
            singletonList(vote), OPEN_BALLOT_ELECTION.getId(), CREATE_LAO.getId(), creation);

    MessageGeneral message = new MessageGeneral(senderKey, castVote, gson);
    messageHandler.handleMessage(messageSender, OPEN_BALLOT_ELECTION.getChannel(), message);
    return message.getMessageId();
  }

  private void handleElectionEnd()
      throws UnknownElectionException, UnknownRollCallException, UnknownLaoException,
          DataHandlingException, NoRollCallException, UnknownWitnessMessageException {
    // Retrieve current election to use the correct vote hash
    Election current = electionRepo.getElection(LAO.getId(), ELECTION_ID);
    ElectionEnd endElection =
        new ElectionEnd(LAO.getId(), ELECTION_ID, current.computeRegisteredVotesHash());

    MessageGeneral message = new MessageGeneral(SENDER_KEY, endElection, gson);
    messageHandler.handleMessage(messageSender, OPEN_BALLOT_ELECTION.getChannel(), message);
  }

  private void handleElectionResults(Set<QuestionResult> results, Channel electionChannel)
      throws UnknownElectionException, UnknownRollCallException, UnknownLaoException,
          DataHandlingException, NoRollCallException, UnknownWitnessMessageException {
    ElectionResultQuestion electionResultQuestion =
        new ElectionResultQuestion(QUESTION.getId(), results);
    ElectionResult electionResult = new ElectionResult(singletonList(electionResultQuestion));
    MessageGeneral message = new MessageGeneral(SENDER_KEY, electionResult, gson);

    // Call the message handler
    messageHandler.handleMessage(messageSender, electionChannel, message);
  }

  @Test
  public void testHandleElectionSetup()
      throws DataHandlingException, UnknownLaoException, UnknownRollCallException,
          UnknownElectionException, NoRollCallException, UnknownWitnessMessageException {
    MessageID messageID = handleElectionSetup(OPEN_BALLOT_ELECTION, LAO_CHANNEL);

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
        witnessingRepository.getWitnessMessage(LAO_ID, messageID);
    assertTrue(witnessMessage.isPresent());

    // Check the Witness message contains the expected title and description
    WitnessMessage expectedMessage = electionSetupWitnessMessage(messageID, election);
    assertEquals(expectedMessage.getTitle(), witnessMessage.get().getTitle());
    assertEquals(expectedMessage.getDescription(), witnessMessage.get().getDescription());
  }

  @Test
  public void testHandleElectionSetupInvalidChannel() {
    // Check that handling the message fails with an invalid channel (channel that is not a lao
    // sub-channel)
    assertThrows(
        InvalidChannelException.class,
        () -> handleElectionSetup(OPEN_BALLOT_ELECTION, Channel.ROOT));
  }

  @Test
  public void testElectionKey()
      throws DataHandlingException, UnknownLaoException, UnknownRollCallException,
          UnknownElectionException, NoRollCallException, UnknownWitnessMessageException {

    handleElectionSetup(OPEN_BALLOT_ELECTION, LAO_CHANNEL);
    handleElectionKey(OPEN_BALLOT_ELECTION, ELECTION_KEY);

    Election election = electionRepo.getElectionByChannel(OPEN_BALLOT_ELECTION.getChannel());
    assertEquals(ELECTION_KEY, election.getElectionKey());
  }

  @Test
  public void testHandleElectionResult()
      throws DataHandlingException, UnknownLaoException, UnknownRollCallException,
          UnknownElectionException, NoRollCallException, UnknownWitnessMessageException {
    Set<QuestionResult> results = Collections.singleton(new QuestionResult(OPTION_1, 1));

    handleElectionSetup(OPEN_BALLOT_ELECTION, LAO_CHANNEL);
    handleElectionKey(OPEN_BALLOT_ELECTION, ELECTION_KEY);
    handleElectionOpen(OPEN_BALLOT_ELECTION);
    handleCastVote(
        new PlainVote(QUESTION.getId(), 0, false, null, ELECTION_ID), SENDER_KEY, OPENED_AT);
    handleElectionEnd();
    handleElectionResults(results, OPEN_BALLOT_ELECTION.getChannel());

    Election election = electionRepo.getElectionByChannel(OPEN_BALLOT_ELECTION.getChannel());
    assertEquals(EventState.RESULTS_READY, election.getState());
    assertEquals(results, election.getResultsForQuestionId(QUESTION.getId()));
  }

  @Test
  public void testHandleElectionOpen()
      throws DataHandlingException, UnknownLaoException, UnknownRollCallException,
          UnknownElectionException, NoRollCallException, UnknownWitnessMessageException {
    handleElectionSetup(OPEN_BALLOT_ELECTION, LAO_CHANNEL);
    handleElectionKey(OPEN_BALLOT_ELECTION, ELECTION_KEY);
    handleElectionOpen(OPEN_BALLOT_ELECTION);

    Election election = electionRepo.getElectionByChannel(OPEN_BALLOT_ELECTION.getChannel());
    assertEquals(OPENED_AT, election.getStartTimestamp());
    assertEquals(EventState.OPENED, election.getState());
  }

  @Test
  public void testHandleElectionOpenInvalidState()
      throws DataHandlingException, UnknownLaoException, UnknownRollCallException,
          UnknownElectionException, NoRollCallException, UnknownWitnessMessageException {
    handleElectionSetup(OPEN_BALLOT_ELECTION, LAO_CHANNEL);
    handleElectionKey(OPEN_BALLOT_ELECTION, ELECTION_KEY);

    // Set the election to a closed state and check that opening the election fails
    Election closedElection = OPEN_BALLOT_ELECTION.builder().setState(EventState.CLOSED).build();
    electionRepo.updateElection(closedElection);
    assertThrows(InvalidStateException.class, () -> handleElectionOpen(closedElection));

    // Set the election to an opened state and check that opening the election fails
    Election openedElection = OPEN_BALLOT_ELECTION.builder().setState(EventState.OPENED).build();
    electionRepo.updateElection(closedElection);
    assertThrows(InvalidStateException.class, () -> handleElectionOpen(openedElection));

    // Set the election to a result ready state and check that opening the election fails
    Election resultsReadyElection =
        OPEN_BALLOT_ELECTION.builder().setState(EventState.RESULTS_READY).build();
    electionRepo.updateElection(closedElection);
    assertThrows(InvalidStateException.class, () -> handleElectionOpen(resultsReadyElection));
  }

  @Test
  public void testHandleElectionEnd()
      throws DataHandlingException, UnknownLaoException, UnknownRollCallException,
          UnknownElectionException, NoRollCallException, UnknownWitnessMessageException {
    handleElectionSetup(OPEN_BALLOT_ELECTION, LAO_CHANNEL);
    handleElectionKey(OPEN_BALLOT_ELECTION, ELECTION_KEY);
    handleElectionOpen(OPEN_BALLOT_ELECTION);
    handleElectionEnd();

    Election election = electionRepo.getElectionByChannel(OPEN_BALLOT_ELECTION.getChannel());
    assertEquals(EventState.CLOSED, election.getState());
  }

  @Test
  public void castVoteWithOpenBallotScenario()
      throws UnknownElectionException, UnknownRollCallException, UnknownLaoException,
          DataHandlingException, NoRollCallException, UnknownWitnessMessageException {
    handleElectionSetup(OPEN_BALLOT_ELECTION, LAO_CHANNEL);
    handleElectionKey(OPEN_BALLOT_ELECTION, ELECTION_KEY);
    handleElectionOpen(OPEN_BALLOT_ELECTION);

    // Handle the votes of two different senders
    handleCastVote(VOTE1, SENDER_KEY, OPENED_AT);
    handleCastVote(VOTE2, ATTENDEE_KEY, OPENED_AT);

    // The expected hash is made on the sorted vote ids
    String[] voteIds = Stream.of(VOTE1, VOTE2).map(Vote::getId).sorted().toArray(String[]::new);
    Election election = electionRepo.getElectionByChannel(OPEN_BALLOT_ELECTION.getChannel());

    assertEquals(Hash.hash(voteIds), election.computeRegisteredVotesHash());
  }

  @Test
  public void castVoteOnlyKeepsLastVote()
      throws UnknownElectionException, UnknownRollCallException, UnknownLaoException,
          DataHandlingException, NoRollCallException, UnknownWitnessMessageException {
    handleElectionSetup(OPEN_BALLOT_ELECTION, LAO_CHANNEL);
    handleElectionKey(OPEN_BALLOT_ELECTION, ELECTION_KEY);
    handleElectionOpen(OPEN_BALLOT_ELECTION);

    // Handle the votes of two different senders. One sender sends two votes, and the first should
    // be discarded
    handleCastVote(VOTE1, SENDER_KEY, OPENED_AT);
    handleCastVote(VOTE2, ATTENDEE_KEY, OPENED_AT + 1);
    handleCastVote(VOTE3, ATTENDEE_KEY, OPENED_AT + 2);

    // The expected hash is made on the sorted vote ids (check that vote2 was discarded)
    String[] voteIds = Stream.of(VOTE1, VOTE3).map(Vote::getId).sorted().toArray(String[]::new);
    Election election = electionRepo.getElectionByChannel(OPEN_BALLOT_ELECTION.getChannel());

    assertEquals(Hash.hash(voteIds), election.computeRegisteredVotesHash());
  }

  @Test
  public void castVoteDiscardsStaleVote()
      throws UnknownElectionException, UnknownRollCallException, UnknownLaoException,
          DataHandlingException, NoRollCallException, UnknownWitnessMessageException {
    handleElectionSetup(OPEN_BALLOT_ELECTION, LAO_CHANNEL);
    handleElectionKey(OPEN_BALLOT_ELECTION, ELECTION_KEY);
    handleElectionOpen(OPEN_BALLOT_ELECTION);

    // Handle the votes of two different senders. One sender sends two votes, and the second should
    // be discarded as it has an older creation date
    handleCastVote(VOTE1, SENDER_KEY, OPENED_AT);
    handleCastVote(VOTE2, ATTENDEE_KEY, OPENED_AT + 2);
    handleCastVote(VOTE3, ATTENDEE_KEY, OPENED_AT + 1);

    // The expected hash is made on the sorted vote ids (check that vote3 was discarded)
    String[] voteIds = Stream.of(VOTE1, VOTE2).map(Vote::getId).sorted().toArray(String[]::new);
    Election election = electionRepo.getElectionByChannel(OPEN_BALLOT_ELECTION.getChannel());

    assertEquals(Hash.hash(voteIds), election.computeRegisteredVotesHash());
  }

  @Test
  public void castVoteFailsOnPreviousMessageDataNull()
      throws UnknownElectionException, UnknownRollCallException, UnknownLaoException,
          DataHandlingException, NoRollCallException, UnknownWitnessMessageException {
    // This test checks that the handler fails if the messageMap of the election already has a
    // message (previously sent by the same sender) that contains null data.
    handleElectionSetup(OPEN_BALLOT_ELECTION, LAO_CHANNEL);
    handleElectionKey(OPEN_BALLOT_ELECTION, ELECTION_KEY);
    handleElectionOpen(OPEN_BALLOT_ELECTION);

    // Create an invalid message with null data and add it to the message repo
    MessageGeneral nullData = new MessageGeneral(SENDER_KEY, null, gson);
    messageRepo.addMessage(nullData, false, true);

    // Update the messageMap in this election to contain the invalid message
    Election prevElection = electionRepo.getElectionByChannel(OPEN_BALLOT_ELECTION.getChannel());
    Election updatedElection =
        prevElection.builder().updateMessageMap(SENDER, nullData.getMessageId()).build();
    electionRepo.updateElection(updatedElection);

    // Check that handling the message fails
    assertThrows(
        IllegalStateException.class, () -> handleCastVote(VOTE1, SENDER_KEY, OPENED_AT + 1));
  }

  @Test
  public void castVoteFailsOnPreviousMessageDataInvalid()
      throws UnknownElectionException, UnknownRollCallException, UnknownLaoException,
          DataHandlingException, NoRollCallException, UnknownWitnessMessageException {
    // This test checks that the handler fails if the messageMap of the election already has a
    // message (previously sent by the same sender) that contains data that is not a CastVote.
    handleElectionSetup(OPEN_BALLOT_ELECTION, LAO_CHANNEL);
    handleElectionKey(OPEN_BALLOT_ELECTION, ELECTION_KEY);
    handleElectionOpen(OPEN_BALLOT_ELECTION);

    // Create an invalid message with data that is not a CastVote and add it to the message repo
    MessageGeneral invalidData = new MessageGeneral(SENDER_KEY, CREATE_LAO, gson);
    messageRepo.addMessage(invalidData, true, true);

    // Update the messageMap in this election to contain the invalid message
    Election prevElection = electionRepo.getElectionByChannel(OPEN_BALLOT_ELECTION.getChannel());
    Election updatedElection =
        prevElection.builder().updateMessageMap(SENDER, invalidData.getMessageId()).build();
    electionRepo.updateElection(updatedElection);

    // Check that handling the message fails
    assertThrows(
        DataHandlingException.class, () -> handleCastVote(VOTE1, SENDER_KEY, OPENED_AT + 1));
  }

  @Test
  public void castVoteIgnoresVoteOnClosedElection()
      throws UnknownElectionException, UnknownRollCallException, UnknownLaoException,
          DataHandlingException, NoRollCallException, UnknownWitnessMessageException {
    handleElectionSetup(OPEN_BALLOT_ELECTION, LAO_CHANNEL);
    handleElectionKey(OPEN_BALLOT_ELECTION, ELECTION_KEY);
    handleElectionOpen(OPEN_BALLOT_ELECTION);
    MessageID messageID = handleCastVote(VOTE1, SENDER_KEY, OPENED_AT);
    handleElectionEnd();
    handleCastVote(VOTE2, SENDER_KEY, END_AT + 1);

    // The expected message map kept the first vote
    Election election = electionRepo.getElectionByChannel(OPEN_BALLOT_ELECTION.getChannel());
    HashMap<PublicKey, MessageID> expectedMessageMap = new HashMap<>();
    expectedMessageMap.put(SENDER, messageID);

    assertEquals(expectedMessageMap, election.getMessageMap());
  }

  @Test
  public void castVoteWithSecretBallotScenario()
      throws UnknownElectionException, UnknownRollCallException, UnknownLaoException,
          DataHandlingException, NoRollCallException, UnknownWitnessMessageException {
    ElectionKeyPair keys = ElectionKeyPair.generateKeyPair();
    ElectionPublicKey pubKey = keys.getEncryptionScheme();
    Base64URLData encodedKey = new Base64URLData(pubKey.getPublicKey().toBytes());

    EncryptedVote vote1 = new EncryptedVote(QUESTION.getId(), "0", false, null, ELECTION_ID);
    EncryptedVote vote2 = new EncryptedVote(QUESTION.getId(), "1", false, null, ELECTION_ID);

    handleElectionSetup(SECRET_BALLOT_ELECTION, LAO_CHANNEL);
    handleElectionKey(SECRET_BALLOT_ELECTION, encodedKey.getEncoded());
    handleElectionOpen(SECRET_BALLOT_ELECTION);

    handleCastVote(vote1, SENDER_KEY, OPENED_AT);
    handleCastVote(vote2, ATTENDEE_KEY, OPENED_AT);

    // The expected hash is made on the sorted vote ids
    String[] voteIds = Stream.of(vote1, vote2).map(Vote::getId).sorted().toArray(String[]::new);
    Election election = electionRepo.getElectionByChannel(OPEN_BALLOT_ELECTION.getChannel());

    assertEquals(Hash.hash(voteIds), election.computeRegisteredVotesHash());
  }
}
