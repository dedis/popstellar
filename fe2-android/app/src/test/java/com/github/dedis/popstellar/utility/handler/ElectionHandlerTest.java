package com.github.dedis.popstellar.utility.handler;

import static com.github.dedis.popstellar.utility.handler.ElectionHandler.electionSetupWitnessMessage;
import static com.github.dedis.popstellar.utility.handler.MessageHandler.handleMessage;

import com.github.dedis.popstellar.Injection;
import com.github.dedis.popstellar.model.network.GenericMessage;
import com.github.dedis.popstellar.model.network.answer.Result;
import com.github.dedis.popstellar.model.network.method.message.MessageGeneral;
import com.github.dedis.popstellar.model.network.method.message.data.Data;
import com.github.dedis.popstellar.model.network.method.message.data.election.ElectionEnd;
import com.github.dedis.popstellar.model.network.method.message.data.election.ElectionQuestion;
import com.github.dedis.popstellar.model.network.method.message.data.election.ElectionResult;
import com.github.dedis.popstellar.model.network.method.message.data.election.ElectionResultQuestion;
import com.github.dedis.popstellar.model.network.method.message.data.election.ElectionSetup;
import com.github.dedis.popstellar.model.network.method.message.data.election.QuestionResult;
import com.github.dedis.popstellar.model.network.method.message.data.lao.CreateLao;
import com.github.dedis.popstellar.model.objects.Election;
import com.github.dedis.popstellar.model.objects.Lao;
import com.github.dedis.popstellar.model.objects.RollCall;
import com.github.dedis.popstellar.model.objects.WitnessMessage;
import com.github.dedis.popstellar.model.objects.event.EventState;
import com.github.dedis.popstellar.repository.LAORepository;
import com.github.dedis.popstellar.repository.LAOState;
import com.github.dedis.popstellar.repository.local.LAOLocalDataSource;
import com.github.dedis.popstellar.repository.remote.LAORemoteDataSource;
import com.github.dedis.popstellar.utility.error.DataHandlingException;
import com.github.dedis.popstellar.utility.scheduler.SchedulerProvider;
import com.github.dedis.popstellar.utility.scheduler.TestSchedulerProvider;
import com.google.crypto.tink.PublicKeySign;
import com.google.crypto.tink.integration.android.AndroidKeysetManager;

import junit.framework.TestCase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.schedulers.TestScheduler;

@RunWith(MockitoJUnitRunner.class)
public class ElectionHandlerTest extends TestCase {

  @Mock LAORemoteDataSource remoteDataSource;

  @Mock LAOLocalDataSource localDataSource;

  @Mock AndroidKeysetManager androidKeysetManager;

  @Mock PublicKeySign signer;

  private static final int REQUEST_ID = 42;
  private static final int RESPONSE_DELAY = 1000;
  private static final CreateLao CREATE_LAO =
      new CreateLao("lao", "Z3DYtBxooGs6KxOAqCWD3ihR8M6ZPBjAmWp_w5VBaws=");
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
    createLaoMessage =
        new MessageGeneral(
            Base64.getUrlDecoder().decode(CREATE_LAO.getOrganizer()),
            CREATE_LAO,
            signer,
            Injection.provideGson());

    // Simulate a network response from the server after the response delay
    Observable<GenericMessage> upstream =
        Observable.fromArray((GenericMessage) new Result(REQUEST_ID))
            .delay(RESPONSE_DELAY, TimeUnit.MILLISECONDS, testScheduler);

    Mockito.when(remoteDataSource.observeMessage()).thenReturn(upstream);
    Mockito.when(remoteDataSource.observeWebsocket()).thenReturn(Observable.empty());

    laoRepository =
        LAORepository.getInstance(
            remoteDataSource,
            localDataSource,
            androidKeysetManager,
            Injection.provideGson(),
            testSchedulerProvider);

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
    election = new Election(lao.getId(), Instant.now().getEpochSecond(), "election 1");
    election.setStart(Instant.now().getEpochSecond());
    election.setEnd(Instant.now().getEpochSecond() + 20L);
    election.setChannel(lao.getChannel() + "/" + election.getId());
    electionQuestion =
        new ElectionQuestion(
            "question", "voting method", false, Collections.singletonList("a"), election.getId());
    election.setElectionQuestions(Collections.singletonList(electionQuestion));
    lao.setElections(
        new HashMap<String, Election>() {
          {
            put(election.getId(), election);
          }
        });

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
  public void testHandleElectionSetup() throws DataHandlingException {
    // Create the setup Election message
    ElectionSetup electionSetup =
        new ElectionSetup(
            "election 2",
            election.getCreation(),
            election.getStartTimestamp(),
            election.getEndTimestamp(),
            Collections.singletonList(electionQuestion.getVotingMethod()),
            Collections.singletonList(electionQuestion.getWriteIn()),
            Collections.singletonList(electionQuestion.getBallotOptions()),
            Collections.singletonList(electionQuestion.getQuestion()),
            lao.getId());
    MessageGeneral message =
        new MessageGeneral(
            Base64.getUrlDecoder().decode(CREATE_LAO.getOrganizer()),
            electionSetup,
            signer,
            Injection.provideGson());

    // Call the message handler
    handleMessage(laoRepository, LAO_CHANNEL, message);

    // Check the Election is present with state OPENED and the correct ID
    Optional<Election> electionOpt =
        laoRepository.getLaoByChannel(LAO_CHANNEL).getElection(electionSetup.getId());
    assertTrue(electionOpt.isPresent());
    assertEquals(EventState.OPENED, electionOpt.get().getState());
    assertEquals(electionSetup.getId(), electionOpt.get().getId());

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
    MessageGeneral message =
        new MessageGeneral(
            Base64.getUrlDecoder().decode(CREATE_LAO.getOrganizer()),
            electionResult,
            signer,
            Injection.provideGson());

    // Call the message handler
    handleMessage(laoRepository, LAO_CHANNEL + "/" + election.getId(), message);

    // Check the Election is present with state RESULTS_READY and the results
    Optional<Election> electionOpt =
        laoRepository.getLaoByChannel(LAO_CHANNEL).getElection(election.getId());
    assertTrue(electionOpt.isPresent());
    assertEquals(EventState.RESULTS_READY, electionOpt.get().getState());
    assertEquals(
        Collections.singletonList(questionResult), electionOpt.get().getResultsForQuestionId("id"));
  }

  @Test
  public void testHandleElectionEnd() throws DataHandlingException {
    // Create the end Election message
    ElectionEnd electionEnd = new ElectionEnd(election.getId(), lao.getId(), "");
    MessageGeneral message =
        new MessageGeneral(
            Base64.getUrlDecoder().decode(CREATE_LAO.getOrganizer()),
            electionEnd,
            signer,
            Injection.provideGson());

    // Call the message handler
    handleMessage(laoRepository, LAO_CHANNEL + "/" + election.getId(), message);

    // Check the Election is present with state CLOSED and the results
    Optional<Election> electionOpt =
        laoRepository.getLaoByChannel(LAO_CHANNEL).getElection(election.getId());
    assertTrue(electionOpt.isPresent());
    assertEquals(EventState.CLOSED, electionOpt.get().getState());
  }
}
