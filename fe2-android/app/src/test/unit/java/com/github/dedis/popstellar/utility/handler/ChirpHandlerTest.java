package com.github.dedis.popstellar.utility.handler;

import com.github.dedis.popstellar.di.DataRegistryModule;
import com.github.dedis.popstellar.di.JsonModule;
import com.github.dedis.popstellar.model.network.method.message.MessageGeneral;
import com.github.dedis.popstellar.model.network.method.message.data.lao.CreateLao;
import com.github.dedis.popstellar.model.network.method.message.data.socialmedia.AddChirp;
import com.github.dedis.popstellar.model.network.method.message.data.socialmedia.DeleteChirp;
import com.github.dedis.popstellar.model.objects.*;
import com.github.dedis.popstellar.model.objects.security.*;
import com.github.dedis.popstellar.model.objects.view.LaoView;
import com.github.dedis.popstellar.repository.*;
import com.github.dedis.popstellar.repository.remote.MessageSender;
import com.github.dedis.popstellar.utility.error.DataHandlingException;
import com.github.dedis.popstellar.utility.error.UnknownLaoException;
import com.github.dedis.popstellar.utility.security.KeyManager;
import com.google.gson.Gson;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.*;

import dagger.hilt.android.testing.HiltAndroidTest;
import io.reactivex.Completable;

import static com.github.dedis.popstellar.testutils.Base64DataUtils.generateKeyPair;
import static com.github.dedis.popstellar.testutils.Base64DataUtils.generateMessageID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@HiltAndroidTest
@RunWith(MockitoJUnitRunner.class)
public class ChirpHandlerTest {

  private static final KeyPair SENDER_KEY = generateKeyPair();
  private static final PublicKey SENDER = SENDER_KEY.getPublicKey();

  private static final long CREATION_TIME = 1631280815;
  private static final long DELETION_TIME = 1642244760;
  private static final String LAO_NAME = "laoName";
  private static final String LAO_ID = Lao.generateLaoId(SENDER, CREATION_TIME, LAO_NAME);
  private static Channel chirpChannel;

  private static final String TEXT = "textOfTheChirp";
  private static final String EMPTY_STRING = "";
  private static final MessageID PARENT_ID = generateMessageID();

  private static final CreateLao CREATE_LAO =
      new CreateLao(LAO_ID, LAO_NAME, CREATION_TIME, SENDER, new ArrayList<>());
  private static final AddChirp ADD_CHIRP = new AddChirp(TEXT, PARENT_ID, CREATION_TIME);

  private static final Gson GSON = JsonModule.provideGson(DataRegistryModule.provideDataRegistry());

  private MessageRepository messageRepo;
  private LAORepository laoRepo;
  private MessageHandler messageHandler;

  @Mock MessageSender messageSender;
  @Mock KeyManager keyManager;

  @Before
  public void setup()
      throws GeneralSecurityException, DataHandlingException, IOException, UnknownLaoException {
    lenient().when(keyManager.getMainKeyPair()).thenReturn(SENDER_KEY);
    lenient().when(keyManager.getMainPublicKey()).thenReturn(SENDER);

    when(messageSender.subscribe(any())).then(args -> Completable.complete());

    Channel channel = Channel.getLaoChannel(LAO_ID);

    messageRepo = new MessageRepository();
    laoRepo = new LAORepository();
    messageHandler =
        new MessageHandler(
            DataRegistryModule.provideDataRegistry(), keyManager, new ServerRepository());

    MessageGeneral createLaoMessage = new MessageGeneral(SENDER_KEY, CREATE_LAO, GSON);
    messageHandler.handleMessage(messageRepo, laoRepo, messageSender, channel, createLaoMessage);

    chirpChannel =
        laoRepo
            .getLaoView(LAO_ID)
            .getChannel()
            .subChannel("social")
            .subChannel(SENDER.getEncoded());
  }

  @Test
  public void testHandleAddChirp() throws DataHandlingException, UnknownLaoException {
    MessageGeneral message = new MessageGeneral(SENDER_KEY, ADD_CHIRP, GSON);
    messageHandler.handleMessage(messageRepo, laoRepo, messageSender, chirpChannel, message);

    LaoView laoView = laoRepo.getLaoView(LAO_ID);
    Lao updatedLao = laoView.createLaoCopy();

    Optional<Chirp> chirpOpt = updatedLao.getChirp(message.getMessageId());
    assertTrue(chirpOpt.isPresent());
    Chirp chirp = chirpOpt.get();

    assertEquals(message.getMessageId(), chirp.getId());
    assertEquals(chirpChannel, chirp.getChannel());
    assertEquals(SENDER, chirp.getSender());
    assertEquals(TEXT, chirp.getText());
    assertEquals(CREATION_TIME, chirp.getTimestamp());
    assertEquals(PARENT_ID, chirp.getParentId());

    Map<MessageID, Chirp> chirps = updatedLao.getAllChirps();
    assertEquals(1, chirps.size());
    assertEquals(chirp, chirps.get(chirp.getId()));
  }

  @Test
  public void testHandleDeleteChirp() throws DataHandlingException, UnknownLaoException {
    MessageGeneral message = new MessageGeneral(SENDER_KEY, ADD_CHIRP, GSON);
    messageHandler.handleMessage(messageRepo, laoRepo, messageSender, chirpChannel, message);

    final DeleteChirp DELETE_CHIRP = new DeleteChirp(message.getMessageId(), DELETION_TIME);

    MessageGeneral message2 = new MessageGeneral(SENDER_KEY, DELETE_CHIRP, GSON);
    messageHandler.handleMessage(messageRepo, laoRepo, messageSender, chirpChannel, message2);

    LaoView laoView = laoRepo.getLaoView(LAO_ID);
    Lao updatedLao = laoView.createLaoCopy();

    Optional<Chirp> chirpOpt = updatedLao.getChirp(message.getMessageId());
    assertTrue(chirpOpt.isPresent());
    Chirp chirp = chirpOpt.get();

    assertEquals(message.getMessageId(), chirp.getId());
    assertEquals(chirpChannel, chirp.getChannel());
    assertEquals(SENDER, chirp.getSender());
    assertEquals(EMPTY_STRING, chirp.getText());
    assertEquals(CREATION_TIME, chirp.getTimestamp());
    assertEquals(PARENT_ID, chirp.getParentId());

    Map<MessageID, Chirp> chirps = updatedLao.getAllChirps();
    assertEquals(1, chirps.size());
    assertEquals(chirp, chirps.get(chirp.getId()));
  }
}
