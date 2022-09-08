package com.github.dedis.popstellar.utility.handler;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.github.dedis.popstellar.di.DataRegistryModule;
import com.github.dedis.popstellar.di.JsonModule;
import com.github.dedis.popstellar.model.network.method.message.MessageGeneral;
import com.github.dedis.popstellar.model.network.method.message.data.lao.CreateLao;
import com.github.dedis.popstellar.model.network.method.message.data.rollcall.*;
import com.github.dedis.popstellar.model.objects.*;
import com.github.dedis.popstellar.model.objects.event.EventState;
import com.github.dedis.popstellar.model.objects.security.*;
import com.github.dedis.popstellar.repository.*;
import com.github.dedis.popstellar.repository.remote.MessageSender;
import com.github.dedis.popstellar.utility.error.DataHandlingException;
import com.github.dedis.popstellar.utility.error.UnknownLaoException;
import com.github.dedis.popstellar.utility.error.keys.KeyException;
import com.github.dedis.popstellar.utility.security.KeyManager;
import com.google.gson.Gson;

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
import static com.github.dedis.popstellar.testutils.Base64DataUtils.generatePoPToken;
import static com.github.dedis.popstellar.utility.handler.data.RollCallHandler.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RollCallHandlerTest {

  private static final KeyPair SENDER_KEY = generateKeyPair();
  private static final PublicKey SENDER = SENDER_KEY.getPublicKey();
  private static final PoPToken POP_TOKEN = generatePoPToken();

  private static final CreateLao CREATE_LAO = new CreateLao("lao", SENDER);
  private static final Channel LAO_CHANNEL = Channel.getLaoChannel(CREATE_LAO.getId());

  private static final Gson GSON = JsonModule.provideGson(DataRegistryModule.provideDataRegistry());

  private MessageRepository messageRepo;
  private LAORepository laoRepo;
  private MessageHandler messageHandler;

  private RollCall rollCall;

  @Mock MessageSender messageSender;
  @Mock KeyManager keyManager;

  @Rule public InstantTaskExecutorRule rule = new InstantTaskExecutorRule();

  @Before
  public void setup() throws GeneralSecurityException, IOException, KeyException {
    lenient().when(keyManager.getMainKeyPair()).thenReturn(SENDER_KEY);
    lenient().when(keyManager.getMainPublicKey()).thenReturn(SENDER);
    lenient().when(keyManager.getValidPoPToken(any(), any())).thenReturn(POP_TOKEN);

    when(messageSender.subscribe(any())).then(args -> Completable.complete());

    messageRepo = new MessageRepository();
    laoRepo = new LAORepository();
    messageHandler =
        new MessageHandler(
            DataRegistryModule.provideDataRegistry(), keyManager, new ServerRepository());

    // Create one LAO
    Lao lao = new Lao(CREATE_LAO.getName(), CREATE_LAO.getOrganizer(), CREATE_LAO.getCreation());
    lao.setLastModified(lao.getCreation());

    // Create one Roll Call and add it to the LAO
    rollCall = new RollCall(lao.getId(), Instant.now().getEpochSecond(), "roll call 1");
    rollCall.setLocation("EPFL");
    lao.setRollCalls(
        new HashMap<String, RollCall>() {
          {
            put(rollCall.getId(), rollCall);
          }
        });

    // Add the LAO to the LAORepository
    laoRepo.updateLao(lao);

    // Add the CreateLao message to the LAORepository
    MessageGeneral createLaoMessage = new MessageGeneral(SENDER_KEY, CREATE_LAO, GSON);
    messageRepo.addMessage(createLaoMessage);
  }

  @Test
  public void testHandleCreateRollCall() throws DataHandlingException, UnknownLaoException {
    // Create the create Roll Call message
    CreateRollCall createRollCall =
        new CreateRollCall(
            "roll call 2",
            rollCall.getCreation(),
            rollCall.getStart(),
            rollCall.getEnd(),
            rollCall.getLocation(),
            rollCall.getDescription(),
            CREATE_LAO.getId());
    MessageGeneral message = new MessageGeneral(SENDER_KEY, createRollCall, GSON);

    // Call the message handler
    messageHandler.handleMessage(messageRepo, laoRepo, messageSender, LAO_CHANNEL, message);

    // Check the new Roll Call is present with state CREATED and the correct ID
    Optional<RollCall> rollCallOpt =
        laoRepo.getLaoByChannel(LAO_CHANNEL).getRollCall(createRollCall.getId());
    assertTrue(rollCallOpt.isPresent());
    assertEquals(EventState.CREATED, rollCallOpt.get().getState().getValue());
    assertEquals(createRollCall.getId(), rollCallOpt.get().getId());

    // Check the WitnessMessage has been created
    Optional<WitnessMessage> witnessMessage =
        laoRepo.getLaoByChannel(LAO_CHANNEL).getWitnessMessage(message.getMessageId());
    assertTrue(witnessMessage.isPresent());

    // Check the Witness message contains the expected title and description
    WitnessMessage expectedMessage =
        createRollCallWitnessMessage(message.getMessageId(), rollCallOpt.get());
    assertEquals(expectedMessage.getTitle(), witnessMessage.get().getTitle());
    assertEquals(expectedMessage.getDescription(), witnessMessage.get().getDescription());
  }

  @Test
  public void testHandleOpenRollCall() throws DataHandlingException, UnknownLaoException {
    // Create the open Roll Call message
    OpenRollCall openRollCall =
        new OpenRollCall(
            CREATE_LAO.getId(), rollCall.getId(), rollCall.getStart(), EventState.CREATED);
    MessageGeneral message = new MessageGeneral(SENDER_KEY, openRollCall, GSON);

    // Call the message handler
    messageHandler.handleMessage(messageRepo, laoRepo, messageSender, LAO_CHANNEL, message);

    // Check the Roll Call is present with state OPENED and the correct ID
    Optional<RollCall> rollCallOpt =
        laoRepo.getLaoByChannel(LAO_CHANNEL).getRollCall(openRollCall.getUpdateId());
    assertTrue(rollCallOpt.isPresent());
    assertEquals(EventState.OPENED, rollCallOpt.get().getState().getValue());
    assertEquals(openRollCall.getUpdateId(), rollCallOpt.get().getId());

    // Check the WitnessMessage has been created
    Optional<WitnessMessage> witnessMessage =
        laoRepo.getLaoByChannel(LAO_CHANNEL).getWitnessMessage(message.getMessageId());
    assertTrue(witnessMessage.isPresent());

    // Check the Witness message contains the expected title and description
    WitnessMessage expectedMessage =
        openRollCallWitnessMessage(message.getMessageId(), rollCallOpt.get());
    assertEquals(expectedMessage.getTitle(), witnessMessage.get().getTitle());
    assertEquals(expectedMessage.getDescription(), witnessMessage.get().getDescription());
  }

  @Test
  public void testHandleCloseRollCall() throws DataHandlingException, UnknownLaoException {
    // Create the close Roll Call message
    CloseRollCall closeRollCall =
        new CloseRollCall(
            CREATE_LAO.getId(), rollCall.getId(), rollCall.getEnd(), new ArrayList<>());
    MessageGeneral message = new MessageGeneral(SENDER_KEY, closeRollCall, GSON);

    // Call the message handler
    messageHandler.handleMessage(messageRepo, laoRepo, messageSender, LAO_CHANNEL, message);

    // Check the Roll Call is present with state CLOSED and the correct ID
    Optional<RollCall> rollCallOpt =
        laoRepo.getLaoByChannel(LAO_CHANNEL).getRollCall(closeRollCall.getUpdateId());
    assertTrue(rollCallOpt.isPresent());
    assertEquals(EventState.CLOSED, rollCallOpt.get().getState().getValue());
    assertTrue(rollCallOpt.get().isClosed());
    assertEquals(closeRollCall.getUpdateId(), rollCallOpt.get().getId());

    // Check the WitnessMessage has been created
    Optional<WitnessMessage> witnessMessage =
        laoRepo.getLaoByChannel(LAO_CHANNEL).getWitnessMessage(message.getMessageId());
    assertTrue(witnessMessage.isPresent());

    // Check the Witness message contains the expected title and description
    WitnessMessage expectedMessage =
        closeRollCallWitnessMessage(message.getMessageId(), rollCallOpt.get());
    assertEquals(expectedMessage.getTitle(), witnessMessage.get().getTitle());
    assertEquals(expectedMessage.getDescription(), witnessMessage.get().getDescription());
  }
}
