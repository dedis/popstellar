package com.github.dedis.popstellar.utility.handler;

import com.github.dedis.popstellar.di.DataRegistryModuleHelper;
import com.github.dedis.popstellar.di.JsonModule;
import com.github.dedis.popstellar.model.network.method.message.MessageGeneral;
import com.github.dedis.popstellar.model.network.method.message.PublicKeySignaturePair;
import com.github.dedis.popstellar.model.network.method.message.data.DataRegistry;
import com.github.dedis.popstellar.model.network.method.message.data.lao.*;
import com.github.dedis.popstellar.model.objects.*;
import com.github.dedis.popstellar.model.objects.security.KeyPair;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.model.objects.view.LaoView;
import com.github.dedis.popstellar.repository.*;
import com.github.dedis.popstellar.repository.remote.MessageSender;
import com.github.dedis.popstellar.utility.error.*;
import com.github.dedis.popstellar.utility.error.keys.NoRollCallException;
import com.github.dedis.popstellar.utility.security.KeyManager;
import com.google.gson.Gson;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.*;

import io.reactivex.Completable;

import static com.github.dedis.popstellar.testutils.Base64DataUtils.*;
import static com.github.dedis.popstellar.utility.handler.data.LaoHandler.updateLaoNameWitnessMessage;
import static com.github.dedis.popstellar.utility.handler.data.LaoHandler.updateLaoWitnessesWitnessMessage;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class LaoHandlerTest {

  private static final KeyPair SENDER_KEY1 = generateKeyPair();
  private static final KeyPair SENDER_KEY2 = generateKeyPair();
  private static final PublicKey SENDER1 = SENDER_KEY1.getPublicKey();
  private static final PublicKey SENDER2 = SENDER_KEY2.getPublicKey();
  private static final long CREATION = Instant.now().getEpochSecond();
  private static final String NAME1 = "lao1";
  private static final String NAME2 = "lao2";
  private static final String NAME3 = "lao3";
  private static final String ID1 = Lao.generateLaoId(SENDER1, CREATION, NAME1);
  private static final String ID2 = Lao.generateLaoId(SENDER1, CREATION, NAME2);
  private static final String ID3 = Lao.generateLaoId(SENDER1, CREATION, NAME3);
  private static final List<PublicKey> WITNESS = new ArrayList<>(
      Collections.singletonList(SENDER2));
  private static final List<PublicKey> WITNESSES = new ArrayList<>(Arrays.asList(SENDER1, SENDER2));
  private static final CreateLao CREATE_LAO1 =
      new CreateLao(ID1, NAME1, CREATION, SENDER1, new ArrayList<>());
  private static final CreateLao CREATE_LAO2 =
      new CreateLao(ID2, NAME2, CREATION, SENDER1, new ArrayList<>());
  private static final CreateLao CREATE_LAO3 =
      new CreateLao(ID3, NAME3, CREATION, SENDER1, WITNESSES);
  private static final Channel LAO_CHANNEL1 = Channel.getLaoChannel(CREATE_LAO1.getId());
  private static final Channel LAO_CHANNEL2 = Channel.getLaoChannel(CREATE_LAO2.getId());
  private static final Channel LAO_CHANNEL3 = Channel.getLaoChannel(CREATE_LAO3.getId());

  private LAORepository laoRepo;
  private MessageHandler messageHandler;
  private ServerRepository serverRepository;
  private Gson gson;

  public static final String RANDOM_KEY = "oOcKZjUeandJOFVgn-E6e-7QksviBBbHUPicdzUgIm8";
  public static final String RANDOM_ADDRESS = "ws://10.0.2.2:9000/organizer/client";
  public static final PeerAddress RANDOM_PEER = new PeerAddress("ws://128.0.0.2:8001/");

  private Lao lao;
  private MessageGeneral createLaoMessage;

  @Mock
  MessageSender messageSender;
  @Mock
  KeyManager keyManager;

  @Before
  public void setup() throws GeneralSecurityException, IOException {
    lenient().when(keyManager.getMainKeyPair()).thenReturn(SENDER_KEY1);
    lenient().when(keyManager.getMainPublicKey()).thenReturn(SENDER1);

    when(messageSender.subscribe(any())).then(args -> Completable.complete());

    laoRepo = new LAORepository();
    serverRepository = new ServerRepository();
    MessageRepository messageRepo = new MessageRepository();

    DataRegistry dataRegistry =
        DataRegistryModuleHelper.buildRegistry(laoRepo, messageRepo, keyManager, serverRepository);
    gson = JsonModule.provideGson(dataRegistry);
    messageHandler = new MessageHandler(messageRepo, dataRegistry);

    // Create one LAO and add it to the LAORepository
    lao = new Lao(CREATE_LAO1.getName(), CREATE_LAO1.getOrganizer(), CREATE_LAO1.getCreation());
    lao.setLastModified(lao.getCreation());
    laoRepo.updateLao(lao);

    // Add the CreateLao message to the LAORepository
    createLaoMessage = new MessageGeneral(SENDER_KEY1, CREATE_LAO1, gson);
    messageRepo.addMessage(createLaoMessage);
  }

  @Test
  public void testHandleCreateLaoOrganizer()
      throws DataHandlingException, UnknownLaoException, UnknownRollCallException,
      UnknownElectionException, NoRollCallException {
    // Create the message (new CreateLao) and call the message handler
    MessageGeneral message = new MessageGeneral(SENDER_KEY1, CREATE_LAO2, gson);
    messageHandler.handleMessage(messageSender, LAO_CHANNEL2, message);

    // Get expected results
    Lao resultLao = laoRepo.getLaoByChannel(LAO_CHANNEL2);
    String expectedName = CREATE_LAO2.getName();
    PublicKey expectedOrganizer = CREATE_LAO2.getOrganizer();
    long expectedCreation = CREATE_LAO2.getCreation();
    String expectedID = Lao.generateLaoId(expectedOrganizer, expectedCreation, expectedName);

    // Check that the expected LAO was created in the LAO repo
    assertEquals(LAO_CHANNEL2, resultLao.getChannel());
    assertEquals(expectedID, resultLao.getId());
    assertEquals(expectedName, resultLao.getName());
    assertEquals(expectedCreation, (long) resultLao.getLastModified());
    assertEquals(expectedCreation, (long) resultLao.getCreation());
    assertEquals(expectedOrganizer, resultLao.getOrganizer());
    assertTrue(resultLao.getWitnesses().isEmpty());
    assertTrue(resultLao.getWitnessMessages().isEmpty());
    assertNull(resultLao.getModificationId());
  }

  @Test
  public void testHandleCreateLaoWitness()
      throws DataHandlingException, UnknownLaoException, UnknownRollCallException,
      UnknownElectionException, NoRollCallException {
    // Main public key is a witness now, and not the organizer
    lenient().when(keyManager.getMainPublicKey()).thenReturn(SENDER2);

    // Create the message (CreateLao with witnesses) and call the message handler
    MessageGeneral message = new MessageGeneral(SENDER_KEY1, CREATE_LAO3, gson);
    messageHandler.handleMessage(messageSender, LAO_CHANNEL3, message);

    // Get expected results
    Lao resultLao = laoRepo.getLaoByChannel(LAO_CHANNEL3);
    String expectedName = CREATE_LAO3.getName();
    PublicKey expectedOrganizer = CREATE_LAO3.getOrganizer();
    long expectedCreation = CREATE_LAO3.getCreation();
    String expectedID = Lao.generateLaoId(expectedOrganizer, expectedCreation, expectedName);

    // Check that the expected LAO was created in the LAO repo
    assertEquals(LAO_CHANNEL3, resultLao.getChannel());
    assertEquals(expectedID, resultLao.getId());
    assertEquals(expectedName, resultLao.getName());
    assertEquals(expectedCreation, (long) resultLao.getLastModified());
    assertEquals(expectedCreation, (long) resultLao.getCreation());
    assertEquals(expectedOrganizer, resultLao.getOrganizer());
    assertEquals(new HashSet<>(WITNESSES), resultLao.getWitnesses());
    assertTrue(resultLao.getWitnessMessages().isEmpty());
    assertNull(resultLao.getModificationId());
  }

  @Test
  public void testHandleUpdateLaoNewName()
      throws DataHandlingException, UnknownLaoException, UnknownRollCallException,
      UnknownElectionException, NoRollCallException {
    // Create the update LAO message with new LAO name
    UpdateLao updateLao =
        new UpdateLao(
            SENDER1,
            CREATE_LAO1.getCreation(),
            "new name",
            Instant.now().getEpochSecond(),
            new HashSet<>());
    MessageGeneral message = new MessageGeneral(SENDER_KEY1, updateLao, gson);

    // Create the expected WitnessMessage
    WitnessMessage expectedMessage =
        updateLaoNameWitnessMessage(message.getMessageId(), updateLao, new LaoView(lao));

    // Call the message handler
    messageHandler.handleMessage(messageSender, LAO_CHANNEL1, message);

    // Check the WitnessMessage has been created
    Optional<WitnessMessage> witnessMessage =
        laoRepo.getLaoByChannel(LAO_CHANNEL1).getWitnessMessage(message.getMessageId());
    assertTrue(witnessMessage.isPresent());
    assertEquals(expectedMessage.getTitle(), witnessMessage.get().getTitle());
    assertEquals(expectedMessage.getDescription(), witnessMessage.get().getDescription());
  }

  @Test
  public void testHandleUpdateLaoNewWitness()
      throws DataHandlingException, UnknownLaoException, UnknownRollCallException,
      UnknownElectionException, NoRollCallException {
    // Set LAO to have one witness
    lao.setWitnesses(new HashSet<>(WITNESS));

    // Create UpdateLao with updated witness set
    UpdateLao updateLao =
        new UpdateLao(
            SENDER1,
            CREATE_LAO1.getCreation(),
            CREATE_LAO1.getName(),
            Instant.now().getEpochSecond(),
            new HashSet<>(WITNESSES));
    MessageGeneral message = new MessageGeneral(SENDER_KEY1, updateLao, gson);

    // Create the expected WitnessMessage and PendingUpdate
    WitnessMessage expectedMessage =
        updateLaoWitnessesWitnessMessage(message.getMessageId(), updateLao, new LaoView(lao));
    PendingUpdate pendingUpdate = new PendingUpdate(updateLao.getLastModified(),
        message.getMessageId());
    HashSet<PendingUpdate> expectedPendingUpdateSet = new HashSet<>();
    expectedPendingUpdateSet.add(pendingUpdate);

    // Call the handler
    messageHandler.handleMessage(messageSender, LAO_CHANNEL1, message);

    // Check the WitnessMessage has been created
    Optional<WitnessMessage> witnessMessage =
        laoRepo.getLaoByChannel(LAO_CHANNEL1).getWitnessMessage(message.getMessageId());
    assertTrue(witnessMessage.isPresent());
    assertEquals(expectedMessage.getTitle(), witnessMessage.get().getTitle());
    assertEquals(expectedMessage.getDescription(), witnessMessage.get().getDescription());

    // Check the PendingUpdate has been added
    assertEquals(expectedPendingUpdateSet,
        laoRepo.getLaoByChannel(LAO_CHANNEL1).getPendingUpdates());
  }

  @Test
  public void testHandleUpdateLaoOldInfo() {
    // Create UpdateLao with no updated name or witness set
    UpdateLao updateLao =
        new UpdateLao(
            SENDER1,
            CREATE_LAO1.getCreation(),
            CREATE_LAO1.getName(),
            Instant.now().getEpochSecond(),
            new HashSet<>());
    MessageGeneral message = new MessageGeneral(SENDER_KEY1, updateLao, gson);

    // Check that handling the message fails
    assertThrows(
        DataHandlingException.class,
        () -> messageHandler.handleMessage(messageSender, LAO_CHANNEL1, message));
  }

  @Test
  public void testHandleUpdateLaoStale() {
    // Create a update LAO message with last modified time older than the current LAO last modified
    // time (was creation)
    UpdateLao updateLao1 =
        new UpdateLao(
            SENDER1,
            CREATE_LAO1.getCreation() - 5,
            "new lao name",
            CREATE_LAO1.getCreation() - 10,
            new HashSet<>());
    MessageGeneral message = new MessageGeneral(SENDER_KEY1, updateLao1, gson);

    // Check that handling the older message fails
    assertThrows(
        DataHandlingException.class,
        () -> messageHandler.handleMessage(messageSender, LAO_CHANNEL1, message));
  }

  @Test
  public void testHandleStateLaoOrganizer()
      throws DataHandlingException, UnknownLaoException, UnknownRollCallException,
      UnknownElectionException, NoRollCallException {
    // Create a valid list of modification signatures
    List<PublicKeySignaturePair> modificationSignatures = getValidModificationSignatures(
        createLaoMessage);

    // Create the state LAO message
    StateLao stateLao =
        new StateLao(
            CREATE_LAO1.getId(),
            CREATE_LAO1.getName(),
            CREATE_LAO1.getCreation(),
            Instant.now().getEpochSecond(),
            CREATE_LAO1.getOrganizer(),
            createLaoMessage.getMessageId(),
            new HashSet<>(),
            modificationSignatures);
    MessageGeneral message = new MessageGeneral(SENDER_KEY1, stateLao, gson);

    // Call the message handler
    messageHandler.handleMessage(messageSender, LAO_CHANNEL1, message);

    // Check the LAO last modification time and ID was updated
    assertEquals(
        (Long) stateLao.getLastModified(), laoRepo.getLaoByChannel(LAO_CHANNEL1).getLastModified());
    assertEquals(
        stateLao.getModificationId(), laoRepo.getLaoByChannel(LAO_CHANNEL1).getModificationId());
  }

  @Test
  public void testHandleStateLaoWitness()
      throws DataHandlingException, UnknownLaoException, UnknownRollCallException,
      UnknownElectionException, NoRollCallException {
    // Main public key is a witness now, and not the organizer
    lenient().when(keyManager.getMainPublicKey()).thenReturn(SENDER2);

    // Create the state LAO message with one witness that has the main public key
    StateLao stateLao =
        new StateLao(
            CREATE_LAO1.getId(),
            CREATE_LAO1.getName(),
            CREATE_LAO1.getCreation(),
            Instant.now().getEpochSecond(),
            CREATE_LAO1.getOrganizer(),
            createLaoMessage.getMessageId(),
            new HashSet<>(WITNESS),
            new ArrayList<>());
    MessageGeneral message = new MessageGeneral(SENDER_KEY1, stateLao, gson);

    // Call the message handler
    messageHandler.handleMessage(messageSender, LAO_CHANNEL1, message);

    // Check the LAO last modification time and ID was updated
    assertEquals(
        (Long) stateLao.getLastModified(), laoRepo.getLaoByChannel(LAO_CHANNEL1).getLastModified());
    assertEquals(
        stateLao.getModificationId(), laoRepo.getLaoByChannel(LAO_CHANNEL1).getModificationId());
  }

  @Test
  public void testHandleStateLaoRemovesStalePendingUpdates()
      throws DataHandlingException, UnknownLaoException, UnknownRollCallException,
      UnknownElectionException, NoRollCallException {
    // Create a list of 2 pending updates: one newer and one older than the stateLao message
    long targetTime = CREATE_LAO1.getCreation() + 5;
    PendingUpdate oldPendingUpdate = mock(PendingUpdate.class);
    PendingUpdate newPendingUpdate = mock(PendingUpdate.class);
    when(oldPendingUpdate.getModificationTime()).thenReturn(targetTime - 1);
    when(newPendingUpdate.getModificationTime()).thenReturn(targetTime + 1);
    Set<PendingUpdate> pendingUpdates = new HashSet<>(
        Arrays.asList(oldPendingUpdate, newPendingUpdate));

    // Add the list of pending updates to the LAO
    Lao createdLao = laoRepo.getLaoByChannel(LAO_CHANNEL1);
    createdLao.setPendingUpdates(pendingUpdates);

    // Create the stateLao message
    StateLao stateLao =
        new StateLao(
            CREATE_LAO1.getId(),
            CREATE_LAO1.getName(),
            CREATE_LAO1.getCreation(),
            targetTime,
            CREATE_LAO1.getOrganizer(),
            createLaoMessage.getMessageId(),
            new HashSet<>(),
            new ArrayList<>());
    MessageGeneral stateMessage = new MessageGeneral(SENDER_KEY1, stateLao, gson);

    // Call the message handler
    messageHandler.handleMessage(messageSender, LAO_CHANNEL1, stateMessage);

    // The old pending update is removed in the expected pending list
    Set<PendingUpdate> expectedPending = new HashSet<>(Collections.singletonList(newPendingUpdate));
    assertEquals(expectedPending, laoRepo.getLaoByChannel(LAO_CHANNEL1).getPendingUpdates());
  }

  @Test
  public void testHandleStateLaoInvalidMessageId() {
    // Create some message that has an invalid ID
    MessageGeneral createLaoMessage2 = new MessageGeneral(SENDER_KEY1, CREATE_LAO2, gson);

    // Create the state LAO message
    StateLao stateLao =
        new StateLao(
            CREATE_LAO1.getId(),
            CREATE_LAO1.getName(),
            CREATE_LAO1.getCreation(),
            Instant.now().getEpochSecond(),
            CREATE_LAO1.getOrganizer(),
            createLaoMessage2.getMessageId(),
            new HashSet<>(),
            new ArrayList<>());
    MessageGeneral message = new MessageGeneral(SENDER_KEY1, stateLao, gson);

    // Check that handling the message with invalid ID fails
    assertThrows(
        InvalidMessageIdException.class,
        () -> messageHandler.handleMessage(messageSender, LAO_CHANNEL1, message));
  }

  @Test
  public void testHandleStateLaoInvalidSignatures() {
    // Create a list with invalid modification signature
    List<PublicKeySignaturePair> modificationSignatures = getInvalidModificationSignatures();

    // Create the a state LAO message with invalid modification signatures
    StateLao stateLao =
        new StateLao(
            CREATE_LAO1.getId(),
            CREATE_LAO1.getName(),
            CREATE_LAO1.getCreation(),
            Instant.now().getEpochSecond(),
            CREATE_LAO1.getOrganizer(),
            createLaoMessage.getMessageId(),
            new HashSet<>(),
            modificationSignatures);
    MessageGeneral message = new MessageGeneral(SENDER_KEY1, stateLao, gson);

    // Check that handling the message with invalid signatures fails
    assertThrows(
        InvalidSignatureException.class,
        () -> messageHandler.handleMessage(messageSender, LAO_CHANNEL1, message));
  }

  @Test()
  public void testGreetLao()
      throws DataHandlingException, UnknownLaoException, UnknownRollCallException,
      UnknownElectionException, NoRollCallException {
    // Create the Greet Lao
    GreetLao greetLao =
        new GreetLao(
            lao.getId(), RANDOM_KEY, RANDOM_ADDRESS, Collections.singletonList(RANDOM_PEER));
    MessageGeneral message = new MessageGeneral(SENDER_KEY1, greetLao, gson);

    // Call the handler
    messageHandler.handleMessage(messageSender, LAO_CHANNEL1, message);

    // Check that the server repository contains the key of the server
    assertEquals(RANDOM_ADDRESS, serverRepository.getServerByLaoId(lao.getId()).getServerAddress());
    // Check that it contains the key as well
    assertEquals(
        new PublicKey(RANDOM_KEY), serverRepository.getServerByLaoId(lao.getId()).getPublicKey());

    // Test for invalid LAO Id
    GreetLao greetLao_invalid =
        new GreetLao("123", RANDOM_KEY, RANDOM_ADDRESS, Collections.singletonList(RANDOM_PEER));
    MessageGeneral message_invalid = new MessageGeneral(SENDER_KEY1, greetLao_invalid, gson);
    assertThrows(
        IllegalArgumentException.class,
        () -> messageHandler.handleMessage(messageSender, LAO_CHANNEL1, message_invalid));
  }

  private static List<PublicKeySignaturePair> getValidModificationSignatures(
      MessageGeneral messageGeneral) {
    PublicKeySignaturePair validKeyPair;
    try {
      validKeyPair = new PublicKeySignaturePair(SENDER1,
          SENDER_KEY1.sign(messageGeneral.getMessageId()));
    } catch (GeneralSecurityException e) {
      throw new RuntimeException(e);
    }
    return Collections.singletonList(validKeyPair);
  }

  private static List<PublicKeySignaturePair> getInvalidModificationSignatures() {
    PublicKeySignaturePair invalidKeyPair =
        new PublicKeySignaturePair(generatePublicKey(), generateSignature());
    return new ArrayList<>(Collections.singletonList(invalidKeyPair));
  }
}
