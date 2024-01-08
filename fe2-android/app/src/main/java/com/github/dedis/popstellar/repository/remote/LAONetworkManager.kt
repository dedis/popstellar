package com.github.dedis.popstellar.repository.remote

import androidx.annotation.VisibleForTesting
import com.github.dedis.popstellar.model.network.GenericMessage
import com.github.dedis.popstellar.model.network.answer.Answer
import com.github.dedis.popstellar.model.network.answer.Error
import com.github.dedis.popstellar.model.network.answer.ResultMessages
import com.github.dedis.popstellar.model.network.method.Broadcast
import com.github.dedis.popstellar.model.network.method.Catchup
import com.github.dedis.popstellar.model.network.method.Publish
import com.github.dedis.popstellar.model.network.method.Query
import com.github.dedis.popstellar.model.network.method.Subscribe
import com.github.dedis.popstellar.model.network.method.Unsubscribe
import com.github.dedis.popstellar.model.network.method.message.MessageGeneral
import com.github.dedis.popstellar.model.network.method.message.data.Data
import com.github.dedis.popstellar.model.objects.Channel
import com.github.dedis.popstellar.model.objects.PeerAddress
import com.github.dedis.popstellar.model.objects.security.KeyPair
import com.github.dedis.popstellar.utility.error.DataHandlingException
import com.github.dedis.popstellar.utility.error.JsonRPCErrorException
import com.github.dedis.popstellar.utility.error.UnknownElectionException
import com.github.dedis.popstellar.utility.error.UnknownLaoException
import com.github.dedis.popstellar.utility.error.UnknownRollCallException
import com.github.dedis.popstellar.utility.error.UnknownWitnessMessageException
import com.github.dedis.popstellar.utility.error.keys.NoRollCallException
import com.github.dedis.popstellar.utility.handler.MessageHandler
import com.github.dedis.popstellar.utility.scheduler.SchedulerProvider
import com.google.gson.Gson
import com.tinder.scarlet.WebSocket
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.observers.TestObserver
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import timber.log.Timber

/** This class handles the JSON-RPC layer of the protocol */
class LAONetworkManager(
    private val messageHandler: MessageHandler,
    private val multiConnection: MultiConnection,
    private val gson: Gson,
    private val schedulerProvider: SchedulerProvider,
    subscribedChannels: Set<Channel>
) : MessageSender {
  private val requestCounter = AtomicInteger()

  // A subject that represents unprocessed messages
  private val unprocessed: Subject<GenericMessage> = PublishSubject.create()
  private val reprocessingCounter = ConcurrentHashMap<GenericMessage, Int>()
  private val subscribedChannels: MutableSet<Channel>
  private val disposables = CompositeDisposable()

  init {
    this.subscribedChannels = HashSet(subscribedChannels)

    // Start the incoming message processing
    processIncomingMessages()
    // Start the routine aimed at resubscribing to channels when the connection is lost
    resubscribeToChannelOnReconnection()
  }

  private fun resubscribeToChannelOnReconnection() {
    disposables.add(
        multiConnection
            .observeConnectionEvents() // Observe the events of a connection
            .subscribeOn(
                schedulerProvider.io()) // Filter out events that are not related to a reconnection
            .filter { event: WebSocket.Event -> event is WebSocket.Event.OnConnectionOpened<*> }
            // Subscribe to the stream and when a connection event is received, send a subscribe
            // message for each channel we are supposed to be subscribed to.
            .subscribe(
                {
                  subscribedChannels.forEach { channel: Channel ->
                    disposables.add(
                        subscribe(channel)
                            .subscribe(
                                { Timber.tag(TAG).d("resubscription successful to : %s", channel) },
                                { error: Throwable ->
                                  Timber.tag(TAG).d(error, "error on resubscription to")
                                }))
                  }
                },
                { error: Throwable -> Timber.tag(TAG).d(error, "Error on resubscription") }))
  }

  private fun processIncomingMessages() {
    disposables.add(
        Observable.merge( // Normal message received over the wire
                multiConnection
                    .observeMessage(), // Packets that could not be processed (maybe due to a
                // reordering), this is merged into incoming message, with a delay of 5 seconds to
                // give
                // priority to new messages.
                unprocessed.delay(
                    REPROCESSING_DELAY.toLong(), TimeUnit.SECONDS, schedulerProvider.computation()))
            .filter { obj: GenericMessage -> obj is Broadcast } // Filter the Broadcast
            .map { obj: GenericMessage -> obj as Broadcast }
            .subscribeOn(schedulerProvider.newThread())
            .subscribe(
                { broadcast: Broadcast -> handleBroadcast(broadcast) },
                { error: Throwable -> Timber.tag(TAG).d(error, "Error on processing message") }))
  }

  override fun catchup(channel: Channel): Completable {
    Timber.tag(TAG).d("sending a catchup to the channel %s", channel)

    val catchup = Catchup(channel, requestCounter.incrementAndGet())
    return request(catchup)
        .map { obj: Answer -> (obj as ResultMessages).messages }
        .doOnError { error: Throwable -> Timber.tag(TAG).d(error, "Error in catchup") }
        .doOnSuccess { msgs: List<MessageGeneral> ->
          Timber.tag(TAG).d("Received catchup response on %s, retrieved : %s", channel, msgs)
        }
        .doOnSuccess { messages: List<MessageGeneral> -> handleMessages(messages, channel) }
        .ignoreElement()
  }

  override fun publish(keyPair: KeyPair, channel: Channel, data: Data): Completable {
    return publish(channel, MessageGeneral(keyPair, data, gson))
  }

  override fun publish(channel: Channel, msg: MessageGeneral): Completable {
    Timber.tag(TAG).d("sending a publish %s to the channel %s", msg.data.javaClass, channel)

    val publish = Publish(channel, requestCounter.incrementAndGet(), msg)
    return request(publish).ignoreElement().doOnComplete {
      Timber.tag(TAG).d("Successfully published %s", msg)
    }
  }

  override fun subscribe(channel: Channel): Completable {
    Timber.tag(TAG).d("sending a subscribe on the channel %s", channel)

    val subscribe = Subscribe(channel, requestCounter.incrementAndGet())
    return request(subscribe) // This is used when reconnecting after a lost connection
        .doOnSuccess {
          Timber.tag(TAG).d("Adding %s to subscriptions", channel)
          subscribedChannels.add(channel)
        }
        .doOnError { error: Throwable -> Timber.tag(TAG).d(error, "error in subscribe") }
        // Catchup already sent messages after the subscription to the channel is complete
        // This allows for the completion of the returned completable only when both subscribe
        // and catchup are completed
        .flatMapCompletable { catchup(channel) }
        .doOnComplete {
          Timber.tag(TAG).d("Successfully subscribed and catchup to channel %s", channel)
        }
  }

  override fun unsubscribe(channel: Channel): Completable {
    Timber.tag(TAG).d("sending an unsubscribe on the channel %s", channel)
    val unsubscribe = Unsubscribe(channel, requestCounter.incrementAndGet())
    return request(unsubscribe) // This is used when reconnecting after a lost connection
        .doOnSuccess {
          Timber.tag(TAG).d("Removing %s from subscriptions", channel)
          subscribedChannels.remove(channel)
        }
        .doOnError { error: Throwable -> Timber.tag(TAG).d(error, "error unsubscribing") }
        .ignoreElement()
  }

  override val connectEvents: Observable<WebSocket.Event>
    get() = multiConnection.observeConnectionEvents()

  override val subscriptions: Set<Channel>
    get() = HashSet(subscribedChannels)

  override fun extendConnection(peerAddressList: List<PeerAddress>) {
    // If the connections to other peers are not created then do nothing
    if (!multiConnection.connectToPeers(peerAddressList)) {
      return
    }
    // First dispose the previous connections
    disposables.clear()
    // Start the incoming message processing for all the new connections
    processIncomingMessages()
    // Start the routine aimed at resubscribing to channels when the connection is lost
    resubscribeToChannelOnReconnection()
  }

  private fun handleBroadcast(broadcast: Broadcast) {
    fun handleError(e: Exception) {
      Timber.tag(TAG).e(e, "Error while handling received message, will try to reprocess it later")
      reprocessMessage(broadcast)
    }

    Timber.tag(TAG).d("handling broadcast msg : %s", broadcast)

    try {
      messageHandler.handleMessage(this, broadcast.channel, broadcast.message)
    } catch (e: Exception) {
      when (e) {
        is DataHandlingException,
        is UnknownLaoException,
        is UnknownRollCallException,
        is NoRollCallException,
        is UnknownElectionException,
        is UnknownWitnessMessageException -> handleError(e)
        else -> throw e
      }
    }
  }

  private fun handleMessages(messages: List<MessageGeneral>, channel: Channel) {
    fun handleError(e: Exception) {
      Timber.tag(TAG).e(e, "Error while handling received catchup message")
    }

    for (msg in messages) {
      try {
        messageHandler.handleMessage(this, channel, msg)
      } catch (e: Exception) {
        when (e) {
          is DataHandlingException,
          is UnknownLaoException,
          is UnknownRollCallException,
          is NoRollCallException,
          is UnknownElectionException,
          is UnknownWitnessMessageException -> handleError(e)
          else -> throw e
        }
      }
    }
  }

  private fun request(query: Query): Single<Answer> {
    return multiConnection
        .observeMessage() // Observe incoming messages
        // Send the message upon subscription the the incoming messages. That way we are
        // certain the reply will be processed and the message is only sent when an observer
        // subscribes to the request answer.
        .doOnSubscribe { multiConnection.sendMessage(query) }
        .filter { obj: GenericMessage -> obj is Answer } // Filter for Answers
        .map { obj: GenericMessage ->
          obj as Answer
        } // This specific request has an id, only let the related Answer pass
        .filter { answer: Answer -> answer.id == query.requestId }
        .doOnNext { answer: Answer ->
          Timber.tag(TAG).d("request id: %s", answer.id)
        } // Transform from an Observable to a Single
        // This Means that we expect a result before the source is disposed and an error
        // will be produced if no value is received.
        .firstOrError() // If we receive an error, transform the flow to a Failure
        .flatMap { answer: Answer ->
          if (answer is Error) {
            return@flatMap Single.error<Answer>(JsonRPCErrorException(answer))
          } else {
            return@flatMap Single.just<Answer>(answer)
          }
        }
        .subscribeOn(schedulerProvider.io())
        .observeOn(
            schedulerProvider
                .mainThread()) // Add a timeout to automatically dispose of the flow and end with a
        // failure
        .timeout(REPROCESSING_DELAY.toLong(), TimeUnit.SECONDS)
        .cache()
  }

  /**
   * This function distinguishes an unrecoverable failure according to the number of reprocessing
   * attempts.
   *
   * @param message Message failed to be handled to be reprocessed
   */
  private fun reprocessMessage(message: GenericMessage) {
    // Check that the message hasn't already reprocessed more than the threshold of dropout
    val count = reprocessingCounter.getOrDefault(message, 0)
    if (count < MAX_REPROCESSING) {
      // Increase the counter and reprocess
      reprocessingCounter[message] = count + 1
      unprocessed.onNext(message)
    } else {
      Timber.tag(TAG).d("Message %s has been reprocessed too many times, it's now dropped", message)
      // Discard the message
      reprocessingCounter.remove(message)
    }
  }

  override fun dispose() {
    multiConnection.close()
    disposables.dispose()
  }

  override fun isDisposed(): Boolean {
    return disposables.isDisposed
  }

  @VisibleForTesting
  fun testUnprocessed(): TestObserver<GenericMessage?> {
    return unprocessed.test()
  }

  companion object {
    private val TAG = LAONetworkManager::class.java.simpleName

    /** Constants to tune the reprocessing of unhandled messages */
    const val MAX_REPROCESSING = 5
    const val REPROCESSING_DELAY = 5
  }
}
