package com.github.dedis.popstellar.ui.lao.socialmedia;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.*;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.model.network.method.message.MessageGeneral;
import com.github.dedis.popstellar.model.network.method.message.data.socialmedia.AddChirp;
import com.github.dedis.popstellar.model.network.method.message.data.socialmedia.DeleteChirp;
import com.github.dedis.popstellar.model.objects.Channel;
import com.github.dedis.popstellar.model.objects.Chirp;
import com.github.dedis.popstellar.model.objects.security.MessageID;
import com.github.dedis.popstellar.model.objects.security.PoPToken;
import com.github.dedis.popstellar.model.objects.view.LaoView;
import com.github.dedis.popstellar.repository.*;
import com.github.dedis.popstellar.repository.remote.GlobalNetworkManager;
import com.github.dedis.popstellar.utility.error.ErrorUtils;
import com.github.dedis.popstellar.utility.error.UnknownLaoException;
import com.github.dedis.popstellar.utility.error.keys.KeyException;
import com.github.dedis.popstellar.utility.scheduler.SchedulerProvider;
import com.github.dedis.popstellar.utility.security.KeyManager;
import com.google.gson.Gson;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.disposables.CompositeDisposable;

@HiltViewModel
public class SocialMediaViewModel extends AndroidViewModel {
  private static final Logger logger = LogManager.getLogger(SocialMediaViewModel.class);
  private static final String LAO_FAILURE_MESSAGE = "failed to retrieve lao";
  private static final String SOCIAL = "social";
  public static final Integer MAX_CHAR_NUMBERS = 300;

  private String laoId;

  /*
   * LiveData objects for capturing events
   */
  private final MutableLiveData<Integer> mNumberCharsLeft = new MutableLiveData<>();
  private final MutableLiveData<SocialMediaTab> bottomNavigationTab =
      new MutableLiveData<>(SocialMediaTab.HOME);

  /*
   * Dependencies for this class
   */
  private final LAORepository laoRepo;
  private final RollCallRepository rollCallRepo;
  private final SchedulerProvider schedulerProvider;
  private final SocialMediaRepository socialMediaRepository;
  private final GlobalNetworkManager networkManager;
  private final Gson gson;
  private final KeyManager keyManager;
  private final CompositeDisposable disposables;

  @Inject
  public SocialMediaViewModel(
      @NonNull Application application,
      LAORepository laoRepo,
      RollCallRepository rollCallRepo,
      SchedulerProvider schedulerProvider,
      SocialMediaRepository socialMediaRepository,
      GlobalNetworkManager networkManager,
      Gson gson,
      KeyManager keyManager) {
    super(application);
    this.laoRepo = laoRepo;
    this.rollCallRepo = rollCallRepo;
    this.schedulerProvider = schedulerProvider;
    this.socialMediaRepository = socialMediaRepository;
    this.networkManager = networkManager;
    this.gson = gson;
    this.keyManager = keyManager;

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

  /*
   * Methods that modify the state or post an Event to update the UI.
   */

  public void setNumberCharsLeft(Integer numberChars) {
    mNumberCharsLeft.setValue(numberChars);
  }

  public LiveData<SocialMediaTab> getBottomNavigationTab() {
    return bottomNavigationTab;
  }

  public void setBottomNavigationTab(SocialMediaTab tab) {
    if (tab != bottomNavigationTab.getValue()) {
      bottomNavigationTab.setValue(tab);
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
  public Single<MessageGeneral> sendChirp(
      String text, @Nullable MessageID parentId, long timestamp) {
    logger.debug("Sending a chirp");

    LaoView laoView;
    try {
      laoView = getLao();
    } catch (UnknownLaoException e) {
      logger.error(LAO_FAILURE_MESSAGE);
      return Single.error(new UnknownLaoException());
    }

    AddChirp addChirp = new AddChirp(text, parentId, timestamp);

    return Single.fromCallable(this::getValidPoPToken)
        .doOnSuccess(token -> logger.debug("Retrieved PoPToken to send Chirp : " + token))
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
    logger.debug("Deleting the chirp with id: " + chirpId);

    final LaoView laoView;
    try {
      laoView = getLao();
    } catch (UnknownLaoException e) {
      logger.error(LAO_FAILURE_MESSAGE);
      return Single.error(new UnknownLaoException());
    }

    DeleteChirp deleteChirp = new DeleteChirp(chirpId, timestamp);

    return Single.fromCallable(this::getValidPoPToken)
        .doOnSuccess(token -> logger.debug("Retrieved PoPToken to delete Chirp : " + token))
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
    logger.debug("Testing if the sender is also the owner");

    try {
      PoPToken token = getValidPoPToken();
      return sender.equals(token.getPublicKey().getEncoded());
    } catch (KeyException e) {
      ErrorUtils.logAndShow(getApplication(), logger, e, R.string.error_retrieve_own_token);
      return false;
    }
  }

  public void setLaoId(String laoId) {
    this.laoId = laoId;
  }

  public PoPToken getValidPoPToken() throws KeyException {
    return keyManager.getValidPoPToken(laoId, rollCallRepo.getLastClosedRollCall(laoId));
  }

  private LaoView getLao() throws UnknownLaoException {
    return laoRepo.getLaoView(laoId);
  }
}
