package com.github.dedis.popstellar.ui.digitalcash;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.LiveDataReactiveStreams;
import androidx.lifecycle.MutableLiveData;

import com.github.dedis.popstellar.SingleEvent;
import com.github.dedis.popstellar.model.objects.Lao;
import com.github.dedis.popstellar.repository.LAORepository;
import com.github.dedis.popstellar.repository.LAOState;
import com.github.dedis.popstellar.repository.remote.GlobalNetworkManager;
import com.github.dedis.popstellar.utility.security.KeyManager;
import com.google.gson.Gson;

import java.util.List;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.BackpressureStrategy;
import io.reactivex.disposables.CompositeDisposable;

@HiltViewModel
public class DigitalCashViewModel extends AndroidViewModel {
  public static final String TAG = DigitalCashViewModel.class.getSimpleName();
  /*
   * Dependencies for this class
   */
  private final LAORepository laoRepository;
  private final GlobalNetworkManager networkManager;
  private final Gson gson;
  private final KeyManager keyManager;
  private final CompositeDisposable disposables;

  private static final String DIGITAL_CASH = "DIGITAL_CASH";

  /*
   * LiveData objects for capturing events
   */
  private final MutableLiveData<SingleEvent<Boolean>> mOpenHomeEvent = new MutableLiveData<>();
  private final MutableLiveData<SingleEvent<Boolean>> mOpenHistoryEvent = new MutableLiveData<>();
  private final MutableLiveData<SingleEvent<Boolean>> mOpenSendEvent = new MutableLiveData<>();
  private final MutableLiveData<SingleEvent<Boolean>> mOpenReceiveEvent = new MutableLiveData<>();
  private final MutableLiveData<SingleEvent<Boolean>> mOpenIssueEvent = new MutableLiveData<>();

  private final LiveData<List<Lao>> mLAOs;
  private final MutableLiveData<String> mLaoId = new MutableLiveData<>();
  private final MutableLiveData<String> mLaoName = new MutableLiveData<>();

  public DigitalCashViewModel(
      @NonNull Application application,
      LAORepository laoRepository,
      GlobalNetworkManager networkManager,
      Gson gson,
      KeyManager keyManager) {
    super(application);
    this.laoRepository = laoRepository;
    this.networkManager = networkManager;
    this.gson = gson;
    this.keyManager = keyManager;
    disposables = new CompositeDisposable();

    this.mLAOs =
        LiveDataReactiveStreams.fromPublisher(
            this.laoRepository.getAllLaos().toFlowable(BackpressureStrategy.BUFFER));
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

  public LiveData<List<Lao>> getLAOs() {
    return mLAOs;
  }

  public LiveData<String> getLaoId() {
    return mLaoId;
  }

  public LiveData<String> getLaoName() {
    return mLaoName;
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

  public void setLaoId(String laoId) {
    mLaoId.setValue(laoId);
  }

  public void setLaoName(String laoName) {
    mLaoName.setValue(laoName);
  }

  @Override
  protected void onCleared() {
    super.onCleared();
    disposables.dispose();
  }

  @Nullable
  public Lao getCurrentLao() {
    return getLao(getLaoId().getValue());
  }

  @Nullable
  private Lao getLao(String laoId) {
    LAOState laoState = laoRepository.getLaoById().get(laoId);
    if (laoState == null) return null;

    return laoState.getLao();
  }
}
