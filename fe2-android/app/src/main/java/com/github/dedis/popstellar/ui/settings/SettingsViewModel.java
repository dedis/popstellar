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
  /** Server URL, updated everytime we change it */
  private final MutableLiveData<String> mServerUrl = new MutableLiveData<>();
  /** Stores the server URL when we open settings to check whether we changed it or not */
  private final MutableLiveData<String> mCheckServerUrl = new MutableLiveData<>();

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

  public LiveData<String> getCheckServerUrl() {
    return mCheckServerUrl;
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

  public void setCheckServerUrl(String serverUrl) {
    mCheckServerUrl.setValue(serverUrl);
  }
}
