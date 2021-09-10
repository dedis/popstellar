package com.github.dedis.popstellar.utility.handler;

import static com.github.dedis.popstellar.utility.handler.MessageHandler.handleMessage;

import com.github.dedis.popstellar.Injection;
import com.github.dedis.popstellar.model.Election;
import com.github.dedis.popstellar.model.Lao;
import com.github.dedis.popstellar.model.RollCall;
import com.github.dedis.popstellar.model.WitnessMessage;
import com.github.dedis.popstellar.model.data.LAOLocalDataSource;
import com.github.dedis.popstellar.model.data.LAORemoteDataSource;
import com.github.dedis.popstellar.model.data.LAORepository;
import com.github.dedis.popstellar.model.data.LAOState;
import com.github.dedis.popstellar.model.event.EventState;
import com.github.dedis.popstellar.model.network.GenericMessage;
import com.github.dedis.popstellar.model.network.answer.Result;
import com.github.dedis.popstellar.model.network.method.message.MessageGeneral;
import com.github.dedis.popstellar.model.network.method.message.data.Data;
import com.github.dedis.popstellar.model.network.method.message.data.ElectionQuestion;
import com.github.dedis.popstellar.model.network.method.message.data.ElectionResultQuestion;
import com.github.dedis.popstellar.model.network.method.message.data.QuestionResult;
import com.github.dedis.popstellar.model.network.method.message.data.election.ElectionEnd;
import com.github.dedis.popstellar.model.network.method.message.data.election.ElectionResult;
import com.github.dedis.popstellar.model.network.method.message.data.election.ElectionSetup;
import com.github.dedis.popstellar.model.network.method.message.data.lao.CreateLao;
import com.github.dedis.popstellar.model.network.method.message.data.lao.StateLao;
import com.github.dedis.popstellar.model.network.method.message.data.lao.UpdateLao;
import com.github.dedis.popstellar.model.network.method.message.data.rollcall.CloseRollCall;
import com.github.dedis.popstellar.model.network.method.message.data.rollcall.CreateRollCall;
import com.github.dedis.popstellar.model.network.method.message.data.rollcall.OpenRollCall;
import com.github.dedis.popstellar.utility.scheduler.SchedulerProvider;
import com.github.dedis.popstellar.utility.scheduler.TestSchedulerProvider;
import com.google.crypto.tink.PublicKeySign;
import com.google.crypto.tink.integration.android.AndroidKeysetManager;
import io.reactivex.Observable;
import io.reactivex.schedulers.TestScheduler;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import junit.framework.TestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class MessageHandlerTest extends TestCase {

  @Mock
  LAORemoteDataSource remoteDataSource;

  @Mock
  LAOLocalDataSource localDataSource;

  @Mock
  AndroidKeysetManager androidKeysetManager;

  @Mock
  PublicKeySign signer;

  private static final int REQUEST_ID = 42;
  private static final int RESPONSE_DELAY = 1000;
  private static final CreateLao CREATE_LAO = new CreateLao("lao",
      "Z3DYtBxooGs6KxOAqCWD3ihR8M6ZPBjAmWp_w5VBaws=");
  private static final String CHANNEL = "/root";
  private static final String LAO_CHANNEL = CHANNEL + "/" + CREATE_LAO.getId();

  private Lao lao;
  private RollCall rollCall;
  private Election election;
  private ElectionQuestion electionQuestion;
  private LAORepository laoRepository;
  private MessageGeneral createLaoMessage;

  @Before
  public void setup() throws GeneralSecurityException {
    SchedulerProvider testSchedulerProvider = new TestSchedulerProvider();
    TestScheduler testScheduler = (TestScheduler) testSchedulerProvider.io();

    // Mock the signing of of any data for the MessageGeneral constructor
    byte[] dataBuf = Injection.provideGson().toJson(CREATE_LAO, Data.class).getBytes();
    Mockito.when(signer.sign(Mockito.any())).thenReturn(dataBuf);
    createLaoMessage = new MessageGeneral(Base64.getUrlDecoder().decode(CREATE_LAO.getOrganizer()),
        CREATE_LAO, signer, Injection.provideGson());

    // Simulate a network response from the server after the response delay
    Observable<GenericMessage> upstream = Observable.fromArray(
        (GenericMessage) new Result(REQUEST_ID))
        .delay(RESPONSE_DELAY, TimeUnit.MILLISECONDS, testScheduler);

    Mockito.when(remoteDataSource.observeMessage()).thenReturn(upstream);
    Mockito.when(remoteDataSource.observeWebsocket()).thenReturn(Observable.empty());

    laoRepository = LAORepository
        .getInstance(remoteDataSource, localDataSource, androidKeysetManager,
            Injection.provideGson(), testSchedulerProvider);

    // Create one LAO to the LAORepository
    lao = new Lao(CREATE_LAO.getName(), CREATE_LAO.getOrganizer(), CREATE_LAO.getCreation());
    lao.setLastModified(lao.getCreation());

    // Create one Roll Call and add it to the LAO
    rollCall = new RollCall(lao.getId(), Instant.now().getEpochSecond(), "roll call 1");
    HashMap<String, RollCall> rollCalls = new HashMap<>();
    rollCalls.put(rollCall.getId(), rollCall);
    lao.setRollCalls(rollCalls);

    // Create one Election and add it to the LAO
    election = new Election(lao.getId(), Instant.now().getEpochSecond(), "election 1");
    election.setStart(Instant.now().getEpochSecond());
    election.setEnd(Instant.now().getEpochSecond() + 20L);
    election.setChannel(lao.getChannel() + "/" + election.getId());
    electionQuestion = new ElectionQuestion("question", "voting method", false,
        Collections.singletonList("a"), election.getId());
    election.setElectionQuestions(Collections.singletonList(electionQuestion));
    HashMap<String, Election> elections = new HashMap<>();
    elections.put(election.getId(), election);
    lao.setElections(elections);

    // Add the LAO to the LAORepository
    laoRepository.getLaoById().put(LAO_CHANNEL, new LAOState(lao));
    laoRepository.setAllLaoSubject();

    // Add the CreateLao message to the LAORepository
    laoRepository.getMessageById().put(createLaoMessage.getMessageId(), createLaoMessage);
  }

  @After
  public void destroy() {
    // Ensure every test has a new LAORepository instance with a different TestSchedulerProvider
    LAORepository.destroyInstance();
  }

  @Test
  public void testHandleUpdateLao() {
    // Create the update LAO message
    UpdateLao updateLao = new UpdateLao(CREATE_LAO.getOrganizer(), CREATE_LAO.getCreation(),
        "new name", Instant.now().getEpochSecond(), new HashSet<>());
    MessageGeneral message = new MessageGeneral(
        Base64.getUrlDecoder().decode(CREATE_LAO.getOrganizer()), updateLao, signer,
        Injection.provideGson());

    // Call the message handler
    assertFalse(handleMessage(laoRepository, LAO_CHANNEL, message));

    // Check the WitnessMessage has been created
    Optional<WitnessMessage> witnessMessage = laoRepository.getLaoByChannel(LAO_CHANNEL)
        .getWitnessMessage(message.getMessageId());
    assertTrue(witnessMessage.isPresent());
    assertEquals(LaoHandler.UPDATE_LAO, witnessMessage.get().getTitle());
    assertEquals(LaoHandler.OLD_NAME + CREATE_LAO.getName() + "\n" + LaoHandler.NEW_NAME + updateLao
            .getName() +
            "\n" + LaoHandler.MESSAGE_ID + message.getMessageId(),
        witnessMessage.get().getDescription());
  }

  @Test
  public void testHandleStateLao() {
    // Create the state LAO message
    StateLao stateLao = new StateLao(CREATE_LAO.getId(), CREATE_LAO.getName(),
        CREATE_LAO.getCreation(), Instant.now().getEpochSecond(), CREATE_LAO.getOrganizer(),
        createLaoMessage.getMessageId(), new HashSet<>(), new ArrayList<>());
    MessageGeneral message = new MessageGeneral(
        Base64.getUrlDecoder().decode(CREATE_LAO.getOrganizer()), stateLao, signer,
        Injection.provideGson());

    // Call the message handler
    assertFalse(handleMessage(laoRepository, LAO_CHANNEL, message));

    // Check the LAO last modification time and ID was updated
    assertEquals((Long) stateLao.getLastModified(),
        laoRepository.getLaoByChannel(LAO_CHANNEL).getLastModified());
    assertEquals(stateLao.getModificationId(),
        laoRepository.getLaoByChannel(LAO_CHANNEL).getModificationId());
  }

  @Test
  public void testHandleCreateRollCall() {
    // Create the create Roll Call message
    CreateRollCall createRollCall = new CreateRollCall("roll call 2", rollCall.getStart(),
        rollCall.getEnd(), rollCall.getLocation(), rollCall.getDescription(), CREATE_LAO.getId());
    MessageGeneral message = new MessageGeneral(
        Base64.getUrlDecoder().decode(CREATE_LAO.getOrganizer()), createRollCall, signer,
        Injection.provideGson());

    // Call the message handler
    assertFalse(handleMessage(laoRepository, LAO_CHANNEL, message));

    // Check the new Roll Call is present with state CREATED and the correct ID
    Optional<RollCall> rollCall = laoRepository.getLaoByChannel(LAO_CHANNEL)
        .getRollCall(createRollCall.getId());
    assertTrue(rollCall.isPresent());
    assertEquals(EventState.CREATED, rollCall.get().getState());
    assertEquals(createRollCall.getId(), rollCall.get().getId());

    // Check the WitnessMessage has been created
    Optional<WitnessMessage> witnessMessage = laoRepository.getLaoByChannel(LAO_CHANNEL)
        .getWitnessMessage(message.getMessageId());
    assertTrue(witnessMessage.isPresent());
    assertEquals(RollCallHandler.ROLL_CALL_CREATION, witnessMessage.get().getTitle());
    assertEquals(RollCallHandler.ROLL_CALL_NAME + createRollCall.getName() + "\n"
            + RollCallHandler.ROLL_CALL_ID + createRollCall.getId() + "\n"
            + RollCallHandler.ROLL_CALL_LOCATION + createRollCall.getLocation() + "\n"
            + RollCallHandler.MESSAGE_ID + message.getMessageId(),
        witnessMessage.get().getDescription());
  }

  @Test
  public void testHandleOpenRollCall() {
    // Create the open Roll Call message
    OpenRollCall openRollCall = new OpenRollCall(CREATE_LAO.getId(), rollCall.getId(),
        rollCall.getStart(), EventState.CREATED);
    MessageGeneral message = new MessageGeneral(
        Base64.getUrlDecoder().decode(CREATE_LAO.getOrganizer()), openRollCall, signer,
        Injection.provideGson());

    // Call the message handler
    assertFalse(handleMessage(laoRepository, LAO_CHANNEL, message));

    // Check the Roll Call is present with state OPENED and the correct ID
    Optional<RollCall> rollCallOpt = laoRepository.getLaoByChannel(LAO_CHANNEL)
        .getRollCall(openRollCall.getUpdateId());
    assertTrue(rollCallOpt.isPresent());
    assertEquals(EventState.OPENED, rollCallOpt.get().getState());
    assertEquals(openRollCall.getUpdateId(), rollCallOpt.get().getId());

    // Check the WitnessMessage has been created
    Optional<WitnessMessage> witnessMessage = laoRepository.getLaoByChannel(LAO_CHANNEL)
        .getWitnessMessage(message.getMessageId());
    assertTrue(witnessMessage.isPresent());
    assertEquals(RollCallHandler.ROLL_CALL_OPENING, witnessMessage.get().getTitle());
    assertEquals(RollCallHandler.ROLL_CALL_NAME + rollCall.getName() + "\n"
            + RollCallHandler.ROLL_CALL_UPDATED_ID + openRollCall.getUpdateId() + "\n"
            + RollCallHandler.MESSAGE_ID + message.getMessageId(),
        witnessMessage.get().getDescription());
  }

  @Test
  public void testHandleCloseRollCall() {
    // Create the close Roll Call message
    CloseRollCall closeRollCall = new CloseRollCall(CREATE_LAO.getId(), rollCall.getId(),
        rollCall.getEnd(), new ArrayList<>());
    MessageGeneral message = new MessageGeneral(
        Base64.getUrlDecoder().decode(CREATE_LAO.getOrganizer()), closeRollCall, signer,
        Injection.provideGson());

    // Call the message handler
    assertFalse(handleMessage(laoRepository, LAO_CHANNEL, message));

    // Check the Roll Call is present with state CLOSED and the correct ID
    Optional<RollCall> rollCallOpt = laoRepository.getLaoByChannel(LAO_CHANNEL)
        .getRollCall(closeRollCall.getUpdateId());
    assertTrue(rollCallOpt.isPresent());
    assertEquals(EventState.CLOSED, rollCallOpt.get().getState());
    assertEquals(closeRollCall.getUpdateId(), rollCallOpt.get().getId());

    // Check the WitnessMessage has been created
    Optional<WitnessMessage> witnessMessage = laoRepository.getLaoByChannel(LAO_CHANNEL)
        .getWitnessMessage(message.getMessageId());
    assertTrue(witnessMessage.isPresent());
    assertEquals(RollCallHandler.ROLL_CALL_DELETION, witnessMessage.get().getTitle());
    assertEquals(RollCallHandler.ROLL_CALL_NAME + rollCall.getName() + "\n"
            + RollCallHandler.ROLL_CALL_UPDATED_ID + closeRollCall.getUpdateId() + "\n"
            + RollCallHandler.MESSAGE_ID + message.getMessageId(),
        witnessMessage.get().getDescription());
  }

  @Test
  public void testHandleElectionSetup() {
    // Create the setup Election message
    ElectionSetup electionSetup = new ElectionSetup("election 2", election.getStartTimestamp(),
        election.getEndTimestamp(), Collections.singletonList(electionQuestion.getVotingMethod()),
        Collections.singletonList(electionQuestion.getWriteIn()),
        Collections.singletonList(electionQuestion.getBallotOptions()),
        Collections.singletonList(electionQuestion.getQuestion()), lao.getId());
    MessageGeneral message = new MessageGeneral(
        Base64.getUrlDecoder().decode(CREATE_LAO.getOrganizer()), electionSetup, signer,
        Injection.provideGson());

    // Call the message handler
    assertFalse(handleMessage(laoRepository, LAO_CHANNEL, message));

    // Check the Election is present with state OPENED and the correct ID
    Optional<Election> electionOpt = laoRepository.getLaoByChannel(LAO_CHANNEL)
        .getElection(electionSetup.getId());
    assertTrue(electionOpt.isPresent());
    assertEquals(EventState.OPENED, electionOpt.get().getState());
    assertEquals(electionSetup.getId(), electionOpt.get().getId());

    // Check the WitnessMessage has been created
    Optional<WitnessMessage> witnessMessage = laoRepository.getLaoByChannel(LAO_CHANNEL)
        .getWitnessMessage(message.getMessageId());
    assertTrue(witnessMessage.isPresent());
    assertEquals(ElectionHandler.ELECTION_SETUP, witnessMessage.get().getTitle());
    assertEquals(ElectionHandler.ELECTION_NAME + electionSetup.getName() + "\n"
            + ElectionHandler.ELECTION_ID + electionSetup.getId() + "\n"
            + ElectionHandler.ELECTION_QUESTION + electionQuestion.getQuestion()
            + "\n" + ElectionHandler.MESSAGE_ID + message.getMessageId(),
        witnessMessage.get().getDescription());
  }

  @Test
  public void testHandleElectionResult() {
    // Create the result Election message
    QuestionResult questionResult = new QuestionResult(electionQuestion.getBallotOptions().get(0),
        2);
    ElectionResultQuestion electionResultQuestion = new ElectionResultQuestion("id",
        Collections.singletonList(questionResult));
    ElectionResult electionResult = new ElectionResult(
        Collections.singletonList(electionResultQuestion));
    MessageGeneral message = new MessageGeneral(
        Base64.getUrlDecoder().decode(CREATE_LAO.getOrganizer()), electionResult, signer,
        Injection.provideGson());

    // Call the message handler
    assertFalse(handleMessage(laoRepository, LAO_CHANNEL + "/" + election.getId(), message));

    // Check the Election is present with state RESULTS_READY and the results
    Optional<Election> electionOpt = laoRepository.getLaoByChannel(LAO_CHANNEL)
        .getElection(election.getId());
    assertTrue(electionOpt.isPresent());
    assertEquals(EventState.RESULTS_READY, electionOpt.get().getState());
    assertEquals(Collections.singletonList(questionResult),
        electionOpt.get().getResultsForQuestionId("id"));
  }

  @Test
  public void testHandleElectionEnd() {
    // Create the end Election message
    ElectionEnd electionEnd = new ElectionEnd(election.getId(), lao.getId(), "");
    MessageGeneral message = new MessageGeneral(
        Base64.getUrlDecoder().decode(CREATE_LAO.getOrganizer()), electionEnd, signer,
        Injection.provideGson());

    // Call the message handler
    assertFalse(handleMessage(laoRepository, LAO_CHANNEL + "/" + election.getId(), message));

    // Check the Election is present with state CLOSED and the results
    Optional<Election> electionOpt = laoRepository.getLaoByChannel(LAO_CHANNEL)
        .getElection(election.getId());
    assertTrue(electionOpt.isPresent());
    assertEquals(EventState.CLOSED, electionOpt.get().getState());
  }
}
