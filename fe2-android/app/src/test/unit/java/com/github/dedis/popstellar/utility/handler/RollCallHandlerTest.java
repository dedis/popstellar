package com.github.dedis.popstellar.utility.handler;

import android.app.Application;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.github.dedis.popstellar.di.DataRegistryModuleHelper;
import com.github.dedis.popstellar.di.JsonModule;
import com.github.dedis.popstellar.model.network.method.message.MessageGeneral;
import com.github.dedis.popstellar.model.network.method.message.data.DataRegistry;
import com.github.dedis.popstellar.model.network.method.message.data.lao.CreateLao;
import com.github.dedis.popstellar.model.network.method.message.data.rollcall.*;
import com.github.dedis.popstellar.model.objects.*;
import com.github.dedis.popstellar.model.objects.event.EventState;
import com.github.dedis.popstellar.model.objects.security.*;
import com.github.dedis.popstellar.repository.*;
import com.github.dedis.popstellar.repository.database.AppDatabase;
import com.github.dedis.popstellar.repository.database.event.rollcall.RollCallDao;
import com.github.dedis.popstellar.repository.database.event.rollcall.RollCallEntity;
import com.github.dedis.popstellar.repository.database.lao.LAODao;
import com.github.dedis.popstellar.repository.database.lao.LAOEntity;
import com.github.dedis.popstellar.repository.database.message.MessageDao;
import com.github.dedis.popstellar.repository.database.message.MessageEntity;
import com.github.dedis.popstellar.repository.database.witnessing.*;
import com.github.dedis.popstellar.repository.remote.MessageSender;
import com.github.dedis.popstellar.utility.error.*;
import com.github.dedis.popstellar.utility.error.keys.KeyException;
import com.github.dedis.popstellar.utility.error.keys.NoRollCallException;
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

import io.reactivex.Completable;
import io.reactivex.Single;

import static com.github.dedis.popstellar.testutils.Base64DataUtils.generateKeyPair;
import static com.github.dedis.popstellar.testutils.Base64DataUtils.generatePoPToken;
import static com.github.dedis.popstellar.utility.handler.data.RollCallHandler.*;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@RunWith(AndroidJUnit4.class)
public class RollCallHandlerTest {

  private static final KeyPair SENDER_KEY = generateKeyPair();
  private static final PublicKey SENDER = SENDER_KEY.getPublicKey();
  private static final PoPToken POP_TOKEN = generatePoPToken();

  private static final CreateLao CREATE_LAO = new CreateLao("lao", SENDER, new ArrayList<>());
  private static final Channel LAO_CHANNEL = Channel.getLaoChannel(CREATE_LAO.getId());
  private static Lao LAO;

  private RollCallRepository rollCallRepo;
  private WitnessingRepository witnessingRepository;

  private MessageHandler messageHandler;
  private Gson gson;

  private RollCall rollCall;

  @Mock AppDatabase appDatabase;
  @Mock LAODao laoDao;
  @Mock MessageDao messageDao;
  @Mock RollCallDao rollCallDao;
  @Mock WitnessingDao witnessingDao;
  @Mock WitnessDao witnessDao;
  @Mock MessageSender messageSender;
  @Mock KeyManager keyManager;

  @Rule public InstantTaskExecutorRule rule = new InstantTaskExecutorRule();

  @Before
  public void setup()
      throws GeneralSecurityException, IOException, KeyException, UnknownRollCallException {
    MockitoAnnotations.openMocks(this);
    Application application = ApplicationProvider.getApplicationContext();

    lenient().when(keyManager.getMainKeyPair()).thenReturn(SENDER_KEY);
    lenient().when(keyManager.getMainPublicKey()).thenReturn(SENDER);
    lenient().when(keyManager.getValidPoPToken(any(), any())).thenReturn(POP_TOKEN);

    lenient().when(messageSender.subscribe(any())).then(args -> Completable.complete());

    when(appDatabase.laoDao()).thenReturn(laoDao);
    when(laoDao.getAllLaos()).thenReturn(Single.just(new ArrayList<>()));
    when(laoDao.insert(any(LAOEntity.class))).thenReturn(Completable.complete());

    when(appDatabase.messageDao()).thenReturn(messageDao);
    when(messageDao.takeFirstNMessages(anyInt())).thenReturn(Single.just(new ArrayList<>()));
    when(messageDao.insert(any(MessageEntity.class))).thenReturn(Completable.complete());
    when(messageDao.getMessageById(any(MessageID.class))).thenReturn(null);

    when(appDatabase.rollCallDao()).thenReturn(rollCallDao);
    when(rollCallDao.getRollCallsByLaoId(anyString())).thenReturn(Single.just(new ArrayList<>()));
    when(rollCallDao.insert(any(RollCallEntity.class))).thenReturn(Completable.complete());

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
    rollCallRepo = new RollCallRepository(appDatabase, application);
    witnessingRepository =
        new WitnessingRepository(
            appDatabase,
            application,
            rollCallRepository,
            electionRepository,
            meetingRepository,
            digitalCashRepository);
    MessageRepository messageRepo =
        new MessageRepository(appDatabase, ApplicationProvider.getApplicationContext());

    DataRegistry dataRegistry =
        DataRegistryModuleHelper.buildRegistry(
            laoRepo, keyManager, rollCallRepo, witnessingRepository);

    gson = JsonModule.provideGson(dataRegistry);
    messageHandler = new MessageHandler(messageRepo, dataRegistry);

    // Create one LAO
    LAO = new Lao(CREATE_LAO.getName(), CREATE_LAO.getOrganizer(), CREATE_LAO.getCreation());
    LAO.setLastModified(LAO.getCreation());

    // Create one Roll Call and add it to the roll call repo
    long now = Instant.now().getEpochSecond();
    rollCall =
        new RollCall(
            LAO.getId(),
            LAO.getId(),
            "roll call 1",
            now,
            now + 1,
            now + 2,
            EventState.CREATED,
            new HashSet<>(),
            "somewhere",
            "desc");
    rollCallRepo.updateRollCall(LAO.getId(), rollCall);

    // Add the LAO to the LAORepository
    laoRepo.updateLao(LAO);

    // Add the CreateLao message to the LAORepository
    MessageGeneral createLaoMessage = new MessageGeneral(SENDER_KEY, CREATE_LAO, gson);
    messageRepo.addMessage(createLaoMessage, false, false);
  }

  @Test
  public void testHandleCreateRollCall()
      throws DataHandlingException, UnknownLaoException, UnknownRollCallException,
          UnknownElectionException, NoRollCallException, UnknownWitnessMessageException {
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
    MessageGeneral message = new MessageGeneral(SENDER_KEY, createRollCall, gson);

    // Call the message handler
    messageHandler.handleMessage(messageSender, LAO_CHANNEL, message);

    // Check the new Roll Call is present with state CREATED and the correct ID
    RollCall rollCallCheck = rollCallRepo.getRollCallWithId(LAO.getId(), createRollCall.getId());
    assertEquals(EventState.CREATED, rollCallCheck.getState());
    assertEquals(createRollCall.getId(), rollCallCheck.getId());

    // Check the WitnessMessage has been created
    Optional<WitnessMessage> witnessMessage =
        witnessingRepository.getWitnessMessage(LAO.getId(), message.getMessageId());
    assertTrue(witnessMessage.isPresent());

    // Check the Witness message contains the expected title and description
    WitnessMessage expectedMessage =
        createRollCallWitnessMessage(message.getMessageId(), rollCallCheck);
    assertEquals(expectedMessage.getTitle(), witnessMessage.get().getTitle());
    assertEquals(expectedMessage.getDescription(), witnessMessage.get().getDescription());
  }

  @Test
  public void testHandleOpenRollCall()
      throws DataHandlingException, UnknownLaoException, UnknownRollCallException,
          UnknownElectionException, NoRollCallException, UnknownWitnessMessageException {
    // Create the open Roll Call message
    OpenRollCall openRollCall =
        new OpenRollCall(
            CREATE_LAO.getId(), rollCall.getId(), rollCall.getStart(), EventState.CREATED);
    MessageGeneral message = new MessageGeneral(SENDER_KEY, openRollCall, gson);

    // Call the message handler
    messageHandler.handleMessage(messageSender, LAO_CHANNEL, message);

    // Check the Roll Call is present with state OPENED and the correct ID
    RollCall rollCallCheck =
        rollCallRepo.getRollCallWithId(LAO.getId(), openRollCall.getUpdateId());
    assertEquals(EventState.OPENED, rollCallCheck.getState());
    assertTrue(rollCallCheck.isOpen());
    assertEquals(openRollCall.getUpdateId(), rollCallCheck.getId());

    // Check the WitnessMessage has been created
    Optional<WitnessMessage> witnessMessage =
        witnessingRepository.getWitnessMessage(LAO.getId(), message.getMessageId());
    assertTrue(witnessMessage.isPresent());

    // Check the Witness message contains the expected title and description
    WitnessMessage expectedMessage =
        openRollCallWitnessMessage(message.getMessageId(), rollCallCheck);
    assertEquals(expectedMessage.getTitle(), witnessMessage.get().getTitle());
    assertEquals(expectedMessage.getDescription(), witnessMessage.get().getDescription());
  }

  @Test
  public void testBlockOpenRollCall()
      throws DataHandlingException, UnknownLaoException, UnknownRollCallException,
          UnknownElectionException, NoRollCallException, UnknownWitnessMessageException {
    // Assert that a Roll Call can be opened
    assertTrue(rollCallRepo.canOpenRollCall(LAO.getId()));

    // Create the open Roll Call message
    OpenRollCall openRollCall =
        new OpenRollCall(
            CREATE_LAO.getId(), rollCall.getId(), rollCall.getStart(), EventState.CREATED);
    MessageGeneral messageOpen = new MessageGeneral(SENDER_KEY, openRollCall, gson);

    // Call the message handler
    messageHandler.handleMessage(messageSender, LAO_CHANNEL, messageOpen);

    // Check that no new Roll Call can be opened
    assertFalse(rollCallRepo.canOpenRollCall(LAO.getId()));

    // Create the close Roll Call message
    CloseRollCall closeRollCall =
        new CloseRollCall(
            CREATE_LAO.getId(), rollCall.getId(), rollCall.getEnd(), new ArrayList<>());
    MessageGeneral messageClose = new MessageGeneral(SENDER_KEY, closeRollCall, gson);

    // Call the message handler
    messageHandler.handleMessage(messageSender, LAO_CHANNEL, messageClose);

    // Check that now new Roll Calls can be opened
    assertTrue(rollCallRepo.canOpenRollCall(LAO.getId()));
  }

  @Test
  public void testHandleCloseRollCall()
      throws DataHandlingException, UnknownLaoException, UnknownRollCallException,
          UnknownElectionException, NoRollCallException, UnknownWitnessMessageException {
    // Create the open Roll Call message
    OpenRollCall openRollCall =
        new OpenRollCall(
            CREATE_LAO.getId(), rollCall.getId(), rollCall.getStart(), EventState.CREATED);

    // Call the message handler
    messageHandler.handleMessage(
        messageSender, LAO_CHANNEL, new MessageGeneral(SENDER_KEY, openRollCall, gson));

    // Create the close Roll Call message
    CloseRollCall closeRollCall =
        new CloseRollCall(
            CREATE_LAO.getId(), rollCall.getId(), rollCall.getEnd(), new ArrayList<>());
    MessageGeneral message = new MessageGeneral(SENDER_KEY, closeRollCall, gson);

    // Call the message handler
    messageHandler.handleMessage(messageSender, LAO_CHANNEL, message);

    // Check the Roll Call is present with state CLOSED and the correct ID
    RollCall rollCallCheck =
        rollCallRepo.getRollCallWithId(LAO.getId(), closeRollCall.getUpdateId());
    assertEquals(EventState.CLOSED, rollCallCheck.getState());
    assertTrue(rollCallCheck.isClosed());
    assertEquals(closeRollCall.getUpdateId(), rollCallCheck.getId());

    // Check the WitnessMessage has been created
    Optional<WitnessMessage> witnessMessage =
        witnessingRepository.getWitnessMessage(LAO.getId(), message.getMessageId());
    assertTrue(witnessMessage.isPresent());

    // Check the Witness message contains the expected title and description
    WitnessMessage expectedMessage =
        closeRollCallWitnessMessage(message.getMessageId(), rollCallCheck);
    assertEquals(expectedMessage.getTitle(), witnessMessage.get().getTitle());
    assertEquals(expectedMessage.getDescription(), witnessMessage.get().getDescription());
  }
}
