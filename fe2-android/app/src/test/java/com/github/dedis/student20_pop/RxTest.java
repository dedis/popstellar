package com.github.dedis.student20_pop;

import com.github.dedis.student20_pop.model.data.LAODataSource;
import com.github.dedis.student20_pop.model.data.LAORepository;
import com.github.dedis.student20_pop.model.entities.LAO;
import com.github.dedis.student20_pop.model.entities.LAOEntity;
import com.github.dedis.student20_pop.model.entities.Meeting;
import com.github.dedis.student20_pop.model.entities.ModificationSignature;
import com.github.dedis.student20_pop.model.entities.Person;
import com.github.dedis.student20_pop.model.entities.RollCall;
import com.github.dedis.student20_pop.model.network.GenericMessage;
import com.github.dedis.student20_pop.model.network.answer.Answer;
import com.github.dedis.student20_pop.model.network.answer.Result;
import com.github.dedis.student20_pop.model.network.method.Broadcast;
import com.github.dedis.student20_pop.model.network.method.Message;
import com.github.dedis.student20_pop.model.network.method.message.MessageGeneral;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Assert;
import org.junit.Test;

public class RxTest {

  @Test
  public void testRPC() throws ExecutionException, InterruptedException {
    LAODataSource.Local local = new TestLAOLocalDataSource();

    Subject<GenericMessage> subject = PublishSubject.create();
    LAODataSource.Remote remote = new TestLAORemoteDataSource(subject);
    LAORepository laoRepository = LAORepository.getInstance(remote, local);
    CompletableFuture<String> done = new CompletableFuture<>();

    // This is what would be in a viewmodel
    Single<Answer> response = laoRepository.sendSubscribe("test");
    response
        .subscribeOn(Schedulers.io())
        .subscribe(
            answer -> {
              System.out.println(
                  "Subscriber: Running on threadid=" + Thread.currentThread().getId());
              System.out.println("Received an answer for subscribe request " + answer.toString());
              done.complete("done");
            });

    // Let's send a couple of broadcasts and then follow up with an ack
    for (int i = 0; i < 10; i++) {
      MessageGeneral message = new MessageGeneral();
      Broadcast broadcast = new Broadcast("test", message);
      subject.onNext(broadcast);
    }

    Result res = new Result(remote.getRequestId(), null);
    subject.onNext(res);

    Assert.assertEquals("done", done.get());
  }

  private class TestLAORemoteDataSource implements LAODataSource.Remote {

    private Subject<GenericMessage> subject;
    private AtomicInteger counter;

    public TestLAORemoteDataSource(Subject<GenericMessage> subject) {
      this.subject = subject;
      this.counter = new AtomicInteger();
    }

    @Override
    public Observable<GenericMessage> observeMessage() {
      return this.subject;
    }

    @Override
    public void sendMessage(Message msg) {}

    @Override
    public int incrementAndGetRequestId() {
      return counter.incrementAndGet();
    }

    @Override
    public int getRequestId() {
      return counter.get();
    }
  }

  private class TestLAOLocalDataSource implements LAODataSource.Local {

    @Override
    public List<LAO> getAll() {
      return null;
    }

    @Override
    public LAOEntity getLAO(String channel) {
      return null;
    }

    @Override
    public void updateLAO(
        LAO lao, List<Person> witnesses, List<ModificationSignature> signatures) {}

    @Override
    public void addRollCall(LAO lao, RollCall rollCall) {}

    @Override
    public void updateRollCall(RollCall rollCall) {}

    @Override
    public void addMeeting(LAO lao, Meeting meeting) {}

    @Override
    public void updateMeeting(Meeting meeting, List<ModificationSignature> signatures) {}
  }
}
