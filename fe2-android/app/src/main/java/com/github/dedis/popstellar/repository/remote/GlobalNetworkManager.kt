package com.github.dedis.popstellar.repository.remote

import com.github.dedis.popstellar.BuildConfig
import com.github.dedis.popstellar.model.objects.Channel
import com.github.dedis.popstellar.utility.handler.MessageHandler
import com.github.dedis.popstellar.utility.scheduler.SchedulerProvider
import com.google.gson.Gson
import io.reactivex.disposables.Disposable
import javax.inject.Inject
import javax.inject.Singleton

/** Module responsible of managing the connection to the backend server. */
@Singleton
class GlobalNetworkManager
@Inject
constructor(
    private val messageHandler: MessageHandler,
    private val connectionFactory: ConnectionFactory,
    private val gson: Gson,
    private val schedulerProvider: SchedulerProvider
) : Disposable {
  private var networkManager: MessageSender? = null
  var currentUrl: String? = null
    private set

  init {
    connect(BuildConfig.DEFAULT_URL)
  }

  @JvmOverloads
  fun connect(url: String, subscriptions: Set<Channel> = HashSet()) {
    networkManager?.dispose()

    networkManager =
        LAONetworkManager(
            messageHandler,
            connectionFactory.createMultiConnection(url),
            gson,
            schedulerProvider,
            subscriptions)
    currentUrl = url
  }

  val messageSender: MessageSender
    get() {
      checkNotNull(networkManager) { "The connection has not been established." }
      return networkManager as MessageSender
    }

  override fun dispose() {
    networkManager?.dispose()
    networkManager = null
  }

  override fun isDisposed(): Boolean {
    return networkManager == null
  }
}
