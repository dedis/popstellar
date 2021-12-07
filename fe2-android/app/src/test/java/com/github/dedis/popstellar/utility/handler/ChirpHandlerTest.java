package com.github.dedis.popstellar.utility.handler;

import static com.github.dedis.popstellar.utility.handler.MessageHandler.handleMessage;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.github.dedis.popstellar.Injection;
import com.github.dedis.popstellar.model.network.GenericMessage;
import com.github.dedis.popstellar.model.network.answer.Result;
import com.github.dedis.popstellar.model.network.method.message.MessageGeneral;
import com.github.dedis.popstellar.model.network.method.message.data.Data;
import com.github.dedis.popstellar.model.network.method.message.data.lao.CreateLao;
import com.github.dedis.popstellar.model.network.method.message.data.socialmedia.AddChirp;
import com.github.dedis.popstellar.model.objects.Chirp;
import com.github.dedis.popstellar.model.objects.Lao;
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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.schedulers.TestScheduler;

@RunWith(MockitoJUnitRunner.class)
public class ChirpHandlerTest {

  private static final String SENDER = "Z3DYtBxooGs6KxOAqCWD3ihR8M6ZPBjAmWp_w5VBaws=";
  private static final long CREATION_TIME = 1631280815;
  private static final String LAO_NAME = "laoName";
  private static final String LAO_ID = Lao.generateLaoId(SENDER, CREATION_TIME, LAO_NAME);
  private static final String LAO_CHANNEL = "/root/" + LAO_ID;
  private static final String CHIRP_CHANNEL = LAO_CHANNEL + "/social/" + SENDER;
  private static final Lao LAO = new Lao(LAO_CHANNEL);

  private static final String TEXT = "textOfTheChirp";
  private static final String PARENT_ID = "parentId";

  private static final CreateLao CREATE_LAO =
      new CreateLao(LAO_ID, LAO_NAME, CREATION_TIME, SENDER, new ArrayList<>());
  private static final AddChirp ADD_CHIRP = new AddChirp(TEXT, PARENT_ID, CREATION_TIME);

  private static final Gson GSON = Injection.provideGson();
  private static final int REQUEST_ID = 42;
  private static final int RESPONSE_DELAY = 1000;

  private LAORepository laoRepository;

  @Mock LAORemoteDataSource remoteDataSource;
  @Mock LAOLocalDataSource localDataSource;
  @Mock AndroidKeysetManager androidKeysetManager;
  @Mock PublicKeySign signer;

  @Before
  public void setup() throws GeneralSecurityException, DataHandlingException {
    SchedulerProvider testSchedulerProvider = new TestSchedulerProvider();
    TestScheduler testScheduler = (TestScheduler) testSchedulerProvider.io();

    byte[] dataBuf = GSON.toJson(CREATE_LAO, Data.class).getBytes();
    Mockito.when(signer.sign(Mockito.any())).thenReturn(dataBuf);
    MessageGeneral createLaoMessage =
        new MessageGeneral(Base64.getUrlDecoder().decode(SENDER), CREATE_LAO, signer, GSON);

    Observable<GenericMessage> upstream =
        Observable.fromArray((GenericMessage) new Result(REQUEST_ID))
            .delay(RESPONSE_DELAY, TimeUnit.MILLISECONDS, testScheduler);

    Mockito.when(remoteDataSource.observeMessage()).thenReturn(upstream);
    Mockito.when(remoteDataSource.observeWebsocket()).thenReturn(Observable.empty());

    Ed25519PrivateKeyManager.registerPair(true);
    KeysetHandle keysetHandle =
        KeysetHandle.generateNew(Ed25519PrivateKeyManager.rawEd25519Template());
    Mockito.when(androidKeysetManager.getKeysetHandle()).thenReturn(keysetHandle);

    LAORepository.destroyInstance();
    laoRepository =
        LAORepository.getInstance(
            remoteDataSource, localDataSource, androidKeysetManager, GSON, testSchedulerProvider);

    laoRepository.getLaoById().put(LAO_CHANNEL, new LAOState(LAO));
    handleMessage(laoRepository, LAO_CHANNEL, createLaoMessage);
  }

  @After
  public void clean() {
    LAORepository.destroyInstance();
  }

  @Test
  public void testHandleAddChirp() throws DataHandlingException {
    MessageGeneral message =
        new MessageGeneral(Base64.getUrlDecoder().decode(SENDER), ADD_CHIRP, signer, GSON);
    handleMessage(laoRepository, CHIRP_CHANNEL, message);

    Optional<Chirp> chirpOpt = LAO.getChirp(message.getMessageId());
    assertTrue(chirpOpt.isPresent());
    Chirp chirp = chirpOpt.get();

    assertEquals(message.getMessageId(), chirp.getId());
    assertEquals(CHIRP_CHANNEL, chirp.getChannel());
    assertEquals(SENDER, chirp.getSender());
    assertEquals(TEXT, chirp.getText());
    assertEquals(CREATION_TIME, chirp.getTimestamp());
    assertEquals(PARENT_ID, chirp.getParentId());

    Map<String, Chirp> chirps = LAO.getChirps();
    assertEquals(1, chirps.size());
    assertEquals(chirp, chirps.get(chirp.getId()));
  }
}
