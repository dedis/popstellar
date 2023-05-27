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
import com.github.dedis.popstellar.model.network.method.message.data.message.WitnessMessageSignature;
import com.github.dedis.popstellar.model.objects.*;
import com.github.dedis.popstellar.model.objects.security.*;
import com.github.dedis.popstellar.repository.LAORepository;
import com.github.dedis.popstellar.repository.MessageRepository;
import com.github.dedis.popstellar.repository.database.AppDatabase;
import com.github.dedis.popstellar.repository.database.lao.LAODao;
import com.github.dedis.popstellar.repository.database.lao.LAOEntity;
import com.github.dedis.popstellar.repository.database.message.MessageDao;
import com.github.dedis.popstellar.repository.database.message.MessageEntity;
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
import java.util.*;

import io.reactivex.Completable;
import io.reactivex.Single;

import static com.github.dedis.popstellar.testutils.Base64DataUtils.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@RunWith(AndroidJUnit4.class)
public class WitnessingHandlerTest {

  private static final KeyPair ORGANIZER_KEY = generateKeyPair();
  private static final KeyPair WITNESS_KEY = generateKeyPair();
  private static final PublicKey ORGANIZER = ORGANIZER_KEY.getPublicKey();
  private static final PublicKey WITNESS = WITNESS_KEY.getPublicKey();
  private static final List<PublicKey> WITNESSES =
      new ArrayList<>(Arrays.asList(ORGANIZER, WITNESS));
  private static final PoPToken POP_TOKEN = generatePoPToken();

  private static final CreateLao CREATE_LAO = new CreateLao("lao", ORGANIZER, WITNESSES);
  private static final Channel LAO_CHANNEL = Channel.getLaoChannel(CREATE_LAO.getId());
  private static final MessageID MESSAGE_ID = generateMessageID();
  private static final WitnessMessage WITNESS_MESSAGE = new WitnessMessage(MESSAGE_ID);
  private static Lao LAO;

  private LAORepository laoRepo;
  private MessageHandler messageHandler;
  private Gson gson;

  @Mock AppDatabase appDatabase;
  @Mock LAODao laoDao;
  @Mock MessageDao messageDao;
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

    laoRepo = new LAORepository(appDatabase, application);
    MessageRepository messageRepo = new MessageRepository(appDatabase, application);

    DataRegistry dataRegistry = DataRegistryModuleHelper.buildRegistry(laoRepo, keyManager);

    gson = JsonModule.provideGson(dataRegistry);
    messageHandler = new MessageHandler(messageRepo, dataRegistry);

    // Create one LAO
    LAO = new Lao(CREATE_LAO.getName(), CREATE_LAO.getOrganizer(), CREATE_LAO.getCreation());
    LAO.setLastModified(LAO.getCreation());
    LAO.setWitnesses(new HashSet<>(CREATE_LAO.getWitnesses()));
    LAO.addWitnessMessage(WITNESS_MESSAGE);

    // Add the LAO to the LAORepository
    laoRepo.updateLao(LAO);

    // Add the CreateLao message to the LAORepository
    MessageGeneral createLaoMessage = new MessageGeneral(ORGANIZER_KEY, CREATE_LAO, gson);
    messageRepo.addMessage(createLaoMessage, false, false);
  }

  @Test
  public void testHandleWitnessMessageSignatureFromOrganizer()
      throws GeneralSecurityException, UnknownElectionException, UnknownRollCallException,
          UnknownLaoException, DataHandlingException, NoRollCallException {
    // Create a valid witnessMessageSignature signed by the organizer
    Signature signature = ORGANIZER_KEY.sign(MESSAGE_ID);
    WitnessMessageSignature witnessMessageSignature =
        new WitnessMessageSignature(MESSAGE_ID, signature);
    MessageGeneral message = new MessageGeneral(ORGANIZER_KEY, witnessMessageSignature, gson);

    // Handle the witnessMessageSignature
    messageHandler.handleMessage(messageSender, LAO_CHANNEL, message);

    // Check that the witness message in the lao repo was updated with the organizer public key
    Lao lao = laoRepo.getLaoByChannel(LAO_CHANNEL);
    WitnessMessage witnessMessage = lao.getWitnessMessages().get(MESSAGE_ID);
    HashSet<PublicKey> expectedWitnesses = new HashSet<>();
    expectedWitnesses.add(ORGANIZER);

    assertEquals(expectedWitnesses, witnessMessage.getWitnesses());
  }

  @Test
  public void testHandleWitnessMessageSignatureFromWitness()
      throws GeneralSecurityException, UnknownElectionException, UnknownRollCallException,
          UnknownLaoException, DataHandlingException, NoRollCallException {
    // Create a valid witnessMessageSignature signed by a witness
    Signature signature = WITNESS_KEY.sign(MESSAGE_ID);
    WitnessMessageSignature witnessMessageSignature =
        new WitnessMessageSignature(MESSAGE_ID, signature);
    MessageGeneral message = new MessageGeneral(WITNESS_KEY, witnessMessageSignature, gson);

    // Handle the witnessMessageSignature
    messageHandler.handleMessage(messageSender, LAO_CHANNEL, message);

    // Check that the witness message in the lao repo was updated with the witness public key
    Lao lao = laoRepo.getLaoByChannel(LAO_CHANNEL);
    WitnessMessage witnessMessage = lao.getWitnessMessages().get(MESSAGE_ID);
    HashSet<PublicKey> expectedWitnesses = new HashSet<>();
    expectedWitnesses.add(WITNESS);

    assertEquals(expectedWitnesses, witnessMessage.getWitnesses());
  }

  @Test
  public void testHandleWitnessMessageSignatureFromNonWitness() throws GeneralSecurityException {
    // Create a witnessMessageSignature signed by a non witness
    KeyPair invalidKeyPair = generateKeyPair();
    Signature signature = invalidKeyPair.sign(MESSAGE_ID);
    WitnessMessageSignature witnessMessageSignature =
        new WitnessMessageSignature(MESSAGE_ID, signature);
    MessageGeneral message = new MessageGeneral(invalidKeyPair, witnessMessageSignature, gson);

    assertThrows(
        InvalidWitnessingException.class,
        () -> messageHandler.handleMessage(messageSender, LAO_CHANNEL, message));
  }

  @Test
  public void testHandleWitnessMessageSignatureWithInvalidSignature() {
    // Create a witnessMessageSignature with an invalid signature
    WitnessMessageSignature witnessMessageSignature =
        new WitnessMessageSignature(MESSAGE_ID, generateSignature());
    MessageGeneral message = new MessageGeneral(ORGANIZER_KEY, witnessMessageSignature, gson);

    assertThrows(
        InvalidSignatureException.class,
        () -> messageHandler.handleMessage(messageSender, LAO_CHANNEL, message));
  }

  @Test
  public void testHandleWitnessMessageSignatureWithNonExistentWitnessMessage()
      throws GeneralSecurityException {
    // Create a witnessMessageSignature for a witness message that does not exist in the lao
    MessageID invalidMessageID = generateMessageIDOtherThan(MESSAGE_ID);
    Signature signature = ORGANIZER_KEY.sign(invalidMessageID);
    WitnessMessageSignature witnessMessageSignature =
        new WitnessMessageSignature(invalidMessageID, signature);
    MessageGeneral message = new MessageGeneral(ORGANIZER_KEY, witnessMessageSignature, gson);

    assertThrows(
        InvalidWitnessingException.class,
        () -> messageHandler.handleMessage(messageSender, LAO_CHANNEL, message));
  }
}
