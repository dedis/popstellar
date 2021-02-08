package com.github.dedis.student20_pop.home;

import android.Manifest;
import android.app.Application;
import android.content.Context;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.github.dedis.student20_pop.Event;

public class HomeViewModel extends AndroidViewModel {

    private final Context mContext;

    private final MutableLiveData<Event<String>> mOpenLaoEvent = new MutableLiveData<>();

    private final MutableLiveData<Event<String>> mOpenConnectEvent = new MutableLiveData<>();

    private final MutableLiveData<Event<Boolean>> mOpenLaunchEvent = new MutableLiveData<>();

    public HomeViewModel(@NonNull Application application) {
        super(application);
        mContext = application.getApplicationContext();
    }

    public LiveData<Event<String>> getOpenLaoEvent() {
        return mOpenLaoEvent;
    }

    public LiveData<Event<String>> getOpenConnectEvent() {
        return mOpenConnectEvent;
    }

    public LiveData<Event<Boolean>> getOpenLaunchEvent() {
        return mOpenLaunchEvent;
    }

    void openLAO(String laoId) {
        mOpenLaoEvent.setValue(new Event<>(laoId));
    }

    public void openConnect() {
        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            // TODO: Const
            mOpenConnectEvent.setValue(new Event<>("SCAN"));
        } else {
            mOpenConnectEvent.setValue(new Event<>("REQUEST_CAMERA_PERMISSION"));
        }
    }

    public void openLaunch() {
        mOpenLaunchEvent.setValue(new Event<>(true));
    }
}
