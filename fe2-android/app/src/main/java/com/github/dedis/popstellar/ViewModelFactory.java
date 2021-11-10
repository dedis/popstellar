package com.github.dedis.popstellar;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.github.dedis.popstellar.ui.detail.LaoDetailViewModel;
import com.github.dedis.popstellar.ui.home.HomeViewModel;
import com.google.crypto.tink.integration.android.AndroidKeysetManager;
import com.google.gson.Gson;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class ViewModelFactory extends ViewModelProvider.NewInstanceFactory {

  private final Application application;

  private final Gson gson = Injection.provideGson();

  private final AndroidKeysetManager keysetManager;

  public ViewModelFactory(Application application) {
    this.application = application;
    try {
      this.keysetManager =
          Injection.provideAndroidKeysetManager(application.getApplicationContext());
    } catch (IOException | GeneralSecurityException e) {
      throw new RuntimeException("Could not provide AndroidKeysetManager", e);
    }
  }

  @NonNull
  @Override
  @SuppressWarnings("unchecked")
  public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
    if (HomeViewModel.class.isAssignableFrom(modelClass)) {
      return (T)
          new HomeViewModel(
              application,
              gson,
              Injection.provideLAORepository(
                  application,
                  Injection.provideLAOService(
                      Injection.provideScarlet(application, Injection.provideOkHttpClient(), gson)),
                  keysetManager,
                  gson),
              keysetManager);
    } else if (LaoDetailViewModel.class.isAssignableFrom(modelClass)) {
      return (T)
          new LaoDetailViewModel(
              application,
              Injection.provideLAORepository(
                  application,
                  Injection.provideLAOService(
                      Injection.provideScarlet(application, Injection.provideOkHttpClient(), gson)),
                  keysetManager,
                  gson),
              gson,
              keysetManager);
    }

    throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass.getName());
  }
}
