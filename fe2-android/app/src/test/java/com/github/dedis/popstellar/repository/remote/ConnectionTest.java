package com.github.dedis.popstellar.repository.remote;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.github.dedis.popstellar.model.network.method.Message;
import com.github.dedis.popstellar.model.network.method.Subscribe;
import com.tinder.scarlet.Lifecycle;
import com.tinder.scarlet.ShutdownReason;

import org.junit.Test;

import io.reactivex.subjects.BehaviorSubject;

public class ConnectionTest {

  @Test
  public void sendMessageDelegatesToService() {
    LAOService service = mock(LAOService.class);
    BehaviorSubject<Lifecycle.State> manualState = BehaviorSubject.create();

    Connection connection = new Connection(service, manualState);
    Message msg = new Subscribe("channel", 12);

    connection.sendMessage(msg);

    verify(service).sendMessage(msg);
    verifyNoMoreInteractions(service);
  }

  @Test
  public void observeMessageDelegatesToService() {
    LAOService service = mock(LAOService.class);
    BehaviorSubject<Lifecycle.State> manualState = BehaviorSubject.create();

    Connection connection = new Connection(service, manualState);

    connection.observeMessage();

    verify(service).observeMessage();
    verifyNoMoreInteractions(service);
  }

  @Test
  public void observeWebsocketDelegatesToService() {
    LAOService service = mock(LAOService.class);
    BehaviorSubject<Lifecycle.State> manualState = BehaviorSubject.create();

    Connection connection = new Connection(service, manualState);

    connection.observeWebsocket();

    verify(service).observeWebsocket();
    verifyNoMoreInteractions(service);
  }

  @Test
  public void connectionClosesGracefully() {
    LAOService service = mock(LAOService.class);
    BehaviorSubject<Lifecycle.State> manualState =
        BehaviorSubject.createDefault(Lifecycle.State.Started.INSTANCE);

    Connection connection = new Connection(service, manualState);
    connection.close();

    assertEquals(
        new Lifecycle.State.Stopped.WithReason(ShutdownReason.GRACEFUL), manualState.getValue());

    verifyNoInteractions(service);
  }
}
