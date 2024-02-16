package com.github.dedis.popstellar.repository.remote

import com.github.dedis.popstellar.model.objects.Channel
import com.github.dedis.popstellar.testutils.MockitoKotlinHelpers
import com.github.dedis.popstellar.utility.handler.MessageHandler
import com.github.dedis.popstellar.utility.scheduler.TestSchedulerProvider
import com.google.gson.Gson
import io.reactivex.subjects.BehaviorSubject
import java.util.concurrent.TimeUnit
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class GlobalNetworkManagerTest {
  @Mock lateinit var handler: MessageHandler

  @Mock lateinit var gson: Gson

  @Test
  fun initializationProducesAValidConnection() {
    val schedulerProvider = TestSchedulerProvider()
    val testScheduler = schedulerProvider.testScheduler
    val factory = Mockito.mock(ConnectionFactory::class.java)
    val firstConnection = Mockito.mock(MultiConnection::class.java)

    Mockito.`when`(firstConnection.observeMessage()).thenReturn(BehaviorSubject.create())
    Mockito.`when`(firstConnection.observeConnectionEvents()).thenReturn(BehaviorSubject.create())
    Mockito.`when`(factory.createMultiConnection(ArgumentMatchers.anyString()))
      .thenReturn(firstConnection)

    val networkManager = GlobalNetworkManager(handler, factory, gson, schedulerProvider)
    Mockito.verify(factory).createMultiConnection(ArgumentMatchers.anyString())

    val sendMessage = networkManager.messageSender.unsubscribe(Channel.ROOT)
    Mockito.verify(firstConnection, Mockito.never()).sendMessage(MockitoKotlinHelpers.any())

    val disposable = sendMessage.subscribe()
    testScheduler.advanceTimeBy(5, TimeUnit.SECONDS)
    disposable.dispose()

    Mockito.verify(firstConnection).sendMessage(MockitoKotlinHelpers.any())
  }

  @Test
  fun connectingToANewConnectionClosesTheLast() {
    val factory = Mockito.mock(ConnectionFactory::class.java)
    val firstConnection = Mockito.mock(MultiConnection::class.java)

    Mockito.`when`(firstConnection.observeMessage()).thenReturn(BehaviorSubject.create())
    Mockito.`when`(firstConnection.observeConnectionEvents()).thenReturn(BehaviorSubject.create())
    Mockito.`when`(factory.createMultiConnection(ArgumentMatchers.anyString()))
      .thenReturn(firstConnection)

    val networkManager = GlobalNetworkManager(handler, factory, gson, TestSchedulerProvider())
    Mockito.verify(factory).createMultiConnection(ArgumentMatchers.anyString())

    val secondConnection = Mockito.mock(MultiConnection::class.java)
    Mockito.`when`(secondConnection.observeMessage()).thenReturn(BehaviorSubject.create())
    Mockito.`when`(secondConnection.observeConnectionEvents()).thenReturn(BehaviorSubject.create())
    Mockito.`when`(factory.createMultiConnection(ArgumentMatchers.anyString()))
      .thenReturn(secondConnection)

    networkManager.connect("new url")

    Mockito.verify(firstConnection).close()
  }
}
