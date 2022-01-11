package com.github.dedis.popstellar.utility.handler;

import static com.github.dedis.popstellar.Base64DataUtils.generateKeyPair;
import static com.github.dedis.popstellar.utility.handler.data.LaoHandler.updateLaoNameWitnessMessage;
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
import com.github.dedis.popstellar.model.network.method.message.data.lao.StateLao;
import com.github.dedis.popstellar.model.network.method.message.data.lao.UpdateLao;
import com.github.dedis.popstellar.model.objects.Lao;
import com.github.dedis.popstellar.model.objects.WitnessMessage;
import com.github.dedis.popstellar.model.objects.security.KeyPair;
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
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.schedulers.TestScheduler;

@RunWith(MockitoJUnitRunner.class)
public class LaoHandlerTest {

  @Mock LAORemoteDataSource remoteDataSource;
  @Mock LAOLocalDataSource localDataSource;
  @Mock KeyManager keyManager;

  private static final Gson GSON = JsonModule.provideGson(DataRegistryModule.provideDataRegistry());
  private static final MessageHandler messageHandler =
      new MessageHandler(DataRegistryModule.provideDataRegistry());

  private static final int REQUEST_ID = 42;
  private static final int RESPONSE_DELAY = 1000;
  private static final KeyPair SENDER_KEY = generateKeyPair();
  private static final PublicKey SENDER = SENDER_KEY.getPublicKey();

  private static final CreateLao CREATE_LAO = new CreateLao("lao", SENDER);
  private static final String CHANNEL = "/root";
  private static final String LAO_CHANNEL = CHANNEL + "/" + CREATE_LAO.getId();

  private Lao lao;
  private LAORepository laoRepository;
  private MessageGeneral createLaoMessage;

  @Before
  public void setup() throws GeneralSecurityException, IOException {
    SchedulerProvider testSchedulerProvider = new TestSchedulerProvider();
    TestScheduler testScheduler = (TestScheduler) testSchedulerProvider.io();

    // Simulate a network response from the server after the response delay
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

    // Create one LAO and add it to the LAORepository
    lao = new Lao(CREATE_LAO.getName(), CREATE_LAO.getOrganizer(), CREATE_LAO.getCreation());
    lao.setLastModified(lao.getCreation());
    laoRepository.getLaoById().put(LAO_CHANNEL, new LAOState(lao));
    laoRepository.setAllLaoSubject();

    // Add the CreateLao message to the LAORepository
    createLaoMessage = new MessageGeneral(SENDER_KEY, CREATE_LAO, GSON);
    laoRepository.getMessageById().put(createLaoMessage.getMessageId(), createLaoMessage);
  }

  @Test
  public void testHandleUpdateLao() throws DataHandlingException {
    // Create the update LAO message
    UpdateLao updateLao =
        new UpdateLao(
            SENDER,
            CREATE_LAO.getCreation(),
            "new name",
            Instant.now().getEpochSecond(),
            new HashSet<>());
    MessageGeneral message = new MessageGeneral(SENDER_KEY, updateLao, GSON);

    // Create the expected WitnessMessage
    WitnessMessage expectedMessage =
        updateLaoNameWitnessMessage(message.getMessageId(), updateLao, lao);

    // Call the message handler
    messageHandler.handleMessage(laoRepository, LAO_CHANNEL, message);

    // Check the WitnessMessage has been created
    Optional<WitnessMessage> witnessMessage =
        laoRepository.getLaoByChannel(LAO_CHANNEL).getWitnessMessage(message.getMessageId());
    assertTrue(witnessMessage.isPresent());
    assertEquals(expectedMessage.getTitle(), witnessMessage.get().getTitle());
    assertEquals(expectedMessage.getDescription(), witnessMessage.get().getDescription());
  }

  @Test
  public void testHandleStateLao() throws DataHandlingException {
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
    MessageGeneral message = new MessageGeneral(SENDER_KEY, stateLao, GSON);

    // Call the message handler
    messageHandler.handleMessage(laoRepository, LAO_CHANNEL, message);

    // Check the LAO last modification time and ID was updated
    assertEquals(
        (Long) stateLao.getLastModified(),
        laoRepository.getLaoByChannel(LAO_CHANNEL).getLastModified());
    assertEquals(
        stateLao.getModificationId(),
        laoRepository.getLaoByChannel(LAO_CHANNEL).getModificationId());
  }
}
