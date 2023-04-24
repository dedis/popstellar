package com.github.dedis.popstellar.repository.remote;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.github.dedis.popstellar.di.DataRegistryModuleHelper;
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
import com.github.dedis.popstellar.testutils.Base64DataUtils;
import com.github.dedis.popstellar.utility.error.*;
import com.github.dedis.popstellar.utility.error.keys.NoRollCallException;
import com.github.dedis.popstellar.utility.handler.MessageHandler;
import com.github.dedis.popstellar.utility.scheduler.TestSchedulerProvider;
import com.google.gson.Gson;
import com.tinder.scarlet.WebSocket;

import org.junit.*;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.stubbing.Answer;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;

import dagger.hilt.android.testing.HiltAndroidRule;
import dagger.hilt.android.testing.HiltAndroidTest;
import io.reactivex.disposables.Disposable;
import io.reactivex.observers.TestObserver;
import io.reactivex.schedulers.TestScheduler;
import io.reactivex.subjects.BehaviorSubject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@HiltAndroidTest
@RunWith(AndroidJUnit4.class)
public class LAONetworkManagerTest {

  private static final Channel CHANNEL = Channel.ROOT.subChannel("channel");
  private static final KeyPair KEY_PAIR = Base64DataUtils.generateKeyPair();
  private static final Data DATA = new CreateLao("LaoName", KEY_PAIR.getPublicKey());

  private final BehaviorSubject<WebSocket.Event> events = BehaviorSubject.create();
  private final BehaviorSubject<GenericMessage> messages = BehaviorSubject.create();

  private final HiltAndroidRule hiltRule = new HiltAndroidRule(this);

  @Rule public RuleChain rule = RuleChain.outerRule(hiltRule).around(MockitoJUnit.testRule(this));

  @Rule public InstantTaskExecutorRule executorRule = new InstantTaskExecutorRule();

  @Inject Gson gson;

  @Mock MessageHandler handler;
  @Mock MultiConnection connection;

  @Before
  public void setup() {
    hiltRule.inject();
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
            handler,
            connection,
            JsonModule.provideGson(DataRegistryModuleHelper.buildRegistry()),
            schedulerProvider,
            new HashSet<>());

    Answer<?> answer =
        args -> {
          Subscribe subscribe = args.getArgument(0); // Retrieve subscribe object
          assertEquals(CHANNEL, subscribe.getChannel()); // Make sure the channel is correct
          messages.onNext(new Result(subscribe.getRequestId())); // Return a positive result
          return null;
        };
    doAnswer(answer).when(connection).sendMessage(any(Subscribe.class));

    // Actual test
    Disposable disposable = networkManager.subscribe(CHANNEL).subscribe();
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
            handler,
            connection,
            JsonModule.provideGson(DataRegistryModuleHelper.buildRegistry()),
            schedulerProvider,
            new HashSet<>());

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
            handler,
            connection,
            JsonModule.provideGson(DataRegistryModuleHelper.buildRegistry()),
            schedulerProvider,
            new HashSet<>());

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
            handler,
            connection,
            JsonModule.provideGson(DataRegistryModuleHelper.buildRegistry()),
            schedulerProvider,
            new HashSet<>());

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
            handler,
            connection,
            JsonModule.provideGson(DataRegistryModuleHelper.buildRegistry()),
            schedulerProvider,
            new HashSet<>());

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
            handler,
            connection,
            JsonModule.provideGson(DataRegistryModuleHelper.buildRegistry()),
            schedulerProvider,
            new HashSet<>());

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

  @Test
  public void identifyUnrecoverableFailures()
      throws UnknownElectionException, UnknownRollCallException, UnknownLaoException,
          DataHandlingException, NoRollCallException {
    TestSchedulerProvider schedulerProvider = new TestSchedulerProvider();
    TestScheduler testScheduler = schedulerProvider.getTestScheduler();

    // Mock to be not able to handle any broadcast message
    doThrow(UnknownLaoException.class)
        .when(handler)
        .handleMessage(any(), any(), any(MessageGeneral.class));

    LAONetworkManager networkManager =
        new LAONetworkManager(
            handler,
            connection,
            JsonModule.provideGson(DataRegistryModuleHelper.buildRegistry()),
            schedulerProvider,
            new HashSet<>());

    Broadcast broadcast = new Broadcast(CHANNEL, new MessageGeneral(KEY_PAIR, DATA, gson));

    Answer<?> answer =
        args -> {
          messages.onNext(broadcast);
          return null;
        };
    doAnswer(answer).when(connection).sendMessage(any());

    // Actual test
    Disposable disposable = networkManager.publish(KEY_PAIR, CHANNEL, DATA).subscribe();
    testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);

    // Now as the message fails to be handled it should be placed in unprocessed
    // Every 5 seconds reprocessing takes place
    for (int i = 0; i < LAONetworkManager.MAX_REPROCESSING; i++) {
      testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);
    }

    // After MAX_REPROCESSING times check the message is discarded permanently
    // Create a TestObserver for the unprocessed subject
    TestObserver<GenericMessage> testObserver = networkManager.testUnprocessed();

    // Assert that the TestObserver has received no values
    testObserver.assertNoValues();

    testObserver.dispose();
    disposable.dispose();
    networkManager.dispose();
  }
}
