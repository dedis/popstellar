package com.github.dedis.popstellar.ui.socialmedia;

import android.app.Application;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.*;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.model.network.method.message.MessageGeneral;
import com.github.dedis.popstellar.model.network.method.message.data.socialmedia.AddChirp;
import com.github.dedis.popstellar.model.network.method.message.data.socialmedia.DeleteChirp;
import com.github.dedis.popstellar.model.objects.*;
import com.github.dedis.popstellar.model.objects.security.MessageID;
import com.github.dedis.popstellar.model.objects.security.PoPToken;
import com.github.dedis.popstellar.repository.LAORepository;
import com.github.dedis.popstellar.repository.LAOState;
import com.github.dedis.popstellar.repository.remote.GlobalNetworkManager;
import com.github.dedis.popstellar.utility.ActivityUtils;
import com.github.dedis.popstellar.ui.navigation.NavigationViewModel;
import com.github.dedis.popstellar.utility.error.ErrorUtils;
import com.github.dedis.popstellar.utility.error.keys.KeyException;
import com.github.dedis.popstellar.utility.security.KeyManager;
import com.google.gson.Gson;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.BackpressureStrategy;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;

@HiltViewModel
public class SocialMediaViewModel extends NavigationViewModel<SocialMediaTab> {
  public static final String TAG = SocialMediaViewModel.class.getSimpleName();
  private static final String LAO_FAILURE_MESSAGE = "failed to retrieve lao";
  private static final String PUBLISH_MESSAGE = "sending publish message";
  private static final String SOCIAL = "social";
  public static final Integer MAX_CHAR_NUMBERS = 300;

  /*
   * LiveData objects for capturing events
   */
  private final MutableLiveData<Integer> mNumberCharsLeft = new MutableLiveData<>();
  private final LiveData<List<Lao>> mLAOs;
  private final MutableLiveData<String> mLaoId = new MutableLiveData<>();
  private final MutableLiveData<String> mLaoName = new MutableLiveData<>();

  /*
   * Dependencies for this class
   */
  private final LAORepository laoRepository;
  private final GlobalNetworkManager networkManager;
  private final Gson gson;
  private final KeyManager keyManager;
  private final CompositeDisposable disposables;
  private final Wallet wallet;

  @Inject
  public SocialMediaViewModel(
      @NonNull Application application,
      LAORepository laoRepository,
      GlobalNetworkManager networkManager,
      Gson gson,
      KeyManager keyManager,
      Wallet wallet) {
    super(application);
    this.laoRepository = laoRepository;
    this.networkManager = networkManager;
    this.gson = gson;
    this.keyManager = keyManager;
    this.wallet = wallet;

    disposables = new CompositeDisposable();

    mLAOs =
        LiveDataReactiveStreams.fromPublisher(
            this.laoRepository.getAllLaos().toFlowable(BackpressureStrategy.BUFFER));
  }

  @Override
  protected void onCleared() {
    super.onCleared();
    disposables.dispose();
  }

  /*
   * Getters for MutableLiveData instances declared above
   */
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

  public void setNumberCharsLeft(Integer numberChars) {
    mNumberCharsLeft.setValue(numberChars);
  }

  public void setLaoId(String laoId) {
    mLaoId.setValue(laoId);
  }

  public void setLaoName(String laoName) {
    mLaoName.setValue(laoName);
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
    Lao lao = getCurrentLao();
    if (lao == null) {
      Log.e(TAG, LAO_FAILURE_MESSAGE);
      return;
    }

    AddChirp addChirp = new AddChirp(text, parentId, timestamp);

    try {
      PoPToken token = keyManager.getValidPoPToken(lao);
      Channel channel =
          lao.getChannel().subChannel(SOCIAL).subChannel(token.getPublicKey().getEncoded());
      Log.d(TAG, PUBLISH_MESSAGE);
      MessageGeneral msg = new MessageGeneral(token, addChirp, gson);

      Disposable disposable =
          networkManager
              .getMessageSender()
              .publish(token, channel, addChirp)
              .subscribe(
                  () -> Log.d(TAG, "sent chirp with messageId: " + msg.getMessageId()),
                  error ->
                      ErrorUtils.logAndShow(
                          getApplication(), TAG, error, R.string.error_sending_chirp));
      disposables.add(disposable);
    } catch (KeyException e) {
      ErrorUtils.logAndShow(getApplication(), TAG, e, R.string.error_retrieve_own_token);
    }
  }

  public void deleteChirp(MessageID chirpId, long timestamp) {
    Log.d(TAG, "Deleting the chirp with id: " + chirpId);
    Lao lao = getCurrentLao();
    if (lao == null) {
      Log.e(TAG, LAO_FAILURE_MESSAGE);
      return;
    }

    DeleteChirp deleteChirp = new DeleteChirp(chirpId, timestamp);

    try {
      PoPToken token = keyManager.getValidPoPToken(lao);
      Channel channel =
          lao.getChannel().subChannel(SOCIAL).subChannel(token.getPublicKey().getEncoded());
      Log.d(TAG, PUBLISH_MESSAGE);
      MessageGeneral msg = new MessageGeneral(token, deleteChirp, gson);

      Disposable disposable =
          networkManager
              .getMessageSender()
              .publish(token, channel, deleteChirp)
              .subscribe(
                  () -> {
                    Log.d(TAG, "Deleted chirp with messageId: " + msg.getMessageId());
                    Toast.makeText(
                            getApplication().getApplicationContext(),
                            "Deleted chirp!",
                            Toast.LENGTH_LONG)
                        .show();
                  },
                  error ->
                      ErrorUtils.logAndShow(
                          getApplication(), TAG, error, R.string.error_delete_chirp));
      disposables.add(disposable);
    } catch (KeyException e) {
      ErrorUtils.logAndShow(getApplication(), TAG, e, R.string.error_retrieve_own_token);
    }
  }

  public List<Chirp> getChirpList(String laoId) {
    Lao lao = getLao(laoId);
    if (lao == null) return Collections.emptyList();
    else return lao.getChirpsInOrder();
  }

  /**
   * Check whether the sender of a chirp is the current user
   *
   * @param sender String of the PoPToken PublicKey
   * @return true if the sender is the current user
   */
  public boolean isOwner(String sender) {
    Log.d(TAG, "Testing if the sender is also the owner");
    Lao lao = getCurrentLao();
    if (lao == null) {
      Log.e(TAG, LAO_FAILURE_MESSAGE);
      return false;
    }

    try {
      PoPToken token = keyManager.getValidPoPToken(lao);
      return sender.equals(token.getPublicKey().getEncoded());
    } catch (KeyException e) {
      ErrorUtils.logAndShow(getApplication(), TAG, e, R.string.error_retrieve_own_token);
      return false;
    }
  }

  public void savePersistentData() {
    ActivityUtils.activitySavingRoutine(
        networkManager, wallet, getApplication().getApplicationContext());
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
