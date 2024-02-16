package com.github.dedis.popstellar.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.github.dedis.popstellar.utility.NetworkLogger.Companion.disableRemote
import com.github.dedis.popstellar.utility.NetworkLogger.Companion.enableRemote
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import timber.log.Timber

@HiltViewModel
class SettingsViewModel @Inject constructor(application: Application) :
    AndroidViewModel(application) {
  fun enableLogging() {
    enableRemote()
    Timber.tag(TAG).d("Enabling remote logging")
  }

  fun disableLogging() {
    Timber.tag(TAG).d("Disabling remote logging")
    disableRemote()
  }

  companion object {
    val TAG: String = SettingsViewModel::class.java.simpleName
  }
}
