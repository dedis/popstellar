package com.github.dedis.popstellar.ui.detail;

import android.app.Application;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.*;

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
import com.github.dedis.popstellar.model.objects.event.EventState;
import com.github.dedis.popstellar.model.objects.security.*;
import com.github.dedis.popstellar.model.objects.view.LaoView;
import com.github.dedis.popstellar.model.qrcode.MainPublicKeyData;
import com.github.dedis.popstellar.model.qrcode.PopTokenData;
import com.github.dedis.popstellar.repository.LAORepository;
import com.github.dedis.popstellar.repository.remote.GlobalNetworkManager;
import com.github.dedis.popstellar.ui.navigation.NavigationViewModel;
import com.github.dedis.popstellar.ui.qrcode.QRCodeScanningViewModel;
import com.github.dedis.popstellar.ui.qrcode.ScanningAction;
import com.github.dedis.popstellar.utility.ActivityUtils;
import com.github.dedis.popstellar.utility.error.*;
import com.github.dedis.popstellar.utility.error.keys.*;
import com.github.dedis.popstellar.utility.security.KeyManager;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.gson.Gson;

import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.*;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

@HiltViewModel
public class LaoDetailViewModel extends NavigationViewModel<LaoTab>
    implements QRCodeScanningViewModel {

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
  private final MutableLiveData<LaoView> mCurrentLao = new MutableLiveData<>();
  private final MutableLiveData<Boolean> mIsOrganizer = new MutableLiveData<>();
  private final MutableLiveData<Boolean> mIsWitness = new MutableLiveData<>();
  private final MutableLiveData<Boolean> mIsSignedByCurrentWitness = new MutableLiveData<>();
  private final MutableLiveData<Integer> mNbAttendees = new MutableLiveData<>();
  private final MutableLiveData<Boolean> showProperties = new MutableLiveData<>(false);
  private final MutableLiveData<List<Integer>> mCurrentElectionVotes = new MutableLiveData<>();
  private final LiveData<List<PublicKey>> mWitnesses =
      Transformations.map(
          mCurrentLao,
          lao -> lao == null ? new ArrayList<>() : new ArrayList<>(lao.getWitnesses()));
  private final LiveData<String> mCurrentLaoName =
      Transformations.map(mCurrentLao, lao -> lao == null ? "" : lao.getName());
  //  Multiple events from Lao may be concatenated using Stream.concat()
  private final LiveData<List<com.github.dedis.popstellar.model.objects.event.Event>> mLaoEvents =
      Transformations.map(
          mCurrentLao,
          laoView ->
              laoView == null
                  ? new ArrayList<>()
                  : Stream.concat(
                          laoView.getRollCalls().values().stream(),
                          laoView.getElections().values().stream())
                      .collect(Collectors.toList()));
  private final LiveData<List<WitnessMessage>> mWitnessMessages =
      Transformations.map(
          mCurrentLao,
          lao ->
              lao == null ? new ArrayList<>() : new ArrayList<>(lao.getWitnessMessages().values()));

  private final LiveData<List<RollCall>> mLaoAttendedRollCalls =
      Transformations.map(
          mCurrentLao,
          lao ->
              lao == null
                  ? new ArrayList<>()
                  : lao.getRollCalls().values().stream()
                      .filter(rollCall -> rollCall.getState() == EventState.CLOSED)
                      .filter(rollCall -> attendedOrOrganized(lao, rollCall))
                      .collect(Collectors.toList()));
  /*
   * Dependencies for this class
   */
  private final LAORepository laoRepository;
  private final GlobalNetworkManager networkManager;
  private final KeyManager keyManager;
  private final CompositeDisposable disposables;
  private final Gson gson;
  private final Wallet wallet;

  private Election currentElection = null;
  private RollCall currentRollCall = null;
  private String currentRollCallId = "";
  private String laoId;
  // used to know which roll call to close
  private final Set<PublicKey> attendees = new HashSet<>();
  // used to dynamically update the set of witnesses when WR code scanned
  private final Set<PublicKey> witnesses = new HashSet<>();
  private ScanningAction scanningAction;

  @Inject
  public LaoDetailViewModel(
      @NonNull Application application,
      LAORepository laoRepository,
      GlobalNetworkManager networkManager,
      KeyManager keyManager,
      Gson gson,
      Wallet wallet) {
    super(application);
    this.laoRepository = laoRepository;
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
   * @param laoView the lao considered
   * @param rollcall the roll-call considered
   * @return boolean saying whether user attended or organized the given roll call
   */
  private boolean attendedOrOrganized(LaoView laoView, RollCall rollcall) {
    // find out if user has attended the rollcall
    String firstLaoId = laoView.getId();
    try {
      PublicKey pk = wallet.generatePoPToken(firstLaoId, rollcall.getPersistentId()).getPublicKey();
      return rollcall.getAttendees().contains(pk) || isOrganizer().getValue();
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
      laoView = getLaoView();
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
      laoView = getLaoView();
    } catch (UnknownLaoException e) {
      Log.d(TAG, LAO_FAILURE_MESSAGE);
      return Completable.error(new UnknownLaoException());
    }

    Channel channel = election.getChannel();
    String laoViewId = laoView.getId();
    ElectionEnd electionEnd =
        new ElectionEnd(election.getId(), laoViewId, election.computerRegisteredVotes());

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
  public Completable sendVote(List<ElectionVote> votes) {
    Election election = currentElection;

    if (election == null) {
      Log.d(TAG, "failed to retrieve current election");
      return Completable.error(new UnknownElectionException());
    }

    Log.d(
        TAG,
        "sending a new vote in election : "
            + election
            + " with election start time"
            + election.getStartTimestamp());

    final LaoView laoView;
    try {
      laoView = getLaoView();
    } catch (UnknownLaoException e) {
      Log.d(TAG, LAO_FAILURE_MESSAGE);
      return Completable.error(new UnknownLaoException());
    }

    return Single.fromCallable(() -> keyManager.getValidPoPToken(laoView))
        .doOnSuccess(token -> Log.d(TAG, "Retrieved PoP Token to send votes : " + token))
        .flatMapCompletable(
            token -> {
              CastVote<?> vote = createCastVote(votes, election, laoView);

              Channel electionChannel = election.getChannel();
              return networkManager.getMessageSender().publish(token, electionChannel, vote);
            });
  }

  @NonNull
  private CastVote<?> createCastVote(List<ElectionVote> votes, Election election, LaoView laoView) {
    CastVote<?> vote;
    if (election.getElectionVersion() == ElectionVersion.OPEN_BALLOT) {
      vote = new CastVote<>(votes, election.getId(), laoView.getId());
    } else {
      List<ElectionEncryptedVote> encryptedVotes = election.encrypt(votes);
      vote = new CastVote<>(encryptedVotes, election.getId(), laoView.getId());
      Toast.makeText(getApplication(), "Vote encrypted !", Toast.LENGTH_LONG).show();
    }
    return vote;
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
   * @param votingMethod the type of voting method (e.g Plurality)
   * @param ballotOptions the list of ballot options
   * @param question the question associated to the election
   */
  public Completable createNewElection(
      ElectionVersion electionVersion,
      String name,
      long creation,
      long start,
      long end,
      List<String> votingMethod,
      List<Boolean> writeIn,
      List<List<String>> ballotOptions,
      List<String> question) {
    Log.d(TAG, "creating a new election with name " + name);

    LaoView laoView;
    try {
      laoView = getLaoView();
    } catch (UnknownLaoException e) {
      Log.d(TAG, LAO_FAILURE_MESSAGE);
      return Completable.error(new UnknownLaoException());
    }

    Channel channel = laoView.getChannel();
    ElectionSetup electionSetup =
        new ElectionSetup(
            writeIn,
            name,
            creation,
            start,
            end,
            votingMethod,
            laoView.getId(),
            ballotOptions,
            question,
            electionVersion);

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
   * @param creation the creation time of the roll call
   * @param proposedStart the proposed start time of the roll call
   * @param proposedEnd the proposed end time of the roll call
   * @return A Single emitting the id of the created rollcall
   */
  public Single<String> createNewRollCall(
      String title, String description, long creation, long proposedStart, long proposedEnd) {
    Log.d(TAG, "creating a new roll call with title " + title);

    LaoView laoView;
    try {
      laoView = getLaoView();
    } catch (UnknownLaoException e) {
      Log.d(TAG, LAO_FAILURE_MESSAGE);
      return Single.error(new UnknownLaoException());
    }

    // FIXME Location : Lausanne ?
    CreateRollCall createRollCall =
        new CreateRollCall(
            title, creation, proposedStart, proposedEnd, "Lausanne", description, laoView.getId());

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
      laoView = getLaoView();
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
      laoView = getLaoView();
    } catch (UnknownLaoException e) {
      Log.d(TAG, LAO_FAILURE_MESSAGE);
      return Completable.error(new UnknownLaoException());
    }

    long openedAt = Instant.now().getEpochSecond();
    Optional<RollCall> optRollCall = laoView.getRollCall(id);
    if (!optRollCall.isPresent()) {
      Log.d(TAG, "failed to retrieve roll call with id " + id + "laoID: " + laoView.getId());
      return Completable.error(new NoRollCallException(laoView));
    }

    RollCall rollCall = optRollCall.get();
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
    mNbAttendees.postValue(attendees.size());
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
      laoView = getLaoView();
    } catch (UnknownLaoException e) {
      Log.d(TAG, LAO_FAILURE_MESSAGE);
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

  public Completable signMessage(WitnessMessage witnessMessage) {
    Log.d(TAG, "signing message with ID " + witnessMessage.getMessageId());
    final LaoView laoView;
    try {
      laoView = getLaoView();
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
  public ScanningAction getScanningAction() {
    return scanningAction;
  }

  public void setScanningAction(ScanningAction scanningAction) {
    this.scanningAction = scanningAction;
  }

  public LiveData<List<com.github.dedis.popstellar.model.objects.event.Event>> getLaoEvents() {
    return mLaoEvents;
  }

  public LiveData<List<RollCall>> getLaoAttendedRollCalls() {
    return mLaoAttendedRollCalls;
  }

  public LiveData<LaoView> getCurrentLao() {
    return mCurrentLao;
  }

  public String getLaoId() {
    return laoId;
  }

  public LaoView getLaoView() throws UnknownLaoException {
    return laoRepository.getLaoView(laoId);
  }

  @VisibleForTesting
  public void setCurrentLao(LaoView laoView) {
    laoId = laoView.getId();
    mCurrentLao.postValue(laoView);
  }

  public LiveData<String> getCurrentLaoName() {
    return mCurrentLaoName;
  }

  public LiveData<Boolean> isOrganizer() {
    return mIsOrganizer;
  }

  public LiveData<Boolean> isWitness() throws UnknownLaoException {
    boolean isWitness = getLaoView().getWitnesses().contains(keyManager.getMainPublicKey());
    Log.d(TAG, "isWitness: " + isWitness);
    mIsWitness.setValue(isWitness);
    return mIsWitness;
  }

  public LiveData<Boolean> isSignedByCurrentWitness(Set<PublicKey> witnesses) {
    boolean isSignedByCurrentWitness = witnesses.contains(keyManager.getMainPublicKey());
    Log.d(TAG, "isSignedByCurrentWitness: " + isSignedByCurrentWitness);
    mIsSignedByCurrentWitness.setValue(isSignedByCurrentWitness);
    return mIsSignedByCurrentWitness;
  }

  public LiveData<Boolean> getShowProperties() {
    return showProperties;
  }

  public LiveData<List<PublicKey>> getWitnesses() {
    return mWitnesses;
  }

  public LiveData<List<WitnessMessage>> getWitnessMessages() {
    return mWitnessMessages;
  }

  public LiveData<Integer> getNbAttendees() {
    return mNbAttendees;
  }

  public LiveData<List<ConsensusNode>> getNodes() throws UnknownLaoException {
    return LiveDataReactiveStreams.fromPublisher(
        laoRepository
            .getNodesByChannel(getLaoView().getChannel())
            .toFlowable(BackpressureStrategy.LATEST));
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

  public Election getCurrentElection() {
    return currentElection;
  }

  public void setCurrentElection(Election e) {
    currentElection = e;
  }

  public MutableLiveData<List<Integer>> getCurrentElectionVotes() {
    return mCurrentElectionVotes;
  }

  public RollCall getCurrentRollCall() {
    return currentRollCall;
  }

  public void setCurrentRollCall(RollCall rc) {
    currentRollCall = rc;
  }

  public void setCurrentRollCallId(String rollCallId) {
    currentRollCallId = rollCallId;
  }

  public void setCurrentElectionVotes(List<Integer> currentElectionVotes) {
    if (currentElectionVotes == null) {
      throw new IllegalArgumentException();
    }
    mCurrentElectionVotes.setValue(currentElectionVotes);
  }

  public void setCurrentElectionQuestionVotes(Integer votes, int position) {
    if (votes == null || position < 0 || position > mCurrentElectionVotes.getValue().size()) {
      throw new IllegalArgumentException();
    }
    if (mCurrentElectionVotes.getValue().size() <= position) {
      mCurrentElectionVotes.getValue().add(votes);
    } else {
      mCurrentElectionVotes.getValue().set(position, votes);
    }
  }

  public void setShowProperties(boolean show) {
    showProperties.postValue(show);
  }

  /**
   * Method to update the list of witnesses of a Lao by sending an updateLao msg and a stateLao msg
   * to the backend
   */
  public Completable updateLaoWitnesses() {
    Log.d(TAG, "Updating lao witnesses ");

    LaoView laoView;
    try {
      laoView = getLaoView();
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
    disposables.add(
        laoRepository
            .getLaoObservable(laoId)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                laoView -> {
                  Log.d(TAG, "got an update for lao: " + laoView.getName());

                  mCurrentLao.postValue(laoView);
                  boolean isOrganizer =
                      laoView.getOrganizer().equals(keyManager.getMainPublicKey());
                  mIsOrganizer.setValue(isOrganizer);
                  updateCurrentObjects(laoView);
                },
                error -> Log.d(TAG, "error updating LAO :" + error)));
  }

  private void updateCurrentObjects(LaoView laoView) {
    if (currentRollCall != null) {
      Optional<RollCall> rcOption =
          laoView.getRollCallWithPersistentId(currentRollCall.getPersistentId());
      if (!rcOption.isPresent()) {
        throw new IllegalStateException("Roll call must be present if in current id");
      }
      currentRollCall = rcOption.get();
    }
    if (currentElection != null) {
      Optional<Election> electionOption = laoView.getElection(currentElection.getId());
      if (!electionOption.isPresent()) {
        throw new IllegalStateException("Election must be present if in current");
      }
      currentElection = electionOption.get();
    }
  }

  public PoPToken getCurrentPopToken() throws KeyException, UnknownLaoException {
    return keyManager.getPoPToken(getLaoView(), currentRollCall);
  }

  public boolean isWalletSetup() {
    return wallet.isSetUp();
  }

  @Override
  public int getScanDescription() {
    if (scanningAction == ScanningAction.ADD_ROLL_CALL_ATTENDEE) {
      return R.string.qrcode_scanning_add_attendee; // Message to add attendees to a roll call
    } else {
      return R.string.qrcode_scanning_add_witness; // Message to add a witness
    }
  }

  @Override
  public void onQRCodeDetected(Barcode barcode) {
    Log.d(TAG, "Detected barcode with value: " + barcode.rawValue);
    handleInputData(barcode.rawValue);
  }

  @Override
  public boolean addManually(String data) {
    Log.d(TAG, "Key manually submitted with value: " + data);
    return handleInputData(data);
  }

  public void savePersistentData() throws GeneralSecurityException {
    ActivityUtils.activitySavingRoutine(
        networkManager, wallet, getApplication().getApplicationContext());
  }

  /**
   * Checks the key validity and handles the attendee addition process
   *
   * @param data the textual representation of the key
   * @return true if an attendee was added false otherwise
   */
  private boolean handleInputData(String data) {
    Log.d(TAG, "data scanned " + data);
    if (scanningAction == ScanningAction.ADD_ROLL_CALL_ATTENDEE) {
      return handleRollCallAddition(data);
    } else if (scanningAction == ScanningAction.ADD_WITNESS) {
      return handleWitnessAddition(data);
    } else {
      throw new IllegalStateException(
          "The scanning action should either be to add witnesses or rc attendees");
    }
  }

  public boolean handleRollCallAddition(String data) {
    PopTokenData tokenData;
    try {
      tokenData = PopTokenData.extractFrom(gson, data);
    } catch (Exception e) {
      ErrorUtils.logAndShow(
          getApplication().getApplicationContext(), TAG, R.string.qr_code_not_pop_token);
      return false;
    }
    PublicKey publicKey = tokenData.getPopToken();
    if (attendees.contains(publicKey)) {
      Log.d(TAG, "Attendee was already scanned");
      mScanWarningEvent.postValue(
          new SingleEvent<>("This attendee key has already been scanned. Please try again."));
      return false;
    }

    attendees.add(publicKey);
    Log.d(TAG, "Attendee " + publicKey + " successfully added");
    mAttendeeScanConfirmEvent.postValue(new SingleEvent<>("Attendee has been added."));
    mNbAttendees.postValue(attendees.size());
    return true;
  }

  public boolean handleWitnessAddition(String data) {
    MainPublicKeyData pkData;
    try {
      pkData = MainPublicKeyData.extractFrom(gson, data);
    } catch (Exception e) {
      ErrorUtils.logAndShow(
          getApplication().getApplicationContext(), TAG, e, R.string.qr_code_not_main_pk);
      return false;
    }
    PublicKey publicKey = pkData.getPublicKey();
    if (witnesses.contains(publicKey)) {
      Log.d(TAG, "Witness was already scanned");
      mScanWarningEvent.postValue(
          new SingleEvent<>("This attendee key has already been scanned. Please try again."));
      return false;
    }

    witnesses.add(publicKey);
    Log.d(TAG, "Added witness " + publicKey + " successfully");
    mWitnessScanConfirmEvent.postValue(new SingleEvent<>(true));
    disposables.add(
        updateLaoWitnesses()
            .subscribe(
                () -> Log.d(TAG, "Witness " + publicKey + " added"),
                error ->
                    ErrorUtils.logAndShow(
                        getApplication(), TAG, error, R.string.error_update_lao)));

    return true;
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
}
