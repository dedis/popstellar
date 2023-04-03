package com.github.dedis.popstellar.utility.handler;

import com.github.dedis.popstellar.di.DataRegistryModuleHelper;
import com.github.dedis.popstellar.di.JsonModule;
import com.github.dedis.popstellar.model.network.method.message.MessageGeneral;
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

import static com.github.dedis.popstellar.testutils.Base64DataUtils.generateKeyPair;
import static com.github.dedis.popstellar.utility.handler.data.LaoHandler.updateLaoNameWitnessMessage;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class LaoHandlerTest {

  private static final KeyPair SENDER_KEY = generateKeyPair();
  private static final PublicKey SENDER = SENDER_KEY.getPublicKey();

  private static final CreateLao CREATE_LAO = new CreateLao("lao", SENDER);
  private static final Channel LAO_CHANNEL = Channel.getLaoChannel(CREATE_LAO.getId());

  private LAORepository laoRepo;
  private MessageHandler messageHandler;
  private ServerRepository serverRepository;
  private Gson gson;

  public static final String RANDOM_KEY = "oOcKZjUeandJOFVgn-E6e-7QksviBBbHUPicdzUgIm8";
  public static final String RANDOM_ADDRESS = "ws://10.0.2.2:9000/organizer/client";
  public static final PeerAddress RANDOM_PEER = new PeerAddress("ws://128.0.0.2:8001/");

  private Lao lao;
  private MessageGeneral createLaoMessage;

  @Mock MessageSender messageSender;
  @Mock KeyManager keyManager;

  @Before
  public void setup() throws GeneralSecurityException, IOException {
    lenient().when(keyManager.getMainKeyPair()).thenReturn(SENDER_KEY);
    lenient().when(keyManager.getMainPublicKey()).thenReturn(SENDER);

    when(messageSender.subscribe(any())).then(args -> Completable.complete());

    laoRepo = new LAORepository();
    serverRepository = new ServerRepository();
    MessageRepository messageRepo = new MessageRepository();

    DataRegistry dataRegistry =
        DataRegistryModuleHelper.buildRegistry(laoRepo, messageRepo, keyManager, serverRepository);
    gson = JsonModule.provideGson(dataRegistry);
    messageHandler = new MessageHandler(messageRepo, dataRegistry);

    // Create one LAO and add it to the LAORepository
    lao = new Lao(CREATE_LAO.getName(), CREATE_LAO.getOrganizer(), CREATE_LAO.getCreation());
    lao.setLastModified(lao.getCreation());
    laoRepo.updateLao(lao);

    // Add the CreateLao message to the LAORepository
    createLaoMessage = new MessageGeneral(SENDER_KEY, CREATE_LAO, gson);
    messageRepo.addMessage(createLaoMessage);
  }

  @Test
  public void testHandleUpdateLao()
      throws DataHandlingException, UnknownLaoException, UnknownRollCallException,
          UnknownElectionException, NoRollCallException {
    // Create the update LAO message
    UpdateLao updateLao =
        new UpdateLao(
            SENDER,
            CREATE_LAO.getCreation(),
            "new name",
            Instant.now().getEpochSecond(),
            new HashSet<>());
    MessageGeneral message = new MessageGeneral(SENDER_KEY, updateLao, gson);

    // Create the expected WitnessMessage
    WitnessMessage expectedMessage =
        updateLaoNameWitnessMessage(message.getMessageId(), updateLao, new LaoView(lao));

    // Call the message handler
    messageHandler.handleMessage(messageSender, LAO_CHANNEL, message);

    // Check the WitnessMessage has been created
    Optional<WitnessMessage> witnessMessage =
        laoRepo.getLaoByChannel(LAO_CHANNEL).getWitnessMessage(message.getMessageId());
    assertTrue(witnessMessage.isPresent());
    assertEquals(expectedMessage.getTitle(), witnessMessage.get().getTitle());
    assertEquals(expectedMessage.getDescription(), witnessMessage.get().getDescription());
  }

  @Test
  public void testHandleStateLao()
      throws DataHandlingException, UnknownLaoException, UnknownRollCallException,
          UnknownElectionException, NoRollCallException {
    // Create the state LAO message
    StateLao stateLao =
        new StateLao(
            CREATE_LAO.getId(),
            CREATE_LAO.getName(),
            CREATE_LAO.getCreation(),
            Instant.now().getEpochSecond(),
            CREATE_LAO.getOrganizer(),
            createLaoMessage.getMessageId(),
            new HashSet<>(),
            new ArrayList<>());
    MessageGeneral message = new MessageGeneral(SENDER_KEY, stateLao, gson);

    // Call the message handler
    messageHandler.handleMessage(messageSender, LAO_CHANNEL, message);

    // Check the LAO last modification time and ID was updated
    assertEquals(
        (Long) stateLao.getLastModified(), laoRepo.getLaoByChannel(LAO_CHANNEL).getLastModified());
    assertEquals(
        stateLao.getModificationId(), laoRepo.getLaoByChannel(LAO_CHANNEL).getModificationId());
  }

  @Test
  public void testHandleStateLaos()
      throws DataHandlingException, UnknownLaoException, UnknownRollCallException,
          UnknownElectionException, NoRollCallException {
    // Create the state LAO message
    long time = CREATE_LAO.getCreation();
    StateLao stateLao =
        new StateLao(
            CREATE_LAO.getId(),
            CREATE_LAO.getName(),
            time,
            time - 1000,
            CREATE_LAO.getOrganizer(),
            createLaoMessage.getMessageId(),
            new HashSet<>(),
            new ArrayList<>());
    MessageGeneral message = new MessageGeneral(SENDER_KEY, stateLao, gson);

    // Call the message handler

    assertThrows(
        DataHandlingException.class,
        () -> messageHandler.handleMessage(messageSender, LAO_CHANNEL, message));
  }

  @Test()
  public void testGreetLao()
      throws DataHandlingException, UnknownLaoException, UnknownRollCallException,
          UnknownElectionException, NoRollCallException {
    // Create the Greet Lao
    GreetLao greetLao =
        new GreetLao(
            lao.getId(), RANDOM_KEY, RANDOM_ADDRESS, Collections.singletonList(RANDOM_PEER));

    MessageGeneral message = new MessageGeneral(SENDER_KEY, greetLao, gson);

    // Call the handler
    messageHandler.handleMessage(messageSender, LAO_CHANNEL, message);

    // Check that the server repository contains the key of the server
    assertEquals(RANDOM_ADDRESS, serverRepository.getServerByLaoId(lao.getId()).getServerAddress());
    // Check that it contains the key as well
    assertEquals(
        new PublicKey(RANDOM_KEY), serverRepository.getServerByLaoId(lao.getId()).getPublicKey());

    // Test for invalid LAO Id
    GreetLao greetLao_invalid =
        new GreetLao("123", RANDOM_KEY, RANDOM_ADDRESS, Collections.singletonList(RANDOM_PEER));
    MessageGeneral message_invalid = new MessageGeneral(SENDER_KEY, greetLao_invalid, gson);
    assertThrows(
        IllegalArgumentException.class,
        () -> messageHandler.handleMessage(messageSender, LAO_CHANNEL, message_invalid));
  }
}
