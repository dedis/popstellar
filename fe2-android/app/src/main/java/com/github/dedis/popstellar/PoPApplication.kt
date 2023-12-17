package com.github.dedis.popstellar

import android.app.Application
import com.github.dedis.popstellar.utility.NetworkLogger
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

/**
 * Application object of the app
 *
 *
 * For now, it is only used by Hilt as an entry point. It extract the object's lifecycle.
 */
@HiltAndroidApp
class PoPApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Associate the custom logger to Timber
        NetworkLogger.loadFromPersistPreference(this)
        Timber.plant(NetworkLogger())
    }
}