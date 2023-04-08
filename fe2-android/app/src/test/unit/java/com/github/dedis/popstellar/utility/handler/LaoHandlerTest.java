package com.github.dedis.popstellar.utility.handler;

import com.github.dedis.popstellar.di.DataRegistryModuleHelper;
import com.github.dedis.popstellar.di.JsonModule;
import com.github.dedis.popstellar.model.network.method.message.MessageGeneral;
import com.github.dedis.popstellar.model.network.method.message.PublicKeySignaturePair;
import com.github.dedis.popstellar.model.network.method.message.data.DataRegistry;
import com.github.dedis.popstellar.model.network.method.message.data.lao.*;
import com.github.dedis.popstellar.model.objects.*;
import com.github.dedis.popstellar.model.objects.security.*;
import com.github.dedis.popstellar.model.objects.view.LaoView;
import com.github.dedis.popstellar.repository.*;
import com.github.dedis.popstellar.repository.remote.MessageSender;
import com.github.dedis.popstellar.utility.error.*;
import com.github.dedis.popstellar.utility.error.keys.NoRollCallException;
import com.github.dedis.popstellar.utility.security.KeyManager;
import com.google.gson.Gson;

import org.checkerframework.checker.units.qual.C;
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
  private static final PublicKey WITNESS1_KEY = generatePublicKey();
  private static final PublicKey WITNESS2_KEY = generatePublicKey();
  private static final PublicKey SENDER1 = SENDER_KEY1.getPublicKey();
  private static final PublicKey SENDER2 = SENDER_KEY2.getPublicKey();
  private static final long CREATION = Instant.now().getEpochSecond();
  private static final String NAME1 = "lao1";
  private static final String NAME2 = "lao2";
  private static final String ID1 = Lao.generateLaoId(SENDER1, CREATION, NAME1);
  private static final String ID2 = Lao.generateLaoId(SENDER1, CREATION, NAME2);
  private static final CreateLao CREATE_LAO1 =
      new CreateLao(ID1, NAME1, CREATION, SENDER1, new ArrayList<>());
  private static final CreateLao CREATE_LAO2 =
      new CreateLao(ID2, NAME2, CREATION, SENDER1, new ArrayList<>());

  private static final Channel LAO_CHANNEL1 = Channel.getLaoChannel(CREATE_LAO1.getId());
  private static final Channel LAO_CHANNEL2 = Channel.getLaoChannel(CREATE_LAO2.getId());

  private LAORepository laoRepo;
  private MessageHandler messageHandler;
  private ServerRepository serverRepository;
  private Gson gson;

  public static final String RANDOM_KEY = "oOcKZjUeandJOFVgn-E6e-7QksviBBbHUPicdzUgIm8";
  public static final String RANDOM_ADDRESS = "ws://10.0.2.2:9000/organizer/client";
  public static final PeerAddress RANDOM_PEER = new PeerAddress("ws://128.0.0.2:8001/");

  private Lao lao;
  private MessageGeneral createLaoMessage1;

  @Mock MessageSender messageSender;
  @Mock KeyManager keyManager;

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
    createLaoMessage1 = new MessageGeneral(SENDER_KEY1, CREATE_LAO1, gson);
    messageRepo.addMessage(createLaoMessage1);
  }

  @Test
  public void testHandleCreateLaoOrganizer()
      throws DataHandlingException, UnknownLaoException, UnknownRollCallException,
          UnknownElectionException, NoRollCallException {

    // Create the message and call the message handler
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
    List<PublicKey> witnesses = new ArrayList<>();
    witnesses.add(SENDER2);

    // Create the message and call the message handler
    CreateLao createLao =
        new CreateLao(ID2, NAME2, CREATION, SENDER1, witnesses);
    MessageGeneral message = new MessageGeneral(SENDER_KEY1, createLao, gson);
    messageHandler.handleMessage(messageSender, LAO_CHANNEL2, message);

    // Get expected results
    Lao resultLao = laoRepo.getLaoByChannel(LAO_CHANNEL2);
    String expectedName = createLao.getName();
    PublicKey expectedOrganizer = createLao.getOrganizer();
    long expectedCreation = createLao.getCreation();
    String expectedID = Lao.generateLaoId(expectedOrganizer, expectedCreation, expectedName);

    // Check that the expected LAO was created in the LAO repo
    assertEquals(LAO_CHANNEL2, resultLao.getChannel());
    assertEquals(expectedID, resultLao.getId());
    assertEquals(expectedName, resultLao.getName());
    assertEquals(expectedCreation, (long) resultLao.getLastModified());
    assertEquals(expectedCreation, (long) resultLao.getCreation());
    assertEquals(expectedOrganizer, resultLao.getOrganizer());
    assertEquals(new HashSet<>(witnesses), resultLao.getWitnesses());
    assertTrue(resultLao.getWitnessMessages().isEmpty());
    assertNull(resultLao.getModificationId());
  }

  @Test
  public void testHandleUpdateLaoNewName()
      throws DataHandlingException, UnknownLaoException, UnknownRollCallException,
          UnknownElectionException, NoRollCallException {
    // Create the update LAO message
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
    lao.setWitnesses(new HashSet<>(Arrays.asList(WITNESS1_KEY)));

    // Create UpdateLao with different witness set
    UpdateLao updateLao =
        new UpdateLao(
            SENDER1,
            CREATE_LAO1.getCreation(),
            CREATE_LAO1.getName(),
            Instant.now().getEpochSecond(),
            new HashSet<>(Arrays.asList(WITNESS1_KEY, WITNESS2_KEY)));
    MessageGeneral message = new MessageGeneral(SENDER_KEY1, updateLao, gson);

    // Create the expected WitnessMessage
    WitnessMessage expectedMessage =
        updateLaoWitnessesWitnessMessage(message.getMessageId(), updateLao, new LaoView(lao));

    // Call the handler
    messageHandler.handleMessage(messageSender, LAO_CHANNEL1, message);

    // Check the WitnessMessage has been created
    Optional<WitnessMessage> witnessMessage =
        laoRepo.getLaoByChannel(LAO_CHANNEL1).getWitnessMessage(message.getMessageId());
    assertTrue(witnessMessage.isPresent());
    assertEquals(expectedMessage.getTitle(), witnessMessage.get().getTitle());
    assertEquals(expectedMessage.getDescription(), witnessMessage.get().getDescription());
  }

  @Test
  public void testHandleUpdateLaoOldInfo()
      throws DataHandlingException, UnknownLaoException, UnknownRollCallException,
          UnknownElectionException, NoRollCallException {
    // Create UpdateLao with no new name or witness
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
  public void testHandleUpdateLaoStale()
      throws DataHandlingException, UnknownLaoException, UnknownRollCallException,
          UnknownElectionException, NoRollCallException {
    // Create a LAO update message with last modified time older than the current LAO last modified
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

    // Create a valid public key signature pair to add to modification signatures
    List<PublicKeySignaturePair> modificationSignatures = new ArrayList<>();

    PublicKeySignaturePair validKeyPair = null;
    try {
      validKeyPair = new PublicKeySignaturePair(SENDER1, SENDER_KEY1.sign(createLaoMessage1.getMessageId()));
    } catch (GeneralSecurityException e) {
      throw new RuntimeException(e);
    }
    modificationSignatures.add(validKeyPair);

    // Create the state LAO message
    StateLao stateLao =
        new StateLao(
            CREATE_LAO1.getId(),
            CREATE_LAO1.getName(),
            CREATE_LAO1.getCreation(),
            Instant.now().getEpochSecond(),
            CREATE_LAO1.getOrganizer(),
            createLaoMessage1.getMessageId(),
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
    Set<PublicKey> witnesses = new HashSet<>();
    witnesses.add(SENDER2);

    // Create the state LAO message
    StateLao stateLao =
        new StateLao(
            CREATE_LAO1.getId(),
            CREATE_LAO1.getName(),
            CREATE_LAO1.getCreation(),
            Instant.now().getEpochSecond(),
            CREATE_LAO1.getOrganizer(),
            createLaoMessage1.getMessageId(),
            witnesses,
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
  public void testHandleStateLaoInvalidMessageId()
      throws DataHandlingException, UnknownLaoException, UnknownRollCallException,
          UnknownElectionException, NoRollCallException {
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
  public void testHandleStateLaoInvalidSignatures()
      throws DataHandlingException, UnknownLaoException, UnknownRollCallException,
          UnknownElectionException, NoRollCallException {
    PublicKeySignaturePair invalidKeyPair =
        new PublicKeySignaturePair(generatePublicKey(), generateSignature());
    List<PublicKeySignaturePair> modificationSignatures = new ArrayList<>();
    modificationSignatures.add(invalidKeyPair);

    // Create the a state LAO message with invalid modification signatures
    StateLao stateLao =
        new StateLao(
            CREATE_LAO1.getId(),
            CREATE_LAO1.getName(),
            CREATE_LAO1.getCreation(),
            Instant.now().getEpochSecond(),
            CREATE_LAO1.getOrganizer(),
            createLaoMessage1.getMessageId(),
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
}
