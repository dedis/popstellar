package com.github.dedis.popstellar.utility.handler;

import static com.github.dedis.popstellar.testutils.Base64DataUtils.generateKeyPair;
import static com.github.dedis.popstellar.utility.handler.data.ElectionHandler.electionSetupWitnessMessage;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.github.dedis.popstellar.di.DataRegistryModule;
import com.github.dedis.popstellar.di.JsonModule;
import com.github.dedis.popstellar.model.network.method.message.MessageGeneral;
import com.github.dedis.popstellar.model.network.method.message.data.election.ElectionEnd;
import com.github.dedis.popstellar.model.network.method.message.data.election.ElectionQuestion;
import com.github.dedis.popstellar.model.network.method.message.data.election.ElectionResult;
import com.github.dedis.popstellar.model.network.method.message.data.election.ElectionResultQuestion;
import com.github.dedis.popstellar.model.network.method.message.data.election.ElectionSetup;
import com.github.dedis.popstellar.model.network.method.message.data.election.OpenElection;
import com.github.dedis.popstellar.model.network.method.message.data.election.QuestionResult;
import com.github.dedis.popstellar.model.network.method.message.data.lao.CreateLao;
import com.github.dedis.popstellar.model.objects.Channel;
import com.github.dedis.popstellar.model.objects.Election;
import com.github.dedis.popstellar.model.objects.Lao;
import com.github.dedis.popstellar.model.objects.RollCall;
import com.github.dedis.popstellar.model.objects.WitnessMessage;
import com.github.dedis.popstellar.model.objects.event.EventState;
import com.github.dedis.popstellar.model.objects.security.KeyPair;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.repository.LAORepository;
import com.github.dedis.popstellar.repository.LAOState;
import com.github.dedis.popstellar.repository.remote.MessageSender;
import com.github.dedis.popstellar.utility.error.DataHandlingException;
import com.github.dedis.popstellar.utility.security.KeyManager;
import com.google.gson.Gson;

import junit.framework.TestCase;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
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
  private ElectionQuestion electionQuestion;

  private LAORepository laoRepository;
  private MessageHandler messageHandler;

  @Mock MessageSender messageSender;
  @Mock KeyManager keyManager;

  @Before
  public void setup() throws GeneralSecurityException, IOException {
    lenient().when(keyManager.getMainKeyPair()).thenReturn(SENDER_KEY);
    lenient().when(keyManager.getMainPublicKey()).thenReturn(SENDER);

    laoRepository = new LAORepository();

    when(messageSender.subscribe(any())).then(args -> Completable.complete());

    laoRepository = new LAORepository();
    messageHandler = new MessageHandler(DataRegistryModule.provideDataRegistry(), keyManager);

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
    election.setChannel(lao.getChannel().subChannel(election.getId()));
    electionQuestion =
        new ElectionQuestion(
            "question", "Plurality", false, Arrays.asList("a", "b"), election.getId());
    election.setElectionQuestions(Collections.singletonList(electionQuestion));
    lao.setElections(
        new HashMap<String, Election>() {
          {
            put(election.getId(), election);
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
    MessageGeneral message = new MessageGeneral(SENDER_KEY, electionSetup, GSON);

    // Call the message handler
    messageHandler.handleMessage(laoRepository, messageSender, LAO_CHANNEL, message);

    // Check the Election is present with state OPENED and the correct ID
    Optional<Election> electionOpt =
        laoRepository.getLaoByChannel(LAO_CHANNEL).getElection(electionSetup.getId());
    assertTrue(electionOpt.isPresent());
    assertEquals(EventState.CREATED, electionOpt.get().getState());
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
    MessageGeneral message = new MessageGeneral(SENDER_KEY, electionResult, GSON);

    // Call the message handler
    messageHandler.handleMessage(
        laoRepository, messageSender, LAO_CHANNEL.subChannel(election.getId()), message);

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
    MessageGeneral message = new MessageGeneral(SENDER_KEY, electionEnd, GSON);

    // Call the message handler
    messageHandler.handleMessage(
        laoRepository, messageSender, LAO_CHANNEL.subChannel(election.getId()), message);

    // Check the Election is present with state CLOSED and the results
    Optional<Election> electionOpt =
        laoRepository.getLaoByChannel(LAO_CHANNEL).getElection(election.getId());
    assertTrue(electionOpt.isPresent());
    assertEquals(EventState.CLOSED, electionOpt.get().getState());
  }

  @Test
  public void testHandleElectionOpen() throws DataHandlingException {
    OpenElection openElection = new OpenElection(lao.getId(), election.getId(), openedAt);
    MessageGeneral message = new MessageGeneral(SENDER_KEY, openElection, GSON);

    for (EventState state : EventState.values()) {
      election.setEventState(state);
      messageHandler.handleMessage(laoRepository, messageSender,
          election.getChannel(), message);
      if (state == EventState.CREATED) {
        assertEquals(EventState.OPENED, election.getState());
        //Test for current TimeStamp
        assertEquals(Instant.now().getEpochSecond(), election.getStartTimestamp());
      } else {
        assertEquals(state, election.getState());}
    }
  }
}
