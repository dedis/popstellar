package com.github.dedis.popstellar.repository.remote;

import com.github.dedis.popstellar.model.network.GenericMessage;
import com.github.dedis.popstellar.model.network.method.Message;
import com.tinder.scarlet.*;
import com.tinder.scarlet.WebSocket.Event.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.subjects.BehaviorSubject;

/** Represents a single websocket connection that can be closed */
public class Connection {

  private static final Logger logger = LogManager.getLogger(Connection.class);

  // Create a new subject whose purpose is to dispatch incoming messages to all subscribers
  private final BehaviorSubject<GenericMessage> messagesSubject;
  private final BehaviorSubject<Lifecycle.State> manualState;
  private final LAOService laoService;
  private final CompositeDisposable disposables;

  public Connection(
      String url, LAOService laoService, BehaviorSubject<Lifecycle.State> manualState) {
    this.laoService = laoService;
    this.manualState = manualState;
    messagesSubject = BehaviorSubject.create();
    disposables = new CompositeDisposable();
    // Subscribe to the incoming messages of the websocket service
    // and simply hand them to the subject
    disposables.add(
        laoService
            .observeMessage()
            .doOnNext(msg -> logger.debug("Received a new message from remote: " + msg))
            .subscribe(messagesSubject::onNext, messagesSubject::onError));

    // Add logs on connection state events
    disposables.add(
        laoService
            .observeWebsocket()
            .subscribe(
                event -> logEvent(event, url),
                err -> logger.debug("Error in connection " + url, err)));
  }

  protected Connection(Connection connection) {
    laoService = connection.laoService;
    manualState = connection.manualState;
    disposables = connection.disposables;
    messagesSubject = connection.messagesSubject;
  }

  private void logEvent(WebSocket.Event event, String url) {
    String baseMsg = "Connection to " + url;

    if (event instanceof OnConnectionOpened) {
      logger.info(baseMsg + " opened");
    } else if (event instanceof OnConnectionClosed) {
      ShutdownReason reason = ((OnConnectionClosed) event).getShutdownReason();
      logger.info(baseMsg + " closed: " + reason);
    } else if (event instanceof OnConnectionFailed) {
      Throwable error = ((OnConnectionFailed) event).getThrowable();
      logger.debug(baseMsg + " failed", error);
    } else if (event instanceof OnConnectionClosing) {
      ShutdownReason reason = ((OnConnectionClosing) event).getShutdownReason();
      logger.debug(baseMsg + " is closing: " + reason);
    }
  }

  public void sendMessage(Message msg) {
    laoService.sendMessage(msg);
  }

  public Observable<GenericMessage> observeMessage() {
    return messagesSubject;
  }

  public Observable<WebSocket.Event> observeConnectionEvents() {
    return laoService.observeWebsocket();
  }

  public void close() {
    // Dispose of any held resources and mark the message subject as complete
    // (i.e. will not be used again)
    messagesSubject.onComplete();
    disposables.dispose();
    manualState.onNext(new Lifecycle.State.Stopped.WithReason(ShutdownReason.GRACEFUL));
  }
}
