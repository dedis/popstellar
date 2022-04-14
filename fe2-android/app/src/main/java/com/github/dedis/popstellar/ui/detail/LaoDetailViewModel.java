package com.github.dedis.popstellar.ui.detail;

import android.Manifest;
import android.app.Application;
import android.content.pm.PackageManager;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.LiveDataReactiveStreams;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.SingleEvent;
import com.github.dedis.popstellar.model.network.method.message.MessageGeneral;
import com.github.dedis.popstellar.model.network.method.message.data.consensus.ConsensusElect;
import com.github.dedis.popstellar.model.network.method.message.data.consensus.ConsensusElectAccept;
import com.github.dedis.popstellar.model.network.method.message.data.election.CastVote;
import com.github.dedis.popstellar.model.network.method.message.data.election.ElectionEnd;
import com.github.dedis.popstellar.model.network.method.message.data.election.ElectionSetup;
import com.github.dedis.popstellar.model.network.method.message.data.election.ElectionVote;
import com.github.dedis.popstellar.model.network.method.message.data.election.OpenElection;
import com.github.dedis.popstellar.model.network.method.message.data.lao.StateLao;
import com.github.dedis.popstellar.model.network.method.message.data.lao.UpdateLao;
import com.github.dedis.popstellar.model.network.method.message.data.message.WitnessMessageSignature;
import com.github.dedis.popstellar.model.network.method.message.data.rollcall.CloseRollCall;
import com.github.dedis.popstellar.model.network.method.message.data.rollcall.CreateRollCall;
import com.github.dedis.popstellar.model.network.method.message.data.rollcall.OpenRollCall;
import com.github.dedis.popstellar.model.objects.Channel;
import com.github.dedis.popstellar.model.objects.ConsensusNode;
import com.github.dedis.popstellar.model.objects.ElectInstance;
import com.github.dedis.popstellar.model.objects.Election;
import com.github.dedis.popstellar.model.objects.Lao;
import com.github.dedis.popstellar.model.objects.RollCall;
import com.github.dedis.popstellar.model.objects.Wallet;
import com.github.dedis.popstellar.model.objects.WitnessMessage;
import com.github.dedis.popstellar.model.objects.event.EventState;
import com.github.dedis.popstellar.model.objects.event.EventType;
import com.github.dedis.popstellar.model.objects.security.KeyPair;
import com.github.dedis.popstellar.model.objects.security.MessageID;
import com.github.dedis.popstellar.model.objects.security.PoPToken;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.model.objects.security.Signature;
import com.github.dedis.popstellar.repository.LAORepository;
import com.github.dedis.popstellar.repository.remote.GlobalNetworkManager;
import com.github.dedis.popstellar.ui.home.HomeViewModel;
import com.github.dedis.popstellar.ui.qrcode.CameraPermissionViewModel;
import com.github.dedis.popstellar.ui.qrcode.QRCodeScanningViewModel;
import com.github.dedis.popstellar.ui.qrcode.ScanningAction;
import com.github.dedis.popstellar.utility.error.ErrorUtils;
import com.github.dedis.popstellar.utility.error.keys.KeyException;
import com.github.dedis.popstellar.utility.error.keys.KeyGenerationException;
import com.github.dedis.popstellar.utility.error.keys.UninitializedWalletException;
import com.github.dedis.popstellar.utility.security.KeyManager;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.gson.Gson;

import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.BackpressureStrategy;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

@HiltViewModel
public class LaoDetailViewModel extends AndroidViewModel
    implements CameraPermissionViewModel, QRCodeScanningViewModel {

  public static final String TAG = LaoDetailViewModel.class.getSimpleName();
  private static final String LAO_FAILURE_MESSAGE = "failed to retrieve current lao";
  private static final String PK_FAILURE_MESSAGE = "failed to retrieve public key";
  private static final String PUBLISH_MESSAGE = "sending publish message";
  /*
   * LiveData objects for capturing events like button clicks
   */
  private final MutableLiveData<SingleEvent<Boolean>> mOpenHomeEvent = new MutableLiveData<>();
  private final MutableLiveData<SingleEvent<PublicKey>> mOpenIdentityEvent =
      new MutableLiveData<>();
  private final MutableLiveData<SingleEvent<Boolean>> mOpenWitnessMessageEvent =
      new MutableLiveData<>();
  private final MutableLiveData<SingleEvent<Boolean>> mShowPropertiesEvent =
      new MutableLiveData<>();
  private final MutableLiveData<SingleEvent<Boolean>> mEditPropertiesEvent =
      new MutableLiveData<>();
  private final MutableLiveData<SingleEvent<Boolean>> mOpenSocialMediaEvent =
      new MutableLiveData<>();
  private final MutableLiveData<SingleEvent<Boolean>> mOpenDigitalCashEvent =
      new MutableLiveData<>();
  private final MutableLiveData<SingleEvent<Boolean>> mOpenLaoDetailEvent = new MutableLiveData<>();
  private final MutableLiveData<SingleEvent<EventType>> mChooseNewLaoEventTypeEvent =
      new MutableLiveData<>();
  private final MutableLiveData<SingleEvent<EventType>> mNewLaoEventCreationEvent =
      new MutableLiveData<>();
  private final MutableLiveData<SingleEvent<Boolean>> mOpenNewRollCallEvent =
      new MutableLiveData<>();
  private final MutableLiveData<SingleEvent<HomeViewModel.HomeViewAction>> mOpenRollCallEvent = new MutableLiveData<>();
  private final MutableLiveData<SingleEvent<String>> mOpenRollCallTokenEvent =
      new MutableLiveData<>();
  private final MutableLiveData<SingleEvent<String>> mOpenAttendeesListEvent =
      new MutableLiveData<>();
  private final MutableLiveData<SingleEvent<Boolean>> mOpenLaoWalletEvent = new MutableLiveData<>();
  private final MutableLiveData<SingleEvent<Boolean>> mOpenElectionResultsEvent =
      new MutableLiveData<>();
  private final MutableLiveData<SingleEvent<Boolean>> mOpenManageElectionEvent =
      new MutableLiveData<>();
  private final MutableLiveData<SingleEvent<Boolean>> mElectionCreatedEvent =
      new MutableLiveData<>();
  private final MutableLiveData<SingleEvent<Boolean>> mOpenCastVotesEvent = new MutableLiveData<>();
  private final MutableLiveData<SingleEvent<HomeViewModel.HomeViewAction>> mOpenAddWitness = new MutableLiveData<>();
  private final MutableLiveData<SingleEvent<Boolean>> mEndElectionEvent =
      new MutableLiveData<>(new SingleEvent<>(false));
  private final MutableLiveData<SingleEvent<Boolean>> mReceivedElectionResultsEvent =
      new MutableLiveData<>(new SingleEvent<>(false));

  private final MutableLiveData<SingleEvent<Integer>> mNbAttendeesEvent = new MutableLiveData<>();
  private final MutableLiveData<SingleEvent<Integer>> mAskCloseRollCallEvent =
      new MutableLiveData<>();
  private final MutableLiveData<SingleEvent<Integer>> mCloseRollCallEvent = new MutableLiveData<>();

  private final MutableLiveData<SingleEvent<Boolean>> mCreatedRollCallEvent =
      new MutableLiveData<>();
  private final MutableLiveData<SingleEvent<PublicKey>> mPkRollCallEvent = new MutableLiveData<>();
  private final MutableLiveData<SingleEvent<Boolean>> mWalletMessageEvent = new MutableLiveData<>();

  private final MutableLiveData<SingleEvent<String>> mAttendeeScanConfirmEvent =
      new MutableLiveData<>();
  private final MutableLiveData<SingleEvent<Boolean>> mWitnessScanConfirmEvent =
      new MutableLiveData<>();
  private final MutableLiveData<SingleEvent<String>> mScanWarningEvent = new MutableLiveData<>();

  private final MutableLiveData<SingleEvent<Boolean>> mOpenStartElectionEvent =
      new MutableLiveData<>();

  private final MutableLiveData<SingleEvent<Boolean>> mOpenElectionEvent = new MutableLiveData<>();
  /*
   * LiveData objects that represent the state in a fragment
   */
  private final MutableLiveData<Lao> mCurrentLao = new MutableLiveData<>();
  private final MutableLiveData<Election> mCurrentElection =
      new MutableLiveData<>(); // Represents the current election being managed/opened in a fragment
  private final MutableLiveData<Boolean> mIsOrganizer = new MutableLiveData<>();
  private final MutableLiveData<Boolean> mIsWitness = new MutableLiveData<>();
  private final MutableLiveData<Boolean> mIsSignedByCurrentWitness = new MutableLiveData<>();
  private final MutableLiveData<Boolean> showProperties = new MutableLiveData<>(false);
  private final MutableLiveData<String> mLaoName = new MutableLiveData<>("");
  private final MutableLiveData<List<List<Integer>>> mCurrentElectionVotes =
      new MutableLiveData<>();
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
          lao ->
              lao == null
                  ? new ArrayList<>()
                  : Stream.concat(
                          lao.getRollCalls().values().stream(),
                          lao.getElections().values().stream())
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
                      .filter(rollcall -> rollcall.getState() == EventState.CLOSED)
                      .filter(rollcall -> attendedOrOrganized(lao, rollcall))
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
  private String currentRollCallId = ""; // used to know which roll call to close
  private Set<PublicKey> attendees = new HashSet<>();
  private Set<PublicKey> witnesses =
      new HashSet<>(); // used to dynamically update the set of witnesses when WR code scanned
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
   * @param lao
   * @param rollcall
   * @return boolean saying whether user attended or organized the given roll call
   */
  private boolean attendedOrOrganized(Lao lao, RollCall rollcall) {
    // find out if user has attended the rollcall
    String firstLaoId = lao.getId();
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
   * Opens the election and publish opening message
   * triggers OpenElection event on success or logs appropriate error
   * @param e election to be opened
   */
  public void openElection(Election e){

    Log.d(TAG, "opening election with name : " + e.getName());
    Lao lao = getCurrentLaoValue();
    if (lao == null) {
      Log.d(TAG, LAO_FAILURE_MESSAGE);
      return;
    }

    Channel channel = e.getChannel();
    String laoId = lao.getId();

    // The time will have to be modified on the backend
    OpenElection openElection =
        new OpenElection(laoId, e.getId(), e.getStartTimestamp());

    Log.d(TAG, PUBLISH_MESSAGE);
    Disposable disposable =
        networkManager
            .getMessageSender()
            .publish(keyManager.getMainKeyPair(), channel, openElection)
            .subscribe(
                () -> {
                  Log.d(TAG, "opened election successfully");
                  // Block action button on expandableListViewAdapter
                  openElectionEvent();
                },
                error ->
                    ErrorUtils.logAndShow(
                        getApplication(), TAG, error, R.string.error_open_election));
    disposables.add(disposable);
  }

  public void endElection(Election election) {
    Log.d(TAG, "ending election with name : " + election.getName());
    Lao lao = getCurrentLaoValue();
    if (lao == null) {
      Log.d(TAG, LAO_FAILURE_MESSAGE);
      return;
    }

    Channel channel = election.getChannel();
    String laoId = lao.getId();
    ElectionEnd electionEnd =
        new ElectionEnd(election.getId(), laoId, election.computerRegisteredVotes());

    Log.d(TAG, PUBLISH_MESSAGE);
    Disposable disposable =
        networkManager
            .getMessageSender()
            .publish(keyManager.getMainKeyPair(), channel, electionEnd)
            .subscribe(
                () -> {
                  Log.d(TAG, "ended election successfully");
                  endElectionEvent();
                },
                error ->
                    ErrorUtils.logAndShow(
                        getApplication(), TAG, error, R.string.error_end_election));

    disposables.add(disposable);
  }

  /**
   * Sends a ElectionCastVotes message .
   *
   * <p>Publish a GeneralMessage containing ElectionCastVotes data.
   *
   * @param votes the corresponding votes for that election
   */
  public void sendVote(List<ElectionVote> votes) {
    Election election = mCurrentElection.getValue();
    if (election == null) {
      Log.d(TAG, "failed to retrieve current election");
      return;
    }
    Log.d(
        TAG,
        "sending a new vote in election : "
            + election
            + " with election start time"
            + election.getStartTimestamp());
    Lao lao = getCurrentLaoValue();
    if (lao == null) {
      Log.d(TAG, LAO_FAILURE_MESSAGE);
      return;
    }

    try {
      PoPToken token = keyManager.getValidPoPToken(lao);
      CastVote castVote = new CastVote(votes, election.getId(), lao.getId());
      Channel electionChannel = election.getChannel();

      Log.d(TAG, PUBLISH_MESSAGE);
      Disposable disposable =
          networkManager
              .getMessageSender()
              .publish(token, electionChannel, castVote)
              .doFinally(this::openLaoDetail)
              .subscribe(
                  () -> {
                    Log.d(TAG, "sent a vote successfully");
                    // Toast ? + send back to election screen or details screen ?
                    Toast.makeText(getApplication(), "vote successfully sent !", Toast.LENGTH_LONG)
                        .show();
                  },
                  error ->
                      ErrorUtils.logAndShow(
                          getApplication(), TAG, error, R.string.error_send_vote));
      disposables.add(disposable);
    } catch (KeyException e) {
      ErrorUtils.logAndShow(getApplication(), TAG, e, R.string.error_retrieve_own_token);
    }
  }

  /**
   * Creates new Election event.
   *
   * <p>Publish a GeneralMessage containing ElectionSetup data.
   *
   * @param name the name of the election
   * @param creation the creation time of the election
   * @param start the start time of the election
   * @param end the end time of the election
   * @param votingMethod the type of voting method (e.g Plurality)
   * @param ballotOptions the list of ballot options
   * @param question the question associated to the election
   * @return the id of the newly created election event, null if fails to create the event
   */
  public String createNewElection(
      String name,
      long creation,
      long start,
      long end,
      List<String> votingMethod,
      List<Boolean> writeIn,
      List<List<String>> ballotOptions,
      List<String> question) {
    Log.d(TAG, "creating a new election with name " + name);

    Lao lao = getCurrentLaoValue();
    if (lao == null) {
      Log.d(TAG, LAO_FAILURE_MESSAGE);
      return null;
    }

    Channel channel = lao.getChannel();
    ElectionSetup electionSetup =
        new ElectionSetup(
            name,
            creation,
            start,
            end,
            votingMethod,
            writeIn,
            ballotOptions,
            question,
            lao.getId());

    Log.d(TAG, PUBLISH_MESSAGE);
    Disposable disposable =
        networkManager
            .getMessageSender()
            .publish(keyManager.getMainKeyPair(), channel, electionSetup)
            .subscribe(
                () -> {
                  Log.d(TAG, "setup an election");
                  mElectionCreatedEvent.postValue(new SingleEvent<>(true));
                },
                error ->
                    ErrorUtils.logAndShow(
                        getApplication(), TAG, error, R.string.error_create_election));

    disposables.add(disposable);

    return electionSetup.getId();
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
   * @param open true if we want to directly open the roll call
   */
  public void createNewRollCall(
      String title,
      String description,
      long creation,
      long proposedStart,
      long proposedEnd,
      boolean open) {
    Log.d(TAG, "creating a new roll call with title " + title);
    Lao lao = getCurrentLaoValue();
    if (lao == null) {
      Log.d(TAG, LAO_FAILURE_MESSAGE);
      return;
    }
    Channel channel = lao.getChannel();
    // FIXME Location : Lausanne ?
    CreateRollCall createRollCall =
        new CreateRollCall(
            title, creation, proposedStart, proposedEnd, "Lausanne", description, lao.getId());

    Log.d(TAG, PUBLISH_MESSAGE);
    Disposable disposable =
        networkManager
            .getMessageSender()
            .publish(keyManager.getMainKeyPair(), channel, createRollCall)
            .subscribe(
                () -> {
                  Log.d(TAG, "created a roll call with id: " + createRollCall.getId());
                  if (open) {
                    openRollCall(createRollCall.getId());
                  } else {
                    mCreatedRollCallEvent.postValue(new SingleEvent<>(true));
                  }
                },
                error ->
                    ErrorUtils.logAndShow(
                        getApplication(), TAG, error, R.string.error_create_rollcall));
    disposables.add(disposable);
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
   */
  public void sendConsensusElect(
      long creation, String objId, String type, String property, Object value) {
    Log.d(
        TAG,
        String.format(
            "creating a new consensus for type: %s, property: %s, value: %s",
            type, property, value));
    Lao lao = getCurrentLaoValue();
    if (lao == null) {
      Log.d(TAG, LAO_FAILURE_MESSAGE);
      return;
    }
    Channel channel = lao.getChannel().subChannel("consensus");

    ConsensusElect consensusElect = new ConsensusElect(creation, objId, type, property, value);

    Log.d(TAG, PUBLISH_MESSAGE);
    MessageGeneral msg = new MessageGeneral(keyManager.getMainKeyPair(), consensusElect, gson);

    Disposable disposable =
        networkManager
            .getMessageSender()
            .publish(channel, msg)
            .subscribe(
                () -> Log.d(TAG, "created a consensus with message id : " + msg.getMessageId()),
                error ->
                    ErrorUtils.logAndShow(
                        getApplication(), TAG, error, R.string.error_start_election));
    disposables.add(disposable);
  }

  /**
   * Sends a ConsensusElectAccept message.
   *
   * <p>Publish a GeneralMessage containing ConsensusElectAccept data.
   *
   * @param electInstance the corresponding ElectInstance
   * @param accept true if accepted, false if rejected
   */
  public void sendConsensusElectAccept(ElectInstance electInstance, boolean accept) {
    MessageID messageId = electInstance.getMessageId();
    Log.d(
        TAG,
        "sending a new elect_accept for consensus with messageId : "
            + messageId
            + " with value "
            + accept);

    Lao lao = getCurrentLaoValue();
    if (lao == null) {
      Log.d(TAG, LAO_FAILURE_MESSAGE);
      return;
    }

    ConsensusElectAccept consensusElectAccept =
        new ConsensusElectAccept(electInstance.getInstanceId(), messageId, accept);

    Log.d(TAG, PUBLISH_MESSAGE);
    Disposable disposable =
        networkManager
            .getMessageSender()
            .publish(keyManager.getMainKeyPair(), electInstance.getChannel(), consensusElectAccept)
            .subscribe(
                () -> Log.d(TAG, "sent an elect_accept successfully"),
                error ->
                    ErrorUtils.logAndShow(
                        getApplication(), TAG, error, R.string.error_consensus_accept));

    disposables.add(disposable);
  }

  /**
   * Opens a roll call event.
   *
   * <p>Publish a GeneralMessage containing OpenRollCall data.
   *
   * @param id the roll call id to open
   */
  public void openRollCall(String id) {
    Log.d(TAG, "call openRollCall");

    Lao lao = getCurrentLaoValue();
    if (lao == null) {
      Log.d(TAG, LAO_FAILURE_MESSAGE);
      return;
    }
    long openedAt = Instant.now().getEpochSecond();
    Channel channel = lao.getChannel();
    String laoId = lao.getId();
    Optional<RollCall> optRollCall = lao.getRollCall(id);
    if (!optRollCall.isPresent()) {
      Log.d(TAG, "failed to retrieve roll call with id " + id + "laoID: " + laoId);
      return;
    }
    RollCall rollCall = optRollCall.get();
    OpenRollCall openRollCall = new OpenRollCall(laoId, id, openedAt, rollCall.getState());
    attendees = new HashSet<>(rollCall.getAttendees());

    try {
      attendees.add(keyManager.getPoPToken(lao, rollCall).getPublicKey());
    } catch (KeyException e) {
      ErrorUtils.logAndShow(getApplication(), TAG, e, R.string.error_retrieve_own_token);
    }

    Disposable disposable =
        networkManager
            .getMessageSender()
            .publish(keyManager.getMainKeyPair(), channel, openRollCall)
            .subscribe(
                () -> {
                  Log.d(TAG, "opened the roll call");
                  currentRollCallId = openRollCall.getUpdateId();
                  scanningAction = ScanningAction.ADD_ROLL_CALL_ATTENDEE;
                  openScanning();
                },
                error ->
                    ErrorUtils.logAndShow(
                        getApplication(), TAG, error, R.string.error_open_rollcall));

    disposables.add(disposable);
  }

  /**
   * Closes the roll call event currently open
   *
   * <p>Publish a GeneralMessage containing CloseRollCall data.
   */
  public void closeRollCall(int nextFragment) {
    Log.d(TAG, "call closeRollCall");
    Lao lao = getCurrentLaoValue();
    if (lao == null) {
      Log.d(TAG, LAO_FAILURE_MESSAGE);
      return;
    }
    long end = Instant.now().getEpochSecond();
    Channel channel = lao.getChannel();
    CloseRollCall closeRollCall =
        new CloseRollCall(lao.getId(), currentRollCallId, end, new ArrayList<>(attendees));
    Disposable disposable =
        networkManager
            .getMessageSender()
            .publish(keyManager.getMainKeyPair(), channel, closeRollCall)
            .subscribe(
                () -> {
                  Log.d(TAG, "closed the roll call");
                  currentRollCallId = "";
                  attendees.clear();
                  mCloseRollCallEvent.setValue(new SingleEvent<>(nextFragment));
                },
                error ->
                    ErrorUtils.logAndShow(
                        getApplication(), TAG, error, R.string.error_close_rollcall));
    disposables.add(disposable);
  }

  public void signMessage(WitnessMessage witnessMessage) {
    Log.d(TAG, "signing message with ID " + witnessMessage.getMessageId());
    Lao lao = getCurrentLaoValue();
    if (lao == null) {
      Log.d(TAG, LAO_FAILURE_MESSAGE);
      return;
    }

    Channel channel = lao.getChannel();

    try {
      KeyPair mainKey = keyManager.getMainKeyPair();
      // Generate the signature of the message
      Signature signature = mainKey.sign(witnessMessage.getMessageId());

      Log.d(TAG, PUBLISH_MESSAGE);
      WitnessMessageSignature signatureMessage =
          new WitnessMessageSignature(witnessMessage.getMessageId(), signature);
      disposables.add(
          networkManager
              .getMessageSender()
              .publish(keyManager.getMainKeyPair(), channel, signatureMessage)
              .subscribe(
                  () ->
                      Log.d(
                          TAG,
                          "Verifying the signature of  message  with id: "
                              + witnessMessage.getMessageId()),
                  error ->
                      ErrorUtils.logAndShow(
                          getApplication(), TAG, error, R.string.error_sign_message)));

    } catch (GeneralSecurityException e) {
      Log.d(TAG, PK_FAILURE_MESSAGE, e);
    }
  }

  /**
   * Remove specific witness from the LAO's list of witnesses.
   *
   * <p>Publish a GeneralMessage containing UpdateLao data.
   *
   * @param witness the id of the witness to remove
   */
  public void removeWitness(String witness) {
    Log.d(TAG, "trying to remove witness: " + witness);
    // TODO: implement this by sending an UpdateLao
  }

  /*
   * Getters for MutableLiveData instances declared above
   */
  public LiveData<SingleEvent<Boolean>> getOpenLaoDetailEvent() {
    return mOpenLaoDetailEvent;
  }

  public LiveData<SingleEvent<Boolean>> getOpenElectionEvent() {
    return mOpenElectionEvent;
  }

  public LiveData<SingleEvent<Boolean>> getEndElectionEvent() {
    return mEndElectionEvent;
  }

  public LiveData<SingleEvent<Boolean>> getOpenElectionResultsEvent() {
    return mOpenElectionResultsEvent;
  }

  public LiveData<SingleEvent<Boolean>> getReceivedElectionResultsEvent() {
    return mReceivedElectionResultsEvent;
  }

  public LiveData<SingleEvent<Boolean>> getElectionCreated() {
    return mElectionCreatedEvent;
  }

  public LiveData<SingleEvent<Boolean>> getOpenManageElectionEvent() {
    return mOpenManageElectionEvent;
  }

  public LiveData<SingleEvent<Boolean>> getOpenCastVotes() {
    return mOpenCastVotesEvent;
  }

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

  public LiveData<SingleEvent<Boolean>> getOpenHomeEvent() {
    return mOpenHomeEvent;
  }

  public LiveData<SingleEvent<PublicKey>> getOpenIdentityEvent() {
    return mOpenIdentityEvent;
  }

  public LiveData<SingleEvent<Boolean>> getOpenWitnessMessageEvent() {
    return mOpenWitnessMessageEvent;
  }

  public LiveData<SingleEvent<Boolean>> getShowPropertiesEvent() {
    return mShowPropertiesEvent;
  }

  public LiveData<SingleEvent<Boolean>> getEditPropertiesEvent() {
    return mEditPropertiesEvent;
  }

  public LiveData<SingleEvent<Boolean>> getOpenSocialMediaEvent() {
    return mOpenSocialMediaEvent;
  }

  public LiveData<SingleEvent<Boolean>> getOpenDigitalCashEvent() {
    return mOpenDigitalCashEvent;
  }

  public LiveData<SingleEvent<EventType>> getNewLaoEventEvent() {
    return mChooseNewLaoEventTypeEvent;
  }

  public LiveData<SingleEvent<EventType>> getNewLaoEventCreationEvent() {
    return mNewLaoEventCreationEvent;
  }

  public LiveData<SingleEvent<Boolean>> getOpenNewRollCallEvent() {
    return mOpenNewRollCallEvent;
  }

  public LiveData<Lao> getCurrentLao() {
    return mCurrentLao;
  }

  public Lao getCurrentLaoValue() {
    return mCurrentLao.getValue();
  }

  @VisibleForTesting
  public void setCurrentLao(Lao lao) {
    mCurrentLao.setValue(lao);
  }

  public LiveData<String> getCurrentLaoName() {
    return mCurrentLaoName;
  }

  public void setCurrentLaoName(String laoName) {
    if (laoName != null && !laoName.isEmpty() && !laoName.equals(getCurrentLaoName().getValue())) {
      Log.d(TAG, "New name for current LAO: " + laoName);
      mLaoName.setValue(laoName);
    }
  }

  public LiveData<Boolean> isOrganizer() {
    return mIsOrganizer;
  }

  public LiveData<Boolean> isWitness() {
    boolean isWitness = getCurrentLaoValue().getWitnesses().contains(keyManager.getMainPublicKey());
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

  public LiveData<SingleEvent<HomeViewModel.HomeViewAction>> getOpenRollCallEvent() {
    return mOpenRollCallEvent;
  }

  public LiveData<SingleEvent<HomeViewModel.HomeViewAction>> getOpenAddWitness() {
    return mOpenAddWitness;
  }

  public LiveData<SingleEvent<String>> getOpenRollCallTokenEvent() {
    return mOpenRollCallTokenEvent;
  }

  public LiveData<SingleEvent<String>> getOpenAttendeesListEvent() {
    return mOpenAttendeesListEvent;
  }

  public LiveData<SingleEvent<Boolean>> getOpenLaoWalletEvent() {
    return mOpenLaoWalletEvent;
  }

  public LiveData<SingleEvent<Integer>> getNbAttendeesEvent() {
    return mNbAttendeesEvent;
  }

  public LiveData<SingleEvent<Integer>> getAskCloseRollCallEvent() {
    return mAskCloseRollCallEvent;
  }

  public LiveData<SingleEvent<Integer>> getCloseRollCallEvent() {
    return mCloseRollCallEvent;
  }

  public LiveData<SingleEvent<Boolean>> getCreatedRollCallEvent() {
    return mCreatedRollCallEvent;
  }

  public LiveData<SingleEvent<Boolean>> getOpenStartElectionEvent() {
    return mOpenStartElectionEvent;
  }

  public LiveData<List<ConsensusNode>> getNodes() {
    return LiveDataReactiveStreams.fromPublisher(
        laoRepository
            .getNodesByChannel(getCurrentLaoValue().getChannel())
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

  public LiveData<SingleEvent<PublicKey>> getPkRollCallEvent() {
    return mPkRollCallEvent;
  }

  public LiveData<SingleEvent<Boolean>> getWalletMessageEvent() {
    return mWalletMessageEvent;
  }

  public Election getCurrentElection() {
    return mCurrentElection.getValue();
  }

  public void setCurrentElection(Election e) {
    mCurrentElection.setValue(e);
  }

  public MutableLiveData<List<List<Integer>>> getCurrentElectionVotes() {
    return mCurrentElectionVotes;
  }

  public void setCurrentElectionVotes(List<List<Integer>> currentElectionVotes) {
    if (currentElectionVotes == null) {
      throw new IllegalArgumentException();
    }
    mCurrentElectionVotes.setValue(currentElectionVotes);
  }

  public void setCurrentElectionQuestionVotes(List<Integer> votes, int position) {
    if (votes == null || position < 0 || position >= mCurrentElectionVotes.getValue().size()) {
      throw new IllegalArgumentException();
    }

    mCurrentElectionVotes.getValue().set(position, votes);
  }

  /*
   * Methods that modify the state or post an Event to update the UI.
   */
  public void openHome() {
    if (currentRollCallId.equals("")) {
      mOpenHomeEvent.setValue(new SingleEvent<>(true));
    } else {
      mAskCloseRollCallEvent.setValue(new SingleEvent<>(R.id.fragment_home));
    }
  }

  public void electionCreated() {
    mElectionCreatedEvent.postValue(new SingleEvent<>(true));
  }

  public void openLaoDetail() {
    mOpenLaoDetailEvent.postValue(new SingleEvent<>(true));
  }

  public void openCastVotes() {
    mOpenCastVotesEvent.postValue(new SingleEvent<>(true));
  }

  public void openIdentity() {
    if (currentRollCallId.equals("")) {
      mOpenIdentityEvent.setValue(new SingleEvent<>(keyManager.getMainPublicKey()));
    } else {
      mAskCloseRollCallEvent.setValue(new SingleEvent<>(R.id.fragment_identity));
    }
  }

  public void openSocialMedia() {
    mOpenSocialMediaEvent.setValue(new SingleEvent<>(true));
  }

  public void openDigitalCash() {
    mOpenDigitalCashEvent.setValue(new SingleEvent<>(true));
  }
  
  /**
   * Propagates the open election event
   */
  public void openElectionEvent() {
    mOpenElectionEvent.postValue(new SingleEvent<>(true));
  }

  public void endElectionEvent() {
    mEndElectionEvent.postValue(new SingleEvent<>(true));
  }

  public void receiveElectionResultsEvent() {
    mReceivedElectionResultsEvent.postValue(new SingleEvent<>(true));
  }

  public void openWitnessMessage() {
    mOpenWitnessMessageEvent.setValue(new SingleEvent<>(true));
  }

  private void openAddWitness() {

    Lao lao = getCurrentLaoValue();
    if (lao == null) {
      Log.d(TAG, LAO_FAILURE_MESSAGE);
      return;
    }
    witnesses = new HashSet<>(lao.getWitnesses());
    mOpenAddWitness.setValue(new SingleEvent<>(HomeViewModel.HomeViewAction.SCAN));
  }

  public void toggleShowHideProperties() {
    boolean val = showProperties.getValue();
    showProperties.postValue(!val);
  }

  public void openEditProperties() {
    mEditPropertiesEvent.setValue(new SingleEvent<>(true));
  }

  public void closeEditProperties() {
    mEditPropertiesEvent.setValue(new SingleEvent<>(false));
  }

  public void terminateCurrentElection() {
    if (mCurrentLao.getValue().removeElection(mCurrentElection.getValue().getId())) {
      Log.d(TAG, "Election deleted : " + mCurrentElection.getValue().getId());
      Lao lao = getCurrentLaoValue();
      mCurrentLao.postValue(lao);
      openLaoDetail();
    } else {
      Log.d(TAG, "Impossible to delete election : " + mCurrentElection.getValue().getId());
    }
  }

  /**
   * Choosing an event type to create on the multiple-choice screen
   *
   * @param eventType the event type to create
   */
  public void chooseEventType(EventType eventType) {
    mChooseNewLaoEventTypeEvent.postValue(new SingleEvent<>(eventType));
  }

  /**
   * Creating a new event of specified type
   *
   * @param eventType the event type of the new event
   */
  public void newLaoEventCreation(EventType eventType) {
    mNewLaoEventCreationEvent.postValue(new SingleEvent<>(eventType));
  }

  public void openNewRollCall(Boolean open) {
    mOpenNewRollCallEvent.postValue(new SingleEvent<>(open));
  }

  public void openElectionResults(Boolean open) {
    mOpenElectionResultsEvent.postValue(new SingleEvent<>(open));
  }

  public void openManageElection(Boolean open) {
    mOpenManageElectionEvent.postValue(new SingleEvent<>(open));
  }

  public void openStartElection(Boolean open) {
    mOpenStartElectionEvent.postValue(new SingleEvent<>(open));
  }

  public void confirmEdit() {
    closeEditProperties();
    if (!mLaoName.getValue().isEmpty()) {
      updateLaoName();
    }
  }

  /**
   * Method to update the name of a Lao by sending an updateLao msg and a stateLao msg to the
   * backend
   */
  public void updateLaoName() {
    Log.d(TAG, "Updating lao name to " + mLaoName.getValue());

    Lao lao = getCurrentLaoValue();
    Channel channel = lao.getChannel();
    KeyPair mainKey = keyManager.getMainKeyPair();
    long now = Instant.now().getEpochSecond();
    UpdateLao updateLao =
        new UpdateLao(
            mainKey.getPublicKey(),
            lao.getCreation(),
            mLaoName.getValue(),
            now,
            lao.getWitnesses());
    MessageGeneral msg = new MessageGeneral(mainKey, updateLao, gson);
    Disposable disposable =
        networkManager
            .getMessageSender()
            .publish(channel, msg)
            .subscribe(
                () -> {
                  Log.d(TAG, "updated lao name");
                  dispatchLaoUpdate("lao name", updateLao, lao, channel, msg);
                },
                error ->
                    ErrorUtils.logAndShow(getApplication(), TAG, error, R.string.error_update_lao));

    disposables.add(disposable);
  }

  /**
   * Method to update the list of witnesses of a Lao by sending an updateLao msg and a stateLao msg
   * to the backend
   */
  public void updateLaoWitnesses() {
    Log.d(TAG, "Updating lao witnesses ");

    Lao lao = getCurrentLaoValue();

    if (lao == null) {
      Log.d(TAG, LAO_FAILURE_MESSAGE);
      return;
    }
    Channel channel = lao.getChannel();
    KeyPair mainKey = keyManager.getMainKeyPair();
    long now = Instant.now().getEpochSecond();
    UpdateLao updateLao =
        new UpdateLao(mainKey.getPublicKey(), lao.getCreation(), lao.getName(), now, witnesses);
    MessageGeneral msg = new MessageGeneral(mainKey, updateLao, gson);
    Disposable disposable =
        networkManager
            .getMessageSender()
            .publish(channel, msg)
            .subscribe(
                () -> {
                  Log.d(TAG, "updated lao witnesses");
                  dispatchLaoUpdate("lao state with new witnesses", updateLao, lao, channel, msg);
                },
                error ->
                    ErrorUtils.logAndShow(getApplication(), TAG, error, R.string.error_update_lao));
    disposables.add(disposable);
  }

  /** Helper method for updateLaoWitnesses and updateLaoName to send a stateLao message */
  private void dispatchLaoUpdate(
      String desc, UpdateLao updateLao, Lao lao, Channel channel, MessageGeneral msg) {
    StateLao stateLao =
        new StateLao(
            updateLao.getId(),
            updateLao.getName(),
            lao.getCreation(),
            updateLao.getLastModified(),
            lao.getOrganizer(),
            msg.getMessageId(),
            updateLao.getWitnesses(),
            new ArrayList<>());

    disposables.add(
        networkManager
            .getMessageSender()
            .publish(keyManager.getMainKeyPair(), channel, stateLao)
            .subscribe(
                () -> Log.d(TAG, "updated " + desc),
                error ->
                    ErrorUtils.logAndShow(getApplication(), TAG, error, R.string.error_state_lao)));
  }

  public void cancelEdit() {
    mLaoName.setValue("");
    closeEditProperties();
  }

  public void subscribeToLao(String laoId) {
    disposables.add(
        laoRepository
            .getLaoObservable(laoId)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                lao -> {
                  Log.d(TAG, "got an update for lao: " + lao.getName());
                  mCurrentLao.postValue(lao);
                  boolean isOrganizer = lao.getOrganizer().equals(keyManager.getMainPublicKey());
                  Log.d(TAG, "isOrganizer: " + isOrganizer);
                  mIsOrganizer.setValue(isOrganizer);
                }));
  }

  public void openQrCodeScanningRollCall() {
    mOpenRollCallEvent.setValue(new SingleEvent<>(HomeViewModel.HomeViewAction.SCAN));
    mNbAttendeesEvent.postValue(
        new SingleEvent<>(attendees.size())); // this to display the initial number of attendees
  }

  public void openCameraPermission() {
    if (scanningAction == ScanningAction.ADD_ROLL_CALL_ATTENDEE) {
      mOpenRollCallEvent.setValue(new SingleEvent<>(HomeViewModel.HomeViewAction.REQUEST_CAMERA_PERMISSION));
    } else if (scanningAction == ScanningAction.ADD_WITNESS) {
      mOpenAddWitness.setValue(new SingleEvent<>(HomeViewModel.HomeViewAction.REQUEST_CAMERA_PERMISSION));
    }
  }

  public void enterRollCall(String id) {
    if (!wallet.isSetUp()) {
      mWalletMessageEvent.setValue(new SingleEvent<>(true));
      return;
    }
    String firstLaoId = getCurrentLaoValue().getId();
    String errorMessage = "failed to retrieve public key from wallet";
    try {
      PublicKey publicKey = wallet.generatePoPToken(firstLaoId, id).getPublicKey();
      mPkRollCallEvent.postValue(new SingleEvent<>(publicKey));
    } catch (Exception e) {
      Log.d(TAG, errorMessage, e);
    }
  }

  public void openScanning() {
    if (ContextCompat.checkSelfPermission(
            getApplication().getApplicationContext(), Manifest.permission.CAMERA)
        == PackageManager.PERMISSION_GRANTED) {
      if (scanningAction == ScanningAction.ADD_ROLL_CALL_ATTENDEE) {
        openQrCodeScanningRollCall();
      } else if (scanningAction == ScanningAction.ADD_WITNESS) {
        openAddWitness();
      }
    } else {
      openCameraPermission();
    }
  }

  public void openLaoWallet() {
    mOpenLaoWalletEvent.postValue(new SingleEvent<>(true));
  }

  public void openRollCallToken(String rollCallId) {
    mOpenRollCallTokenEvent.postValue(new SingleEvent<>(rollCallId));
  }

  public void openAttendeesList(String rollCallId) {
    mOpenAttendeesListEvent.postValue(new SingleEvent<>(rollCallId));
  }

  @Override
  public void onPermissionGranted() {
    if (scanningAction == ScanningAction.ADD_ROLL_CALL_ATTENDEE) {
      openQrCodeScanningRollCall();
    } else if (scanningAction == ScanningAction.ADD_WITNESS) {
      openAddWitness();
    }
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
    PublicKey attendee;
    try {
      attendee = new PublicKey(barcode.rawValue);
    } catch (IllegalArgumentException e) {
      mScanWarningEvent.postValue(new SingleEvent<>("Invalid QR code. Please try again."));
      return;
    }

    if (attendees.contains(attendee)
        || Objects.requireNonNull(mWitnesses.getValue()).contains(attendee)) {
      mScanWarningEvent.postValue(
          new SingleEvent<>("This QR code has already been scanned. Please try again."));
      return;
    }
    if (scanningAction == (ScanningAction.ADD_ROLL_CALL_ATTENDEE)) {
      attendees.add(attendee);
      mAttendeeScanConfirmEvent.postValue(new SingleEvent<>("Attendee has been added."));
      mNbAttendeesEvent.postValue(new SingleEvent<>(attendees.size()));
    } else if (scanningAction == (ScanningAction.ADD_WITNESS)) {
      witnesses.add(attendee);
      mWitnessScanConfirmEvent.postValue(new SingleEvent<>(true));
      updateLaoWitnesses();
    }
  }
}
