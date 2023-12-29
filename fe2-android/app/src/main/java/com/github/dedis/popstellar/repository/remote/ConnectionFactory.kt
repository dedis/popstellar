package com.github.dedis.popstellar.repository.remote

import android.app.Application
import com.github.dedis.popstellar.utility.scheduler.SchedulerProvider
import com.google.gson.Gson
import com.tinder.scarlet.Lifecycle
import com.tinder.scarlet.Scarlet
import com.tinder.scarlet.Scarlet.Builder
import com.tinder.scarlet.lifecycle.android.AndroidLifecycle.ofApplicationForeground
import com.tinder.scarlet.messageadapter.gson.GsonMessageAdapter
import com.tinder.scarlet.streamadapter.rxjava2.RxJava2StreamAdapterFactory
import com.tinder.scarlet.websocket.okhttp.newWebSocketFactory
import io.reactivex.BackpressureStrategy
import io.reactivex.subjects.BehaviorSubject
import javax.inject.Inject
import okhttp3.OkHttpClient

/** Factory used to produce [Connection] to a specified URL */
class ConnectionFactory
@Inject
constructor(
    private val application: Application,
    private val schedulerProvider: SchedulerProvider,
    private val okHttpClient: OkHttpClient,
    private val gson: Gson
) {
  private fun createConnection(url: String): Connection {
    // Create a behavior subject that will be used to close or start the socket manually
    val manualState: BehaviorSubject<Lifecycle.State> =
        BehaviorSubject.createDefault(Lifecycle.State.Started)

    // Create the Scarlet instance
    val scarlet: Scarlet =
        Builder()
            .webSocketFactory(okHttpClient.newWebSocketFactory(url))
            .addMessageAdapterFactory(GsonMessageAdapter.Factory(gson))
            .addStreamAdapterFactory(RxJava2StreamAdapterFactory())
            .lifecycle(
                ofApplicationForeground(application)
                    .combineWith(
                        FlowableLifecycleInt(
                            manualState.toFlowable(BackpressureStrategy.LATEST),
                            schedulerProvider.computation()))) // .backoffStrategy(new
            // ExponentialBackoffStrategy())
            .build()

    // And return a bundled object of the service and the subject
    return Connection(url, scarlet.create(LAOService::class.java), manualState)
  }

  fun createMultiConnection(url: String): MultiConnection {
    return MultiConnection({ createConnection(url) }, url)
  }
}
