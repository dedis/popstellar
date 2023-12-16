package com.github.dedis.popstellar.utility.handler;

import static com.github.dedis.popstellar.testutils.Base64DataUtils.*;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import android.app.Application;
import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.github.dedis.popstellar.di.DataRegistryModuleHelper;
import com.github.dedis.popstellar.di.JsonModule;
import com.github.dedis.popstellar.model.network.method.message.MessageGeneral;
import com.github.dedis.popstellar.model.network.method.message.data.DataRegistry;
import com.github.dedis.popstellar.model.network.method.message.data.lao.CreateLao;
import com.github.dedis.popstellar.model.network.method.message.data.message.WitnessMessageSignature;
import com.github.dedis.popstellar.model.objects.*;
import com.github.dedis.popstellar.model.objects.security.*;
import com.github.dedis.popstellar.repository.*;
import com.github.dedis.popstellar.repository.database.AppDatabase;
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
import io.reactivex.Completable;
import io.reactivex.Single;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(AndroidJUnit4.class)
public class WitnessingHandlerTest {

  private static final KeyPair ORGANIZER_KEY = generateKeyPair();
  private static final KeyPair WITNESS_KEY = generateKeyPair();
  private static final PublicKey ORGANIZER = ORGANIZER_KEY.publicKey;
  private static final PublicKey WITNESS = WITNESS_KEY.publicKey;
  private static final List<PublicKey> WITNESSES =
      new ArrayList<>(Arrays.asList(ORGANIZER, WITNESS));
  private static final PoPToken POP_TOKEN = generatePoPToken();

  private static final CreateLao CREATE_LAO = new CreateLao("lao", ORGANIZER, WITNESSES);
  private static final Channel LAO_CHANNEL = Channel.getLaoChannel(CREATE_LAO.id);
  private static final MessageID MESSAGE_ID1 = generateMessageID();
  private static final MessageID MESSAGE_ID2 = generateMessageIDOtherThan(MESSAGE_ID1);
  private static final WitnessMessage WITNESS_MESSAGE1 = new WitnessMessage(MESSAGE_ID1);
  private static final WitnessMessage WITNESS_MESSAGE2 = new WitnessMessage(MESSAGE_ID2);

  private LAORepository laoRepo;
  private static WitnessingRepository witnessingRepository;
  private MessageHandler messageHandler;
  private Gson gson;

  @Mock AppDatabase appDatabase;
  @Mock LAODao laoDao;
  @Mock MessageDao messageDao;
  @Mock WitnessDao witnessDao;
  @Mock WitnessingDao witnessingDao;
  @Mock PendingDao pendingDao;
  @Mock MessageSender messageSender;
  @Mock KeyManager keyManager;

  @Rule public InstantTaskExecutorRule rule = new InstantTaskExecutorRule();

  @Before
  public void setup()
      throws GeneralSecurityException, IOException, KeyException, UnknownRollCallException {
    MockitoAnnotations.openMocks(this);
    Application application = ApplicationProvider.getApplicationContext();

    lenient().when(keyManager.getMainKeyPair()).thenReturn(ORGANIZER_KEY);
    lenient().when(keyManager.getMainPublicKey()).thenReturn(ORGANIZER);
    lenient().when(keyManager.getValidPoPToken(any(), any())).thenReturn(POP_TOKEN);

    lenient().when(messageSender.subscribe(any())).then(args -> Completable.complete());

    when(appDatabase.laoDao()).thenReturn(laoDao);
    when(laoDao.getAllLaos()).thenReturn(Single.just(new ArrayList<>()));
    when(laoDao.insert(any(LAOEntity.class))).thenReturn(Completable.complete());

    when(appDatabase.messageDao()).thenReturn(messageDao);
    when(messageDao.takeFirstNMessages(anyInt())).thenReturn(Single.just(new ArrayList<>()));
    when(messageDao.insert(any(MessageEntity.class))).thenReturn(Completable.complete());
    when(messageDao.getMessageById(any(MessageID.class))).thenReturn(null);

    when(appDatabase.witnessDao()).thenReturn(witnessDao);
    when(witnessDao.getWitnessesByLao(anyString())).thenReturn(Single.just(new ArrayList<>()));
    when(witnessDao.insertAll(any())).thenReturn(Completable.complete());
    when(witnessDao.isWitness(anyString(), any(PublicKey.class))).thenReturn(0);

    when(appDatabase.witnessingDao()).thenReturn(witnessingDao);
    when(witnessingDao.getWitnessMessagesByLao(anyString()))
        .thenReturn(Single.just(new ArrayList<>()));
    when(witnessingDao.insert(any(WitnessingEntity.class))).thenReturn(Completable.complete());
    when(witnessingDao.deleteMessagesByIds(anyString(), any())).thenReturn(Completable.complete());

    when(appDatabase.pendingDao()).thenReturn(pendingDao);
    when(pendingDao.getPendingObjectsFromLao(anyString()))
        .thenReturn(Single.just(new ArrayList<>()));
    when(pendingDao.insert(any(PendingEntity.class))).thenReturn(Completable.complete());
    when(pendingDao.removePendingObject(any(MessageID.class))).thenReturn(Completable.complete());

    laoRepo = new LAORepository(appDatabase, application);
    RollCallRepository rollCallRepo = new RollCallRepository(appDatabase, application);
    ElectionRepository electionRepo = new ElectionRepository(appDatabase, application);
    MeetingRepository meetingRepo = new MeetingRepository(appDatabase, application);
    DigitalCashRepository digitalCashRepo = new DigitalCashRepository(appDatabase, application);
    witnessingRepository =
        new WitnessingRepository(
            appDatabase, application, rollCallRepo, electionRepo, meetingRepo, digitalCashRepo);
    MessageRepository messageRepo = new MessageRepository(appDatabase, application);

    DataRegistry dataRegistry =
        DataRegistryModuleHelper.buildRegistry(laoRepo, witnessingRepository, keyManager);

    gson = JsonModule.provideGson(dataRegistry);
    messageHandler = new MessageHandler(messageRepo, dataRegistry);

    // Create one LAO
    Lao LAO = new Lao(CREATE_LAO.name, CREATE_LAO.organizer, CREATE_LAO.creation);
    LAO.lastModified = LAO.creation;
    LAO.initKeyToNode(new HashSet<>(CREATE_LAO.getWitnesses()));

    witnessingRepository.addWitnesses(LAO.getId(), new HashSet<>(WITNESSES));
    witnessingRepository.addWitnessMessage(LAO.getId(), WITNESS_MESSAGE1);
    witnessingRepository.addWitnessMessage(LAO.getId(), WITNESS_MESSAGE2);

    // Add the LAO to the LAORepository
    laoRepo.updateLao(LAO);

    // Add the CreateLao message to the LAORepository
    MessageGeneral createLaoMessage = new MessageGeneral(ORGANIZER_KEY, CREATE_LAO, gson);
    messageRepo.addMessage(createLaoMessage, false, false);
  }

  @Test
  public void testHandleWitnessMessageSignatureFromOrganizer()
      throws GeneralSecurityException,
          UnknownElectionException,
          UnknownRollCallException,
          UnknownLaoException,
          DataHandlingException,
          NoRollCallException,
          UnknownWitnessMessageException {
    // Create a valid witnessMessageSignature signed by the organizer
    Signature signature = ORGANIZER_KEY.sign(MESSAGE_ID1);
    WitnessMessageSignature witnessMessageSignature =
        new WitnessMessageSignature(MESSAGE_ID1, signature);
    MessageGeneral message = new MessageGeneral(ORGANIZER_KEY, witnessMessageSignature, gson);

    // Handle the witnessMessageSignature
    messageHandler.handleMessage(messageSender, LAO_CHANNEL, message);

    // Check that the witness message in the lao repo was updated with the organizer public key
    Lao lao = laoRepo.getLaoByChannel(LAO_CHANNEL);

    Optional<WitnessMessage> witnessMessage =
        witnessingRepository.getWitnessMessage(lao.getId(), MESSAGE_ID1);
    assertTrue(witnessMessage.isPresent());

    HashSet<PublicKey> expectedWitnesses = new HashSet<>();
    expectedWitnesses.add(ORGANIZER);

    assertEquals(expectedWitnesses, witnessMessage.get().getWitnesses());
  }

  @Test
  public void testHandleWitnessMessageSignatureFromWitness()
      throws GeneralSecurityException,
          UnknownElectionException,
          UnknownRollCallException,
          UnknownLaoException,
          DataHandlingException,
          NoRollCallException,
          UnknownWitnessMessageException {
    // Create a valid witnessMessageSignature signed by a witness
    Signature signature = WITNESS_KEY.sign(MESSAGE_ID2);
    WitnessMessageSignature witnessMessageSignature =
        new WitnessMessageSignature(MESSAGE_ID2, signature);
    MessageGeneral message = new MessageGeneral(WITNESS_KEY, witnessMessageSignature, gson);

    // Handle the witnessMessageSignature
    messageHandler.handleMessage(messageSender, LAO_CHANNEL, message);

    // Check that the witness message in the lao repo was updated with the witness public key
    Lao lao = laoRepo.getLaoByChannel(LAO_CHANNEL);

    Optional<WitnessMessage> witnessMessage =
        witnessingRepository.getWitnessMessage(lao.getId(), MESSAGE_ID2);
    assertTrue(witnessMessage.isPresent());

    HashSet<PublicKey> expectedWitnesses = new HashSet<>();
    expectedWitnesses.add(WITNESS);

    assertEquals(expectedWitnesses, witnessMessage.get().getWitnesses());
  }

  @Test
  public void testHandleWitnessMessageSignatureFromNonWitness() throws GeneralSecurityException {
    // Create a witnessMessageSignature signed by a non witness
    KeyPair invalidKeyPair = generateKeyPair();
    Signature signature = invalidKeyPair.sign(MESSAGE_ID1);
    WitnessMessageSignature witnessMessageSignature =
        new WitnessMessageSignature(MESSAGE_ID1, signature);
    MessageGeneral message = new MessageGeneral(invalidKeyPair, witnessMessageSignature, gson);

    assertThrows(
        InvalidWitnessingException.class,
        () -> messageHandler.handleMessage(messageSender, LAO_CHANNEL, message));
  }

  @Test
  public void testHandleWitnessMessageSignatureWithInvalidSignature() {
    // Create a witnessMessageSignature with an invalid signature
    WitnessMessageSignature witnessMessageSignature =
        new WitnessMessageSignature(MESSAGE_ID1, generateSignature());
    MessageGeneral message = new MessageGeneral(ORGANIZER_KEY, witnessMessageSignature, gson);

    assertThrows(
        InvalidSignatureException.class,
        () -> messageHandler.handleMessage(messageSender, LAO_CHANNEL, message));
  }

  @Test
  public void testHandleWitnessMessageSignatureWithNonExistentWitnessMessage()
      throws GeneralSecurityException {
    // Create a witnessMessageSignature for a witness message that does not exist in the lao
    MessageID invalidMessageID = generateMessageIDOtherThan(MESSAGE_ID1);
    Signature signature = ORGANIZER_KEY.sign(invalidMessageID);
    WitnessMessageSignature witnessMessageSignature =
        new WitnessMessageSignature(invalidMessageID, signature);
    MessageGeneral message = new MessageGeneral(ORGANIZER_KEY, witnessMessageSignature, gson);

    assertThrows(
        InvalidWitnessingException.class,
        () -> messageHandler.handleMessage(messageSender, LAO_CHANNEL, message));
  }
}
