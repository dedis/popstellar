package com.github.dedis.student20_pop.model.data;

import com.github.dedis.student20_pop.Injection;
import com.github.dedis.student20_pop.model.Lao;
import com.github.dedis.student20_pop.model.network.GenericMessage;
import com.github.dedis.student20_pop.model.network.answer.Answer;
import com.github.dedis.student20_pop.model.network.answer.Result;
import com.github.dedis.student20_pop.model.network.method.message.MessageGeneral;
import com.github.dedis.student20_pop.model.network.method.message.data.lao.CreateLao;
import com.github.dedis.student20_pop.utility.scheduler.SchedulerProvider;
import com.github.dedis.student20_pop.utility.scheduler.TestSchedulerProvider;
import com.google.crypto.tink.integration.android.AndroidKeysetManager;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.observers.TestObserver;
import io.reactivex.schedulers.TestScheduler;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import junit.framework.TestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;


@RunWith(MockitoJUnitRunner.class)
public class LAORepositoryTest extends TestCase {

  @Mock
  LAORemoteDataSource remoteDataSource;

  @Mock
  LAOLocalDataSource localDataSource;

  @Mock
  AndroidKeysetManager androidKeysetManager;

  @Mock
  MessageGeneral messageGeneral;

  private static final int REQUEST_ID = 42;
  private static final int RESPONSE_DELAY = 1000;
  private static final String CHANNEL = "/root/";
  private static final String LAO_CHANNEL = "/root/123";

  private LAORepository repository;
  private TestScheduler testScheduler;
  private TestObserver<Answer> testObserver;

  @Before
  public void setup() {
    SchedulerProvider testSchedulerProvider = new TestSchedulerProvider();
    testScheduler = (TestScheduler) testSchedulerProvider.io();

    // Simulate a network response from the server after the response delay
    Observable<GenericMessage> upstream = Observable.just((GenericMessage) new Result(REQUEST_ID))
        .delay(RESPONSE_DELAY, TimeUnit.MILLISECONDS, testScheduler);

    Mockito.when(remoteDataSource.observeMessage()).thenReturn(upstream);
    Mockito.when(remoteDataSource.incrementAndGetRequestId()).thenReturn(REQUEST_ID);
    Mockito.when(remoteDataSource.observeWebsocket()).thenReturn(Observable.empty());

    repository = LAORepository
        .getInstance(remoteDataSource, localDataSource, androidKeysetManager,
            Injection.provideGson(), testSchedulerProvider);

    // Create the observer that will simulate the network answer
    testObserver = TestObserver.create();
  }

  @After
  public void destroy() {
    // Ensure every test has a new LAORepository instance with a different TestSchedulerProvider
    LAORepository.destroyInstance();
  }

  @Test
  public void testSendCatchup() {
    // Send a catchup request and subscribe to the answer
    Single<Answer> answerCatchup = repository.sendCatchup(CHANNEL);
    answerCatchup.subscribe(testObserver);

    // Check the correct DataSource is being used
    Mockito.verify(remoteDataSource, Mockito.times(1)).sendMessage(Mockito.any());

    // Check there is no answer before the response delay time
    testScheduler.advanceTimeTo(RESPONSE_DELAY - 1, TimeUnit.MILLISECONDS);
    testObserver.assertNotComplete();

    // Check the catchup result is ready at response delay time
    // and verify the id of the result corresponds to the expected id
    testScheduler.advanceTimeTo(RESPONSE_DELAY, TimeUnit.MILLISECONDS);
    testObserver.assertComplete().assertNoErrors()
        .assertValue(r -> r.getId() == REQUEST_ID);
  }

  @Test
  public void testSendPublish() {
    // Publish a message and subscribe to the answer
    Single<Answer> answerPublish = repository.sendPublish(CHANNEL, messageGeneral);
    answerPublish.subscribe(testObserver);

    // Check the correct DataSource is being used
    Mockito.verify(remoteDataSource, Mockito.times(1)).sendMessage(Mockito.any());

    // Check there is no answer before the response delay time
    testScheduler.advanceTimeTo(RESPONSE_DELAY - 1, TimeUnit.MILLISECONDS);
    testObserver.assertNotComplete();

    // Check the publish result is ready at response delay time
    // and verify the id of the result corresponds to the expected id
    testScheduler.advanceTimeTo(RESPONSE_DELAY, TimeUnit.MILLISECONDS);
    testObserver.assertComplete().assertNoErrors()
        .assertValue(r -> r.getId() == REQUEST_ID);
  }

  @Test
  public void testSendSubscribe() {
    // Send a subscribe request and subscribe to the answer
    Single<Answer> answerSubscribe = repository.sendSubscribe(CHANNEL);
    answerSubscribe.subscribe(testObserver);

    // Check the correct DataSource is being used
    Mockito.verify(remoteDataSource, Mockito.times(1)).sendMessage(Mockito.any());

    // Check there is no answer before the response delay time
    testScheduler.advanceTimeTo(RESPONSE_DELAY - 1, TimeUnit.MILLISECONDS);
    testObserver.assertNotComplete();

    // Check the subscribe result is ready at response delay time
    // and verify the id of the result corresponds to the expected id
    testScheduler.advanceTimeTo(RESPONSE_DELAY, TimeUnit.MILLISECONDS);
    testObserver.assertComplete().assertNoErrors()
        .assertValue(r -> r.getId() == REQUEST_ID);
  }

  @Test
  public void testSendUnsubscribe() {
    // Send an unsubscribe request and subscribe to the answer
    Single<Answer> answerUnsubscribe = repository.sendUnsubscribe(CHANNEL);
    answerUnsubscribe.subscribe(testObserver);

    // Check the correct DataSource is being used
    Mockito.verify(remoteDataSource, Mockito.times(1)).sendMessage(Mockito.any());

    // Check there is no answer before the response delay time
    testScheduler.advanceTimeTo(RESPONSE_DELAY - 1, TimeUnit.MILLISECONDS);
    testObserver.assertNotComplete();

    // Check the unsubscribe result is ready at response delay time
    // and verify the id of the result corresponds to the expected id
    testScheduler.advanceTimeTo(RESPONSE_DELAY, TimeUnit.MILLISECONDS);
    testObserver.assertComplete().assertNoErrors()
        .assertValue(r -> r.getId() == REQUEST_ID);
  }

  @Test
  public void testGetLaos() {
    // Does not use LAOLocalDataSource

    // Create the LAO and the LAO list to test from
    Lao lao = new Lao(LAO_CHANNEL);

    // Subscribe to a LAO and wait for the request to finish
    repository.sendSubscribe(LAO_CHANNEL);
    testScheduler.advanceTimeBy(RESPONSE_DELAY, TimeUnit.MILLISECONDS);

    // TODO: Use TestSubscriber
    // Check the LAO is present in both LAO lists of LAORepository
  }

  @Test
  public void testBroadcast() {

    CreateLao createLao = new CreateLao(Mockito.any(), Mockito.any());

  }
}