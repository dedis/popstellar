package com.github.dedis.popstellar.ui.socialmedia;

import android.app.Application;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.LiveDataReactiveStreams;
import androidx.lifecycle.MutableLiveData;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.SingleEvent;
import com.github.dedis.popstellar.model.network.answer.Result;
import com.github.dedis.popstellar.model.network.method.message.MessageGeneral;
import com.github.dedis.popstellar.model.network.method.message.data.socialmedia.AddChirp;
import com.github.dedis.popstellar.model.network.method.message.data.socialmedia.DeleteChirp;
import com.github.dedis.popstellar.model.objects.Chirp;
import com.github.dedis.popstellar.model.objects.Lao;
import com.github.dedis.popstellar.model.objects.security.MessageID;
import com.github.dedis.popstellar.model.objects.security.PoPToken;
import com.github.dedis.popstellar.repository.LAORepository;
import com.github.dedis.popstellar.repository.LAOState;
import com.github.dedis.popstellar.utility.error.keys.KeyException;
import com.github.dedis.popstellar.utility.security.KeyManager;
import com.google.gson.Gson;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.BackpressureStrategy;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

@HiltViewModel
public class SocialMediaViewModel extends AndroidViewModel {
  public static final String TAG = SocialMediaViewModel.class.getSimpleName();
  private static final String LAO_FAILURE_MESSAGE = "failed to retrieve lao";
  private static final String PUBLISH_MESSAGE = "sending publish message";
  private static final String ROOT = "/root/";
  private static final String SOCIAL = "/social/";
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
  private final MutableLiveData<SingleEvent<MessageID>> mDeleteChirpEvent = new MutableLiveData<>();

  private final MutableLiveData<Integer> mNumberCharsLeft = new MutableLiveData<>();
  private final LiveData<List<Lao>> mLAOs;
  private final MutableLiveData<String> mLaoId = new MutableLiveData<>();
  private final MutableLiveData<String> mLaoName = new MutableLiveData<>();

  /*
   * Dependencies for this class
   */
  private final LAORepository mLaoRepository;
  private final Gson mGson;
  private final KeyManager mKeyManager;
  private final CompositeDisposable disposables;

  @Inject
  public SocialMediaViewModel(
      @NonNull Application application,
      LAORepository laoRepository,
      Gson gson,
      KeyManager keyManager) {
    super(application);
    mLaoRepository = laoRepository;
    mGson = gson;
    mKeyManager = keyManager;
    disposables = new CompositeDisposable();

    mLAOs =
        LiveDataReactiveStreams.fromPublisher(
            mLaoRepository.getAllLaos().toFlowable(BackpressureStrategy.BUFFER));
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

  public LiveData<SingleEvent<MessageID>> getDeleteChirpEvent() {
    return mDeleteChirpEvent;
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

  public LiveData<String> getLaoName() {
    return mLaoName;
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

  public void deleteChirpEvent(MessageID chirpId) {
    mDeleteChirpEvent.postValue(new SingleEvent<>(chirpId));
  }

  public void setNumberCharsLeft(Integer numberChars) {
    mNumberCharsLeft.setValue(numberChars);
  }

  public void setLaoId(String laoId) {
    mLaoId.setValue(laoId);
  }

  public void setLaoName(String laoName) {
    mLaoName.setValue(laoName);
  }

  /** Subscribe to the channel: /root/<lao_id>/social/chirps */
  public void subscribeToGeneralChannel(String laoId) {
    Log.d(TAG, "subscribing to channel: " + ROOT + laoId + "/social/chirps");

    String channel = ROOT + laoId + "/social/chirps";

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
    Log.d(TAG, "subscribing to channel: " + ROOT + laoId + "/social/<sender>");

    LAOState laoState = mLaoRepository.getLaoById().get(ROOT + laoId);
    if (laoState == null) {
      Log.e(TAG, LAO_FAILURE_MESSAGE);
      return;
    }

    try {
      String channel =
          ROOT
              + laoId
              + SOCIAL
              + mKeyManager.getValidPoPToken(laoState.getLao()).getPublicKey().getEncoded();

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
    } catch (KeyException e) {
      Log.e(TAG, getApplication().getString(R.string.error_pop_token), e);
      Toast.makeText(
              getApplication().getApplicationContext(),
              R.string.error_toast_pop_token + e.getMessage(),
              Toast.LENGTH_LONG)
          .show();
    }
  }

  /**
   * Send a chirp to your own channel.
   *
   * <p>Publish a MessageGeneral containing AddChirp data.
   *
   * @param text the text written in the chirp
   * @param parentId the id of the chirp to which you replied
   * @param timestamp the time at which you sent the chirp
   */
  public void sendChirp(String text, @Nullable MessageID parentId, long timestamp) {
    Log.d(TAG, "Sending a chirp");
    String laoChannel = ROOT + getLaoId().getValue();
    LAOState laoState = mLaoRepository.getLaoById().get(laoChannel);
    if (laoState == null) {
      Log.e(TAG, LAO_FAILURE_MESSAGE);
      return;
    }

    AddChirp addChirp = new AddChirp(text, parentId, timestamp);

    try {
      PoPToken token = mKeyManager.getValidPoPToken(laoState.getLao());
      String channel = laoChannel + SOCIAL + token.getPublicKey().getEncoded();
      Log.d(TAG, PUBLISH_MESSAGE);
      MessageGeneral msg = new MessageGeneral(token, addChirp, mGson);

      Disposable disposable =
          mLaoRepository
              .sendPublish(channel, msg)
              .subscribeOn(Schedulers.io())
              .observeOn(AndroidSchedulers.mainThread())
              .timeout(5, TimeUnit.SECONDS)
              .subscribe(
                  answer -> {
                    if (answer instanceof Result) {
                      Log.d(TAG, "sent chirp with messageId: " + msg.getMessageId());
                    } else {
                      Log.d(TAG, "failed to send chirp");
                      Toast.makeText(
                              getApplication().getApplicationContext(),
                              R.string.error_sending_chirp,
                              Toast.LENGTH_LONG)
                          .show();
                    }
                  },
                  throwable -> Log.d(TAG, "timed out waiting for result on chirp/add", throwable));
      disposables.add(disposable);
    } catch (KeyException e) {
      Log.e(TAG, getApplication().getString(R.string.error_pop_token), e);
      Toast.makeText(
              getApplication().getApplicationContext(),
              R.string.error_toast_pop_token + e.getMessage(),
              Toast.LENGTH_LONG)
          .show();
    }
  }

  public void deleteChirp(MessageID chirpId, long timestamp) {
    Log.d(TAG, "Deleting the chirp with id: " + chirpId);
    String laoChannel = ROOT + getLaoId().getValue();
    LAOState laoState = mLaoRepository.getLaoById().get(laoChannel);
    if (laoState == null) {
      Log.e(TAG, LAO_FAILURE_MESSAGE);
      return;
    }

    DeleteChirp deleteChirp = new DeleteChirp(chirpId, timestamp);

    try {
      PoPToken token = mKeyManager.getValidPoPToken(laoState.getLao());
      String channel = laoChannel + SOCIAL + token.getPublicKey().getEncoded();
      Log.d(TAG, PUBLISH_MESSAGE);
      MessageGeneral msg = new MessageGeneral(token, deleteChirp, mGson);

      Disposable disposable =
          mLaoRepository
              .sendPublish(channel, msg)
              .subscribeOn(Schedulers.io())
              .observeOn(AndroidSchedulers.mainThread())
              .timeout(5, TimeUnit.SECONDS)
              .subscribe(
                  answer -> {
                    if (answer instanceof Result) {
                      Log.d(TAG, "Deleted chirp with messageId: " + msg.getMessageId());
                      Toast.makeText(
                              getApplication().getApplicationContext(),
                              "Deleted chirp!",
                              Toast.LENGTH_LONG)
                          .show();
                    } else {
                      Log.d(TAG, "Failed to delete chirp");
                      Toast.makeText(
                              getApplication().getApplicationContext(),
                              "Error when deleting chirp. Please try again",
                              Toast.LENGTH_LONG)
                          .show();
                    }
                  },
                  throwable ->
                      Log.d(TAG, "Timed out waiting for result on chirp/delete", throwable));
      disposables.add(disposable);
    } catch (KeyException e) {
      Log.e(TAG, getApplication().getString(R.string.error_pop_token), e);
      Toast.makeText(
              getApplication().getApplicationContext(),
              R.string.error_toast_pop_token + e.getMessage(),
              Toast.LENGTH_LONG)
          .show();
    }
  }

  public List<MessageID> getChirpIdList(String laoId) {
    Lao lao = mLaoRepository.getLaoByChannel(ROOT + laoId);
    return lao.getChirpsIdInOrder();
  }

  public Map<MessageID, Chirp> getAllChirps(String laoId) {
    Lao lao = mLaoRepository.getLaoByChannel(ROOT + laoId);
    return lao.getAllChirps();
  }

  /**
   * Check whether the sender of a chirp is the current user
   *
   * @param sender String of the PoPToken PublicKey
   * @return true if the sender is the current user
   */
  public boolean isOwner(String sender) {
    Log.d(TAG, "Testing if the sender is also the owner");
    boolean isOwner = false;
    String laoChannel = ROOT + getLaoId().getValue();
    LAOState laoState = mLaoRepository.getLaoById().get(laoChannel);
    if (laoState == null) {
      Log.e(TAG, LAO_FAILURE_MESSAGE);
      return false;
    }

    try {
      PoPToken token = mKeyManager.getValidPoPToken(laoState.getLao());
      isOwner = sender.equals(token.getPublicKey().getEncoded());
    } catch (KeyException e) {
      Log.e(TAG, getApplication().getString(R.string.error_pop_token), e);
    }
    return isOwner;
  }
}
