package com.github.dedis.popstellar.ui.home.connecting;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.*;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.SingleEvent;
import com.github.dedis.popstellar.model.objects.Lao;
import com.github.dedis.popstellar.repository.LAORepository;
import com.github.dedis.popstellar.repository.remote.GlobalNetworkManager;
import com.github.dedis.popstellar.utility.error.ErrorUtils;
import com.tinder.scarlet.WebSocket;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.observables.ConnectableObservable;

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

  /**
   * Handles the subscription and catchup process to an LAO
   *
   * @param channelId
   */
  private void handleConnecting(String channelId) {
    Lao lao = new Lao(channelId);

    Log.d(TAG, "connecting to lao " + channelId);
    disposables.add(
        networkManager
            .getMessageSender()
            .subscribe(lao.getChannel())
            .subscribe(
                () -> {
                  Log.d(TAG, "subscribing to LAO with id " + lao.getId());
                  openLAO(lao.getId());
                },
                error -> {
                  // In case of error, log it and go to home activity
                  ErrorUtils.logAndShow(getApplication(), TAG, error, R.string.error_subscribe_lao);
                  openHome();
                }));
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

  /**
   * Waits in background for the connection to the new server address to be opened And then starts
   * call for the subscribe and catchup process
   *
   * @param channelId
   */
  public void handleOpenConnection(String channelId) {
    // Sets the lao id displayed to users
    setConnectingLao(channelId);

    // This object will allow us to get all the event of the underlying observable, even if they
    // were emitted prior to the subscribe
    ConnectableObservable<WebSocket.Event> replay =
        networkManager.getMessageSender().getConnectEvents().replay();
    disposables.add(
        replay.subscribe(
            v -> {
              if (v.toString().contains("OnConnectionOpened")) {
                Log.d(TAG, "connection opened with new server address");
                handleConnecting(channelId);
              }
            }));

    // Now that we observe the events we let the observable know it can begin to emit events
    replay.connect();
  }
}
