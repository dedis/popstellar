package com.github.dedis.popstellar.ui.detail;

import android.Manifest;
import android.app.Application;
import android.content.pm.PackageManager;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
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
import com.github.dedis.popstellar.repository.LAORepository;
import com.github.dedis.popstellar.repository.local.PersistentData;
import com.github.dedis.popstellar.repository.remote.GlobalNetworkManager;
import com.github.dedis.popstellar.ui.detail.event.rollcall.RollCallFragment;
import com.github.dedis.popstellar.ui.qrcode.*;
import com.github.dedis.popstellar.utility.ActivityUtils;
import com.github.dedis.popstellar.utility.error.ErrorUtils;
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
import io.reactivex.BackpressureStrategy;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

import static androidx.core.content.ContextCompat.checkSelfPermission;
import static com.github.dedis.popstellar.ui.detail.LaoDetailActivity.setCurrentFragment;

@HiltViewModel
public class LaoDetailViewModel extends AndroidViewModel implements QRCodeScanningViewModel {

  public static final String TAG = LaoDetailViewModel.class.getSimpleName();
  private static final String LAO_FAILURE_MESSAGE = "failed to retrieve current lao";
  private static final String PK_FAILURE_MESSAGE = "failed to retrieve public key";
  private static final String PUBLISH_MESSAGE = "sending publish message";
  /*
   * LiveData objects for capturing events like button clicks
   */
  // FIXME These events should be removed once the QRScanning is refactored
  private final MutableLiveData<SingleEvent<String>> mAttendeeScanConfirmEvent =
      new MutableLiveData<>();
  private final MutableLiveData<SingleEvent<Boolean>> mWitnessScanConfirmEvent =
      new MutableLiveData<>();
  private final MutableLiveData<SingleEvent<String>> mScanWarningEvent = new MutableLiveData<>();

  private final MutableLiveData<LaoTab> currentTab = new MutableLiveData<>(LaoTab.EVENTS);

  /*
   * LiveData objects that represent the state in a fragment
   */
  private final MutableLiveData<Lao> mCurrentLao = new MutableLiveData<>();
  private final MutableLiveData<Election> mCurrentElection =
      new MutableLiveData<>(); // Represents the current election being opened in a fragment
  private final MutableLiveData<RollCall> mCurrentRollCall =
      new MutableLiveData<>(); // Represents the current roll-call being opened in a fragment
  private final MutableLiveData<Boolean> mIsOrganizer = new MutableLiveData<>();
  private final MutableLiveData<Boolean> mIsWitness = new MutableLiveData<>();
  private final MutableLiveData<Boolean> mIsSignedByCurrentWitness = new MutableLiveData<>();
  private final MutableLiveData<Integer> mNbAttendees = new MutableLiveData<>();
  private final MutableLiveData<Boolean> showProperties = new MutableLiveData<>(false);
  private final MutableLiveData<String> mLaoName = new MutableLiveData<>("");
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
                      .filter(rollCall -> rollCall.getState().getValue() == EventState.CLOSED)
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
   * @param lao the lao considered
   * @param rollcall the roll-call considered
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

  public void savePersistentData() {
    String serverAddress = networkManager.getCurrentUrl();
    Set<Channel> subscriptions = networkManager.getMessageSender().getSubscriptions();

    List<String> seed;
    try {
      seed = Arrays.asList(wallet.exportSeed());
    } catch (GeneralSecurityException e) {
      e.printStackTrace();
      return;
    }
    if (seed == null) {
      // it returns empty array if not initialized
      throw new IllegalStateException("Seed should not be null");
    }

    ActivityUtils.storePersistentData(
        getApplication().getApplicationContext(),
        new PersistentData(seed, serverAddress, subscriptions));
  }

  /**
   * Opens the election and publish opening message triggers OpenElection event on success or logs
   * appropriate error
   *
   * @param e election to be opened
   */
  public void openElection(Election e) {

    Log.d(TAG, "opening election with name : " + e.getName());
    Lao lao = getCurrentLaoValue();
    if (lao == null) {
      Log.d(TAG, LAO_FAILURE_MESSAGE);
      return;
    }

    Channel channel = e.getChannel();
    String laoId = lao.getId();

    // The time will have to be modified on the backend
    OpenElection openElection = new OpenElection(laoId, e.getId(), e.getStartTimestamp());

    Log.d(TAG, PUBLISH_MESSAGE);
    Disposable disposable =
        networkManager
            .getMessageSender()
            .publish(keyManager.getMainKeyPair(), channel, openElection)
            .subscribe(
                () -> Log.d(TAG, "opened election successfully")
                // stay on the election fragment for now
                ,
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
  public void sendVote(List<ElectionVote> votes, FragmentManager manager) {
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
      CastVote vote;
      // Construct the cast vote depending if the messages need to be encrypted or not
      if (election.getElectionVersion() == ElectionVersion.OPEN_BALLOT) {
        vote = new CastVote<>(votes, election.getId(), lao.getId());
      } else {
        List<ElectionEncryptedVote> encryptedVotes = election.encrypt(votes);
        vote = new CastVote<>(encryptedVotes, election.getId(), lao.getId());
        Toast.makeText(getApplication(), "Vote encrypted !", Toast.LENGTH_LONG).show();
      }
      Channel electionChannel = election.getChannel();
      Log.d(TAG, PUBLISH_MESSAGE);
      Disposable disposable =
          networkManager
              .getMessageSender()
              .publish(token, electionChannel, vote)
              .doFinally(
                  () ->
                      setCurrentFragment(
                          manager, R.id.fragment_lao_detail, LaoDetailFragment::newInstance))
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
   * @param manager the fragment manager on which to open any activity
   * @param electionVersion the version of the election
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
      FragmentManager manager,
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

    Lao lao = getCurrentLaoValue();
    if (lao == null) {
      Log.d(TAG, LAO_FAILURE_MESSAGE);
      return null;
    }

    Channel channel = lao.getChannel();
    ElectionSetup electionSetup =
        new ElectionSetup(
            writeIn,
            name,
            creation,
            start,
            end,
            votingMethod,
            lao.getId(),
            ballotOptions,
            question,
            electionVersion);

    Log.d(TAG, PUBLISH_MESSAGE);
    Disposable disposable =
        networkManager
            .getMessageSender()
            .publish(keyManager.getMainKeyPair(), channel, electionSetup)
            .subscribe(
                () -> {
                  Log.d(TAG, "setup an election");
                  setCurrentFragment(
                      manager, R.id.fragment_lao_detail, LaoDetailFragment::newInstance);
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
      FragmentActivity activity,
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
                    openRollCall(activity, createRollCall.getId());
                  } else {
                    setCurrentFragment(
                        activity.getSupportFragmentManager(),
                        R.id.fragment_lao_detail,
                        LaoDetailFragment::newInstance);
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
  public void openRollCall(FragmentActivity activity, String id) {
    Log.d(TAG, "call openRollCall with id" + id);
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
    OpenRollCall openRollCall =
        new OpenRollCall(laoId, id, openedAt, rollCall.getState().getValue());
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
                  currentRollCallId = openRollCall.getUpdateId();
                  Log.d(TAG, "opening rc with current id = " + currentRollCallId);
                  scanningAction = ScanningAction.ADD_ROLL_CALL_ATTENDEE;
                  openRollCallScanning(activity);
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
  public void closeRollCall(FragmentManager manager) {
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
                  setCurrentFragment(
                      manager, R.id.fragment_lao_detail, LaoDetailFragment::newInstance);
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

  /*
   * Getters for MutableLiveData instances declared above
   */

  public LiveData<LaoTab> getCurrentTab() {
    return currentTab;
  }

  public void setCurrentTab(LaoTab tab) {
    currentTab.postValue(tab);
  }

  public ScanningAction getScanningAction() {
    return scanningAction;
  }

  public LiveData<List<com.github.dedis.popstellar.model.objects.event.Event>> getLaoEvents() {
    return mLaoEvents;
  }

  public LiveData<List<RollCall>> getLaoAttendedRollCalls() {
    return mLaoAttendedRollCalls;
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

  public LiveData<Integer> getNbAttendees() {
    return mNbAttendees;
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

  public Election getCurrentElection() {
    return mCurrentElection.getValue();
  }

  public void setCurrentElection(Election e) {
    mCurrentElection.setValue(e);
  }

  public MutableLiveData<List<Integer>> getCurrentElectionVotes() {
    return mCurrentElectionVotes;
  }

  public RollCall getCurrentRollCall() {
    return mCurrentRollCall.getValue();
  }

  public void setCurrentRollCall(RollCall rc) {
    mCurrentRollCall.setValue(rc);
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

  public void endElectionEvent() {
    // TODO This is not implemented ?
  }

  public void setShowProperties(boolean show) {
    showProperties.postValue(show);
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
                  mIsOrganizer.setValue(isOrganizer);
                },
                error -> Log.d(TAG, "error updating LAO")));
  }

  public void enterRollCall(FragmentActivity activity, String id) {
    String firstLaoId = getCurrentLaoValue().getId();

    try {
      PublicKey publicKey = wallet.generatePoPToken(firstLaoId, id).getPublicKey();
      setCurrentFragment(
          activity.getSupportFragmentManager(),
          R.id.fragment_roll_call,
          () -> RollCallFragment.newInstance(publicKey));
    } catch (Exception e) {
      Log.d(TAG, "failed to retrieve public key from wallet", e);
    }
  }

  public boolean isWalletSetup() {
    return wallet.isSetUp();
  }

  public void openRollCallScanning(FragmentActivity activity) {
    FragmentManager manager = activity.getSupportFragmentManager();

    if (checkSelfPermission(activity.getApplicationContext(), Manifest.permission.CAMERA)
        == PackageManager.PERMISSION_GRANTED) {

      setCurrentFragment(manager, R.id.add_attendee_layout, QRCodeScanningFragment::new);
      // this to display the initial number of attendees
      mNbAttendees.postValue(attendees.size());
    } else {
      // Setup result listener to open the scanning tab once the permission is granted
      manager.setFragmentResultListener(
          CameraPermissionFragment.REQUEST_KEY, activity, (k, b) -> openRollCallScanning(activity));

      setCurrentFragment(
          manager,
          R.id.fragment_camera_perm,
          () -> CameraPermissionFragment.newInstance(activity.getActivityResultRegistry()));
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
    handleAttendeeAddition(barcode.rawValue);
  }

  @Override
  public boolean addManually(String data) {
    Log.d(TAG, "Key manually submitted with value: " + data);
    return handleAttendeeAddition(data);
  }

  /**
   * Checks the key validity and handles the attendee addition process
   *
   * @param data the textual representation of the key
   * @return true if an attendee was added false otherwise
   */
  private boolean handleAttendeeAddition(String data) {
    PublicKey attendee;
    try {
      attendee = new PublicKey(data);
    } catch (IllegalArgumentException e) {
      mScanWarningEvent.postValue(new SingleEvent<>("Invalid key format code. Please try again."));
      return false;
    }
    if (attendees.contains(attendee) || witnesses.contains(attendee)) {
      mScanWarningEvent.postValue(
          new SingleEvent<>("This attendee key has already been scanned. Please try again."));
      return false;
    }
    if (scanningAction == (ScanningAction.ADD_ROLL_CALL_ATTENDEE)) {
      attendees.add(attendee);
      mAttendeeScanConfirmEvent.postValue(new SingleEvent<>("Attendee has been added."));
      mNbAttendees.postValue(attendees.size());
    } else if (scanningAction == (ScanningAction.ADD_WITNESS)) {
      witnesses.add(attendee);
      mWitnessScanConfirmEvent.postValue(new SingleEvent<>(true));
      updateLaoWitnesses();
    }
    return true;
  }
}
