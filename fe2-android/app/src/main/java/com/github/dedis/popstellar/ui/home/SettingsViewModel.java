package com.github.dedis.popstellar.ui.home;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class SettingsViewModel extends AndroidViewModel {

  private static final Logger logger = LogManager.getLogger(SettingsViewModel.class);

  @Inject
  public SettingsViewModel(@NonNull Application application) {
    super(application);
  }

  public void enableServerLogging() {
    logger.info("Enabling the logging to the server");
    System.setProperty("enableRemoteLogging", "true");
  }

  public void disableServerLogging() {
    logger.info("Disabling the logging to the server");
    System.setProperty("enableRemoteLogging", "false");
  }
}
