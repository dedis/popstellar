package com.github.dedis.student20_pop.home;

import android.app.Application;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.github.dedis.student20_pop.Event;

public class HomeViewModel extends AndroidViewModel {

    private final Context mContext;

    private final MutableLiveData<Event<String>> mOpenLaoEvent = new MutableLiveData<Event<String>>();

    public HomeViewModel(@NonNull Application application) {
        super(application);
        mContext = application.getApplicationContext();
    }

    public LiveData<Event<String>> getOpenLaoEvent() {
        return mOpenLaoEvent;
    }

    void openLAO(String laoId) {
        mOpenLaoEvent.setValue(new Event<>(laoId));
    }
}
