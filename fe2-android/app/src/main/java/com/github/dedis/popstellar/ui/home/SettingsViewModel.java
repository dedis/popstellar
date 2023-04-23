package com.github.dedis.popstellar.ui.home;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

import com.github.dedis.popstellar.utility.NetworkLogger;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import timber.log.Timber;

@HiltViewModel
public class SettingsViewModel extends AndroidViewModel {

  public static final String TAG = SettingsViewModel.class.getSimpleName();

  @Inject
  public SettingsViewModel(@NonNull Application application) {
    super(application);
  }

  public void enableLogging() {
    NetworkLogger.enableRemote();
    Timber.tag(TAG).d("Enabling remote logging");
  }

  public void disableLogging() {
    Timber.tag(TAG).d("Disabling remote logging");
    NetworkLogger.disableRemote();
  }
}
