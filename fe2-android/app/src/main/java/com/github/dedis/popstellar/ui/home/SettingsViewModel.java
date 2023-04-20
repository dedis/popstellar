package com.github.dedis.popstellar.ui.home;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class SettingsViewModel extends AndroidViewModel {

  public static final String TAG = SettingsViewModel.class.getSimpleName();

  @Inject
  public SettingsViewModel(@NonNull Application application) {
    super(application);
  }

  public void enableLogging() {}

  public void disableLogging() {}
}
