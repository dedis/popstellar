package com.github.dedis.popstellar.repository;

import android.app.Activity;
import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.Lifecycle;

import com.github.dedis.popstellar.model.objects.*;
import com.github.dedis.popstellar.model.objects.security.MessageID;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.repository.database.AppDatabase;
import com.github.dedis.popstellar.repository.database.witnessing.*;
import com.github.dedis.popstellar.utility.ActivityUtils;
import com.github.dedis.popstellar.utility.error.UnknownWitnessMessageException;
import com.github.dedis.popstellar.utility.handler.data.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;
import timber.log.Timber;

import static java.util.Collections.*;

/** This class is the repository for witness messages */
@Singleton
public class WitnessingRepository {

  private static final String TAG = WitnessingRepository.class.getSimpleName();

  /** Constant used to decide the percentage of witness signatures required */
  private static final float WITNESSING_THRESHOLD = 2.0f / 3.0f;

  private final Map<String, LaoWitness> witnessByLao = new HashMap<>();

  private final WitnessingDao witnessingDao;
  private final WitnessDao witnessDao;
  private final PendingDao pendingDao;

  private final RollCallRepository rollCallRepository;
  private final ElectionRepository electionRepository;
  private final MeetingRepository meetingRepository;
  private final DigitalCashRepository digitalCashRepository;

  private final CompositeDisposable disposables = new CompositeDisposable();

  @Inject
  public WitnessingRepository(
      AppDatabase appDatabase,
      Application application,
      RollCallRepository rollCallRepository,
      ElectionRepository electionRepository,
      MeetingRepository meetingRepository,
      DigitalCashRepository digitalCashRepository) {
    this.rollCallRepository = rollCallRepository;
    this.electionRepository = electionRepository;
    this.meetingRepository = meetingRepository;
    this.digitalCashRepository = digitalCashRepository;
    witnessingDao = appDatabase.witnessingDao();
    witnessDao = appDatabase.witnessDao();
    pendingDao = appDatabase.pendingDao();
    Map<Lifecycle.Event, Consumer<Activity>> consumerMap = new EnumMap<>(Lifecycle.Event.class);
    consumerMap.put(Lifecycle.Event.ON_STOP, activity -> disposables.clear());
    application.registerActivityLifecycleCallbacks(
        ActivityUtils.buildLifecycleCallback(consumerMap));
  }

  /**
   * This adds to the repository a witness message. This is done at creation of a particular
   * message, such that the witnesses of the message are empty.
   *
   * @param laoId identifier of the lao where the message belongs
   * @param witnessMessage witness message containing the message id to sign
   */
  public void addWitnessMessage(String laoId, WitnessMessage witnessMessage) {
    Timber.tag(TAG).d("Adding a witness message on lao %s : %s", laoId, witnessMessage);

    // Persist the message
    WitnessingEntity witnessingEntity = new WitnessingEntity(laoId, witnessMessage);
    disposables.add(
        witnessingDao
            .insert(witnessingEntity)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                () ->
                    Timber.tag(TAG).d("Successfully persisted witness message %s", witnessMessage),
                err -> Timber.tag(TAG).e(err, "Error in persisting witness message")));

    // Retrieve Lao data and add the witness message to it
    getLaoWitness(laoId).add(witnessMessage);
  }

  public void addPendingRollcall(String laoId, MessageID messageID, RollCall rollCall) {
    addPendingEntity(messageID, new PendingEntity(laoId, messageID, rollCall));
  }

  public void addPendingElection(String laoId, MessageID messageID, Election election) {
    addPendingEntity(messageID, new PendingEntity(laoId, messageID, election));
  }

  public void addPendingMeeting(String laoId, MessageID messageID, Meeting meeting) {
    addPendingEntity(messageID, new PendingEntity(laoId, messageID, meeting));
  }

  /**
   * This adds a set of witnesses to a lao.
   *
   * @param laoId identifier of the lao
   * @param witnesses set of public keys of the witnesses to add
   */
  public void addWitnesses(String laoId, Set<PublicKey> witnesses) {
    Timber.tag(TAG).d("Adding witnesses to lao %s : %s", laoId, witnesses);

    // Persist all the witnesses at once
    List<WitnessEntity> witnessEntities =
        witnesses.stream().map(pk -> new WitnessEntity(laoId, pk)).collect(Collectors.toList());
    disposables.add(
        witnessDao
            .insertAll(witnessEntities)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                () -> Timber.tag(TAG).d("Successfully persisted witnesses %s", witnesses),
                err -> Timber.tag(TAG).e(err, "Error in persisting witnesses")));

    // Retrieve Lao data and add the witnesses to it
    getLaoWitness(laoId).addWitnesses(witnesses);
  }

  /**
   * This function adds a witness public key to a witness message contained in the repository when
   * such witness has correctly signed the message.
   *
   * @param laoId identifier of the lao where the message belongs
   * @param messageID id of the message signed
   * @param witness whose public key to add
   * @return true if it exists a witness message with that messageId, false otherwise
   */
  public boolean addWitnessToMessage(String laoId, MessageID messageID, PublicKey witness) {
    Timber.tag(TAG).d("Adding a witness to a witness message on lao %s : %s", laoId, messageID);
    // Retrieve Lao data and add the witness message to it
    return getLaoWitness(laoId).addWitnessToMessage(messageID, witness);
  }

  /**
   * It checks if a public keys belongs to the set of witnesses for a given lao.
   *
   * @param laoId lao identifier
   * @param witness public key to check
   * @return true if present, false otherwise
   */
  public boolean isWitness(String laoId, PublicKey witness) {
    return getLaoWitness(laoId).isWitness(witness);
  }

  /** Check whether the witness set is empty for a given lao. */
  public boolean areWitnessesEmpty(String laoId) {
    return getLaoWitness(laoId).isWitnessEmpty();
  }

  /**
   * This returns the set of public keys of the witnesses of a lao.
   *
   * @param laoId lao identifier whose witnesses are being retrieved
   * @return set of public keys of the witnesses
   */
  public Set<PublicKey> getWitnesses(String laoId) {
    return getLaoWitness(laoId).getWitnesses();
  }

  /**
   * Returns an observable over the set of witnesses in a lao.
   *
   * @param laoId the id of the Lao whose witnesses we want to observe
   * @return an observable over the set of public keys which corresponds to the set of witnesses on
   *     the given lao
   */
  public Observable<Set<PublicKey>> getWitnessesObservableInLao(String laoId) {
    return getLaoWitness(laoId).getWitnessesSubject();
  }

  /**
   * Returns an observable of the list of witness messages. Each time a witness is added to a
   * witness message the observable is notified with the updated list.
   *
   * @param laoId identifier of the lao whose witness messages are observed
   * @return an observable list of witness messages (message id and witnesses having signed it)
   */
  public Observable<List<WitnessMessage>> getWitnessMessagesObservableInLao(String laoId) {
    return getLaoWitness(laoId).getWitnessMessagesSubject();
  }

  /**
   * This function is the core function used in the different handlers for inserting a given item
   * (RollCall, Election, ...) and calling a certain routine code only after being sure that enough
   * witnesses have signed the relative message
   *
   * @param laoId identifier of the lao whose witness messages are observed
   * @param messageId id of the message to observe
   * @param action action to perform after having the witness policy satisfied
   * @throws UnknownWitnessMessageException if no witness message with that id is found
   */
  public void performActionWhenWitnessThresholdReached(
      String laoId, MessageID messageId, Runnable action) throws UnknownWitnessMessageException {
    // Special case : there's no witness, so the observable won't be updated, thus we need to check
    if (areWitnessesEmpty(laoId)) {
      action.run();
      return;
    }
    // Atomic bool triggered when the policy is satisfied.
    // It ensures the action is executed once only
    AtomicBoolean pass = new AtomicBoolean(false);
    disposables.add(
        getWitnessMessageObservableInLao(laoId, messageId)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                witnessMessage -> {
                  // Perform the action ONLY when the witnessing policy is satisfied (only once)
                  if (!pass.get() && isAcceptedByWitnesses(laoId, messageId)) {
                    Timber.tag(TAG)
                        .d(
                            "The message %s has received enough signatures and it's now processed",
                            messageId);
                    // Remove the pending object from disk
                    disposables.add(
                        pendingDao
                            .removePendingObject(messageId)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(
                                () ->
                                    Timber.tag(TAG)
                                        .d("Removed successfully pending object from disk"),
                                err ->
                                    Timber.tag(TAG)
                                        .e(err, "Error in removing the pending object from disk")));
                    action.run();
                    pass.set(true);
                  }
                },
                err ->
                    Timber.tag(TAG)
                        .e(
                            err,
                            "Error in observing the update of a witness message in lao %s",
                            laoId)));
  }

  /**
   * This function deletes all the accepted messages (signed by the threshold of witnesses)
   *
   * @param laoId the id of the Lao
   */
  public void deleteSignedMessages(String laoId) {
    getLaoWitness(laoId).deleteAcceptedMessages();
  }

  /**
   * This provides an observable of a witness message that triggers an update when modified.
   *
   * @param laoId the id of the Lao
   * @param messageID the id of the message
   * @return the observable wrapping the wanted witness message
   * @throws UnknownWitnessMessageException if no witness message with the provided id could be
   *     found
   */
  private Observable<WitnessMessage> getWitnessMessageObservableInLao(
      String laoId, MessageID messageID) throws UnknownWitnessMessageException {
    return getLaoWitness(laoId).getWitnessMessageObservable(messageID);
  }

  /**
   * This function implements the witnessing policy (i.e. number of witnesses' signatures required
   * to accept the message). It checks whether a given message can be accepted.
   *
   * @param messageId id of the message to check for validity
   * @return true if the signature threshold is reached, false otherwise
   */
  private boolean isAcceptedByWitnesses(String laoId, MessageID messageId) {
    return getLaoWitness(laoId).hasRequiredSignatures(messageId);
  }

  /**
   * This function adds to the database a pending entity.
   *
   * @param messageID identifier of the message to which the entity corresponds
   * @param pendingEntity object (rollcall, election or meeting) that still needs to be approved by
   *     the witnesses
   */
  private void addPendingEntity(MessageID messageID, PendingEntity pendingEntity) {
    disposables.add(
        pendingDao
            .insert(pendingEntity)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                () ->
                    Timber.tag(TAG)
                        .d("Persisted the pending entity corresponding to message %s", messageID),
                err ->
                    Timber.tag(TAG)
                        .e(
                            err,
                            "Error in persisting the pending entity of message %s",
                            messageID)));
  }

  /** Get in a thread-safe fashion the witness object for the lao, computes it if absent. */
  @NonNull
  private synchronized LaoWitness getLaoWitness(String laoId) {
    // Create the lao witness object if it is not present yet
    return witnessByLao.computeIfAbsent(laoId, lao -> new LaoWitness(laoId, this));
  }

  @VisibleForTesting
  public Optional<WitnessMessage> getWitnessMessage(String laoId, MessageID messageID) {
    return Optional.ofNullable(getLaoWitness(laoId).witnessMessages.get(messageID));
  }

  @VisibleForTesting
  public boolean areWitnessMessagesEmpty(String laoId) {
    return getLaoWitness(laoId).witnessMessages.isEmpty();
  }

  private static class LaoWitness {

    private final String laoId;
    private final WitnessingRepository repo;
    private boolean alreadyRetrieved = false;

    /** Thread-safe structure for saving the witnesses of a given lao */
    private final Set<PublicKey> witnesses = ConcurrentHashMap.newKeySet();

    /** Subject to observe the witnesses collection as a whole */
    private final Subject<Set<PublicKey>> witnessesSubject =
        BehaviorSubject.createDefault(unmodifiableSet(emptySet()));

    /** Thread-safe map to save witness messages by their ids */
    private final ConcurrentHashMap<MessageID, WitnessMessage> witnessMessages =
        new ConcurrentHashMap<>();

    /** Subject to observe the witness messages collection as a whole */
    private final Subject<List<WitnessMessage>> witnessMessagesSubject =
        BehaviorSubject.createDefault(unmodifiableList(emptyList()));

    /** Thread-safe map that assigns a subject to observe a specific witness message */
    private final ConcurrentHashMap<MessageID, Subject<WitnessMessage>> witnessMessageSubject =
        new ConcurrentHashMap<>();

    public LaoWitness(String laoId, WitnessingRepository repo) {
      this.laoId = laoId;
      this.repo = repo;
      loadFromDisk();
    }

    public void add(WitnessMessage witnessMessage) {
      MessageID messageID = witnessMessage.getMessageId();
      witnessMessages.put(messageID, witnessMessage);

      // Publish the new value in a thread-safe fashion
      Subject<WitnessMessage> subject = witnessMessageSubject.get(messageID);
      if (subject == null) {
        witnessMessageSubject.put(messageID, BehaviorSubject.createDefault(witnessMessage));
      } else {
        subject.toSerialized().onNext(witnessMessage);
      }

      // Publish the updated collection in a thread-safe fashion
      witnessMessagesSubject
          .toSerialized()
          .onNext(unmodifiableList(new ArrayList<>(witnessMessages.values())));
    }

    public void addWitnesses(Set<PublicKey> witnesses) {
      this.witnesses.addAll(witnesses);
      // Publish the update collection in a thread-safe fashion
      witnessesSubject.toSerialized().onNext(unmodifiableSet(new HashSet<>(this.witnesses)));
    }

    public boolean addWitnessToMessage(MessageID messageID, PublicKey witness) {
      WitnessMessage witnessMessage = witnessMessages.get(messageID);
      if (witnessMessage == null) {
        return false;
      }
      witnessMessage.addWitness(witness);

      // Persist the new message
      repo.disposables.add(
          repo.witnessingDao
              .insert(new WitnessingEntity(laoId, witnessMessage))
              .subscribeOn(Schedulers.io())
              .observeOn(AndroidSchedulers.mainThread())
              .subscribe(
                  () ->
                      Timber.tag(TAG)
                          .d("Successfully persisted witness message %s", witnessMessage),
                  err -> Timber.tag(TAG).e(err, "Error in persisting witness message")));

      // Reinsert the message to update the subjects and trigger the notifications
      add(witnessMessage);
      return true;
    }

    public boolean isWitness(PublicKey witness) {
      // Check if the witness is currently stored in memory. It may happen that this check
      // is scheduled before the db retrieval, so to be sure we perform a disk lookup
      // By the compiler optimizations, if the first check is true the lookup is avoided.
      return witnesses.contains(witness) || repo.witnessDao.isWitness(laoId, witness) != 0;
    }

    public boolean isWitnessEmpty() {
      return witnesses.isEmpty();
    }

    public boolean hasRequiredSignatures(MessageID messageId) {
      WitnessMessage witnessMessage = witnessMessages.get(messageId);
      if (witnessMessage == null) {
        return false;
      }

      // In the future more complex policies can be defined, even at lao creation the organizer
      // could specify its own, s.t. we have personalized policies across laos. For now (25.05.2023)
      // the very simple rule consists of having around 2/3 of witnesses signed the message.
      int requiredSignatures = Math.round(witnesses.size() * WITNESSING_THRESHOLD);

      return witnessMessage.getWitnesses().size() >= requiredSignatures;
    }

    public void deleteAcceptedMessages() {
      Set<MessageID> idsToDelete =
          witnessMessages.keySet().stream()
              .filter(this::hasRequiredSignatures)
              .collect(Collectors.toSet());

      // Delete from db
      repo.disposables.add(
          repo.witnessingDao
              .deleteMessagesByIds(laoId, idsToDelete)
              .subscribeOn(Schedulers.io())
              .observeOn(AndroidSchedulers.mainThread())
              .subscribe(
                  () ->
                      Timber.tag(TAG)
                          .d("Deleted accepted witness messages of lao %s from the disk", laoId),
                  err ->
                      Timber.tag(TAG).e(err, "Error deleting witness messages in lao %s", laoId)));

      // Delete them from memory
      idsToDelete.forEach(witnessMessages::remove);

      // Publish the updated collection
      witnessMessagesSubject
          .toSerialized()
          .onNext(unmodifiableList(new ArrayList<>(witnessMessages.values())));
    }

    public Set<PublicKey> getWitnesses() {
      return new HashSet<>(witnesses);
    }

    public Observable<Set<PublicKey>> getWitnessesSubject() {
      return witnessesSubject;
    }

    public Observable<WitnessMessage> getWitnessMessageObservable(MessageID messageID)
        throws UnknownWitnessMessageException {
      Observable<WitnessMessage> observable = witnessMessageSubject.get(messageID);
      if (observable == null) {
        throw new UnknownWitnessMessageException(messageID);
      } else {
        return observable;
      }
    }

    public Observable<List<WitnessMessage>> getWitnessMessagesSubject() {
      return witnessMessagesSubject;
    }

    /** This function loads the lao witnessing state from the disk at creation of the lao */
    private void loadFromDisk() {
      if (alreadyRetrieved) {
        return;
      }
      repo.disposables.add(
          // Load first all the witnesses
          repo.witnessDao
              .getWitnessesByLao(laoId)
              .subscribeOn(Schedulers.io())
              .observeOn(AndroidSchedulers.mainThread())
              .subscribe(
                  witnessList -> {
                    addWitnesses(new HashSet<>(witnessList));
                    // And then all the witness messages of a given lao
                    repo.disposables.add(
                        repo.witnessingDao
                            .getWitnessMessagesByLao(laoId)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(
                                witnessMessageList -> {
                                  witnessMessageList.forEach(this::add);
                                  // Finally, load the pending entities (the objects not already
                                  // achieving the witnessing threshold)
                                  repo.disposables.add(
                                      repo.pendingDao
                                          .getPendingObjectsFromLao(laoId)
                                          .subscribeOn(Schedulers.io())
                                          .observeOn(AndroidSchedulers.mainThread())
                                          .subscribe(
                                              pendingEntities ->
                                                  pendingEntities.forEach(this::loadPendingEntity),
                                              err ->
                                                  Timber.tag(TAG)
                                                      .e(
                                                          err,
                                                          "Error loading the pending entities from disk")));
                                },
                                err ->
                                    Timber.tag(TAG)
                                        .e(err, "No witness messages found on the disk")));
                  },
                  err -> Timber.tag(TAG).e(err, "No witnesses found on the disk")));
      alreadyRetrieved = true;
    }

    /**
     * This function loads a pending entity from disk into memory.
     *
     * @param pendingEntity object (for now rollcall, election or meeting) which hasn't been yet
     *     witnessed by the threshold established
     */
    private void loadPendingEntity(PendingEntity pendingEntity) {
      MessageID messageID = pendingEntity.getMessageID();
      final Runnable action;
      switch (pendingEntity.getObjectType()) {
        case ROLL_CALL:
          RollCall rollCall = pendingEntity.getRollCall();
          action =
              () ->
                  RollCallHandler.addRollCallRoutine(
                      repo.rollCallRepository, repo.digitalCashRepository, laoId, rollCall);
          break;
        case ELECTION:
          Election election = pendingEntity.getElection();
          action = () -> ElectionHandler.addElectionRoutine(repo.electionRepository, election);
          break;
        case MEETING:
          Meeting meeting = pendingEntity.getMeeting();
          action = () -> MeetingHandler.addMeetingRoutine(repo.meetingRepository, laoId, meeting);
          break;
        default:
          action = () -> {};
      }
      try {
        repo.performActionWhenWitnessThresholdReached(laoId, messageID, action);
      } catch (UnknownWitnessMessageException e) {
        Timber.tag(TAG).e(e, "Error loading pending entity %s", messageID);
      }
    }
  }
}
