package com.github.dedis.popstellar.repository.remote;

import com.github.dedis.popstellar.model.network.GenericMessage;
import com.github.dedis.popstellar.model.network.method.Message;
import com.github.dedis.popstellar.model.network.method.Subscribe;
import com.github.dedis.popstellar.model.objects.Channel;
import com.tinder.scarlet.Lifecycle;
import com.tinder.scarlet.ShutdownReason;

import org.junit.Test;

import io.reactivex.subjects.BehaviorSubject;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class ConnectionTest {

  @Test
  public void sendMessageDelegatesToService() {
    LAOService service = mock(LAOService.class);
    BehaviorSubject<GenericMessage> messages = BehaviorSubject.create();
    when(service.observeMessage()).thenReturn(messages);

    BehaviorSubject<Lifecycle.State> manualState = BehaviorSubject.create();

    Connection connection = new Connection(service, manualState);
    Message msg = new Subscribe(Channel.ROOT, 12);

    connection.sendMessage(msg);

    verify(service).sendMessage(msg);
    verify(service).observeMessage();
    verifyNoMoreInteractions(service);
  }

  @Test
  public void observeMessageDelegatesToService() {
    LAOService service = mock(LAOService.class);
    BehaviorSubject<GenericMessage> messages = BehaviorSubject.create();
    when(service.observeMessage()).thenReturn(messages);

    BehaviorSubject<Lifecycle.State> manualState = BehaviorSubject.create();

    Connection connection = new Connection(service, manualState);

    connection.observeMessage();

    verify(service).observeMessage();
    verifyNoMoreInteractions(service);
  }

  @Test
  public void observeWebsocketDelegatesToService() {
    LAOService service = mock(LAOService.class);
    BehaviorSubject<GenericMessage> messages = BehaviorSubject.create();
    when(service.observeMessage()).thenReturn(messages);

    BehaviorSubject<Lifecycle.State> manualState = BehaviorSubject.create();

    Connection connection = new Connection(service, manualState);

    connection.observeConnectionEvents();

    verify(service).observeWebsocket();
    verify(service).observeMessage();
    verifyNoMoreInteractions(service);
  }

  @Test
  public void connectionClosesGracefully() {
    LAOService service = mock(LAOService.class);
    BehaviorSubject<GenericMessage> messages = BehaviorSubject.create();
    when(service.observeMessage()).thenReturn(messages);

    BehaviorSubject<Lifecycle.State> manualState =
        BehaviorSubject.createDefault(Lifecycle.State.Started.INSTANCE);

    Connection connection = new Connection(service, manualState);
    connection.close();

    assertEquals(
        new Lifecycle.State.Stopped.WithReason(ShutdownReason.GRACEFUL), manualState.getValue());

    verify(service).observeMessage();
    verifyNoMoreInteractions(service);
  }
}
