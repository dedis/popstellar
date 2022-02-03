package com.github.dedis.popstellar.repository;

import static com.github.dedis.popstellar.testutils.Base64DataUtils.generateKeyPair;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.dedis.popstellar.di.DataRegistryModule;
import com.github.dedis.popstellar.di.JsonModule;
import com.github.dedis.popstellar.model.network.GenericMessage;
import com.github.dedis.popstellar.model.network.answer.Answer;
import com.github.dedis.popstellar.model.network.answer.Result;
import com.github.dedis.popstellar.model.network.method.Broadcast;
import com.github.dedis.popstellar.model.network.method.message.MessageGeneral;
import com.github.dedis.popstellar.model.network.method.message.data.lao.CreateLao;
import com.github.dedis.popstellar.model.objects.Lao;
import com.github.dedis.popstellar.model.objects.security.KeyPair;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.repository.local.LAOLocalDataSource;
import com.github.dedis.popstellar.repository.remote.LAORemoteDataSource;
import com.github.dedis.popstellar.utility.handler.MessageHandler;
import com.github.dedis.popstellar.utility.scheduler.SchedulerProvider;
import com.github.dedis.popstellar.utility.scheduler.TestSchedulerProvider;
import com.github.dedis.popstellar.utility.security.KeyManager;
import com.google.gson.Gson;

import junit.framework.TestCase;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.security.GeneralSecurityException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.observers.TestObserver;
import io.reactivex.schedulers.TestScheduler;

@RunWith(MockitoJUnitRunner.class)
public class LAORepositoryTest extends TestCase {

  @Mock LAORemoteDataSource remoteDataSource;
  @Mock LAOLocalDataSource localDataSource;
  @Mock KeyManager keyManager;

  private static final Gson GSON = JsonModule.provideGson(DataRegistryModule.provideDataRegistry());
  private static final MessageHandler messageHandler =
      new MessageHandler(DataRegistryModule.provideDataRegistry());

  private static final int REQUEST_ID = 42;
  private static final int RESPONSE_DELAY = 1000;
  private static final KeyPair ORGANIZER_KEY = generateKeyPair();
  private static final PublicKey ORGANIZER = ORGANIZER_KEY.getPublicKey();
  private static final String LAO_NAME = "Lao";
  private static final CreateLao CREATE_LAO = new CreateLao(LAO_NAME, ORGANIZER);
  private static final String CHANNEL = "/root";
  private static final String LAO_CHANNEL = CHANNEL + "/" + CREATE_LAO.getId();

  private LAORepository repository;
  private TestScheduler testScheduler;
  private MessageGeneral createLaoMessage;
  private TestObserver<Answer> answerTestObserver;
  private TestObserver<Lao> laoTestObserver;
  private TestObserver<List<Lao>> laoListTestObserver;
  private SchedulerProvider testSchedulerProvider;

  @Before
  public void setup() throws GeneralSecurityException {
    testSchedulerProvider = new TestSchedulerProvider();
    testScheduler = (TestScheduler) testSchedulerProvider.io();

    // Create the message containing a CreateLao data
    createLaoMessage = new MessageGeneral(ORGANIZER_KEY, CREATE_LAO, GSON);

    // Mock the remote data source to always return the same request id
    when(remoteDataSource.incrementAndGetRequestId()).thenReturn(REQUEST_ID);
    when(remoteDataSource.observeWebsocket()).thenReturn(Observable.empty());

    // Create the observer that will simulate the network answer
    answerTestObserver = TestObserver.create();

    // Create the observers for the Repository's LAO lists to test the LAO creation and subscription
    laoTestObserver = TestObserver.create();
    laoListTestObserver = TestObserver.create();
  }

  @Test
  public void testSendCatchup() {
    // Simulate a network response from the server after the response delay
    Observable<GenericMessage> upstream =
        Observable.fromArray((GenericMessage) new Result(REQUEST_ID))
            .delay(RESPONSE_DELAY, TimeUnit.MILLISECONDS, testScheduler);

    // Mock the remote data source to receive a response
    when(remoteDataSource.observeMessage()).thenReturn(upstream);

    repository =
        new LAORepository(
            remoteDataSource,
            localDataSource,
            keyManager,
            messageHandler,
            GSON,
            testSchedulerProvider);

    // Send a catchup request and subscribe to the answer
    Single<Answer> answerCatchup = repository.sendCatchup(CHANNEL);
    answerCatchup.subscribe(answerTestObserver);

    // Check the correct DataSource is being used
    verify(remoteDataSource, times(1)).sendMessage(any());

    // Check there is no answer before the response delay time
    testScheduler.advanceTimeTo(RESPONSE_DELAY - 1, TimeUnit.MILLISECONDS);
    answerTestObserver.assertNotComplete();

    // Check the catchup result is ready at response delay time
    // and verify the id of the result corresponds to the expected id
    testScheduler.advanceTimeTo(RESPONSE_DELAY, TimeUnit.MILLISECONDS);
    answerTestObserver.assertComplete().assertNoErrors().assertValue(r -> r.getId() == REQUEST_ID);
  }

  @Test
  public void testSendPublish() {
    // Simulate a network response from the server after the response delay
    Observable<GenericMessage> upstream =
        Observable.fromArray((GenericMessage) new Result(REQUEST_ID))
            .delay(RESPONSE_DELAY, TimeUnit.MILLISECONDS, testScheduler);

    // Mock the remote data source to receive a response
    when(remoteDataSource.observeMessage()).thenReturn(upstream);

    repository =
        new LAORepository(
            remoteDataSource,
            localDataSource,
            keyManager,
            messageHandler,
            GSON,
            testSchedulerProvider);

    // Publish a CreateLao message and subscribe to the answer
    Single<Answer> answerPublish = repository.sendPublish(CHANNEL, createLaoMessage);
    answerPublish.subscribe(answerTestObserver);

    // Check the correct DataSource is being used
    verify(remoteDataSource, times(1)).sendMessage(any());

    // Check there is no answer before the response delay time
    testScheduler.advanceTimeTo(RESPONSE_DELAY - 1, TimeUnit.MILLISECONDS);
    answerTestObserver.assertNotComplete();

    // Check the publish result is ready at response delay time
    // and verify the id of the result corresponds to the expected id
    testScheduler.advanceTimeTo(RESPONSE_DELAY, TimeUnit.MILLISECONDS);
    answerTestObserver.assertComplete().assertNoErrors().assertValue(r -> r.getId() == REQUEST_ID);

    // Check the LAO is present in the LAORepository's lists
    checkLao(LAO_CHANNEL);
  }

  @Test
  public void testSendSubscribe() {
    // Simulate a network response from the server after the response delay
    Observable<GenericMessage> upstream =
        Observable.fromArray((GenericMessage) new Result(REQUEST_ID))
            .delay(RESPONSE_DELAY, TimeUnit.MILLISECONDS, testScheduler);

    // Mock the remote data source to receive a response
    when(remoteDataSource.observeMessage()).thenReturn(upstream);

    repository =
        new LAORepository(
            remoteDataSource,
            localDataSource,
            keyManager,
            messageHandler,
            GSON,
            testSchedulerProvider);

    // Send a subscribe request and subscribe to the answer
    Single<Answer> answerSubscribe = repository.sendSubscribe(CHANNEL);
    answerSubscribe.subscribe(answerTestObserver);

    // Check the correct DataSource is being used
    verify(remoteDataSource, times(1)).sendMessage(any());

    // Check there is no answer before the response delay time
    testScheduler.advanceTimeTo(RESPONSE_DELAY - 1, TimeUnit.MILLISECONDS);
    answerTestObserver.assertNotComplete();

    // Check the subscribe result is ready at response delay time
    // and verify the id of the result corresponds to the expected id
    testScheduler.advanceTimeTo(RESPONSE_DELAY, TimeUnit.MILLISECONDS);
    answerTestObserver.assertComplete().assertNoErrors().assertValue(r -> r.getId() == REQUEST_ID);
  }

  @Test
  public void testSubscribeLaoChannel() {
    // Simulate a network response from the server after the response delay
    Observable<GenericMessage> upstream =
        Observable.fromArray((GenericMessage) new Result(REQUEST_ID))
            .delay(RESPONSE_DELAY, TimeUnit.MILLISECONDS, testScheduler);

    // Mock the remote data source to receive a response
    when(remoteDataSource.observeMessage()).thenReturn(upstream);

    repository =
        new LAORepository(
            remoteDataSource,
            localDataSource,
            keyManager,
            messageHandler,
            GSON,
            testSchedulerProvider);

    // Send a subscribe request and subscribe to the answer
    Single<Answer> answerSubscribe = repository.sendSubscribe(LAO_CHANNEL);
    answerSubscribe.subscribe(answerTestObserver);

    // Check the correct DataSource is being used
    verify(remoteDataSource, times(1)).sendMessage(any());

    // Check there is no answer before the response delay time
    testScheduler.advanceTimeTo(RESPONSE_DELAY - 1, TimeUnit.MILLISECONDS);
    answerTestObserver.assertNotComplete();

    // Check the subscribe result is ready at response delay time
    // and verify the id of the result corresponds to the expected id
    testScheduler.advanceTimeTo(RESPONSE_DELAY, TimeUnit.MILLISECONDS);
    answerTestObserver.assertComplete().assertNoErrors().assertValue(r -> r.getId() == REQUEST_ID);

    // Check the LAO is present in the LAORepository's lists
    checkLao(LAO_CHANNEL);
  }

  @Test
  public void testSendUnsubscribe() {
    // Simulate a network response from the server after the response delay
    Observable<GenericMessage> upstream =
        Observable.fromArray((GenericMessage) new Result(REQUEST_ID))
            .delay(RESPONSE_DELAY, TimeUnit.MILLISECONDS, testScheduler);

    // Mock the remote data source to receive a response
    when(remoteDataSource.observeMessage()).thenReturn(upstream);

    repository =
        new LAORepository(
            remoteDataSource,
            localDataSource,
            keyManager,
            messageHandler,
            GSON,
            testSchedulerProvider);

    // Send an unsubscribe request and subscribe to the answer
    Single<Answer> answerUnsubscribe = repository.sendUnsubscribe(CHANNEL);
    answerUnsubscribe.subscribe(answerTestObserver);

    // Check the correct DataSource is being used
    verify(remoteDataSource, times(1)).sendMessage(any());

    // Check there is no answer before the response delay time
    testScheduler.advanceTimeTo(RESPONSE_DELAY - 1, TimeUnit.MILLISECONDS);
    answerTestObserver.assertNotComplete();

    // Check the unsubscribe result is ready at response delay time
    // and verify the id of the result corresponds to the expected id
    testScheduler.advanceTimeTo(RESPONSE_DELAY, TimeUnit.MILLISECONDS);
    answerTestObserver.assertComplete().assertNoErrors().assertValue(r -> r.getId() == REQUEST_ID);
  }

  @Test
  public void testBroadcast() {
    // Simulate a network response and then a broadcast from the server after the response delay
    Observable<GenericMessage> upstream =
        Observable.fromArray(new Result(REQUEST_ID), new Broadcast(LAO_CHANNEL, createLaoMessage))
            .delay(RESPONSE_DELAY, TimeUnit.MILLISECONDS, testScheduler);

    // Mock the remote data source to receive a response and then a broadcast
    when(remoteDataSource.observeMessage()).thenReturn(upstream);

    repository =
        new LAORepository(
            remoteDataSource,
            localDataSource,
            keyManager,
            messageHandler,
            GSON,
            testSchedulerProvider);

    // Subscribe to a LAO and wait for the request to finish
    repository.sendPublish(LAO_CHANNEL, createLaoMessage);
    testScheduler.advanceTimeBy(RESPONSE_DELAY, TimeUnit.MILLISECONDS);

    // Check the LAO is present in the LAORepository's lists
    checkLao(LAO_CHANNEL);

    // Check the LAO information is updated after the broadcast
    List<Object> valuesObserved = laoTestObserver.getEvents().get(0);
    Lao repositoryLao = (Lao) valuesObserved.get(0);
    assertEquals(repositoryLao.getLastModified(), (Long) CREATE_LAO.getCreation());
    assertNotNull(repositoryLao.getWitnesses());
  }

  private void checkLao(String laoChannel) {
    // Observe the LAORepository's LAO lists
    repository.getLaoObservable(laoChannel).subscribe(laoTestObserver);
    repository.getAllLaos().subscribe(laoListTestObserver);

    // Check the LAO is present in the allLaoSubject list of LAORepository
    List<Object> valuesObserved = laoTestObserver.getEvents().get(0);
    Lao repositoryLao = (Lao) valuesObserved.get(0);
    assertEquals(repositoryLao.getChannel(), laoChannel);

    // Check the LAO is present in the laoById map of LAORepository
    valuesObserved = laoListTestObserver.getEvents().get(0);
    //noinspection unchecked
    List<Lao> repositoryLaoList = (List<Lao>) valuesObserved.get(0);
    assertEquals(repositoryLaoList.get(0).getChannel(), laoChannel);
  }
}
