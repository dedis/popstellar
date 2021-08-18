package com.github.dedis.student20_pop.model.data;

import android.app.Application;
import com.github.dedis.student20_pop.Injection;
import com.github.dedis.student20_pop.model.network.answer.Answer;
import com.github.dedis.student20_pop.model.network.answer.Result;
import io.reactivex.Single;
import junit.framework.TestCase;

public class LAORepositoryTest extends TestCase {

  private FakeLAOLocalDataSource fakeLocalDataSource;
  private FakeLAORemoteDataSource fakeRemoteDataSource;
  private LAORepository laoRepository;

  public void setUp() throws Exception {
    super.setUp();

    fakeLocalDataSource = FakeLAOLocalDataSource.getInstance();
    fakeRemoteDataSource = FakeLAORemoteDataSource.getInstance();

    // TODO: use Injection class instead
    laoRepository = LAORepository.getInstance(fakeRemoteDataSource, fakeLocalDataSource, Injection.provideAndroidKeysetManager(new Application()), Injection.provideGson());
  }

  // Uses RemoteDataSource
  public void testSendCatchup() {
    // TODO: verify the answer is the expected one
    Single<Answer> answer = laoRepository.sendCatchup("channel");
    // result is empty here
    Result result = new Result(fakeRemoteDataSource.getRequestId());
    result.setGeneral();
    answer.test().assertResult(result);
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