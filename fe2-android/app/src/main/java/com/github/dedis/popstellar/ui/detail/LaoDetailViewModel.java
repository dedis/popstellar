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
import com.github.dedis.popstellar.model.network.answer.Error;
import com.github.dedis.popstellar.model.network.answer.Result;
import com.github.dedis.popstellar.model.network.method.message.MessageGeneral;
import com.github.dedis.popstellar.model.network.method.message.data.consensus.ConsensusElect;
import com.github.dedis.popstellar.model.network.method.message.data.consensus.ConsensusElectAccept;
import com.github.dedis.popstellar.model.network.method.message.data.election.CastVote;
import com.github.dedis.popstellar.model.network.method.message.data.election.ElectionEnd;
import com.github.dedis.popstellar.model.network.method.message.data.election.ElectionSetup;
import com.github.dedis.popstellar.model.network.method.message.data.election.ElectionVote;
import com.github.dedis.popstellar.model.network.method.message.data.lao.StateLao;
import com.github.dedis.popstellar.model.network.method.message.data.lao.UpdateLao;
import com.github.dedis.popstellar.model.network.method.message.data.message.WitnessMessageSignature;
import com.github.dedis.popstellar.model.network.method.message.data.rollcall.CloseRollCall;
import com.github.dedis.popstellar.model.network.method.message.data.rollcall.CreateRollCall;
import com.github.dedis.popstellar.model.network.method.message.data.rollcall.OpenRollCall;
import com.github.dedis.popstellar.model.objects.Consensus;
import com.github.dedis.popstellar.model.objects.ConsensusNode;
import com.github.dedis.popstellar.model.objects.Election;
import com.github.dedis.popstellar.model.objects.Lao;
import com.github.dedis.popstellar.model.objects.RollCall;
import com.github.dedis.popstellar.model.objects.Wallet;
import com.github.dedis.popstellar.model.objects.WitnessMessage;
import com.github.dedis.popstellar.model.objects.event.EventState;
import com.github.dedis.popstellar.model.objects.event.EventType;
import com.github.dedis.popstellar.model.objects.security.KeyPair;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.model.objects.security.Signature;
import com.github.dedis.popstellar.repository.LAORepository;
import com.github.dedis.popstellar.ui.home.HomeViewModel;
import com.github.dedis.popstellar.ui.qrcode.CameraPermissionViewModel;
import com.github.dedis.popstellar.ui.qrcode.QRCodeScanningViewModel;
import com.github.dedis.popstellar.ui.qrcode.ScanningAction;
import com.github.dedis.popstellar.utility.security.KeyManager;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.gson.Gson;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
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
  private static final String KEYSET_HANDLE_FAILURE_MESSAGE = "failed to get public keyset handle";
  private static final String GET_PK_FAILURE = "failed to get public key";
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
  private final MutableLiveData<SingleEvent<Boolean>> mOpenLaoDetailEvent = new MutableLiveData<>();
  private final MutableLiveData<SingleEvent<EventType>> mChooseNewLaoEventTypeEvent =
      new MutableLiveData<>();
  private final MutableLiveData<SingleEvent<EventType>> mNewLaoEventCreationEvent =
      new MutableLiveData<>();
  private final MutableLiveData<SingleEvent<Boolean>> mOpenNewRollCallEvent =
      new MutableLiveData<>();
  private final MutableLiveData<SingleEvent<String>> mOpenRollCallEvent = new MutableLiveData<>();
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
  private final MutableLiveData<SingleEvent<String>> mOpenAddWitness = new MutableLiveData<>();
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
  private final LAORepository mLAORepository;
  private final KeyManager mKeyManager;
  private final CompositeDisposable disposables;
  private final Gson mGson;
  private final Wallet wallet;
  private String mCurrentRollCallId = ""; // used to know which roll call to close
  private Set<PublicKey> attendees = new HashSet<>();
  private Set<PublicKey> witnesses =
      new HashSet<>(); // used to dynamically update the set of witnesses when WR code scanned
  private ScanningAction scanningAction;

  @Inject
  public LaoDetailViewModel(
      @NonNull Application application,
      LAORepository laoRepository,
      KeyManager keyManager,
      Gson gson,
      Wallet wallet) {
    super(application);
    mLAORepository = laoRepository;
    mKeyManager = keyManager;
    mGson = gson;
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
    String firstLaoId = lao.getChannel().substring(6);
    try {
      PublicKey pk = wallet.findKeyPair(firstLaoId, rollcall.getPersistentId()).getPublicKey();
      return rollcall.getAttendees().contains(pk) || isOrganizer().getValue();
    } catch (GeneralSecurityException e) {
      Log.d(TAG, "failed to retrieve public key from wallet", e);
      return false;
    }
  }

  /**
   * Returns the public key or null if an error occurred.
   *
   * @return the public key
   */
  public PublicKey getPublicKey() {
    try {
      return mKeyManager.getMainPublicKey();
    } catch (GeneralSecurityException | IOException e) {
      Log.d(TAG, PK_FAILURE_MESSAGE, e);
      return null;
    }
  }

  @Override
  protected void onCleared() {
    super.onCleared();
    disposables.dispose();
  }

  public void endElection(Election election) {
    Log.d(TAG, "ending election with name : " + election.getName());
    Lao lao = getCurrentLaoValue();
    if (lao == null) {
      Log.d(TAG, LAO_FAILURE_MESSAGE);
      return;
    }

    String channel = election.getChannel();
    String laoId = lao.getChannel().substring(6); // removing /root/ prefix
    ElectionEnd electionEnd =
        new ElectionEnd(election.getId(), laoId, election.computerRegisteredVotes());

    try {
      MessageGeneral msg = new MessageGeneral(mKeyManager.getMainKeyPair(), electionEnd, mGson);

      Log.d(TAG, PUBLISH_MESSAGE);
      Disposable disposable =
          mLAORepository
              .sendPublish(channel, msg)
              .subscribeOn(Schedulers.io())
              .observeOn(AndroidSchedulers.mainThread())
              .timeout(5, TimeUnit.SECONDS)
              .subscribe(
                  answer -> {
                    if (answer instanceof Result) {
                      Log.d(TAG, "ended election successfully");
                      endElectionEvent();
                    } else {
                      Log.d(TAG, "failed to end the election");
                    }
                  },
                  throwable ->
                      Log.d(TAG, "timed out waiting for result on election/end", throwable));

      disposables.add(disposable);
    } catch (GeneralSecurityException | IOException e) {
      Log.d(TAG, PK_FAILURE_MESSAGE, e);
    }
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

    String laoChannel = lao.getChannel();
    String laoId = laoChannel.substring(6);
    CastVote castVote = new CastVote(votes, election.getId(), laoId);
    // Is channel set ?
    String electionChannel = election.getChannel();

    try {
      MessageGeneral msg = new MessageGeneral(mKeyManager.getMainKeyPair(), castVote, mGson);

      Log.d(TAG, PUBLISH_MESSAGE);
      Disposable disposable =
          mLAORepository
              .sendPublish(electionChannel, msg)
              .subscribeOn(Schedulers.io())
              .observeOn(AndroidSchedulers.mainThread())
              .timeout(5, TimeUnit.SECONDS)
              .subscribe(
                  answer -> {
                    if (answer instanceof Result) {
                      Log.d(TAG, "sent a vote successfully");
                      // Toast ? + send back to election screen or details screen ?
                      Toast.makeText(
                              getApplication(), "vote successfully sent !", Toast.LENGTH_LONG)
                          .show();
                    } else {
                      Log.d(TAG, "failed to send the vote");
                      Toast.makeText(
                              getApplication(), "vote was sent too late !", Toast.LENGTH_LONG)
                          .show();
                    }
                    openLaoDetail();
                  },
                  throwable -> Log.d(TAG, "timed out waiting for result on cast_vote", throwable));

      disposables.add(disposable);
    } catch (GeneralSecurityException | IOException e) {
      Log.d(TAG, PK_FAILURE_MESSAGE, e);
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

    String channel = lao.getChannel();
    ElectionSetup electionSetup;
    String laoId = channel.substring(6);

    electionSetup =
        new ElectionSetup(
            name, creation, start, end, votingMethod, writeIn, ballotOptions, question, laoId);

    try {
      // Retrieve identity of who is creating the election
      MessageGeneral msg = new MessageGeneral(mKeyManager.getMainKeyPair(), electionSetup, mGson);

      Log.d(TAG, PUBLISH_MESSAGE);
      Disposable disposable =
          mLAORepository
              .sendPublish(channel, msg)
              .subscribeOn(Schedulers.io())
              .observeOn(AndroidSchedulers.mainThread())
              .timeout(5, TimeUnit.SECONDS)
              .subscribe(
                  answer -> {
                    if (answer instanceof Result) {
                      Log.d(TAG, "setup an election");
                      mElectionCreatedEvent.postValue(new SingleEvent<>(true));
                    } else if (answer instanceof Error) {
                      Log.d(
                          TAG,
                          "failed to setup an election because of the following error : "
                              + ((Error) answer).getError().getDescription());
                    } else {
                      Log.d(TAG, "failed to setup an election");
                    }
                  },
                  throwable ->
                      Log.d(TAG, "timed out waiting for result on election/create", throwable));

      disposables.add(disposable);

    } catch (GeneralSecurityException | IOException e) {
      Log.d(TAG, PK_FAILURE_MESSAGE, e);
      return null;
    }
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
    String channel = lao.getChannel();
    CreateRollCall createRollCall;
    String laoId = channel.substring(6); // removing /root/ prefix
    createRollCall =
        new CreateRollCall(
            title, creation, proposedStart, proposedEnd, "Lausanne", description, laoId);

    try {
      Log.d(TAG, PUBLISH_MESSAGE);
      MessageGeneral msg = new MessageGeneral(mKeyManager.getMainKeyPair(), createRollCall, mGson);
      Disposable disposable =
          mLAORepository
              .sendPublish(channel, msg)
              .subscribeOn(Schedulers.io())
              .observeOn(AndroidSchedulers.mainThread())
              .timeout(5, TimeUnit.SECONDS)
              .subscribe(
                  answer -> {
                    if (answer instanceof Result) {
                      Log.d(TAG, "created a roll call with id: " + createRollCall.getId());
                      if (open) {
                        openRollCall(createRollCall.getId());
                      } else {
                        mCreatedRollCallEvent.postValue(new SingleEvent<>(true));
                      }
                    } else {
                      Log.d(TAG, "failed to create a roll call");
                    }
                  },
                  throwable ->
                      Log.d(TAG, "timed out waiting for result on roll_call/create", throwable));
      disposables.add(disposable);
    } catch (GeneralSecurityException | IOException e) {
      Log.d(TAG, PK_FAILURE_MESSAGE, e);
    }
  }

  /**
   * Creates new consensus.
   *
   * <p>Publish a GeneralMessage containing ConsensusElect data.
   *
   * @param creation the creation time of the consensus
   * @param objId the id of the object the consensus refers to (e.g. election_id)
   * @param type the type of object the consensus refers to (e.g. election)
   * @param property the property the value refers to (e.g. "state")
   * @param value the proposed new value for the property (e.g. "started")
   */
  public void createNewConsensus(
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
    String channel = lao.getChannel() + "/consensus";

    ConsensusElect consensusElect = new ConsensusElect(creation, objId, type, property, value);

    try {
      Log.d(TAG, PUBLISH_MESSAGE);
      MessageGeneral msg = new MessageGeneral(mKeyManager.getMainKeyPair(), consensusElect, mGson);

      Disposable disposable =
          mLAORepository
              .sendPublish(channel, msg)
              .subscribeOn(Schedulers.io())
              .observeOn(AndroidSchedulers.mainThread())
              .timeout(5, TimeUnit.SECONDS)
              .subscribe(
                  answer -> {
                    if (answer instanceof Result) {
                      Log.d(TAG, "created a consensus with messageId: " + msg.getMessageId());
                    } else {
                      Log.d(TAG, "failed to create a consensus");
                    }
                  },
                  throwable ->
                      Log.d(TAG, "timed out waiting for result on consensus/elect", throwable));
      disposables.add(disposable);
    } catch (GeneralSecurityException | IOException e) {
      Log.d(TAG, PK_FAILURE_MESSAGE, e);
    }
  }

  /**
   * Sends an ConsensusElectAccept message.
   *
   * <p>Publish a GeneralMessage containing ConsensusElectAccept data.
   *
   * @param consensus the corresponding consensus
   * @param accept true if accepted, false if rejected
   */
  public void sendConsensusElectAccept(Consensus consensus, boolean accept) {
    Log.d(
        TAG,
        "sending a new elect_accept for consensus with messageId : "
            + consensus.getMessageId()
            + " with value "
            + accept);

    Lao lao = getCurrentLaoValue();
    if (lao == null) {
      Log.d(TAG, LAO_FAILURE_MESSAGE);
      return;
    }

    ConsensusElectAccept consensusElectAccept =
        new ConsensusElectAccept(consensus.getId(), consensus.getMessageId(), accept);

    try {
      MessageGeneral msg =
          new MessageGeneral(mKeyManager.getMainKeyPair(), consensusElectAccept, mGson);

      Log.d(TAG, PUBLISH_MESSAGE);
      Disposable disposable =
          mLAORepository
              .sendPublish(consensus.getChannel(), msg)
              .subscribeOn(Schedulers.io())
              .observeOn(AndroidSchedulers.mainThread())
              .timeout(5, TimeUnit.SECONDS)
              .subscribe(
                  answer -> {
                    if (answer instanceof Result) {
                      Log.d(TAG, "sent an elect_accept successfully");
                    } else {
                      Log.d(
                          TAG,
                          "failed to send the elect_accept for consensus with messageId : "
                              + consensus.getMessageId());
                    }
                  },
                  throwable ->
                      Log.d(
                          TAG,
                          "timed out waiting for result on consensus/elect_accept",
                          throwable));

      disposables.add(disposable);
    } catch (GeneralSecurityException | IOException e) {
      Log.d(TAG, PK_FAILURE_MESSAGE, e);
    }
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
    String channel = lao.getChannel();
    String laoId = channel.substring(6); // removing /root/ prefix
    Optional<RollCall> optRollCall = lao.getRollCall(id);
    if (!optRollCall.isPresent()) {
      Log.d(TAG, "failed to retrieve roll call with id " + id + "laoID: " + laoId);
      return;
    }
    RollCall rollCall = optRollCall.get();
    OpenRollCall openRollCall = new OpenRollCall(laoId, id, openedAt, rollCall.getState());
    attendees = new HashSet<>(rollCall.getAttendees());

    try {
      MessageGeneral msg = new MessageGeneral(mKeyManager.getMainKeyPair(), openRollCall, mGson);
      Disposable disposable =
          mLAORepository
              .sendPublish(channel, msg)
              .subscribeOn(Schedulers.io())
              .observeOn(AndroidSchedulers.mainThread())
              .timeout(5, TimeUnit.SECONDS)
              .subscribe(
                  answer -> {
                    if (answer instanceof Result) {
                      Log.d(TAG, "opened the roll call");
                      mCurrentRollCallId = openRollCall.getUpdateId();
                      scanningAction = ScanningAction.ADD_ROLL_CALL_ATTENDEE;
                      openScanning();
                    } else {
                      Log.d(TAG, "failed to open the roll call");
                    }
                  },
                  throwable ->
                      Log.d(TAG, "timed out waiting for result on roll_call/open", throwable));
      disposables.add(disposable);
    } catch (GeneralSecurityException | IOException e) {
      Log.d(TAG, PK_FAILURE_MESSAGE, e);
    }
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
    String channel = lao.getChannel();
    String laoId = channel.substring(6); // removing /root/ prefix
    CloseRollCall closeRollCall =
        new CloseRollCall(laoId, mCurrentRollCallId, end, new ArrayList<>(attendees));
    try {
      MessageGeneral msg = new MessageGeneral(mKeyManager.getMainKeyPair(), closeRollCall, mGson);
      Disposable disposable =
          mLAORepository
              .sendPublish(channel, msg)
              .subscribeOn(Schedulers.io())
              .observeOn(AndroidSchedulers.mainThread())
              .timeout(5, TimeUnit.SECONDS)
              .subscribe(
                  answer -> {
                    if (answer instanceof Result) {
                      Log.d(TAG, "closed the roll call");
                      mCurrentRollCallId = "";
                      attendees.clear();
                      mCloseRollCallEvent.setValue(new SingleEvent<>(nextFragment));
                    } else {
                      Log.d(TAG, "failed to close the roll call");
                    }
                  },
                  throwable ->
                      Log.d(TAG, "timed out waiting for result on roll_call/open", throwable));
      disposables.add(disposable);
    } catch (GeneralSecurityException | IOException e) {
      Log.d(TAG, PK_FAILURE_MESSAGE, e);
    }
  }

  public void signMessage(WitnessMessage witnessMessage) {
    Log.d(TAG, "signing message with ID " + witnessMessage.getMessageId());
    Lao lao = getCurrentLaoValue();
    if (lao == null) {
      Log.d(TAG, LAO_FAILURE_MESSAGE);
      return;
    }

    String channel = lao.getChannel();

    try {
      KeyPair mainKey = mKeyManager.getMainKeyPair();
      // generate the signature of the message
      Signature signature = mainKey.sign(witnessMessage.getMessageId());

      Log.d(TAG, PUBLISH_MESSAGE);
      WitnessMessageSignature signatureMessage =
          new WitnessMessageSignature(witnessMessage.getMessageId(), signature);
      MessageGeneral msg = new MessageGeneral(mainKey, signatureMessage, mGson);
      disposables.add(
          mLAORepository
              .sendPublish(channel, msg)
              .subscribeOn(Schedulers.io())
              .observeOn(AndroidSchedulers.mainThread())
              .timeout(5, TimeUnit.SECONDS)
              .subscribe(
                  answer -> {
                    if (answer instanceof Result) {
                      Log.d(
                          TAG,
                          "Verifying the signature of  message  with id: "
                              + witnessMessage.getMessageId());

                    } else {
                      Log.d(TAG, "failed to sign message ");
                    }
                  },
                  throwable ->
                      Log.d(TAG, "timed out waiting for result on sign message", throwable)));

    } catch (GeneralSecurityException | IOException e) {
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
    try {
      boolean isWitness =
          getCurrentLaoValue().getWitnesses().contains(mKeyManager.getMainPublicKey());
      Log.d(TAG, "isWitness: " + isWitness);
      mIsWitness.setValue(isWitness);
      return mIsWitness;

    } catch (GeneralSecurityException e) {
      Log.d(TAG, KEYSET_HANDLE_FAILURE_MESSAGE, e);
    } catch (IOException e) {
      Log.d(TAG, GET_PK_FAILURE, e);
    }
    mIsWitness.setValue(false);
    return mIsWitness;
  }

  public LiveData<Boolean> isSignedByCurrentWitness(Set<PublicKey> witnesses) {
    try {
      boolean isSignedByCurrentWitness = witnesses.contains(mKeyManager.getMainPublicKey());
      Log.d(TAG, "isSignedByCurrentWitness: " + isSignedByCurrentWitness);
      mIsSignedByCurrentWitness.setValue(isSignedByCurrentWitness);
      return mIsSignedByCurrentWitness;
    } catch (GeneralSecurityException e) {
      Log.d(TAG, KEYSET_HANDLE_FAILURE_MESSAGE, e);
    } catch (IOException e) {
      Log.d(TAG, GET_PK_FAILURE, e);
    }
    mIsSignedByCurrentWitness.setValue(false);
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

  public LiveData<SingleEvent<String>> getOpenRollCallEvent() {
    return mOpenRollCallEvent;
  }

  public LiveData<SingleEvent<String>> getOpenAddWitness() {
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
        mLAORepository
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
    if (mCurrentRollCallId.equals("")) {
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
    if (mCurrentRollCallId.equals("")) {
      PublicKey publicKey = null;
      try {
        publicKey = mKeyManager.getMainPublicKey();
      } catch (GeneralSecurityException | IOException e) {
        Log.d(TAG, PK_FAILURE_MESSAGE, e);
      }

      mOpenIdentityEvent.setValue(new SingleEvent<>(publicKey));
    } else {
      mAskCloseRollCallEvent.setValue(new SingleEvent<>(R.id.fragment_identity));
    }
  }

  public void openSocialMedia() {
    mOpenSocialMediaEvent.setValue(new SingleEvent<>(true));
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
    mOpenAddWitness.setValue(new SingleEvent<>(HomeViewModel.SCAN));
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
    String channel = lao.getChannel();
    try {
      KeyPair mainKey = mKeyManager.getMainKeyPair();
      long now = Instant.now().getEpochSecond();
      UpdateLao updateLao =
          new UpdateLao(
              mainKey.getPublicKey(),
              lao.getCreation(),
              mLaoName.getValue(),
              now,
              lao.getWitnesses());
      MessageGeneral msg = new MessageGeneral(mainKey, updateLao, mGson);
      Disposable disposable =
          mLAORepository
              .sendPublish(channel, msg)
              .subscribeOn(Schedulers.io())
              .observeOn(AndroidSchedulers.mainThread())
              .timeout(5, TimeUnit.SECONDS)
              .subscribe(
                  answer -> {
                    if (answer instanceof Result) {
                      Log.d(TAG, "updated lao name");
                      dipatchLaoUpdate("lao name", updateLao, lao, channel, msg);
                    } else {
                      Log.d(TAG, "failed to update lao name");
                    }
                  },
                  throwable ->
                      Log.d(TAG, "timed out waiting for result on update lao name", throwable));
      disposables.add(disposable);
    } catch (GeneralSecurityException | IOException e) {
      Log.d(TAG, PK_FAILURE_MESSAGE, e);
    }
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
    String channel = lao.getChannel();
    try {
      KeyPair mainKey = mKeyManager.getMainKeyPair();
      long now = Instant.now().getEpochSecond();
      UpdateLao updateLao =
          new UpdateLao(mainKey.getPublicKey(), lao.getCreation(), lao.getName(), now, witnesses);
      MessageGeneral msg = new MessageGeneral(mainKey, updateLao, mGson);
      Disposable disposable =
          mLAORepository
              .sendPublish(channel, msg)
              .subscribeOn(Schedulers.io())
              .observeOn(AndroidSchedulers.mainThread())
              .timeout(5, TimeUnit.SECONDS)
              .subscribe(
                  answer -> {
                    if (answer instanceof Result) {
                      Log.d(TAG, "updated lao witnesses");
                      dipatchLaoUpdate(
                          "lao state with new witnesses", updateLao, lao, channel, msg);
                    } else {
                      Log.d(TAG, "failed to update lao witnesses");
                    }
                  },
                  throwable ->
                      Log.d(
                          TAG, "timed out waiting for result on update lao witnesses", throwable));
      disposables.add(disposable);
    } catch (GeneralSecurityException | IOException e) {
      Log.d(TAG, PK_FAILURE_MESSAGE, e);
    }
  }

  /** Helper method for updateLaoWitnesses and updateLaoName to send a stateLao message */
  private void dipatchLaoUpdate(
      String desc, UpdateLao updateLao, Lao lao, String channel, MessageGeneral msg) {
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
    try {
      MessageGeneral stateMsg = new MessageGeneral(mKeyManager.getMainKeyPair(), stateLao, mGson);
      disposables.add(
          mLAORepository
              .sendPublish(channel, stateMsg)
              .subscribeOn(Schedulers.io())
              .observeOn(AndroidSchedulers.mainThread())
              .timeout(5, TimeUnit.SECONDS)
              .subscribe(
                  answer2 -> {
                    if (answer2 instanceof Result) {
                      Log.d(TAG, "updated " + desc);
                    } else {
                      Log.d(TAG, "failed to update " + desc);
                    }
                  },
                  throwable ->
                      Log.d(TAG, "timed out waiting for result on update " + desc, throwable)));
    } catch (IOException | GeneralSecurityException e) {
      Log.d(TAG, PK_FAILURE_MESSAGE, e);
    }
  }

  public void cancelEdit() {
    mLaoName.setValue("");
    closeEditProperties();
  }

  public void subscribeToLao(String laoId) {
    disposables.add(
        mLAORepository
            .getLaoObservable(laoId)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                lao -> {
                  Log.d(TAG, "got an update for lao: " + lao.getName());
                  mCurrentLao.postValue(lao);
                  try {
                    boolean isOrganizer = lao.getOrganizer().equals(mKeyManager.getMainPublicKey());
                    Log.d(TAG, "isOrganizer: " + isOrganizer);
                    mIsOrganizer.setValue(isOrganizer);
                    return;
                  } catch (GeneralSecurityException e) {
                    Log.d(TAG, KEYSET_HANDLE_FAILURE_MESSAGE, e);
                  } catch (IOException e) {
                    Log.d(TAG, GET_PK_FAILURE, e);
                  }
                  mIsOrganizer.setValue(false);
                }));
  }

  public void openQrCodeScanningRollCall() {
    mOpenRollCallEvent.setValue(new SingleEvent<>(HomeViewModel.SCAN));
    mNbAttendeesEvent.postValue(
        new SingleEvent<>(attendees.size())); // this to display the initial number of attendees
  }

  public void openCameraPermission() {
    if (scanningAction == ScanningAction.ADD_ROLL_CALL_ATTENDEE) {
      mOpenRollCallEvent.setValue(new SingleEvent<>(HomeViewModel.REQUEST_CAMERA_PERMISSION));
    } else if (scanningAction == ScanningAction.ADD_WITNESS) {
      mOpenAddWitness.setValue(new SingleEvent<>(HomeViewModel.REQUEST_CAMERA_PERMISSION));
    }
  }

  public void enterRollCall(String id) {
    if (!wallet.isSetUp()) {
      mWalletMessageEvent.setValue(new SingleEvent<>(true));
      return;
    }
    String firstLaoId =
        getCurrentLaoValue()
            .getChannel()
            .substring(6); // use the laoId set at creation + need to remove /root/ prefix
    String errorMessage = "failed to retrieve public key from wallet";
    try {
      PublicKey publicKey = wallet.findKeyPair(firstLaoId, id).getPublicKey();
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

  public void addAttendee(PublicKey attendee) {
    if (attendees.contains(attendee)) {
      mScanWarningEvent.postValue(
          new SingleEvent<>("This QR code has already been scanned. Please try again."));
      return;
    }

    attendees.add(attendee);
    mAttendeeScanConfirmEvent.postValue(new SingleEvent<>("Attendee has been added."));
    mNbAttendeesEvent.postValue(new SingleEvent<>(attendees.size()));
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
      addAttendee(attendee);
    } else if (scanningAction == (ScanningAction.ADD_WITNESS)) {
      witnesses.add(attendee);
      mWitnessScanConfirmEvent.postValue(new SingleEvent<>(true));
      updateLaoWitnesses();
    }
  }
}
