package com.github.dedis.student20_pop.launch;

import android.app.Application;
import android.content.Context;
import android.text.Editable;

import androidx.annotation.NonNull;
import androidx.databinding.BindingAdapter;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.github.dedis.student20_pop.Event;

public class LaunchViewModel extends AndroidViewModel {

    private final Context mContext;

    private final MutableLiveData<Event<Boolean>> mLaunchLaoEvent = new MutableLiveData<>();

    private final MutableLiveData<Event<Boolean>> mCancelLaunchEvent = new MutableLiveData<>();

    private final MutableLiveData<String> laoName = new MutableLiveData<>();

    public LaunchViewModel(@NonNull Application application) {
        super(application);
        mContext = application.getApplicationContext();
    }

    public LiveData<Event<Boolean>> getLaunchLaoEvent() {
        return mLaunchLaoEvent;
    }

    public LiveData<Event<Boolean>> getCancelLaunchEvent() {
        return mCancelLaunchEvent;
    }

    public MutableLiveData<String> getLaoName() {
        return laoName;
    }

    @BindingAdapter("android:afterTextChanged")
    public void setLaoName(Editable name) {
        this.laoName.setValue(name.toString());
    }

    public void launchLao() {
        mLaunchLaoEvent.setValue(new Event<>(true));
    }

    public void cancelLaunch() {
        mCancelLaunchEvent.setValue(new Event<>(true));
    }
}