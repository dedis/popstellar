package com.github.dedis.popstellar.repository

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.ConnectivityManager.NetworkCallback
import android.net.Network
import io.reactivex.Observable
import io.reactivex.observers.TestObserver
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.*
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner


@RunWith(MockitoJUnitRunner::class)
class ConnectivityRepositoryTest {
    @Mock
    private lateinit var application: Application

    @Mock
    private lateinit var context: Context

    @Mock
    lateinit var connectivityManager: ConnectivityManager

    private lateinit var connectivityRepository: ConnectivityRepository

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        `when`(application.applicationContext).thenReturn(context)
        `when`(context.getSystemService(Context.CONNECTIVITY_SERVICE))
            .thenReturn(connectivityManager)
        connectivityRepository = ConnectivityRepository(application)
    }

    @Test
    fun testObserveConnectivity_onAvailable() {
        val callbackCaptor = ArgumentCaptor.forClass(
            NetworkCallback::class.java
        )

        val observable: Observable<Boolean> = connectivityRepository.observeConnectivity()
        val testObserver: TestObserver<Boolean> = observable.test()

        verify(connectivityManager).registerDefaultNetworkCallback(callbackCaptor.capture())
        val callback = callbackCaptor.value

        callback.onAvailable(mock(Network::class.java))
        testObserver.assertValue(true)
    }

    @Test
    fun testObserveConnectivity_onLost() {
        val callbackCaptor = ArgumentCaptor.forClass(
            NetworkCallback::class.java
        )

        val observable = connectivityRepository.observeConnectivity()
        val testObserver = observable.test()

        verify(connectivityManager).registerDefaultNetworkCallback(callbackCaptor.capture())
        val callback = callbackCaptor.value

        callback.onLost(mock(Network::class.java))
        testObserver.assertValue(false)
    }

    @Test
    fun testObserveConnectivity_cancellable() {
        val callbackCaptor = ArgumentCaptor.forClass(
            NetworkCallback::class.java
        )

        val observable = connectivityRepository.observeConnectivity()
        val testObserver = observable.test()

        verify(connectivityManager).registerDefaultNetworkCallback(callbackCaptor.capture())
        val callback = callbackCaptor.value

        testObserver.dispose()
        verify(connectivityManager).unregisterNetworkCallback(callback)
    }
}