package com.github.dedis.student20_pop;

import android.app.Application;

import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.github.dedis.student20_pop.detail.LaoDetailViewModel;
import com.github.dedis.student20_pop.home.HomeViewModel;
import com.google.gson.Gson;

public class ViewModelFactory extends ViewModelProvider.NewInstanceFactory {

    private static volatile ViewModelFactory INSTANCE;

    private final Application application;

    private final Gson gson = Injection.provideGson();

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
    }

    @Override
    public <T extends ViewModel> T create(Class<T> modelClass) {
        if (modelClass.isAssignableFrom(HomeViewModel.class)) {
            return (T) new HomeViewModel(application, gson, Injection.provideLAORepository(application, gson));
        }
        else if (modelClass.isAssignableFrom(LaoDetailViewModel.class)) {
            return (T) new LaoDetailViewModel(application, Injection.provideLAORepository(application, gson));
        }

        throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass.getName());
    }


}
