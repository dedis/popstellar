package com.github.dedis.popstellar;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.github.dedis.popstellar.ui.detail.LaoDetailViewModel;
import com.github.dedis.popstellar.ui.home.HomeViewModel;
import com.github.dedis.popstellar.ui.settings.SettingsViewModel;
import com.google.crypto.tink.integration.android.AndroidKeysetManager;
import com.google.gson.Gson;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class ViewModelFactory extends ViewModelProvider.NewInstanceFactory {

  private static volatile ViewModelFactory INSTANCE;

  private final Application application;

  private final Gson gson = Injection.provideGson();

  private final AndroidKeysetManager keysetManager;

  public static ViewModelFactory getInstance(Application application) {
    if (INSTANCE == null) {
      synchronized (ViewModelFactory.class) {
        if (INSTANCE == null) {
          Log.d(
              ViewModelFactory.class.getSimpleName(),
              "Creating new instance of " + ViewModelFactory.class.getSimpleName());
          INSTANCE = new ViewModelFactory(application);
        }
      }
    }
    return INSTANCE;
  }

  public static void destroyInstance() {
    Log.d(
        ViewModelFactory.class.getSimpleName(),
        "Destroying " + ViewModelFactory.class.getSimpleName() + " current instance");
    INSTANCE = null;
  }

  private ViewModelFactory(Application application) {
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
    } else if (SettingsViewModel.class.isAssignableFrom(modelClass)) {
      return (T)
          new SettingsViewModel(
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
