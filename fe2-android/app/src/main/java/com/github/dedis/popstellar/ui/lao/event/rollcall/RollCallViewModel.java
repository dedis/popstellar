package com.github.dedis.popstellar.ui.lao.event.rollcall;

import android.app.Application;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.*;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.model.network.method.message.data.rollcall.*;
import com.github.dedis.popstellar.model.objects.*;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.model.objects.view.LaoView;
import com.github.dedis.popstellar.model.qrcode.PopTokenData;
import com.github.dedis.popstellar.repository.LAORepository;
import com.github.dedis.popstellar.repository.RollCallRepository;
import com.github.dedis.popstellar.repository.remote.GlobalNetworkManager;
import com.github.dedis.popstellar.ui.qrcode.QRCodeScanningViewModel;
import com.github.dedis.popstellar.utility.error.*;
import com.github.dedis.popstellar.utility.error.keys.*;
import com.github.dedis.popstellar.utility.scheduler.SchedulerProvider;
import com.github.dedis.popstellar.utility.security.KeyManager;
import com.google.gson.Gson;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.Observable;
import io.reactivex.*;

@HiltViewModel
public class RollCallViewModel extends AndroidViewModel implements QRCodeScanningViewModel {
  public static final String TAG = RollCallViewModel.class.getSimpleName();
  private String laoId;

  private final Set<PublicKey> attendees = new HashSet<>();

  private final MutableLiveData<Integer> nbScanned = new MutableLiveData<>();
  private Observable<List<RollCall>> attendedRollCalls;

  private final LAORepository laoRepo;
  private final RollCallRepository rollCallRepo;
  private final GlobalNetworkManager networkManager;
  private final KeyManager keyManager;
  private final SchedulerProvider schedulerProvider;
  private final Wallet wallet;
  private final Gson gson;

  @Inject
  public RollCallViewModel(
      @NonNull Application application,
      LAORepository laoRepo,
      RollCallRepository rollCallRepo,
      GlobalNetworkManager networkManager,
      KeyManager keyManager,
      Wallet wallet,
      SchedulerProvider schedulerProvider,
      Gson gson) {
    super(application);
    this.laoRepo = laoRepo;
    this.rollCallRepo = rollCallRepo;
    this.networkManager = networkManager;
    this.keyManager = keyManager;
    this.wallet = wallet;
    this.schedulerProvider = schedulerProvider;
    this.gson = gson;
  }

  public void setLaoId(String laoId) {
    this.laoId = laoId;

    this.attendedRollCalls =
        rollCallRepo
            .getRollCallsObservableInLao(laoId)
            .map(
                rcs ->
                    rcs.stream()
                        // Keep only attended roll calls
                        .filter(this::isRollCallAttended)
                        .collect(Collectors.toList()));
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
    Log.d(TAG, "call openRollCall with id " + id);

    LaoView laoView;
    try {
      laoView = getLao();
    } catch (UnknownLaoException e) {
      ErrorUtils.logAndShow(getApplication(), TAG, e, R.string.unknown_lao_exception);
      return Completable.error(new UnknownLaoException());
    }

    if (!rollCallRepo.canOpenRollCall(laoId)) {
      Log.d(
          TAG,
          "failed to open roll call with id "
              + id
              + " because another roll call was already opened, laoID: "
              + laoView.getId());
      return Completable.error(new DoubleOpenedRollCallException(id));
    }

    long openedAt = Instant.now().getEpochSecond();

    RollCall rollCall;
    try {
      Log.d(TAG, "failed to retrieve roll call with id " + id + ", laoID: " + laoView.getId());
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
    Log.d(TAG, "opening rollcall with id " + currentId);
    attendees.addAll(rollCall.getAttendees());

    try {
      attendees.add(keyManager.getPoPToken(laoView, rollCall).getPublicKey());
    } catch (KeyException e) {
      ErrorUtils.logAndShow(getApplication(), TAG, e, R.string.error_retrieve_own_token);
    }

    // this to display the initial number of attendees
    nbScanned.postValue(attendees.size());
  }

  /**
   * Closes the roll call event currently open
   *
   * <p>Publish a GeneralMessage containing CloseRollCall data.
   */
  public Completable closeRollCall(String id) {
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
        new CloseRollCall(laoView.getId(), id, end, new ArrayList<>(attendees));

    return networkManager
        .getMessageSender()
        .publish(keyManager.getMainKeyPair(), channel, closeRollCall)
        .doOnComplete(
            () -> {
              Log.d(TAG, "closed the roll call with id " + id);
              attendees.clear();
            });
  }

  public Observable<RollCall> getRollCallObservable(String persistentId) {
    try {
      return rollCallRepo
          .getRollCallObservable(laoId, persistentId)
          .observeOn(schedulerProvider.mainThread());
    } catch (UnknownRollCallException e) {
      return Observable.error(new UnknownRollCallException(persistentId));
    }
  }

  public Observable<List<RollCall>> getAttendedRollCalls() {
    return attendedRollCalls;
  }

  private LaoView getLao() throws UnknownLaoException {
    return laoRepo.getLaoView(laoId);
  }

  /**
   * Predicate used for filtering rollcalls to make sure that the user either attended the rollcall
   * or was the organizer
   *
   * @param rollcall the roll-call considered
   * @return boolean saying whether user attended or organized the given roll call
   */
  private boolean isRollCallAttended(RollCall rollcall) {
    // find out if user has attended the rollcall
    try {
      boolean isOrganizer =
          laoRepo.getLaoView(laoId).getOrganizer().equals(keyManager.getMainPublicKey());
      PublicKey pk = wallet.generatePoPToken(laoId, rollcall.getPersistentId()).getPublicKey();

      return rollcall.getAttendees().contains(pk) || isOrganizer;
    } catch (KeyGenerationException | UninitializedWalletException e) {
      ErrorUtils.logAndShow(getApplication(), TAG, e, R.string.key_generation_exception);
      return false;
    } catch (UnknownLaoException e) {
      ErrorUtils.logAndShow(getApplication(), TAG, e, R.string.unknown_lao_exception);
      return false;
    }
  }

  @Override
  public void handleData(String data) {
    PopTokenData tokenData;
    try {
      tokenData = PopTokenData.extractFrom(gson, data);
    } catch (Exception e) {
      ErrorUtils.logAndShow(
          getApplication().getApplicationContext(), TAG, R.string.qr_code_not_pop_token);
      return;
    }
    PublicKey publicKey = tokenData.getPopToken();
    if (attendees.contains(publicKey)) {
      ErrorUtils.logAndShow(getApplication(), TAG, R.string.attendee_already_scanned_warning);
      return;
    }

    attendees.add(publicKey);
    Log.d(TAG, "Attendee " + publicKey + " successfully added");
    Toast.makeText(getApplication(), R.string.attendee_scan_success, Toast.LENGTH_SHORT).show();
    nbScanned.postValue(attendees.size());
  }

  @Override
  public LiveData<Integer> getNbScanned() {
    return nbScanned;
  }

  public Set<PublicKey> getAttendees() {
    return attendees;
  }
}
