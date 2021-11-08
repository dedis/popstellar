package com.github.dedis.popstellar.ui.settings;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.github.dedis.popstellar.SingleEvent;

import io.reactivex.disposables.CompositeDisposable;

public class SettingsViewModel extends AndroidViewModel {

  public static final String TAG = SettingsViewModel.class.getSimpleName();

  /*
   * LiveData objects for capturing events like button clicks
   */
  private final MutableLiveData<SingleEvent<Boolean>> mApplyChangesEvent = new MutableLiveData<>();

  /*
   * LiveData objects that represent the state in a fragment
   */
  private final MutableLiveData<String> mServerUrl = new MutableLiveData<>();
  private final MutableLiveData<String> mTempServerUrl = new MutableLiveData<>();

  /*
   * Dependencies for this class
   */
  private final CompositeDisposable disposables;

  public SettingsViewModel(@NonNull Application application) {
    super(application);
    disposables = new CompositeDisposable();
  }

  /*
   * Getters for MutableLiveData instances declared above
   */
  public LiveData<SingleEvent<Boolean>> getApplyChangesEvent() {
    return mApplyChangesEvent;
  }

  public LiveData<String> getServerUrl() {
    return mServerUrl;
  }

  public LiveData<String> getTempServerUrl() {
    return mTempServerUrl;
  }

  /*
   * Methods that modify the state or post an Event to update the UI.
   */
  public void applyChanges() {
    mApplyChangesEvent.setValue(new SingleEvent<>(true));
  }

  public void setServerUrl(String serverUrl) {
    mServerUrl.setValue(serverUrl);
  }

  public void setTempServerUrl(String serverUrl) {
    mTempServerUrl.setValue(serverUrl);
  }
}
