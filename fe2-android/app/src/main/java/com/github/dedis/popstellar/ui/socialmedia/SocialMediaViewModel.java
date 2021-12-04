package com.github.dedis.popstellar.ui.socialmedia;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.github.dedis.popstellar.SingleEvent;
import com.github.dedis.popstellar.model.network.answer.Result;
import com.github.dedis.popstellar.model.objects.Lao;
import com.github.dedis.popstellar.repository.LAORepository;
import com.github.dedis.popstellar.utility.security.Keys;
import com.google.crypto.tink.KeysetHandle;
import com.google.crypto.tink.integration.android.AndroidKeysetManager;
import com.google.gson.Gson;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class SocialMediaViewModel extends AndroidViewModel {
  public static final String TAG = SocialMediaViewModel.class.getSimpleName();
  private static final String PK_FAILURE_MESSAGE = "failed to retrieve public key";
  public static final Integer MAX_CHAR_NUMBERS = 300;

  /*
   * LiveData objects for capturing events
   */
  private final MutableLiveData<SingleEvent<Boolean>> mOpenHomeEvent = new MutableLiveData<>();
  private final MutableLiveData<SingleEvent<Boolean>> mOpenSendEvent = new MutableLiveData<>();
  private final MutableLiveData<SingleEvent<Boolean>> mOpenSearchEvent = new MutableLiveData<>();
  private final MutableLiveData<SingleEvent<Boolean>> mOpenFollowingEvent = new MutableLiveData<>();
  private final MutableLiveData<SingleEvent<Boolean>> mOpenProfileEvent = new MutableLiveData<>();
  private final MutableLiveData<SingleEvent<Boolean>> mSendNewChirpEvent = new MutableLiveData<>();

  private final MutableLiveData<Integer> mNumberCharsLeft = new MutableLiveData<>();
  private final MutableLiveData<List<Lao>> mLAOs = new MutableLiveData<>();
  private final MutableLiveData<String> mLaoId = new MutableLiveData<>();

  /*
   * Dependencies for this class
   */
  private final LAORepository mLaoRepository;
  private final Gson mGson;
  private final AndroidKeysetManager mKeysetManager;
  private final CompositeDisposable disposables;

  public SocialMediaViewModel(
      @NonNull Application application,
      LAORepository laoRepository,
      Gson gson,
      AndroidKeysetManager keysetManager) {
    super(application);
    mLaoRepository = laoRepository;
    mGson = gson;
    mKeysetManager = keysetManager;
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

  public LiveData<SingleEvent<Boolean>> getOpenSearchEvent() {
    return mOpenSearchEvent;
  }

  public LiveData<SingleEvent<Boolean>> getOpenFollowingEvent() {
    return mOpenFollowingEvent;
  }

  public LiveData<SingleEvent<Boolean>> getOpenProfileEvent() {
    return mOpenProfileEvent;
  }

  public LiveData<SingleEvent<Boolean>> getSendNewChirpEvent() {
    return mSendNewChirpEvent;
  }

  public LiveData<Integer> getNumberCharsLeft() {
    return mNumberCharsLeft;
  }

  public LiveData<List<Lao>> getLAOs() {
    return mLAOs;
  }

  public LiveData<String> getLaoId() {
    return mLaoId;
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

  public void openSearch() {
    mOpenSearchEvent.postValue(new SingleEvent<>(true));
  }

  public void openFollowing() {
    mOpenFollowingEvent.postValue(new SingleEvent<>(true));
  }

  public void openProfile() {
    mOpenProfileEvent.postValue(new SingleEvent<>(true));
  }

  public void sendNewChirpEvent() {
    mSendNewChirpEvent.postValue(new SingleEvent<>(true));
  }

  public void setNumberCharsLeft(Integer numberChars) {
    mNumberCharsLeft.setValue(numberChars);
  }

  public void setLAOs(List<Lao> laos) {
    mLAOs.setValue(laos);
  }

  public void setLaoId(String laoId) {
    mLaoId.setValue(laoId);
  }

  /** Subscribe to the general channel: /root/<lao_id>/social/chirps */
  public void subscribeToGeneralChannel(String laoId) {
    Log.d(TAG, "subscribing to channel: /root/" + laoId + "/social/chirps");

    String channel = "/root/" + laoId + "/social/chirps";

    Disposable disposable =
        mLaoRepository
            .sendSubscribe(channel)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .timeout(5, TimeUnit.SECONDS)
            .subscribe(
                answer -> {
                  if (answer instanceof Result) {
                    Log.d(TAG, "subscribed to the channel");
                  } else {
                    Log.d(TAG, "failed to subscribe to the channel");
                  }
                },
                throwable ->
                    Log.d(TAG, "timed out waiting for result on subscribe/channel", throwable));

    disposables.add(disposable);
  }

  /** Subscribe to a channel: /root/<lao_id>/social/<sender> */
  public void subscribeToChannel(String laoId) {
    Log.d(TAG, "subscribing to channel: /root/" + laoId + "/social/<sender>");

    try {
      KeysetHandle publicKeysetHandle = mKeysetManager.getKeysetHandle().getPublicKeysetHandle();
      String publicKey = Keys.getEncodedKey(publicKeysetHandle);
      String channel = "/root/" + laoId + "/social/" + publicKey;

      Disposable disposable =
          mLaoRepository
              .sendSubscribe(channel)
              .subscribeOn(Schedulers.io())
              .observeOn(AndroidSchedulers.mainThread())
              .timeout(5, TimeUnit.SECONDS)
              .subscribe(
                  answer -> {
                    if (answer instanceof Result) {
                      Log.d(TAG, "subscribed to the channel");
                    } else {
                      Log.d(TAG, "failed to subscribe to the channel");
                    }
                  },
                  throwable ->
                      Log.d(TAG, "timed out waiting for result on subscribe/channel", throwable));

      disposables.add(disposable);

    } catch (GeneralSecurityException | IOException e) {
      Log.d(TAG, PK_FAILURE_MESSAGE, e);
    }
  }

  /** Send a chirp to your own channel */
  public void sendChirp() {}
}
