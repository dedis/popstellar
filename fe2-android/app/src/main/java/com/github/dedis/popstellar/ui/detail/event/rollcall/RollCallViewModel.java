package com.github.dedis.popstellar.ui.detail.event.rollcall;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.model.network.method.message.data.rollcall.*;
import com.github.dedis.popstellar.model.objects.Channel;
import com.github.dedis.popstellar.model.objects.RollCall;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.model.objects.view.LaoView;
import com.github.dedis.popstellar.repository.LAORepository;
import com.github.dedis.popstellar.repository.RollCallRepository;
import com.github.dedis.popstellar.repository.remote.GlobalNetworkManager;
import com.github.dedis.popstellar.ui.qrcode.ScanningAction;
import com.github.dedis.popstellar.utility.error.*;
import com.github.dedis.popstellar.utility.error.keys.KeyException;
import com.github.dedis.popstellar.utility.scheduler.SchedulerProvider;
import com.github.dedis.popstellar.utility.security.KeyManager;

import java.time.Instant;
import java.util.*;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.Observable;
import io.reactivex.*;

@HiltViewModel
public class RollCallViewModel extends AndroidViewModel {
  public static final String TAG = RollCallViewModel.class.getSimpleName();
  private String laoId;
  private String currentRollCallId = "";

  private final Set<PublicKey> attendees = new HashSet<>();
  private ScanningAction scanningAction;

  private final MutableLiveData<Integer> attendeeCount = new MutableLiveData<>();

  private final LAORepository laoRepo;
  private final RollCallRepository rollCallRepo;
  private final GlobalNetworkManager networkManager;
  private final KeyManager keyManager;
  private final SchedulerProvider schedulerProvider;

  @Inject
  public RollCallViewModel(
      @NonNull Application application,
      LAORepository laoRepo,
      RollCallRepository rollCallRepo,
      GlobalNetworkManager networkManager,
      KeyManager keyManager,
      SchedulerProvider schedulerProvider) {
    super(application);
    this.laoRepo = laoRepo;
    this.rollCallRepo = rollCallRepo;
    this.networkManager = networkManager;
    this.keyManager = keyManager;
    this.schedulerProvider = schedulerProvider;
  }

  public void setLaoId(String laoId) {
    this.laoId = laoId;
  }

  /**
   * Creates new roll call event.
   *
   * <p>Publish a GeneralMessage containing CreateRollCall data.
   *
   * @param title the title of the roll call
   * @param description the description of the roll call, can be empty
   * @param location the location of the roll call
   * @param creation the creation time of the roll call
   * @param proposedStart the proposed start time of the roll call
   * @param proposedEnd the proposed end time of the roll call
   * @return A Single emitting the id of the created rollcall
   */
  public Single<String> createNewRollCall(
      String title,
      String description,
      String location,
      long creation,
      long proposedStart,
      long proposedEnd) {
    Log.d(TAG, "creating a new roll call with title " + title);

    LaoView laoView;
    try {
      laoView = getLao();
    } catch (UnknownLaoException e) {
      ErrorUtils.logAndShow(getApplication(), TAG, e, R.string.unknown_lao_exception);
      return Single.error(new UnknownLaoException());
    }

    CreateRollCall createRollCall =
        new CreateRollCall(
            title, creation, proposedStart, proposedEnd, location, description, laoView.getId());

    return networkManager
        .getMessageSender()
        .publish(keyManager.getMainKeyPair(), laoView.getChannel(), createRollCall)
        .toSingleDefault(createRollCall.getId());
  }

  /**
   * Opens a roll call event.
   *
   * <p>Publish a GeneralMessage containing OpenRollCall data.
   *
   * @param id the roll call id to open
   */
  public Completable openRollCall(String id) {
    Log.d(TAG, "call openRollCall with id" + id);

    LaoView laoView;
    try {
      laoView = getLao();
    } catch (UnknownLaoException e) {
      ErrorUtils.logAndShow(getApplication(), TAG, e, R.string.unknown_lao_exception);
      return Completable.error(new UnknownLaoException());
    }
    long openedAt = Instant.now().getEpochSecond();

    RollCall rollCall;
    try {
      Log.d(TAG, "failed to retrieve roll call with id " + id + "laoID: " + laoView.getId());
      rollCall = rollCallRepo.getRollCallWithId(laoId, id);
    } catch (UnknownRollCallException e) {
      return Completable.error(new UnknownRollCallException(id));
    }

    OpenRollCall openRollCall =
        new OpenRollCall(laoView.getId(), id, openedAt, rollCall.getState());

    Channel channel = laoView.getChannel();
    return networkManager
        .getMessageSender()
        .publish(keyManager.getMainKeyPair(), channel, openRollCall)
        .doOnComplete(() -> openRollCall(openRollCall.getUpdateId(), laoView, rollCall));
  }

  private void openRollCall(String currentId, LaoView laoView, RollCall rollCall) {
    currentRollCallId = currentId;
    Log.d(TAG, "opening rollcall with id " + currentRollCallId);
    scanningAction = ScanningAction.ADD_ROLL_CALL_ATTENDEE;
    attendees.addAll(rollCall.getAttendees());

    try {
      attendees.add(keyManager.getPoPToken(laoView, rollCall).getPublicKey());
    } catch (KeyException e) {
      ErrorUtils.logAndShow(getApplication(), TAG, e, R.string.error_retrieve_own_token);
    }

    // this to display the initial number of attendees
    attendeeCount.postValue(attendees.size());
  }

  /**
   * Closes the roll call event currently open
   *
   * <p>Publish a GeneralMessage containing CloseRollCall data.
   */
  public Completable closeRollCall() {
    Log.d(TAG, "call closeRollCall");

    LaoView laoView;
    try {
      laoView = getLao();
    } catch (UnknownLaoException e) {
      ErrorUtils.logAndShow(getApplication(), TAG, e, R.string.unknown_lao_exception);
      return Completable.error(new UnknownLaoException());
    }

    long end = Instant.now().getEpochSecond();
    Channel channel = laoView.getChannel();
    CloseRollCall closeRollCall =
        new CloseRollCall(laoView.getId(), currentRollCallId, end, new ArrayList<>(attendees));

    return networkManager
        .getMessageSender()
        .publish(keyManager.getMainKeyPair(), channel, closeRollCall)
        .doOnComplete(
            () -> {
              Log.d(TAG, "closed the roll call with id " + currentRollCallId);
              currentRollCallId = "";
              attendees.clear();
            });
  }

  public io.reactivex.Observable<RollCall> getRollCallObservable(String persistentId) {
    try {
      return rollCallRepo
          .getRollCallObservable(laoId, persistentId)
          .observeOn(schedulerProvider.mainThread());
    } catch (UnknownRollCallException e) {
      return Observable.error(new UnknownRollCallException(persistentId));
    }
  }

  public void setCurrentRollCallId(String rollCallId) {
    this.currentRollCallId = rollCallId;
  }

  private LaoView getLao() throws UnknownLaoException {
    return laoRepo.getLaoView(laoId);
  }
}
