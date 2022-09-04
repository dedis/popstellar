package com.github.dedis.popstellar.repository.remote;

import com.github.dedis.popstellar.di.DataRegistryModule;
import com.github.dedis.popstellar.di.JsonModule;
import com.github.dedis.popstellar.model.network.GenericMessage;
import com.github.dedis.popstellar.model.network.answer.Error;
import com.github.dedis.popstellar.model.network.answer.*;
import com.github.dedis.popstellar.model.network.method.*;
import com.github.dedis.popstellar.model.network.method.message.MessageGeneral;
import com.github.dedis.popstellar.model.network.method.message.data.Data;
import com.github.dedis.popstellar.model.network.method.message.data.lao.CreateLao;
import com.github.dedis.popstellar.model.objects.Channel;
import com.github.dedis.popstellar.model.objects.security.KeyPair;
import com.github.dedis.popstellar.repository.LAORepository;
import com.github.dedis.popstellar.repository.MessageRepository;
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

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.TestScheduler;
import io.reactivex.subjects.BehaviorSubject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class LAONetworkManagerTest {

  private static final Channel CHANNEL = Channel.ROOT.subChannel("channel");
  private static final KeyPair KEY_PAIR = Base64DataUtils.generateKeyPair();
  private static final Data DATA = new CreateLao("LaoName", KEY_PAIR.getPublicKey());

  private final BehaviorSubject<WebSocket.Event> events = BehaviorSubject.create();
  private final BehaviorSubject<GenericMessage> messages = BehaviorSubject.create();

  @Mock MessageRepository messageRepo;
  @Mock LAORepository laoRepo;
  @Mock MessageHandler handler;
  @Mock Connection connection;

  @Before
  public void setup() {
    when(connection.observeMessage()).thenReturn(messages);
    when(connection.observeConnectionEvents()).thenReturn(events);

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
            messageRepo,
            laoRepo,
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
    verify(connection).observeConnectionEvents();
    verify(connection).close();
    verifyNoMoreInteractions(connection);
  }

  @Test
  public void unsubscribeSendsTheRightMessage() {
    TestSchedulerProvider schedulerProvider = new TestSchedulerProvider();
    TestScheduler testScheduler = schedulerProvider.getTestScheduler();
    LAONetworkManager networkManager =
        new LAONetworkManager(
            messageRepo,
            laoRepo,
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
    verify(connection).observeConnectionEvents();
    verify(connection).close();
    verifyNoMoreInteractions(connection);
  }

  @Test
  public void publishSendsRightMessage() {
    TestSchedulerProvider schedulerProvider = new TestSchedulerProvider();
    TestScheduler testScheduler = schedulerProvider.getTestScheduler();
    LAONetworkManager networkManager =
        new LAONetworkManager(
            messageRepo,
            laoRepo,
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
    verify(connection).observeConnectionEvents();
    verify(connection).close();
    verifyNoMoreInteractions(connection);
  }

  @Test
  public void errorsAreDispatchedCorrectly() {
    TestSchedulerProvider schedulerProvider = new TestSchedulerProvider();
    TestScheduler testScheduler = schedulerProvider.getTestScheduler();
    LAONetworkManager networkManager =
        new LAONetworkManager(
            messageRepo,
            laoRepo,
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
    verify(connection).observeConnectionEvents();
    verify(connection).close();
    verifyNoMoreInteractions(connection);
  }

  @Test
  public void resubscribeToChannelWhenConnectionReopened() {
    TestSchedulerProvider schedulerProvider = new TestSchedulerProvider();
    TestScheduler testScheduler = schedulerProvider.getTestScheduler();
    LAONetworkManager networkManager =
        new LAONetworkManager(
            messageRepo,
            laoRepo,
            handler,
            connection,
            JsonModule.provideGson(DataRegistryModule.provideDataRegistry()),
            schedulerProvider);

    networkManager.subscribe(CHANNEL).subscribe(); // First subscribe
    testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);

    verify(connection).sendMessage(any(Subscribe.class));
    verify(connection).sendMessage(any(Catchup.class));

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

    networkManager.dispose();

    verify(connection, times(2)).sendMessage(any(Subscribe.class));
    verify(connection, times(2)).sendMessage(any(Catchup.class));
    verify(connection, atLeastOnce()).observeMessage();
    verify(connection).observeConnectionEvents();
    verify(connection).close();
    verifyNoMoreInteractions(connection);
  }

  @Test
  public void multipleRequestsAtATimeShouldAllSucceed() {
    TestSchedulerProvider schedulerProvider = new TestSchedulerProvider();
    TestScheduler testScheduler = schedulerProvider.getTestScheduler();
    LAONetworkManager networkManager =
        new LAONetworkManager(
            messageRepo,
            laoRepo,
            handler,
            connection,
            JsonModule.provideGson(DataRegistryModule.provideDataRegistry()),
            schedulerProvider);

    // Set a response that stores requested ids
    Set<Integer> requests = new HashSet<>();
    Set<Integer> catchups = new HashSet<>();

    // Setup mock answer
    Answer<?> answer =
        args -> {
          Query query = args.getArgument(0); // Retrieve subscribe object
          if (query instanceof Catchup) catchups.add(query.getRequestId());
          else requests.add(query.getRequestId());
          return null;
        };
    doAnswer(answer).when(connection).sendMessage(any());

    // Actual test
    // Create
    AtomicBoolean sub1Called = new AtomicBoolean(false);
    AtomicBoolean sub2Called = new AtomicBoolean(false);

    Disposable disposable1 =
        networkManager.subscribe(CHANNEL).subscribe(() -> sub1Called.set(true));

    Disposable disposable2 =
        networkManager.subscribe(CHANNEL).subscribe(() -> sub2Called.set(true));

    testScheduler.advanceTimeBy(1, TimeUnit.SECONDS);
    // Responses for subscribes
    requests.forEach(id -> messages.onNext(new Result(id)));
    testScheduler.advanceTimeBy(1, TimeUnit.SECONDS);
    // Expect catchups to be sent now
    testScheduler.advanceTimeBy(1, TimeUnit.SECONDS);
    // Responses to catchups
    catchups.forEach(id -> messages.onNext(new ResultMessages(id, Collections.emptyList())));
    testScheduler.advanceTimeBy(1, TimeUnit.SECONDS);

    // Make sure the subscription succeed
    assertTrue(sub1Called.get());
    assertTrue(sub2Called.get());

    disposable1.dispose();
    disposable2.dispose();
    networkManager.dispose();

    verify(connection, times(2)).sendMessage(any(Subscribe.class));
    verify(connection, times(2)).sendMessage(any(Catchup.class));
    verify(connection, atLeastOnce()).observeMessage();
    verify(connection).observeConnectionEvents();
    verify(connection).close();
    verifyNoMoreInteractions(connection);
  }
}
