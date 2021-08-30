package com.github.dedis.student20_pop.model.data;

import com.github.dedis.student20_pop.Injection;
import com.github.dedis.student20_pop.model.Lao;
import com.github.dedis.student20_pop.model.network.GenericMessage;
import com.github.dedis.student20_pop.model.network.answer.Answer;
import com.github.dedis.student20_pop.model.network.answer.Result;
import com.github.dedis.student20_pop.model.network.method.message.MessageGeneral;
import com.github.dedis.student20_pop.utility.scheduler.SchedulerProvider;
import com.github.dedis.student20_pop.utility.scheduler.TestSchedulerProvider;
import com.google.crypto.tink.integration.android.AndroidKeysetManager;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.observers.TestObserver;
import io.reactivex.schedulers.TestScheduler;
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

  private static final int RESPONSE_DELAY = 1000;
  private TestScheduler testScheduler;
  private LAORepository repository;

  @Before
  public void setup() {
    SchedulerProvider testSchedulerProvider = new TestSchedulerProvider();
    testScheduler = (TestScheduler) testSchedulerProvider.io();

    // Simulate a network response from the server after 1 second
    Observable<GenericMessage> upstream = Observable.just((GenericMessage) new Result(42))
        .delay(RESPONSE_DELAY, TimeUnit.MILLISECONDS, testScheduler);

    Mockito.when(remoteDataSource.observeMessage()).thenReturn(upstream);
    Mockito.when(remoteDataSource.observeWebsocket()).thenReturn(Observable.empty());
    Mockito.when(remoteDataSource.incrementAndGetRequestId()).thenReturn(42);
    repository = LAORepository
        .getInstance(remoteDataSource, localDataSource, androidKeysetManager,
            Injection.provideGson(), testSchedulerProvider);
  }

  @After
  public void destroy() {
    LAORepository.destroyInstance();
  }

  @Test
  public void testSendCatchup() {
    Single<Answer> answerCatchup = repository.sendCatchup("/root/");
    Mockito.verify(remoteDataSource, Mockito.times(1)).sendMessage(Mockito.any());

    TestObserver<Answer> testObserverCatchup = TestObserver.create();

    answerCatchup.subscribe(testObserverCatchup);

    testScheduler.advanceTimeTo(RESPONSE_DELAY - 1, TimeUnit.MILLISECONDS);
    testObserverCatchup.assertNotComplete();

    testScheduler.advanceTimeTo(RESPONSE_DELAY, TimeUnit.MILLISECONDS);
    testObserverCatchup.assertComplete().assertNoErrors()
        .assertValue(r -> r.getId() == 42);
  }

  @Test
  public void testSendPublish() {
    Single<Answer> answerPublish = repository.sendPublish("/root/", messageGeneral);
    Mockito.verify(remoteDataSource, Mockito.times(1)).sendMessage(Mockito.any());

    TestObserver<Answer> testObserverPublish = TestObserver.create();

    answerPublish.subscribe(testObserverPublish);

    testScheduler.advanceTimeBy(950, TimeUnit.MILLISECONDS);
    testObserverPublish.assertNotComplete();

    testScheduler.advanceTimeBy(100, TimeUnit.MILLISECONDS);
    testObserverPublish.assertComplete().assertNoErrors()
        .assertValue(r -> r.getId() == 42);
  }

  @Test
  public void testSendSubscribe() {
    Single<Answer> answerSubscribe = repository.sendSubscribe("/root/");
    Mockito.verify(remoteDataSource, Mockito.times(1)).sendMessage(Mockito.any());

    TestObserver<Answer> testObserverSubscribe = TestObserver.create();

    answerSubscribe.subscribe(testObserverSubscribe);

    testScheduler.advanceTimeBy(950, TimeUnit.MILLISECONDS);
    testObserverSubscribe.assertNotComplete();

    testScheduler.advanceTimeBy(100, TimeUnit.MILLISECONDS);
    testObserverSubscribe.assertComplete().assertNoErrors()
        .assertValue(r -> r.getId() == 42);
  }

  @Test
  public void testSendUnsubscribe() {
    Single<Answer> answerUnsubscribe = repository.sendUnsubscribe("/root/");
    Mockito.verify(remoteDataSource, Mockito.times(1)).sendMessage(Mockito.any());

    TestObserver<Answer> testObserverUnsubscribe = TestObserver.create();

    answerUnsubscribe.subscribe(testObserverUnsubscribe);

    testScheduler.advanceTimeBy(950, TimeUnit.MILLISECONDS);
    testObserverUnsubscribe.assertNotComplete();

    testScheduler.advanceTimeBy(100, TimeUnit.MILLISECONDS);
    testObserverUnsubscribe.assertComplete().assertNoErrors()
        .assertValue(r -> r.getId() == 42);
  }

  // Test the get methods: getLaoObservable and getAllLaos
  @Test
  public void testGetLaos() {
    // Does not use LAOLocalDataSource

    // Send subscribe to fill laoById and allLaoSubject in LAORepository
    repository.sendSubscribe("/root/123");
    TestObserver<Lao> testObserverLao = TestObserver.create();
    TestObserver<List<Lao>> testObserverListLao = TestObserver.create();

    testScheduler.advanceTimeBy(1500, TimeUnit.MILLISECONDS);

    repository.getLaoObservable("/root/123").subscribe(testObserverLao);
    repository.getAllLaos().subscribe(testObserverListLao);

    testScheduler.advanceTimeBy(1500, TimeUnit.MILLISECONDS);

    testObserverLao.assertNoErrors().assertValueCount(1);
    testObserverListLao.assertNoErrors().assertValueCount(1);
  }
}