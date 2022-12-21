package com.github.dedis.popstellar.ui.socialmedia;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.model.network.method.message.MessageGeneral;
import com.github.dedis.popstellar.model.network.method.message.data.socialmedia.AddChirp;
import com.github.dedis.popstellar.model.network.method.message.data.socialmedia.DeleteChirp;
import com.github.dedis.popstellar.model.objects.*;
import com.github.dedis.popstellar.model.objects.security.MessageID;
import com.github.dedis.popstellar.model.objects.security.PoPToken;
import com.github.dedis.popstellar.model.objects.view.LaoView;
import com.github.dedis.popstellar.repository.*;
import com.github.dedis.popstellar.repository.remote.GlobalNetworkManager;
import com.github.dedis.popstellar.ui.navigation.NavigationViewModel;
import com.github.dedis.popstellar.utility.ActivityUtils;
import com.github.dedis.popstellar.utility.error.ErrorUtils;
import com.github.dedis.popstellar.utility.error.UnknownLaoException;
import com.github.dedis.popstellar.utility.error.keys.KeyException;
import com.github.dedis.popstellar.utility.scheduler.SchedulerProvider;
import com.github.dedis.popstellar.utility.security.KeyManager;
import com.google.gson.Gson;

import java.security.GeneralSecurityException;
import java.util.*;
import java.util.stream.Collectors;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;

@HiltViewModel
public class SocialMediaViewModel extends NavigationViewModel<SocialMediaTab> {
  public static final String TAG = SocialMediaViewModel.class.getSimpleName();
  private static final String LAO_FAILURE_MESSAGE = "failed to retrieve lao";
  private static final String SOCIAL = "social";
  public static final Integer MAX_CHAR_NUMBERS = 300;
  private String laoId;

  /*
   * LiveData objects for capturing events
   */
  private final MutableLiveData<Integer> mNumberCharsLeft = new MutableLiveData<>();
  private final MutableLiveData<String> mLaoName = new MutableLiveData<>();
  private final MutableLiveData<Integer> mPageTitle = new MutableLiveData<>();

  /*
   * Dependencies for this class
   */
  private final LAORepository laoRepository;
  private final RollCallRepository rollCallRepo;
  private final SchedulerProvider schedulerProvider;
  private final SocialMediaRepository socialMediaRepository;
  private final GlobalNetworkManager networkManager;
  private final Gson gson;
  private final KeyManager keyManager;
  private final CompositeDisposable disposables;
  private final Wallet wallet;

  @Inject
  public SocialMediaViewModel(
      @NonNull Application application,
      LAORepository laoRepository,
      RollCallRepository rollCallRepo,
      SchedulerProvider schedulerProvider,
      SocialMediaRepository socialMediaRepository,
      GlobalNetworkManager networkManager,
      Gson gson,
      KeyManager keyManager,
      Wallet wallet) {
    super(application);
    this.laoRepository = laoRepository;
    this.rollCallRepo = rollCallRepo;
    this.schedulerProvider = schedulerProvider;
    this.socialMediaRepository = socialMediaRepository;
    this.networkManager = networkManager;
    this.gson = gson;
    this.keyManager = keyManager;
    this.wallet = wallet;

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
  public LiveData<Integer> getNumberCharsLeft() {
    return mNumberCharsLeft;
  }

  public LiveData<String> getLaoName() {
    return mLaoName;
  }

  public LiveData<Integer> getPageTitle() {
    return mPageTitle;
  }

  /*
   * Methods that modify the state or post an Event to update the UI.
   */

  public void setNumberCharsLeft(Integer numberChars) {
    mNumberCharsLeft.setValue(numberChars);
  }

  public void setLaoId(String laoId) {
    this.laoId = laoId;
  }

  public void setLaoName(String laoName) {
    mLaoName.setValue(laoName);
  }

  public void setPageTitle(int titleId) {
    mPageTitle.postValue(titleId);
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
  public Single<MessageGeneral> sendChirp(
      String text, @Nullable MessageID parentId, long timestamp) {
    Log.d(TAG, "Sending a chirp");

    LaoView laoView;
    try {
      laoView = getCurrentLaoView();
    } catch (UnknownLaoException e) {
      Log.e(TAG, LAO_FAILURE_MESSAGE);
      return Single.error(new UnknownLaoException());
    }

    AddChirp addChirp = new AddChirp(text, parentId, timestamp);

    return Single.fromCallable(this::getValidPoPToken)
        .doOnSuccess(token -> Log.d(TAG, "Retrieved PoPToken to send Chirp : " + token))
        .flatMap(
            token -> {
              Channel channel =
                  laoView
                      .getChannel()
                      .subChannel(SOCIAL)
                      .subChannel(token.getPublicKey().getEncoded());
              MessageGeneral msg = new MessageGeneral(token, addChirp, gson);

              return networkManager.getMessageSender().publish(channel, msg).toSingleDefault(msg);
            });
  }

  public Single<MessageGeneral> deleteChirp(MessageID chirpId, long timestamp) {
    Log.d(TAG, "Deleting the chirp with id: " + chirpId);

    final LaoView laoView;
    try {
      laoView = getCurrentLaoView();
    } catch (UnknownLaoException e) {
      Log.e(TAG, LAO_FAILURE_MESSAGE);
      return Single.error(new UnknownLaoException());
    }

    DeleteChirp deleteChirp = new DeleteChirp(chirpId, timestamp);

    return Single.fromCallable(this::getValidPoPToken)
        .doOnSuccess(token -> Log.d(TAG, "Retrieved PoPToken to delete Chirp : " + token))
        .flatMap(
            token -> {
              Channel channel =
                  laoView
                      .getChannel()
                      .subChannel(SOCIAL)
                      .subChannel(token.getPublicKey().getEncoded());
              MessageGeneral msg = new MessageGeneral(token, deleteChirp, gson);

              return networkManager.getMessageSender().publish(channel, msg).toSingleDefault(msg);
            });
  }

  public Observable<List<Chirp>> getChirps() {
    return socialMediaRepository
        .getChirpsOfLao(laoId)
        // Retrieve chirp subjects per id
        .map(
            ids -> {
              List<Observable<Chirp>> chirps = new ArrayList<>(ids.size());
              for (MessageID id : ids) {
                chirps.add(socialMediaRepository.getChirp(laoId, id));
              }
              return chirps;
            })
        // Zip the subjects together to a sorted list
        .flatMap(
            observables ->
                Observable.combineLatest(
                    observables,
                    chirps ->
                        Arrays.stream(chirps)
                            .map(Chirp.class::cast)
                            .sorted(
                                Comparator.comparing(
                                    (Chirp chirp) -> chirp != null ? -chirp.getTimestamp() : 0))
                            .collect(Collectors.toList())))
        // We want to observe these changes on the main thread such that any modification done to
        // the view are done on the thread. Otherwise, the app might crash
        .observeOn(schedulerProvider.mainThread());
  }

  /**
   * Check whether the sender of a chirp is the current user
   *
   * @param sender String of the PoPToken PublicKey
   * @return true if the sender is the current user
   */
  public boolean isOwner(String sender) {
    Log.d(TAG, "Testing if the sender is also the owner");

    try {
      PoPToken token = getValidPoPToken();
      return sender.equals(token.getPublicKey().getEncoded());
    } catch (KeyException e) {
      ErrorUtils.logAndShow(getApplication(), TAG, e, R.string.error_retrieve_own_token);
      return false;
    }
  }

  public void savePersistentData() throws GeneralSecurityException {
    ActivityUtils.activitySavingRoutine(
        networkManager, wallet, getApplication().getApplicationContext());
  }

  /**
   * This function should be used to add disposable object generated from subscription to sent
   * messages flows
   *
   * <p>They will be disposed of when the view model is cleaned which ensures that the subscription
   * stays relevant throughout the whole lifecycle of the activity and it is not bound to a fragment
   *
   * @param disposable to add
   */
  public void addDisposable(Disposable disposable) {
    this.disposables.add(disposable);
  }

  public LaoView getLaoView(String laoId) throws UnknownLaoException {
    return laoRepository.getLaoView(laoId);
  }

  public LaoView getCurrentLaoView() throws UnknownLaoException {
    return getLaoView(laoId);
  }

  public String getLaoId() {
    return laoId;
  }

  public PoPToken getValidPoPToken() throws KeyException {
    return keyManager.getValidPoPToken(laoId, rollCallRepo.getLastClosedRollCall(laoId));
  }
}
