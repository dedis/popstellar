package com.github.dedis.student20_pop.model.data;

import com.github.dedis.student20_pop.model.data.LAODataSource.Remote;
import com.github.dedis.student20_pop.model.network.GenericMessage;
import com.github.dedis.student20_pop.model.network.method.Message;
import com.tinder.scarlet.WebSocket.Event;
import io.reactivex.Observable;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Fake test double of LAORemoteDataSource
 */
public class FakeLAORemoteDataSource implements Remote {

  // TODO: create variables for generic messages: this will be the upstream in the LaoRepository.
  //  This upstream represents the answers that the frontend received from the backend.
  //  Test idea: add answer of a Publish, call sendPublish and verify the answer comes is the expected one
  private AtomicInteger requestId;
  private Observable<GenericMessage> upstream;

  public FakeLAORemoteDataSource(Observable<GenericMessage> upstream) {
    // TODO: initialize the generic messages with examples of catchup and publish,
    //  need a GenericMessage for each (create using json file, follow protocol specs?)
    requestId = new AtomicInteger();
    this.upstream = upstream;
  }

  @Override
  public Observable<GenericMessage> observeMessage() {
    // TODO: return the fake list of catchup, publish, etc.
    return upstream;
  }

  @Override
  public Observable<Event> observeWebsocket() {
    return Observable.fromArray();
  }

  @Override
  public void sendMessage(Message msg) {

  }

  @Override
  public int getRequestId() {
    return requestId.get();
  }

  @Override
  public int incrementAndGetRequestId() {
    return requestId.getAndIncrement();
  }
}
