package com.github.dedis.popstellar.utility.handler;

import static com.github.dedis.popstellar.utility.handler.MessageHandler.handleMessage;
import static com.github.dedis.popstellar.utility.handler.RollCallHandler.closeRollCallWitnessMessage;
import static com.github.dedis.popstellar.utility.handler.RollCallHandler.createRollCallWitnessMessage;
import static com.github.dedis.popstellar.utility.handler.RollCallHandler.openRollCallWitnessMessage;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.github.dedis.popstellar.Injection;
import com.github.dedis.popstellar.model.network.GenericMessage;
import com.github.dedis.popstellar.model.network.answer.Result;
import com.github.dedis.popstellar.model.network.method.message.MessageGeneral;
import com.github.dedis.popstellar.model.network.method.message.data.Data;
import com.github.dedis.popstellar.model.network.method.message.data.lao.CreateLao;
import com.github.dedis.popstellar.model.network.method.message.data.rollcall.CloseRollCall;
import com.github.dedis.popstellar.model.network.method.message.data.rollcall.CreateRollCall;
import com.github.dedis.popstellar.model.network.method.message.data.rollcall.OpenRollCall;
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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.schedulers.TestScheduler;

@RunWith(MockitoJUnitRunner.class)
public class RollCallHandlerTest {

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
  public void testHandleCreateRollCall() throws DataHandlingException {
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
    MessageGeneral message =
        new MessageGeneral(
            Base64.getUrlDecoder().decode(CREATE_LAO.getOrganizer()),
            createRollCall,
            signer,
            Injection.provideGson());

    // Call the message handler
    handleMessage(laoRepository, LAO_CHANNEL, message);

    // Check the new Roll Call is present with state CREATED and the correct ID
    Optional<RollCall> rollCallOpt =
        laoRepository.getLaoByChannel(LAO_CHANNEL).getRollCall(createRollCall.getId());
    assertTrue(rollCallOpt.isPresent());
    assertEquals(EventState.CREATED, rollCallOpt.get().getState());
    assertEquals(createRollCall.getId(), rollCallOpt.get().getId());

    // Check the WitnessMessage has been created
    Optional<WitnessMessage> witnessMessage =
        laoRepository.getLaoByChannel(LAO_CHANNEL).getWitnessMessage(message.getMessageId());
    assertTrue(witnessMessage.isPresent());

    // Check the Witness message contains the expected title and description
    WitnessMessage expectedMessage =
        createRollCallWitnessMessage(message.getMessageId(), rollCallOpt.get());
    assertEquals(expectedMessage.getTitle(), witnessMessage.get().getTitle());
    assertEquals(expectedMessage.getDescription(), witnessMessage.get().getDescription());
  }

  @Test
  public void testHandleOpenRollCall() throws DataHandlingException {
    // Create the open Roll Call message
    OpenRollCall openRollCall =
        new OpenRollCall(
            CREATE_LAO.getId(), rollCall.getId(), rollCall.getStart(), EventState.CREATED);
    MessageGeneral message =
        new MessageGeneral(
            Base64.getUrlDecoder().decode(CREATE_LAO.getOrganizer()),
            openRollCall,
            signer,
            Injection.provideGson());

    // Call the message handler
    handleMessage(laoRepository, LAO_CHANNEL, message);

    // Check the Roll Call is present with state OPENED and the correct ID
    Optional<RollCall> rollCallOpt =
        laoRepository.getLaoByChannel(LAO_CHANNEL).getRollCall(openRollCall.getUpdateId());
    assertTrue(rollCallOpt.isPresent());
    assertEquals(EventState.OPENED, rollCallOpt.get().getState());
    assertEquals(openRollCall.getUpdateId(), rollCallOpt.get().getId());

    // Check the WitnessMessage has been created
    Optional<WitnessMessage> witnessMessage =
        laoRepository.getLaoByChannel(LAO_CHANNEL).getWitnessMessage(message.getMessageId());
    assertTrue(witnessMessage.isPresent());

    // Check the Witness message contains the expected title and description
    WitnessMessage expectedMessage =
        openRollCallWitnessMessage(message.getMessageId(), rollCallOpt.get());
    assertEquals(expectedMessage.getTitle(), witnessMessage.get().getTitle());
    assertEquals(expectedMessage.getDescription(), witnessMessage.get().getDescription());
  }

  @Test
  public void testHandleCloseRollCall() throws DataHandlingException {
    // Create the close Roll Call message
    CloseRollCall closeRollCall =
        new CloseRollCall(
            CREATE_LAO.getId(), rollCall.getId(), rollCall.getEnd(), new ArrayList<>());
    MessageGeneral message =
        new MessageGeneral(
            Base64.getUrlDecoder().decode(CREATE_LAO.getOrganizer()),
            closeRollCall,
            signer,
            Injection.provideGson());

    // Call the message handler
    handleMessage(laoRepository, LAO_CHANNEL, message);

    // Check the Roll Call is present with state CLOSED and the correct ID
    Optional<RollCall> rollCallOpt =
        laoRepository.getLaoByChannel(LAO_CHANNEL).getRollCall(closeRollCall.getUpdateId());
    assertTrue(rollCallOpt.isPresent());
    assertEquals(EventState.CLOSED, rollCallOpt.get().getState());
    assertEquals(closeRollCall.getUpdateId(), rollCallOpt.get().getId());

    // Check the WitnessMessage has been created
    Optional<WitnessMessage> witnessMessage =
        laoRepository.getLaoByChannel(LAO_CHANNEL).getWitnessMessage(message.getMessageId());
    assertTrue(witnessMessage.isPresent());

    // Check the Witness message contains the expected title and description
    WitnessMessage expectedMessage =
        closeRollCallWitnessMessage(message.getMessageId(), rollCallOpt.get());
    assertEquals(expectedMessage.getTitle(), witnessMessage.get().getTitle());
    assertEquals(expectedMessage.getDescription(), witnessMessage.get().getDescription());
  }
}
