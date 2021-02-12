package com.github.dedis.student20_pop;

import android.app.Application;
import android.util.AndroidRuntimeException;

import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.github.dedis.student20_pop.home.HomeViewModel;
import com.google.crypto.tink.KeysetHandle;
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
                    INSTANCE = new ViewModelFactory(application);
                }
            }
        }
        return INSTANCE;
    }

    public static void destroyInstance() {
        INSTANCE = null;
    }

    private ViewModelFactory(Application application) {
        this.application = application;
        try {
            this.keysetManager = Injection.provideAndroidKeysetManager(application.getApplicationContext());
        } catch (IOException | GeneralSecurityException e) {
            throw new RuntimeException("Could not provide AndroidKeysetManager", e);
        }
    }

    @Override
    public <T extends ViewModel> T create(Class<T> modelClass) {
        if (modelClass.isAssignableFrom(HomeViewModel.class)) {
            return (T) new HomeViewModel(
                    application,
                    gson,
                    Injection.provideLAORepository(application, gson),
                    keysetManager
            );
        }

        throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass.getName());
    }


}
