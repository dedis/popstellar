package com.github.dedis.popstellar.repository.remote;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.dedis.popstellar.repository.LAORepository;
import com.github.dedis.popstellar.utility.handler.MessageHandler;
import com.github.dedis.popstellar.utility.scheduler.TestSchedulerProvider;
import com.google.gson.Gson;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import io.reactivex.subjects.BehaviorSubject;

@RunWith(MockitoJUnitRunner.class)
public class GlobalNetworkManagerTest {

  @Mock LAORepository repository;
  @Mock MessageHandler handler;
  @Mock Gson gson;

  @Test
  public void initializationProducesAValidConnection() {
    ConnectionFactory factory = mock(ConnectionFactory.class);

    Connection firstConnection = mock(Connection.class);
    when(firstConnection.observeMessage()).thenReturn(BehaviorSubject.create());
    when(firstConnection.observeWebsocket()).thenReturn(BehaviorSubject.create());

    when(factory.createConnection(anyString())).thenReturn(firstConnection);

    GlobalNetworkManager networkManager =
        new GlobalNetworkManager(repository, handler, factory, gson, new TestSchedulerProvider());
    verify(factory).createConnection(anyString());

    networkManager.getQuerySender().unsubscribe("channel");
    verify(firstConnection).sendMessage(any());
  }

  @Test
  public void connectingToANewConnectionClosesTheLast() {
    ConnectionFactory factory = mock(ConnectionFactory.class);

    Connection firstConnection = mock(Connection.class);
    when(firstConnection.observeMessage()).thenReturn(BehaviorSubject.create());
    when(firstConnection.observeWebsocket()).thenReturn(BehaviorSubject.create());

    when(factory.createConnection(anyString())).thenReturn(firstConnection);

    GlobalNetworkManager networkManager =
        new GlobalNetworkManager(repository, handler, factory, gson, new TestSchedulerProvider());
    verify(factory).createConnection(anyString());

    Connection secondConnection = mock(Connection.class);
    when(secondConnection.observeMessage()).thenReturn(BehaviorSubject.create());
    when(secondConnection.observeWebsocket()).thenReturn(BehaviorSubject.create());

    when(factory.createConnection(anyString())).thenReturn(secondConnection);
    networkManager.connect("new url");

    verify(firstConnection).close();
  }
}
