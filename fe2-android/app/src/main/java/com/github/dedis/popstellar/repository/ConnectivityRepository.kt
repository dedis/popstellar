package com.github.dedis.popstellar.repository

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConnectivityRepository @Inject constructor(application: Application) {

  private val connectivityManager =
      application.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE)
          as ConnectivityManager

  fun observeConnectivity(): Observable<Boolean> {
    return Observable.create<Boolean> { emitter ->
          val callback =
              object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: android.net.Network) {
                  emitter.onNext(true)
                }

                override fun onLost(network: android.net.Network) {
                  emitter.onNext(false)
                }
              }

          connectivityManager.registerDefaultNetworkCallback(callback)

          emitter.setCancellable { connectivityManager.unregisterNetworkCallback(callback) }
        }
        .subscribeOn(Schedulers.io())
  }
}
