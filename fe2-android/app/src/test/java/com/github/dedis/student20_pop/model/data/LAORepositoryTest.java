package com.github.dedis.student20_pop.model.data;

import com.github.dedis.student20_pop.Injection;
import com.github.dedis.student20_pop.model.network.GenericMessage;
import com.github.dedis.student20_pop.model.network.answer.Answer;
import com.github.dedis.student20_pop.model.network.answer.Result;
import com.github.dedis.student20_pop.utility.scheduler.SchedulerProvider;
import com.github.dedis.student20_pop.utility.scheduler.TestSchedulerProvider;
import com.google.crypto.tink.integration.android.AndroidKeysetManager;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.observers.TestObserver;
import io.reactivex.schedulers.TestScheduler;
import java.util.concurrent.TimeUnit;
import junit.framework.TestCase;
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

  // Uses RemoteDataSource
  @Test
  public void testSendCatchup() throws Exception {
    SchedulerProvider testSchedulerProvider = new TestSchedulerProvider();
    TestScheduler testScheduler = (TestScheduler) testSchedulerProvider.io();

    // Simulate a network response from the server after 1 second
    Observable<GenericMessage> upstream = Observable.just((GenericMessage) new Result(42))
        .delay(1, TimeUnit.SECONDS, testScheduler);

    Mockito.when(remoteDataSource.observeMessage()).thenReturn(upstream);
    Mockito.when(remoteDataSource.observeWebsocket()).thenReturn(Observable.empty());
    Mockito.when(remoteDataSource.incrementAndGetRequestId()).thenReturn(42);
    LAORepository repository = LAORepository
        .getInstance(remoteDataSource, localDataSource, androidKeysetManager,
            Injection.provideGson(), testSchedulerProvider);

    Single<Answer> answer = repository.sendCatchup("/root/");
    Mockito.verify(remoteDataSource, Mockito.times(1)).sendMessage(Mockito.any());

    TestObserver<Answer> testObserver = TestObserver.create();
    answer.subscribe(testObserver);

    testScheduler.advanceTimeBy(950, TimeUnit.MILLISECONDS);
    testObserver.assertNotComplete();

    testScheduler.advanceTimeBy(100, TimeUnit.MILLISECONDS);
    testObserver.assertComplete().assertNoErrors()
        .assertValue(r -> r.getId() == 42);
  }

  // Uses RemoteDataSource
  public void testSendPublish() {
  }

  // Uses RemoteDataSource
  public void testSendSubscribe() {
  }

  // Uses RemoteDataSource
  public void testSendUnsubscribe() {
  }

  public void testGetAllLaos() {
  }

  public void testGetLaoObservable() {
  }
}