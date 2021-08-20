package com.github.dedis.student20_pop.model.data;

import android.app.Application;
import com.github.dedis.student20_pop.Injection;
import com.github.dedis.student20_pop.model.network.answer.Answer;
import com.github.dedis.student20_pop.model.network.answer.Result;
import io.reactivex.Observable;
import io.reactivex.Single;
import junit.framework.TestCase;
import org.junit.Test;


public class LAORepositoryTest extends TestCase {

  private FakeLAOLocalDataSource fakeLocalDataSource;
  private FakeLAORemoteDataSource fakeRemoteDataSource;
  private LAORepository laoRepository;

  public void setUp() throws Exception {
    super.setUp();

    /*
    CreateLao data = new CreateLao("LAO", "123");
    // TODO: modify with actual Ed25519
    byte[] sender = Base64.encode(new byte[]{1,2,3}, 2, 32, Base64.NO_WRAP | Base64.URL_SAFE);
    MessageGeneral m = new MessageGeneral(sender, data, null, Injection.provideGson());
    Result r0 = new Result(0);
    r0.setMessages(Collections.singletonList(m));
*/
    fakeLocalDataSource = new FakeLAOLocalDataSource();
    fakeRemoteDataSource = new FakeLAORemoteDataSource(Observable.fromArray());

    // TODO: use Injection class instead
    laoRepository = LAORepository.getInstance(fakeRemoteDataSource, fakeLocalDataSource,
        Injection.provideAndroidKeysetManager(new Application()), Injection.provideGson());
  }

  // Uses RemoteDataSource
  @Test
  public void testSendCatchup() {
    // TODO: verify the answer is the expected one
    Single<Answer> answer = laoRepository.sendCatchup("channel");
    // result is empty here
    Result result = new Result(fakeRemoteDataSource.getRequestId());
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