package com.github.dedis.popstellar.ui.home;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

import com.github.dedis.popstellar.repository.remote.GlobalNetworkManager;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class SettingsViewModel extends AndroidViewModel {

  public static final String TAG = SettingsViewModel.class.getSimpleName();

  private final GlobalNetworkManager networkManager;

  @Inject
  public SettingsViewModel(@NonNull Application application, GlobalNetworkManager networkManager) {
    super(application);
    this.networkManager = networkManager;
  }

  public void enableLogging() {
    Log.d(TAG, "Enabling logging");
  }

  public void disableLogging() {
    Log.d(TAG, "Disabling logging");
  }
}
