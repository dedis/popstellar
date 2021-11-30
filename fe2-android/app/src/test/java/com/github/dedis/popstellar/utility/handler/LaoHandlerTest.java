package com.github.dedis.popstellar.utility.handler;

import static com.github.dedis.popstellar.utility.handler.LaoHandler.updateLaoNameWitnessMessage;
import static com.github.dedis.popstellar.utility.handler.MessageHandler.handleMessage;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.github.dedis.popstellar.di.JsonModule;
import com.github.dedis.popstellar.model.network.GenericMessage;
import com.github.dedis.popstellar.model.network.answer.Result;
import com.github.dedis.popstellar.model.network.method.message.MessageGeneral;
import com.github.dedis.popstellar.model.network.method.message.data.Data;
import com.github.dedis.popstellar.model.network.method.message.data.lao.CreateLao;
import com.github.dedis.popstellar.model.network.method.message.data.lao.StateLao;
import com.github.dedis.popstellar.model.network.method.message.data.lao.UpdateLao;
import com.github.dedis.popstellar.model.objects.Lao;
import com.github.dedis.popstellar.model.objects.WitnessMessage;
import com.github.dedis.popstellar.repository.LAORepository;
import com.github.dedis.popstellar.repository.LAOState;
import com.github.dedis.popstellar.repository.local.LAOLocalDataSource;
import com.github.dedis.popstellar.repository.remote.LAORemoteDataSource;
import com.github.dedis.popstellar.utility.error.DataHandlingException;
import com.github.dedis.popstellar.utility.scheduler.SchedulerProvider;
import com.github.dedis.popstellar.utility.scheduler.TestSchedulerProvider;
import com.google.crypto.tink.KeysetHandle;
import com.google.crypto.tink.PublicKeySign;
import com.google.crypto.tink.integration.android.AndroidKeysetManager;
import com.google.crypto.tink.signature.Ed25519PrivateKeyManager;
import com.google.gson.Gson;

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
import java.util.HashSet;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.schedulers.TestScheduler;

@RunWith(MockitoJUnitRunner.class)
public class LaoHandlerTest {

  @Mock LAORemoteDataSource remoteDataSource;
  @Mock LAOLocalDataSource localDataSource;
  @Mock AndroidKeysetManager androidKeysetManager;
  @Mock PublicKeySign signer;

  private static final Gson GSON = JsonModule.provideGson();

  private static final int REQUEST_ID = 42;
  private static final int RESPONSE_DELAY = 1000;
  private static final CreateLao CREATE_LAO =
      new CreateLao("lao", "Z3DYtBxooGs6KxOAqCWD3ihR8M6ZPBjAmWp_w5VBaws=");
  private static final String CHANNEL = "/root";
  private static final String LAO_CHANNEL = CHANNEL + "/" + CREATE_LAO.getId();

  private Lao lao;
  private LAORepository laoRepository;
  private MessageGeneral createLaoMessage;

  @Before
  public void setup() throws GeneralSecurityException {
    SchedulerProvider testSchedulerProvider = new TestSchedulerProvider();
    TestScheduler testScheduler = (TestScheduler) testSchedulerProvider.io();

    // Mock the signing of of any data for the MessageGeneral constructor
    byte[] dataBuf = GSON.toJson(CREATE_LAO, Data.class).getBytes();
    Mockito.when(signer.sign(Mockito.any())).thenReturn(dataBuf);
    createLaoMessage =
        new MessageGeneral(
            Base64.getUrlDecoder().decode(CREATE_LAO.getOrganizer()), CREATE_LAO, signer, GSON);

    // Simulate a network response from the server after the response delay
    Observable<GenericMessage> upstream =
        Observable.fromArray((GenericMessage) new Result(REQUEST_ID))
            .delay(RESPONSE_DELAY, TimeUnit.MILLISECONDS, testScheduler);

    Mockito.when(remoteDataSource.observeMessage()).thenReturn(upstream);
    Mockito.when(remoteDataSource.observeWebsocket()).thenReturn(Observable.empty());

    Ed25519PrivateKeyManager.registerPair(true);
    KeysetHandle keysetHandle =
        KeysetHandle.generateNew(Ed25519PrivateKeyManager.rawEd25519Template());
    Mockito.when(androidKeysetManager.getKeysetHandle()).thenReturn(keysetHandle);

    laoRepository =
        new LAORepository(
            remoteDataSource, localDataSource, androidKeysetManager, GSON, testSchedulerProvider);

    // Create one LAO and add it to the LAORepository
    lao = new Lao(CREATE_LAO.getName(), CREATE_LAO.getOrganizer(), CREATE_LAO.getCreation());
    lao.setLastModified(lao.getCreation());
    laoRepository.getLaoById().put(LAO_CHANNEL, new LAOState(lao));
    laoRepository.setAllLaoSubject();

    // Add the CreateLao message to the LAORepository
    laoRepository.getMessageById().put(createLaoMessage.getMessageId(), createLaoMessage);
  }

  @Test
  public void testHandleUpdateLao() throws DataHandlingException {
    // Create the update LAO message
    UpdateLao updateLao =
        new UpdateLao(
            CREATE_LAO.getOrganizer(),
            CREATE_LAO.getCreation(),
            "new name",
            Instant.now().getEpochSecond(),
            new HashSet<>());
    MessageGeneral message =
        new MessageGeneral(
            Base64.getUrlDecoder().decode(CREATE_LAO.getOrganizer()), updateLao, signer, GSON);

    // Create the expected WitnessMessage
    WitnessMessage expectedMessage =
        updateLaoNameWitnessMessage(message.getMessageId(), updateLao, lao);

    // Call the message handler
    handleMessage(laoRepository, LAO_CHANNEL, message);

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
    MessageGeneral message =
        new MessageGeneral(
            Base64.getUrlDecoder().decode(CREATE_LAO.getOrganizer()), stateLao, signer, GSON);

    // Call the message handler
    handleMessage(laoRepository, LAO_CHANNEL, message);

    // Check the LAO last modification time and ID was updated
    assertEquals(
        (Long) stateLao.getLastModified(),
        laoRepository.getLaoByChannel(LAO_CHANNEL).getLastModified());
    assertEquals(
        stateLao.getModificationId(),
        laoRepository.getLaoByChannel(LAO_CHANNEL).getModificationId());
  }
}
