package com.github.dedis.popstellar.repository.remote;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.github.dedis.popstellar.di.DataRegistryModule;
import com.github.dedis.popstellar.di.JsonModule;
import com.github.dedis.popstellar.model.network.GenericMessage;
import com.github.dedis.popstellar.model.network.answer.Error;
import com.github.dedis.popstellar.model.network.answer.ErrorCode;
import com.github.dedis.popstellar.model.network.answer.Result;
import com.github.dedis.popstellar.model.network.answer.ResultMessages;
import com.github.dedis.popstellar.model.network.method.Catchup;
import com.github.dedis.popstellar.model.network.method.Publish;
import com.github.dedis.popstellar.model.network.method.Query;
import com.github.dedis.popstellar.model.network.method.Subscribe;
import com.github.dedis.popstellar.model.network.method.Unsubscribe;
import com.github.dedis.popstellar.model.network.method.message.MessageGeneral;
import com.github.dedis.popstellar.model.network.method.message.data.Data;
import com.github.dedis.popstellar.model.network.method.message.data.lao.CreateLao;
import com.github.dedis.popstellar.model.objects.security.KeyPair;
import com.github.dedis.popstellar.repository.LAORepository;
import com.github.dedis.popstellar.testutils.Base64DataUtils;
import com.github.dedis.popstellar.utility.error.JsonRPCErrorException;
import com.github.dedis.popstellar.utility.handler.MessageHandler;
import com.github.dedis.popstellar.utility.scheduler.TestSchedulerProvider;
import com.tinder.scarlet.WebSocket;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.TestScheduler;
import io.reactivex.subjects.BehaviorSubject;

@RunWith(MockitoJUnitRunner.class)
public class LAONetworkManagerTest {

  private static final String CHANNEL = "channel";
  private static final KeyPair KEY_PAIR = Base64DataUtils.generateKeyPair();
  private static final Data DATA = new CreateLao("LaoName", KEY_PAIR.getPublicKey());

  private final BehaviorSubject<WebSocket.Event> events = BehaviorSubject.create();
  private final BehaviorSubject<GenericMessage> messages = BehaviorSubject.create();

  @Mock LAORepository laoRepository;
  @Mock MessageHandler handler;
  @Mock Connection connection;

  @Before
  public void setup() {
    when(connection.observeMessage()).thenReturn(messages);
    when(connection.observeWebsocket()).thenReturn(events);

    // Default behavior : success
    Answer<?> answer =
        args -> {
          Query query = args.getArgument(0);
          if (query instanceof Catchup) {
            messages.onNext(new ResultMessages(query.getRequestId(), Collections.emptyList()));
          } else {
            messages.onNext(new Result(query.getRequestId()));
          }
          return null;
        };

    doAnswer(answer).when(connection).sendMessage(any());
  }

  @Test
  public void subscribeSendsTheRightMessages() {
    TestSchedulerProvider schedulerProvider = new TestSchedulerProvider();
    TestScheduler testScheduler = schedulerProvider.getTestScheduler();
    LAONetworkManager networkManager =
        new LAONetworkManager(
            laoRepository,
            handler,
            connection,
            JsonModule.provideGson(DataRegistryModule.provideDataRegistry()),
            schedulerProvider);

    Answer<?> answer =
        args -> {
          Subscribe subscribe = args.getArgument(0); // Retrieve subscribe object
          assertEquals(CHANNEL, subscribe.getChannel()); // Make sure the channel is correct
          messages.onNext(new Result(subscribe.getRequestId())); // Return a positive result
          return null;
        };
    doAnswer(answer).when(connection).sendMessage(any(Subscribe.class));

    // Actual test
    Disposable disposable =
        networkManager
            .subscribe(CHANNEL)
            // Make sure catchup is not called yet
            .subscribe(() -> verify(connection, never()).sendMessage(any(Catchup.class)));
    testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);

    disposable.dispose();
    networkManager.dispose();

    verify(connection).sendMessage(any(Subscribe.class));
    verify(connection).sendMessage(any(Catchup.class));
    verify(connection, atLeastOnce()).observeMessage();
    verify(connection).observeWebsocket();
    verifyNoMoreInteractions(connection);
  }

  @Test
  public void unsubscribeSendsTheRightMessage() {
    TestSchedulerProvider schedulerProvider = new TestSchedulerProvider();
    TestScheduler testScheduler = schedulerProvider.getTestScheduler();
    LAONetworkManager networkManager =
        new LAONetworkManager(
            laoRepository,
            handler,
            connection,
            JsonModule.provideGson(DataRegistryModule.provideDataRegistry()),
            schedulerProvider);

    Answer<?> answer =
        args -> {
          Unsubscribe subscribe = args.getArgument(0); // Retrieve subscribe object
          assertEquals(CHANNEL, subscribe.getChannel()); // Make sure the channel is correct
          messages.onNext(new Result(subscribe.getRequestId())); // Return a positive result
          return null;
        };
    doAnswer(answer).when(connection).sendMessage(any(Unsubscribe.class));

    // Actual test
    Disposable disposable = networkManager.unsubscribe(CHANNEL).subscribe();
    testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);

    disposable.dispose();
    networkManager.dispose();

    verify(connection).sendMessage(any(Unsubscribe.class));
    verify(connection, atLeastOnce()).observeMessage();
    verify(connection).observeWebsocket();
    verifyNoMoreInteractions(connection);
  }

  @Test
  public void publishSendsRightMessage() {
    TestSchedulerProvider schedulerProvider = new TestSchedulerProvider();
    TestScheduler testScheduler = schedulerProvider.getTestScheduler();
    LAONetworkManager networkManager =
        new LAONetworkManager(
            laoRepository,
            handler,
            connection,
            JsonModule.provideGson(DataRegistryModule.provideDataRegistry()),
            schedulerProvider);

    Answer<?> answer =
        args -> {
          Publish publish = args.getArgument(0); // Retrieve subscribe object
          assertEquals(CHANNEL, publish.getChannel()); // Make sure the channel is correct
          MessageGeneral messageGeneral = publish.getMessage();
          assertEquals(DATA, messageGeneral.getData());
          messages.onNext(new Result(publish.getRequestId())); // Return a positive result
          return null;
        };
    doAnswer(answer).when(connection).sendMessage(any(Publish.class));

    // Actual test
    Disposable disposable = networkManager.publish(KEY_PAIR, CHANNEL, DATA).subscribe();
    testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);

    disposable.dispose();
    networkManager.dispose();

    verify(connection).sendMessage(any(Publish.class));
    verify(connection, atLeastOnce()).observeMessage();
    verify(connection).observeWebsocket();
    verifyNoMoreInteractions(connection);
  }

  @Test
  public void errorsAreDispatchedCorrectly() {
    TestSchedulerProvider schedulerProvider = new TestSchedulerProvider();
    TestScheduler testScheduler = schedulerProvider.getTestScheduler();
    LAONetworkManager networkManager =
        new LAONetworkManager(
            laoRepository,
            handler,
            connection,
            JsonModule.provideGson(DataRegistryModule.provideDataRegistry()),
            schedulerProvider);

    ErrorCode error = new ErrorCode(3, "error");

    // Setup mock answer
    Answer<?> answer =
        args -> {
          Query query = args.getArgument(0); // Retrieve subscribe object
          messages.onNext(new Error(query.getRequestId(), error)); // Return a negative result
          return null;
        };
    doAnswer(answer).when(connection).sendMessage(any());

    Disposable disposable =
        networkManager
            .subscribe(CHANNEL)
            .subscribe(
                () -> {
                  throw new IllegalAccessException("The subscription should have failed.");
                },
                err -> assertTrue(err instanceof JsonRPCErrorException));
    testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);

    disposable.dispose();
    networkManager.dispose();

    verify(connection).sendMessage(any(Subscribe.class));
    verify(connection, atLeastOnce()).observeMessage();
    verify(connection).observeWebsocket();
    verifyNoMoreInteractions(connection);
  }

  @Test
  public void resubscribeToChannelWhenConnectionReopened() {
    TestSchedulerProvider schedulerProvider = new TestSchedulerProvider();
    TestScheduler testScheduler = schedulerProvider.getTestScheduler();
    LAONetworkManager networkManager =
        new LAONetworkManager(
            laoRepository,
            handler,
            connection,
            JsonModule.provideGson(DataRegistryModule.provideDataRegistry()),
            schedulerProvider);

    networkManager.subscribe(CHANNEL); // First subscribe
    testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);

    // Setup mock answer
    Answer<?> answer =
        args -> {
          Subscribe subscribe = args.getArgument(0); // Retrieve subscribe object
          assertEquals(CHANNEL, subscribe.getChannel()); // Make sure the channel is correct
          messages.onNext(new Result(subscribe.getRequestId())); // Return a positive result
          return null;
        };
    doAnswer(answer).when(connection).sendMessage(any(Subscribe.class));

    // Push Connection open event
    events.onNext(new WebSocket.Event.OnConnectionOpened<>(mock(WebSocket.class)));
    testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);

    verify(connection, times(2)).sendMessage(any(Subscribe.class));
    verify(connection, times(2)).sendMessage(any(Catchup.class));
    verify(connection, atLeastOnce()).observeMessage();
    verify(connection).observeWebsocket();
    verifyNoMoreInteractions(connection);
  }
}
