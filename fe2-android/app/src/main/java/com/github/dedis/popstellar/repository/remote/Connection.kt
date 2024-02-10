package com.github.dedis.popstellar.repository.remote

import com.github.dedis.popstellar.model.network.GenericMessage
import com.github.dedis.popstellar.model.network.method.Message
import com.tinder.scarlet.Lifecycle
import com.tinder.scarlet.ShutdownReason
import com.tinder.scarlet.WebSocket
import com.tinder.scarlet.WebSocket.Event.OnConnectionClosed
import com.tinder.scarlet.WebSocket.Event.OnConnectionClosing
import com.tinder.scarlet.WebSocket.Event.OnConnectionFailed
import com.tinder.scarlet.WebSocket.Event.OnConnectionOpened
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.BehaviorSubject
import timber.log.Timber

/** Represents a single websocket connection that can be closed */
open class Connection {
  // Create a new subject whose purpose is to dispatch incoming messages to all subscribers
  private val messagesSubject: BehaviorSubject<GenericMessage>
  private val manualState: BehaviorSubject<Lifecycle.State>
  private val laoService: LAOService
  private val disposables: CompositeDisposable

  constructor(url: String, laoService: LAOService, manualState: BehaviorSubject<Lifecycle.State>) {
    this.laoService = laoService
    this.manualState = manualState
    messagesSubject = BehaviorSubject.create()
    disposables = CompositeDisposable()

    // Subscribe to the incoming messages of the websocket service
    // and simply hand them to the subject
    disposables.add(
        laoService
            .observeMessage()
            .doOnNext { msg: GenericMessage ->
              Timber.tag(TAG).d("Received a new message from remote: %s", msg)
            }
            .subscribe(
                { t: GenericMessage -> messagesSubject.onNext(t) },
                { t: Throwable -> messagesSubject.onError(t) }))

    // Add logs on connection state events
    disposables.add(
        laoService
            .observeWebsocket()
            .subscribe(
                { event: WebSocket.Event -> logEvent(event, url) },
                { err: Throwable -> Timber.tag(TAG).d(err, "Error in connection %s", url) }))
  }

  protected constructor(connection: Connection) {
    laoService = connection.laoService
    manualState = connection.manualState
    disposables = connection.disposables
    messagesSubject = connection.messagesSubject
  }

  private fun logEvent(event: WebSocket.Event, url: String) {
    val baseMsg = "Connection to $url"
    when (event) {
      is OnConnectionOpened<*> -> {
        Timber.tag(TAG).i("%s opened", baseMsg)
      }
      is OnConnectionClosed -> {
        val reason: ShutdownReason = event.shutdownReason
        Timber.tag(TAG).i("%s closed: %s", baseMsg, reason)
      }
      is OnConnectionFailed -> {
        val error: Throwable = event.throwable
        Timber.tag(TAG).d(error, "%s failed", baseMsg)
      }
      is OnConnectionClosing -> {
        val reason: ShutdownReason = event.shutdownReason
        Timber.tag(TAG).d("%s is closing: %s", baseMsg, reason)
      }
      else -> {
        /* Don't log anything in the other cases */
      }
    }
  }

  open fun sendMessage(msg: Message) {
    laoService.sendMessage(msg)
  }

  open fun observeMessage(): Observable<GenericMessage> {
    return messagesSubject
  }

  open fun observeConnectionEvents(): Observable<WebSocket.Event> {
    return laoService.observeWebsocket()
  }

  open fun close() {
    // Dispose of any held resources and mark the message subject as complete
    // (i.e. will not be used again)
    messagesSubject.onComplete()
    disposables.dispose()
    manualState.onNext(Lifecycle.State.Stopped.WithReason(ShutdownReason.GRACEFUL))
  }

  companion object {
    val TAG: String = Connection::class.java.simpleName
  }
}
