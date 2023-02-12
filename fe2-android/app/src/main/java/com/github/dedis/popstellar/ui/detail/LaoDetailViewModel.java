package com.github.dedis.popstellar.ui.detail;

import android.app.Application;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.SingleEvent;
import com.github.dedis.popstellar.model.network.method.message.MessageGeneral;
import com.github.dedis.popstellar.model.network.method.message.data.consensus.ConsensusElect;
import com.github.dedis.popstellar.model.network.method.message.data.consensus.ConsensusElectAccept;
import com.github.dedis.popstellar.model.network.method.message.data.election.*;
import com.github.dedis.popstellar.model.network.method.message.data.lao.StateLao;
import com.github.dedis.popstellar.model.network.method.message.data.lao.UpdateLao;
import com.github.dedis.popstellar.model.network.method.message.data.message.WitnessMessageSignature;
import com.github.dedis.popstellar.model.network.method.message.data.rollcall.*;
import com.github.dedis.popstellar.model.objects.*;
import com.github.dedis.popstellar.model.objects.event.Event;
import com.github.dedis.popstellar.model.objects.security.*;
import com.github.dedis.popstellar.model.objects.view.LaoView;
import com.github.dedis.popstellar.model.qrcode.MainPublicKeyData;
import com.github.dedis.popstellar.model.qrcode.PopTokenData;
import com.github.dedis.popstellar.repository.*;
import com.github.dedis.popstellar.repository.remote.GlobalNetworkManager;
import com.github.dedis.popstellar.ui.navigation.LaoViewModel;
import com.github.dedis.popstellar.ui.qrcode.QRCodeScanningViewModel;
import com.github.dedis.popstellar.ui.qrcode.ScanningAction;
import com.github.dedis.popstellar.utility.ActivityUtils;
import com.github.dedis.popstellar.utility.error.*;
import com.github.dedis.popstellar.utility.error.keys.*;
import com.github.dedis.popstellar.utility.scheduler.SchedulerProvider;
import com.github.dedis.popstellar.utility.security.KeyManager;
import com.google.gson.Gson;

import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.Observable;
import io.reactivex.*;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

@HiltViewModel
public class LaoDetailViewModel extends LaoViewModel implements QRCodeScanningViewModel {

  public static final String TAG = LaoDetailViewModel.class.getSimpleName();
  private static final String LAO_FAILURE_MESSAGE = "failed to retrieve current lao";
  /*
   * LiveData objects for capturing events like button clicks
   */
  // FIXME These events should be removed once the QRScanning is refactored
  private final MutableLiveData<SingleEvent<String>> mAttendeeScanConfirmEvent =
      new MutableLiveData<>();
  private final MutableLiveData<SingleEvent<Boolean>> mWitnessScanConfirmEvent =
      new MutableLiveData<>();
  private final MutableLiveData<SingleEvent<String>> mScanWarningEvent = new MutableLiveData<>();

  /*
   * LiveData objects that represent the state in a fragment
   */
  private final MutableLiveData<Boolean> mIsSignedByCurrentWitness = new MutableLiveData<>();
  private final MutableLiveData<Integer> nbScanned = new MutableLiveData<>();

  private Observable<Set<Event>> events;
  private Observable<List<RollCall>> attendedRollCalls;

  /*
   * Dependencies for this class
   */
  private final LAORepository laoRepository;
  private final RollCallRepository rollCallRepo;
  private final ElectionRepository electionRepo;
  private final SchedulerProvider schedulerProvider;
  private final GlobalNetworkManager networkManager;
  private final KeyManager keyManager;
  private final CompositeDisposable disposables;
  private final Gson gson;
  private final Wallet wallet;

  private String laoId;
  // used to know which roll call to close
  private final Set<PublicKey> attendees = new HashSet<>();
  // used to dynamically update the set of witnesses when WR code scanned
  private final Set<PublicKey> witnesses = new HashSet<>();

  @Inject
  public LaoDetailViewModel(
      @NonNull Application application,
      LAORepository laoRepository,
      RollCallRepository rollCallRepo,
      ElectionRepository electionRepo,
      SchedulerProvider schedulerProvider,
      GlobalNetworkManager networkManager,
      KeyManager keyManager,
      Gson gson,
      Wallet wallet) {
    super(application);
    this.laoRepository = laoRepository;
    this.rollCallRepo = rollCallRepo;
    this.electionRepo = electionRepo;
    this.schedulerProvider = schedulerProvider;
    this.networkManager = networkManager;
    this.keyManager = keyManager;
    this.gson = gson;
    this.wallet = wallet;
    disposables = new CompositeDisposable();
  }

  /**
   * Predicate used for filtering rollcalls to make sure that the user either attended the rollcall
   * or was the organizer
   *
   * @param rollcall the roll-call considered
   * @return boolean saying whether user attended or organized the given roll call
   */
  private boolean attendedOrOrganized(RollCall rollcall) {
    // find out if user has attended the rollcall
    try {
      PublicKey pk = wallet.generatePoPToken(laoId, rollcall.getPersistentId()).getPublicKey();
      return rollcall.getAttendees().contains(pk) || isOrganizer();
    } catch (KeyGenerationException | UninitializedWalletException e) {
      Log.e(TAG, "failed to retrieve public key from wallet", e);
      return false;
    }
  }

  /**
   * Returns the public key or null if an error occurred.
   *
   * @return the public key
   */
  public PublicKey getPublicKey() {
    return keyManager.getMainPublicKey();
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

  public RollCall getRollCall(String persistentId) throws UnknownRollCallException {
    return rollCallRepo.getRollCallWithPersistentId(laoId, persistentId);
  }

  @Override
  protected void onCleared() {
    super.onCleared();
    disposables.dispose();
  }

  /**
   * Opens the election and publish opening message triggers OpenElection event on success or logs
   * appropriate error
   *
   * @param e election to be opened
   */
  public Completable openElection(Election e) {
    Log.d(TAG, "opening election with name : " + e.getName());
    LaoView laoView;
    try {
      laoView = getLao();
    } catch (UnknownLaoException unknownLaoException) {
      Log.d(TAG, LAO_FAILURE_MESSAGE);
      return Completable.error(new UnknownLaoException());
    }

    Channel channel = e.getChannel();
    String laoViewId = laoView.getId();

    // The time will have to be modified on the backend
    OpenElection openElection = new OpenElection(laoViewId, e.getId(), e.getStartTimestamp());

    return networkManager
        .getMessageSender()
        .publish(keyManager.getMainKeyPair(), channel, openElection);
  }

  public Completable endElection(Election election) {
    Log.d(TAG, "ending election with name : " + election.getName());
    LaoView laoView;
    try {
      laoView = getLao();
    } catch (UnknownLaoException e) {
      Log.d(TAG, LAO_FAILURE_MESSAGE);
      return Completable.error(new UnknownLaoException());
    }

    Channel channel = election.getChannel();
    String laoViewId = laoView.getId();
    ElectionEnd electionEnd =
        new ElectionEnd(election.getId(), laoViewId, election.computeRegisteredVotesHash());

    return networkManager
        .getMessageSender()
        .publish(keyManager.getMainKeyPair(), channel, electionEnd);
  }

  /**
   * Sends a ElectionCastVotes message .
   *
   * <p>Publish a GeneralMessage containing ElectionCastVotes data.
   *
   * @param votes the corresponding votes for that election
   */
  public Completable sendVote(String electionId, List<PlainVote> votes) {
    Election election;
    try {
      election = electionRepo.getElection(laoId, electionId);
    } catch (UnknownElectionException e) {
      Log.d(TAG, "failed to retrieve current election");
      return Completable.error(e);
    }

    Log.d(
        TAG,
        "sending a new vote in election : "
            + election
            + " with election start time"
            + election.getStartTimestamp());

    final LaoView laoView;
    try {
      laoView = getLao();
    } catch (UnknownLaoException e) {
      Log.d(TAG, LAO_FAILURE_MESSAGE);
      return Completable.error(new UnknownLaoException());
    }

    return Single.fromCallable(
            () -> keyManager.getValidPoPToken(laoId, rollCallRepo.getLastClosedRollCall(laoId)))
        .doOnSuccess(token -> Log.d(TAG, "Retrieved PoP Token to send votes : " + token))
        .flatMapCompletable(
            token -> {
              CastVote vote = createCastVote(votes, election, laoView);

              Channel electionChannel = election.getChannel();
              return networkManager.getMessageSender().publish(token, electionChannel, vote);
            });
  }

  @NonNull
  private CastVote createCastVote(List<PlainVote> votes, Election election, LaoView laoView) {
    if (election.getElectionVersion() == ElectionVersion.OPEN_BALLOT) {
      return new CastVote(votes, election.getId(), laoView.getId());
    } else {
      List<EncryptedVote> encryptedVotes = election.encrypt(votes);

      Toast.makeText(getApplication(), "Vote encrypted !", Toast.LENGTH_LONG).show();
      return new CastVote(encryptedVotes, election.getId(), laoView.getId());
    }
  }

  /**
   * Creates new Election event.
   *
   * <p>Publish a GeneralMessage containing ElectionSetup data.
   *
   * @param electionVersion the version of the election
   * @param name the name of the election
   * @param creation the creation time of the election
   * @param start the start time of the election
   * @param end the end time of the election
   * @param questions questions of the election
   */
  public Completable createNewElection(
      ElectionVersion electionVersion,
      String name,
      long creation,
      long start,
      long end,
      List<ElectionQuestion.Question> questions) {
    Log.d(TAG, "creating a new election with name " + name);

    LaoView laoView;
    try {
      laoView = getLao();
    } catch (UnknownLaoException e) {
      Log.d(TAG, LAO_FAILURE_MESSAGE);
      return Completable.error(new UnknownLaoException());
    }

    Channel channel = laoView.getChannel();
    ElectionSetup electionSetup =
        new ElectionSetup(name, creation, start, end, laoView.getId(), electionVersion, questions);

    return networkManager
        .getMessageSender()
        .publish(keyManager.getMainKeyPair(), channel, electionSetup);
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
      Log.d(TAG, LAO_FAILURE_MESSAGE);
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
   * Sends a ConsensusElect message.
   *
   * <p>Publish a GeneralMessage containing ConsensusElect data.
   *
   * @param creation the creation time of the consensus
   * @param objId the id of the object the consensus refers to (e.g. election_id)
   * @param type the type of object the consensus refers to (e.g. election)
   * @param property the property the value refers to (e.g. "state")
   * @param value the proposed new value for the property (e.g. "started")
   * @return A single emitting the published message
   */
  public Single<MessageGeneral> sendConsensusElect(
      long creation, String objId, String type, String property, Object value) {
    Log.d(
        TAG,
        String.format(
            "creating a new consensus for type: %s, property: %s, value: %s",
            type, property, value));

    LaoView laoView;
    try {
      laoView = getLao();
    } catch (UnknownLaoException e) {
      Log.d(TAG, LAO_FAILURE_MESSAGE);
      return Single.error(new UnknownLaoException());
    }

    Channel channel = laoView.getChannel().subChannel("consensus");
    ConsensusElect consensusElect = new ConsensusElect(creation, objId, type, property, value);

    MessageGeneral msg = new MessageGeneral(keyManager.getMainKeyPair(), consensusElect, gson);

    return networkManager.getMessageSender().publish(channel, msg).toSingleDefault(msg);
  }

  /**
   * Sends a ConsensusElectAccept message.
   *
   * <p>Publish a GeneralMessage containing ConsensusElectAccept data.
   *
   * @param electInstance the corresponding ElectInstance
   * @param accept true if accepted, false if rejected
   */
  public Completable sendConsensusElectAccept(ElectInstance electInstance, boolean accept) {
    MessageID messageId = electInstance.getMessageId();
    Log.d(
        TAG,
        "sending a new elect_accept for consensus with messageId : "
            + messageId
            + " with value "
            + accept);

    ConsensusElectAccept consensusElectAccept =
        new ConsensusElectAccept(electInstance.getInstanceId(), messageId, accept);

    return networkManager
        .getMessageSender()
        .publish(keyManager.getMainKeyPair(), electInstance.getChannel(), consensusElectAccept);
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
      Log.d(TAG, LAO_FAILURE_MESSAGE);
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
        .doOnComplete(() -> openRollCall(laoView, rollCall));
  }

  private void openRollCall(LaoView laoView, RollCall rollCall) {
    Log.d(TAG, "opening roll call with id " + rollCall.getId());

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
   * Closes the roll call with provided id
   *
   * <p>Publish a GeneralMessage containing CloseRollCall data.
   *
   * @param id the mutable id of the roll call
   * @return a completable which succeeds if the close rc message was successfully received and
   * acknowledged  by the backend
   */
  public Completable closeRollCall(String id) {
    Log.d(TAG, "call closeRollCall");

    LaoView laoView;
    try {
      laoView = getLao();
    } catch (UnknownLaoException e) {
      Log.d(TAG, LAO_FAILURE_MESSAGE);
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

  public Completable signMessage(WitnessMessage witnessMessage) {
    Log.d(TAG, "signing message with ID " + witnessMessage.getMessageId());
    final LaoView laoView;
    try {
      laoView = getLao();
    } catch (UnknownLaoException e) {
      Log.d(TAG, LAO_FAILURE_MESSAGE);
      return Completable.error(new UnknownLaoException());
    }

    return Single.fromCallable(keyManager::getMainKeyPair)
        .flatMapCompletable(
            keyPair -> {
              // Generate the signature of the message
              Signature signature = keyPair.sign(witnessMessage.getMessageId());

              Log.d(TAG, "Signed message id, resulting signature : " + signature);
              WitnessMessageSignature signatureMessage =
                  new WitnessMessageSignature(witnessMessage.getMessageId(), signature);

              return networkManager
                  .getMessageSender()
                  .publish(keyManager.getMainKeyPair(), laoView.getChannel(), signatureMessage);
            });
  }

  /** Getters for MutableLiveData instances declared above */
  public Observable<Set<Event>> getEvents() {
    return events;
  }

  @Override
  public LaoView getLao() throws UnknownLaoException {
    return laoRepository.getLaoView(laoId);
  }

  public LiveData<Boolean> isSignedByCurrentWitness(Set<PublicKey> witnesses) {
    boolean isSignedByCurrentWitness = witnesses.contains(keyManager.getMainPublicKey());
    Log.d(TAG, "isSignedByCurrentWitness: " + isSignedByCurrentWitness);
    mIsSignedByCurrentWitness.setValue(isSignedByCurrentWitness);
    return mIsSignedByCurrentWitness;
  }

  @Override
  public LiveData<Integer> getNbScanned() {
    return nbScanned;
  }

  @Override
  public void setScannerTitle(int title) {
    super.setPageTitle(title);
  }

  public Observable<List<ConsensusNode>> getNodes() throws UnknownLaoException {
    return laoRepository.getNodesByChannel(getLao().getChannel());
  }

  public LiveData<SingleEvent<String>> getAttendeeScanConfirmEvent() {
    return mAttendeeScanConfirmEvent;
  }

  public LiveData<SingleEvent<Boolean>> getWitnessScanConfirmEvent() {
    return mWitnessScanConfirmEvent;
  }

  public LiveData<SingleEvent<String>> getScanWarningEvent() {
    return mScanWarningEvent;
  }

  public RollCall getLastClosedRollCall() throws NoRollCallException {
    return rollCallRepo.getLastClosedRollCall(laoId);
  }

  /**
   * Method to update the list of witnesses of a Lao by sending an updateLao msg and a stateLao msg
   * to the backend
   */
  public Completable updateLaoWitnesses() {
    Log.d(TAG, "Updating lao witnesses ");

    LaoView laoView;
    try {
      laoView = getLao();
    } catch (UnknownLaoException e) {
      Log.d(TAG, LAO_FAILURE_MESSAGE);
      return Completable.error(new UnknownLaoException());
    }

    Channel channel = laoView.getChannel();
    KeyPair mainKey = keyManager.getMainKeyPair();
    long now = Instant.now().getEpochSecond();
    UpdateLao updateLao =
        new UpdateLao(
            mainKey.getPublicKey(), laoView.getCreation(), laoView.getName(), now, witnesses);
    MessageGeneral msg = new MessageGeneral(mainKey, updateLao, gson);

    return networkManager
        .getMessageSender()
        .publish(channel, msg)
        .doOnComplete(() -> Log.d(TAG, "updated lao witnesses"))
        .andThen(dispatchLaoUpdate(updateLao, laoView, channel, msg));
  }

  public Observable<List<RollCall>> getAttendedRollCalls() {
    return attendedRollCalls;
  }

  /** Helper method for updateLaoWitnesses and updateLaoName to send a stateLao message */
  private Completable dispatchLaoUpdate(
      UpdateLao updateLao, LaoView laoView, Channel channel, MessageGeneral msg) {
    StateLao stateLao =
        new StateLao(
            updateLao.getId(),
            updateLao.getName(),
            laoView.getCreation(),
            updateLao.getLastModified(),
            laoView.getOrganizer(),
            msg.getMessageId(),
            updateLao.getWitnesses(),
            new ArrayList<>());

    return networkManager
        .getMessageSender()
        .publish(keyManager.getMainKeyPair(), channel, stateLao)
        .doOnComplete(() -> Log.d(TAG, "updated lao with " + stateLao));
  }

  public void subscribeToLao(String laoId) {
    this.laoId = laoId;

    // For some reason, trying to use the same observable twice breaks the event list,
    // even while sharing it.
    //
    // Thus, we need to create the rollcall event list twice ¯\_(ツ)_/¯
    this.attendedRollCalls =
        rollCallRepo
            .getRollCallsObservableInLao(laoId)
            .map(
                rcs ->
                    rcs.stream()
                        // Keep only attended roll calls
                        .filter(this::attendedOrOrganized)
                        .collect(Collectors.toList()));

    // Create the events set observable
    this.events =
        Observable.combineLatest(
                rollCallRepo.getRollCallsObservableInLao(laoId),
                electionRepo.getElectionsObservable(laoId),
                (rcs, elecs) -> {
                  Set<Event> union = new HashSet<>(rcs);
                  union.addAll(elecs);
                  return union;
                })
            // Only dispatch the latest element once every 50 milliseconds
            // This avoids multiple updates in a short period of time
            .throttleLatest(50, TimeUnit.MILLISECONDS)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread());
  }

  public PoPToken getCurrentPopToken(RollCall rollCall) throws KeyException, UnknownLaoException {
    return keyManager.getPoPToken(getLao(), rollCall);
  }

  public boolean isWalletSetup() {
    return wallet.isSetUp();
  }

  public void savePersistentData() throws GeneralSecurityException {
    ActivityUtils.activitySavingRoutine(
        networkManager, wallet, getApplication().getApplicationContext());
  }

  /**
   * Checks the key validity and handles the attendee addition process
   *
   * @param data the textual representation of the key
   */
  @Override
  public void handleData(String data, ScanningAction scanningAction) {
    Log.d(TAG, "data input " + data);
    if (scanningAction == ScanningAction.ADD_ROLL_CALL_ATTENDEE) {
      handleRollCallAddition(data);
    } else if (scanningAction == ScanningAction.ADD_WITNESS) {
      handleWitnessAddition(data);
    } else {
      throw new IllegalStateException(
          "The scanning action should either be to add witnesses or rc attendees");
    }
  }

  public void handleRollCallAddition(String data) {
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
    String successMessage = "Attendee " + publicKey + " successfully added";
    Log.d(TAG, successMessage);
    Toast.makeText(getApplication(), successMessage, Toast.LENGTH_SHORT).show();
    nbScanned.postValue(attendees.size());
  }

  public void handleWitnessAddition(String data) {
    MainPublicKeyData pkData;
    try {
      pkData = MainPublicKeyData.extractFrom(gson, data);
    } catch (Exception e) {
      ErrorUtils.logAndShow(
          getApplication().getApplicationContext(), TAG, e, R.string.qr_code_not_main_pk);
      return;
    }
    PublicKey publicKey = pkData.getPublicKey();
    if (witnesses.contains(publicKey)) {
      ErrorUtils.logAndShow(getApplication(), TAG, R.string.witness_already_scanned_warning);
      return;
    }

    witnesses.add(publicKey);
    String successMessage = "Witness " + publicKey + " successfully scanned";
    Log.d(TAG, successMessage);
    Toast.makeText(getApplication(), successMessage, Toast.LENGTH_SHORT).show();
    disposables.add(
        updateLaoWitnesses()
            .subscribe(
                () -> {
                  String networkSuccess = "Witness " + publicKey + " successfully added to LAO";
                  Log.d(TAG, networkSuccess);
                  Toast.makeText(getApplication(), networkSuccess, Toast.LENGTH_SHORT).show();
                },
                error ->
                    ErrorUtils.logAndShow(
                        getApplication(), TAG, error, R.string.error_update_lao)));
  }
}
