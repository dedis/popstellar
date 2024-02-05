package com.github.dedis.popstellar.repository.remote

import com.github.dedis.popstellar.model.network.GenericMessage
import com.github.dedis.popstellar.model.network.answer.Result
import com.github.dedis.popstellar.model.network.method.Message
import com.github.dedis.popstellar.model.network.method.Subscribe
import com.github.dedis.popstellar.model.objects.Channel
import com.github.dedis.popstellar.model.objects.PeerAddress
import com.tinder.scarlet.Lifecycle
import com.tinder.scarlet.ShutdownReason
import com.tinder.scarlet.WebSocket
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import java.util.function.Function
import org.junit.Assert
import org.junit.Test
import org.mockito.Mockito

class ConnectionTest {
  @Test
  fun sendMessageDelegatesToService() {
    val service = Mockito.mock(LAOService::class.java)
    val messages = BehaviorSubject.create<GenericMessage>()
    Mockito.`when`(service.observeMessage()).thenReturn(messages)
    Mockito.`when`(service.observeWebsocket()).thenReturn(BehaviorSubject.create())

    val manualState: BehaviorSubject<Lifecycle.State> = BehaviorSubject.create()
    val connection = Connection("url", service, manualState)
    val msg: Message = Subscribe(Channel.ROOT, 12)
    connection.sendMessage(msg)

    Mockito.verify(service).sendMessage(msg)
    Mockito.verify(service).observeMessage()
    Mockito.verify(service, Mockito.atLeastOnce()).observeWebsocket()
    Mockito.verifyNoMoreInteractions(service)
  }

  @Test
  fun observeMessageDelegatesToService() {
    val service = Mockito.mock(LAOService::class.java)
    val messages = BehaviorSubject.create<GenericMessage>()

    Mockito.`when`(service.observeMessage()).thenReturn(messages)
    Mockito.`when`(service.observeWebsocket()).thenReturn(BehaviorSubject.create())

    val manualState: BehaviorSubject<Lifecycle.State> = BehaviorSubject.create()

    // Create connection and retrieve events
    val connection = Connection(URL, service, manualState)
    val connectionMessages = connection.observeMessage()
    // Publish message to the pipeline
    val message: GenericMessage = Result(5)
    messages.onNext(message)

    // Make sure the event was receive
    connectionMessages.test().assertValueCount(1).assertValue(message)
    Mockito.verify(service).observeMessage()
    Mockito.verify(service, Mockito.atLeastOnce()).observeWebsocket()
    Mockito.verifyNoMoreInteractions(service)
  }

  @Test
  fun observeWebsocketDelegatesToService() {
    val service = Mockito.mock(LAOService::class.java)
    val messages = BehaviorSubject.create<GenericMessage>()
    val events: BehaviorSubject<WebSocket.Event> = BehaviorSubject.create()

    Mockito.`when`(service.observeMessage()).thenReturn(messages)
    Mockito.`when`(service.observeWebsocket()).thenReturn(events)
    val manualState: BehaviorSubject<Lifecycle.State> = BehaviorSubject.create()

    // Create connection and retrieve events
    val connection = Connection("url", service, manualState)
    val connectionEvents: Observable<WebSocket.Event> = connection.observeConnectionEvents()
    // Publish event to the pipeline
    val event: WebSocket.Event = WebSocket.Event.OnConnectionOpened("Fake WebSocket")
    events.onNext(event)

    // Make sure the event was receive
    connectionEvents.test().assertValueCount(1).assertValue(event)
    Mockito.verify(service).observeMessage()
    Mockito.verify(service, Mockito.atLeastOnce()).observeWebsocket()
    Mockito.verifyNoMoreInteractions(service)
  }

  @Test
  fun connectionClosesGracefully() {
    val service = Mockito.mock(LAOService::class.java)
    val messages = BehaviorSubject.create<GenericMessage>()

    Mockito.`when`(service.observeMessage()).thenReturn(messages)
    Mockito.`when`(service.observeWebsocket()).thenReturn(BehaviorSubject.create())

    val manualState: BehaviorSubject<Lifecycle.State> =
      BehaviorSubject.createDefault(Lifecycle.State.Started)
    val connection = Connection("url", service, manualState)
    connection.close()

    Assert.assertEquals(
      Lifecycle.State.Stopped.WithReason(ShutdownReason.GRACEFUL),
      manualState.value
    )
    Mockito.verify(service).observeMessage()
    Mockito.verify(service, Mockito.atLeastOnce()).observeWebsocket()
    Mockito.verifyNoMoreInteractions(service)
  }

  @Test
  fun testMultiConnection() {
    val service = Mockito.mock(LAOService::class.java)
    val messages = BehaviorSubject.create<GenericMessage>()

    Mockito.`when`(service.observeMessage()).thenReturn(messages)
    Mockito.`when`(service.observeWebsocket()).thenReturn(BehaviorSubject.create())

    val manualState: BehaviorSubject<Lifecycle.State> =
      BehaviorSubject.createDefault(Lifecycle.State.Started)
    val provider = Function { url: String -> Connection(url, service, manualState) }
    val multiConnection = MultiConnection(provider, "url")

    // Extend the connections with a new peer
    val peers: MutableList<PeerAddress> = ArrayList()
    peers.add(PeerAddress("url2"))
    multiConnection.connectToPeers(peers)

    val msg: Message = Subscribe(Channel.ROOT, 12)
    multiConnection.sendMessage(msg)

    Mockito.verify(service, Mockito.times(2)).sendMessage(msg)
    Mockito.verify(service, Mockito.times(2)).observeMessage()
    Mockito.verify(service, Mockito.times(2)).observeWebsocket()
    Mockito.verifyNoMoreInteractions(service)
  }

  companion object {
    const val URL = "url"
  }
}
