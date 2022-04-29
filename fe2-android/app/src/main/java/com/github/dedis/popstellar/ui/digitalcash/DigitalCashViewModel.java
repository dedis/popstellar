package com.github.dedis.popstellar.ui.digitalcash;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.github.dedis.popstellar.SingleEvent;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class DigitalCashViewModel extends AndroidViewModel {
    public static final String TAG = DigitalCashViewModel.class.getSimpleName();

    /*
     * LiveData objects for capturing events
     */
    private final MutableLiveData<SingleEvent<Boolean>> mOpenHomeEvent = new MutableLiveData<>();
    private final MutableLiveData<SingleEvent<Boolean>> mOpenHistoryEvent = new MutableLiveData<>();
    private final MutableLiveData<SingleEvent<Boolean>> mOpenSendEvent = new MutableLiveData<>();
    private final MutableLiveData<SingleEvent<Boolean>> mOpenReceiveEvent = new MutableLiveData<>();
    private final MutableLiveData<SingleEvent<Boolean>> mOpenIssueEvent = new MutableLiveData<>();
    private final MutableLiveData<SingleEvent<Boolean>> mOpenReceiptEvent = new MutableLiveData<>();

    @Inject
    public DigitalCashViewModel(@NonNull Application application) {
        super(application);
    }

    /*
     * Getters for MutableLiveData instances declared above
     */
    public LiveData<SingleEvent<Boolean>> getOpenHomeEvent() {
        return mOpenHomeEvent;
    }

    public LiveData<SingleEvent<Boolean>> getOpenHistoryEvent() {
        return mOpenHistoryEvent;
    }

    public LiveData<SingleEvent<Boolean>> getOpenSendEvent() {
        return mOpenSendEvent;
    }

    public LiveData<SingleEvent<Boolean>> getOpenReceiveEvent() {
        return mOpenReceiveEvent;
    }

    public LiveData<SingleEvent<Boolean>> getOpenIssueEvent() {
        return mOpenIssueEvent;
    }

    public LiveData<SingleEvent<Boolean>> getOpenReceiptEvent() {
        return mOpenReceiptEvent;
    }

    /*
     * Methods that modify the state or post an Event to update the UI.
     */
    public void openHome() {
        mOpenHomeEvent.postValue(new SingleEvent<>(true));
    }

    public void openHistory() {
        mOpenHistoryEvent.postValue(new SingleEvent<>(true));
    }

    public void openIssue() {
        mOpenIssueEvent.postValue(new SingleEvent<>(true));
    }

    public void openReceive() {
        mOpenReceiveEvent.postValue(new SingleEvent<>(true));
    }

    public void openSend() {
        mOpenSendEvent.postValue(new SingleEvent<>(true));
    }

    public void openReceipt() {
        mOpenReceiptEvent.postValue(new SingleEvent<>(true));
    }
}
