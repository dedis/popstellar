/*package com.github.dedis.popstellar.utility.handler;

import static com.github.dedis.popstellar.testutils.Base64DataUtils.generateKeyPair;
import static com.github.dedis.popstellar.testutils.Base64DataUtils.generateMessageID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.github.dedis.popstellar.di.DataRegistryModule;
import com.github.dedis.popstellar.di.JsonModule;
import com.github.dedis.popstellar.model.network.GenericMessage;
import com.github.dedis.popstellar.model.network.answer.Result;
import com.github.dedis.popstellar.model.network.method.message.MessageGeneral;
import com.github.dedis.popstellar.model.network.method.message.data.lao.CreateLao;
import com.github.dedis.popstellar.model.network.method.message.data.socialmedia.AddChirp;
import com.github.dedis.popstellar.model.network.method.message.data.socialmedia.DeleteChirp;
import com.github.dedis.popstellar.model.objects.Chirp;
import com.github.dedis.popstellar.model.objects.Lao;
import com.github.dedis.popstellar.model.objects.security.KeyPair;
import com.github.dedis.popstellar.model.objects.security.MessageID;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.repository.LAORepository;
import com.github.dedis.popstellar.repository.LAOState;
import com.github.dedis.popstellar.repository.local.LAOLocalDataSource;
import com.github.dedis.popstellar.repository.remote.LAORemoteDataSource;
import com.github.dedis.popstellar.utility.error.DataHandlingException;
import com.github.dedis.popstellar.utility.scheduler.SchedulerProvider;
import com.github.dedis.popstellar.utility.scheduler.TestSchedulerProvider;
import com.github.dedis.popstellar.utility.security.KeyManager;
import com.google.gson.Gson;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.schedulers.TestScheduler;

@RunWith(MockitoJUnitRunner.class)
public class ChirpHandlerTest {

  private static final KeyPair SENDER_KEY = generateKeyPair();
  private static final PublicKey SENDER = SENDER_KEY.getPublicKey();

  private static final long CREATION_TIME = 1631280815;
  private static final long DELETION_TIME = 1642244760;
  private static final String LAO_NAME = "laoName";
  private static final String LAO_ID = Lao.generateLaoId(SENDER, CREATION_TIME, LAO_NAME);
  private static final String LAO_CHANNEL = "/root/" + LAO_ID;
  private static final String CHIRP_CHANNEL = LAO_CHANNEL + "/social/" + SENDER;
  private static final Lao LAO = new Lao(LAO_ID);

  private static final String TEXT = "textOfTheChirp";
  private static final String EMPTY_STRING = "";
  private static final MessageID PARENT_ID = generateMessageID();

  private static final CreateLao CREATE_LAO =
      new CreateLao(LAO_ID, LAO_NAME, CREATION_TIME, SENDER, new ArrayList<>());
  private static final AddChirp ADD_CHIRP = new AddChirp(TEXT, PARENT_ID, CREATION_TIME);

  private static final Gson GSON = JsonModule.provideGson(DataRegistryModule.provideDataRegistry());
  private static final MessageHandler messageHandler =
      new MessageHandler(DataRegistryModule.provideDataRegistry());

  private static final int REQUEST_ID = 42;
  private static final int RESPONSE_DELAY = 1000;

  private LAORepository laoRepository;

  @Mock LAORemoteDataSource remoteDataSource;
  @Mock LAOLocalDataSource localDataSource;
  @Mock KeyManager keyManager;

  @Before
  public void setup() throws GeneralSecurityException, DataHandlingException, IOException {
    SchedulerProvider testSchedulerProvider = new TestSchedulerProvider();
    TestScheduler testScheduler = (TestScheduler) testSchedulerProvider.io();

    Observable<GenericMessage> upstream =
        Observable.fromArray((GenericMessage) new Result(REQUEST_ID))
            .delay(RESPONSE_DELAY, TimeUnit.MILLISECONDS, testScheduler);

    when(remoteDataSource.observeMessage()).thenReturn(upstream);
    when(remoteDataSource.observeWebsocket()).thenReturn(Observable.empty());

    lenient().when(keyManager.getMainKeyPair()).thenReturn(SENDER_KEY);
    lenient().when(keyManager.getMainPublicKey()).thenReturn(SENDER);

    laoRepository =
        new LAORepository(
            remoteDataSource,
            localDataSource,
            keyManager,
            messageHandler,
            GSON,
            testSchedulerProvider);

    laoRepository.getLaoById().put(LAO_CHANNEL, new LAOState(LAO));

    MessageGeneral createLaoMessage = new MessageGeneral(SENDER_KEY, CREATE_LAO, GSON);
    messageHandler.handleMessage(laoRepository, LAO_CHANNEL, createLaoMessage);
  }

  @Test
  public void testHandleAddChirp() throws DataHandlingException {
    MessageGeneral message = new MessageGeneral(SENDER_KEY, ADD_CHIRP, GSON);
    messageHandler.handleMessage(laoRepository, CHIRP_CHANNEL, message);

    Optional<Chirp> chirpOpt = LAO.getChirp(message.getMessageId());
    assertTrue(chirpOpt.isPresent());
    Chirp chirp = chirpOpt.get();

    assertEquals(message.getMessageId(), chirp.getId());
    assertEquals(CHIRP_CHANNEL, chirp.getChannel());
    assertEquals(SENDER, chirp.getSender());
    assertEquals(TEXT, chirp.getText());
    assertEquals(CREATION_TIME, chirp.getTimestamp());
    assertEquals(PARENT_ID, chirp.getParentId());

    Map<MessageID, Chirp> chirps = LAO.getAllChirps();
    assertEquals(1, chirps.size());
    assertEquals(chirp, chirps.get(chirp.getId()));
  }

  @Test
  public void testHandleDeleteChirp() throws DataHandlingException {
    MessageGeneral message = new MessageGeneral(SENDER_KEY, ADD_CHIRP, GSON);
    messageHandler.handleMessage(laoRepository, CHIRP_CHANNEL, message);

    final DeleteChirp DELETE_CHIRP = new DeleteChirp(message.getMessageId(), DELETION_TIME);

    MessageGeneral message2 = new MessageGeneral(SENDER_KEY, DELETE_CHIRP, GSON);
    messageHandler.handleMessage(laoRepository, CHIRP_CHANNEL, message2);

    Optional<Chirp> chirpOpt = LAO.getChirp(message.getMessageId());
    assertTrue(chirpOpt.isPresent());
    Chirp chirp = chirpOpt.get();

    assertEquals(message.getMessageId(), chirp.getId());
    assertEquals(CHIRP_CHANNEL, chirp.getChannel());
    assertEquals(SENDER, chirp.getSender());
    assertEquals(EMPTY_STRING, chirp.getText());
    assertEquals(CREATION_TIME, chirp.getTimestamp());
    assertEquals(PARENT_ID, chirp.getParentId());

    Map<MessageID, Chirp> chirps = LAO.getAllChirps();
    assertEquals(1, chirps.size());
    assertEquals(chirp, chirps.get(chirp.getId()));
  }
}*/
