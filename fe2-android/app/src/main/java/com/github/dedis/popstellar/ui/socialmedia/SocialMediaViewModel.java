package com.github.dedis.popstellar.ui.socialmedia;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.github.dedis.popstellar.SingleEvent;

import io.reactivex.disposables.CompositeDisposable;

public class SocialMediaViewModel extends AndroidViewModel {
  public static final String TAG = SocialMediaViewModel.class.getSimpleName();

  /*
   * LiveData objects for capturing events
   */
  private final MutableLiveData<SingleEvent<Boolean>> mOpenHomeEvent = new MutableLiveData<>();
  private final MutableLiveData<SingleEvent<Boolean>> mOpenSendEvent = new MutableLiveData<>();
  private final MutableLiveData<SingleEvent<Boolean>> mOpenFollowingEvent = new MutableLiveData<>();
  private final MutableLiveData<SingleEvent<Boolean>> mOpenProfileEvent = new MutableLiveData<>();

  /*
   * Dependencies for this class
   */
  private final CompositeDisposable disposables;

  public SocialMediaViewModel(@NonNull Application application) {
    super(application);
    disposables = new CompositeDisposable();
  }

  @Override
  protected void onCleared() {
    super.onCleared();
    disposables.dispose();
  }

  /*
   * Getters for MutableLiveData instances declared above
   */
  public LiveData<SingleEvent<Boolean>> getOpenHomeEvent() {
    return mOpenHomeEvent;
  }

  public LiveData<SingleEvent<Boolean>> getOpenSendEvent() {
    return mOpenSendEvent;
  }

  public LiveData<SingleEvent<Boolean>> getOpenFollowingEvent() {
    return mOpenFollowingEvent;
  }

  public LiveData<SingleEvent<Boolean>> getOpenProfileEvent() {
    return mOpenProfileEvent;
  }

  /*
   * Methods that modify the state or post an Event to update the UI.
   */
  public void openHome() {
    mOpenHomeEvent.postValue(new SingleEvent<>(true));
  }

  public void openSend() {
    mOpenSendEvent.postValue(new SingleEvent<>(true));
  }

  public void openFollowing() {
    mOpenFollowingEvent.postValue(new SingleEvent<>(true));
  }

  public void openProfile() {
    mOpenProfileEvent.postValue(new SingleEvent<>(true));
  }
}
