package com.github.dedis.popstellar.ui.home;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.SingleEvent;
import com.github.dedis.popstellar.model.objects.Lao;
import com.github.dedis.popstellar.repository.LAORepository;
import com.github.dedis.popstellar.repository.LAOState;
import com.github.dedis.popstellar.repository.remote.GlobalNetworkManager;
import com.github.dedis.popstellar.utility.error.ErrorUtils;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.disposables.CompositeDisposable;

@HiltViewModel
public class ConnectingViewModel extends AndroidViewModel {

  public static final String TAG = ConnectingViewModel.class.getSimpleName();

  private final MutableLiveData<SingleEvent<String>> mOpenLaoEvent = new MutableLiveData<>();
  private final MutableLiveData<SingleEvent<Boolean>> mOpenHomeEvent = new MutableLiveData<>();
  private final MutableLiveData<SingleEvent<Boolean>> mCancelConnectEvent = new MutableLiveData<>();

  private final MutableLiveData<String> mConnectingLao = new MutableLiveData<>();

  private final GlobalNetworkManager networkManager;
  private final LAORepository laoRepository;

  private final CompositeDisposable disposables = new CompositeDisposable();

  @Inject
  public ConnectingViewModel(
      @NonNull Application application,
      GlobalNetworkManager networkManager,
      LAORepository laoRepository) {
    super(application);

    this.laoRepository = laoRepository;
    this.networkManager = networkManager;
  }

  public void handleConnecting(String channelId) {
    Lao lao = new Lao(channelId);
    Log.d(TAG, "about to start connecting to lao " + channelId);
    // Create the new LAO and add it to the LAORepository LAO lists
    laoRepository.getLaoById().put(lao.getId(), new LAOState(lao));
    laoRepository.setAllLaoSubject();

    disposables.add(
        networkManager
            .getMessageSender()
            .subscribe(lao.getChannel())
            .doAfterTerminate(() -> openLAO(lao.getId()))
            .subscribe(
                () -> {
                  Log.d(TAG, "subscribing to LAO with id " + lao.getId());

                  Log.d(TAG, "got success result for subscribe to lao");

                  //  openLAO(lao.getId());
                },
                error -> {
                  // In case of error, log it and go to home activity
                  ErrorUtils.logAndShow(getApplication(), TAG, error, R.string.error_subscribe_lao);
                  openHome();
                }));

    setConnectingLao(lao.getId());
  }

  public void openLAO(String laoId) {
    mOpenLaoEvent.setValue(new SingleEvent<>(laoId));
  }

  public void setConnectingLao(String lao) {
    this.mConnectingLao.postValue(lao);
  }

  public void cancelConnect() {
    mCancelConnectEvent.setValue(new SingleEvent<>(true));
  }

  public void openHome() {
    mOpenHomeEvent.postValue(new SingleEvent<>(true));
  }

  public LiveData<String> getConnectingLao() {
    return mConnectingLao;
  }

  public LiveData<SingleEvent<String>> getOpenLaoEvent() {
    return mOpenLaoEvent;
  }

  public LiveData<SingleEvent<Boolean>> getOpenHomeEvent() {
    return mOpenHomeEvent;
  }

  public LiveData<SingleEvent<Boolean>> getCancelConnectEvent() {
    return mCancelConnectEvent;
  }

  public GlobalNetworkManager getNetworkManager() {
    return networkManager;
  }
}
