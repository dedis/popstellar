package com.github.dedis.popstellar.repository;

import android.app.Activity;
import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.Lifecycle;

import com.github.dedis.popstellar.model.objects.WitnessMessage;
import com.github.dedis.popstellar.model.objects.security.MessageID;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.repository.database.AppDatabase;
import com.github.dedis.popstellar.repository.database.witnessing.*;
import com.github.dedis.popstellar.utility.ActivityUtils;
import com.github.dedis.popstellar.utility.handler.data.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
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

/**
 * This class is the repository for witnesses and witness messages.
 *
 * <p>In particular, this repository:
 *
 * <ul>
 *   <li>Stores the public keys of the witnesses of a given LAO.
 *   <li>Stores the witness messages of a given LAO.
 *   <li>Stores the pending objects of a given LAO.
 *   <li>Process the pending objects when they are approved by "enough" witnesses
 * </ul>
 *
 * <em>- What is a pending object?</em><br>
 * When we receive a message which is subject to the witnessing policy (it has to be witnessed by a
 * given number of witnesses before being accepted), the underlying object of this message is called
 * pending object, as it cannot be directly added to the repository before being approved. This
 * represents a transient state, where the object waits to receive enough signature to be finally
 * processed and added to its repository. A pending object consists of the object itself plus the
 * message identifier from which it comes. This allows upon the reception of a witness signature to
 * know the pending object only from the message id.<br>
 * <em>- What does "enough" mean?</em><br>
 * The number of signatures to make a message valid (and consequently its relative pending object)
 * are established by the witnessing policy. This repository is in charge of checking the witnessing
 * policy through the method hasRequiredSignatures().<br>
 * <em>- What does processing a pending object mean?</em><br>
 * It means considering that object as valid, adding it to its own repository and doing other
 * processing job involved depending the object's type. <br>
 * <br>
 * <em>Example</em><br>
 * We receive an OpenRollCall, which contains the data of a RollCall object. We create an empty
 * witnessing message (i.e. with no witnesses) and add it to the repo. That RollCall object is also
 * put in the pending state. When we receive witnessing signatures matching the id of the
 * OpenRollCall message, the witnessing message is updated and each time we check if the witnessing
 * policy is passing. As soon as it passes, the pending RollCall is removed from the pending state
 * and processed. In the case of OpenRollCall processing means simply adding it to the
 * RollCallRepository. In some other cases other actions are performed depending on the object.
 */
@Singleton
public class WitnessingRepository {

  private static final String TAG = WitnessingRepository.class.getSimpleName();

  /** Constant used to decide the percentage of witness signatures required */
  private static final float WITNESSING_THRESHOLD = 2.0f / 3.0f;

  private final Map<String, LaoWitness> witnessByLao = new HashMap<>();

  private final WitnessingDao witnessingDao;
  private final WitnessDao witnessDao;
  private final PendingDao pendingDao;

  /**
   * Dependencies for other repositories, as they're needed to process the pending objects (roll
   * calls, meetings and elections) and insert those in their respective repos.
   */
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
   * This function deletes all the accepted messages (signed by the threshold of witnesses)
   *
   * @param laoId the id of the Lao
   */
  public void deleteSignedMessages(String laoId) {
    getLaoWitness(laoId).deleteAcceptedMessages();
  }

  /**
   * This function adds a pending entity.
   *
   * @param pendingEntity object (rollcall, election or meeting) that still needs to be approved by
   *     the witnesses
   */
  public void addPendingEntity(PendingEntity pendingEntity) {
    MessageID messageId = pendingEntity.getMessageID();
    // Persist the pending entity
    disposables.add(
        pendingDao
            .insert(pendingEntity)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                () ->
                    Timber.tag(TAG)
                        .d("Persisted the pending entity corresponding to message %s", messageId),
                err ->
                    Timber.tag(TAG)
                        .e(
                            err,
                            "Error in persisting the pending entity of message %s",
                            messageId)));
    // Add it to the memory for faster lookups
    getLaoWitness(pendingEntity.getLaoId()).addPendingEntity(pendingEntity);
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

    /** Thread-safe map to save pending entities by their message id */
    private final ConcurrentHashMap<MessageID, PendingEntity> pendingEntities =
        new ConcurrentHashMap<>();

    public LaoWitness(String laoId, WitnessingRepository repo) {
      this.laoId = laoId;
      this.repo = repo;
      loadFromDisk();
    }

    /**
     * This function adds (or replaces if there's already a witness message with the same id) a
     * WitnessMessage to the in-memory data structure and updates the subject containing the whole
     * collection of witness messages.
     *
     * @param witnessMessage the new witness message to add/replace
     */
    public void add(WitnessMessage witnessMessage) {
      MessageID messageID = witnessMessage.getMessageId();
      witnessMessages.put(messageID, witnessMessage);

      // Publish the updated collection in a thread-safe fashion
      witnessMessagesSubject
          .toSerialized()
          .onNext(unmodifiableList(new ArrayList<>(witnessMessages.values())));
    }

    /**
     * This adds a set of public keys representing the witnesses to a LAO. For now this is done only
     * at creation, as the witnesses set is not dynamic due to consensus missing.
     *
     * @param witnesses set of public keys representing the witnesses' public keys
     */
    public void addWitnesses(Set<PublicKey> witnesses) {
      this.witnesses.addAll(witnesses);
      // Publish the update collection in a thread-safe fashion
      witnessesSubject.toSerialized().onNext(unmodifiableSet(new HashSet<>(this.witnesses)));
    }

    /**
     * This function is triggered when a witness signs a given message, identified by the message
     * id, and adds such witness to the list of witnesses of that message.
     *
     * @param messageID identifier of the message signed by the witness
     * @param witness public key
     * @return false if there's no message matching the given id, true otherwise
     */
    public boolean addWitnessToMessage(MessageID messageID, PublicKey witness) {
      WitnessMessage witnessMessage = witnessMessages.get(messageID);
      if (witnessMessage == null) {
        return false;
      }
      // Add the witness to the witness message
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

      // Upon reception of a new signature check that the witnessing policy is passing.
      // The following function is designed to return true only once (when it just achieves the
      // minimum threshold).
      if (hasRequiredSignatures(witnessMessage)) {
        // Get and remove the pending entity
        PendingEntity pendingEntity = pendingEntities.remove(messageID);
        if (pendingEntity != null) {
          // Process the entity by calling its action: the pending object can be now be considered
          // valid and added to its repository
          processPendingEntity(pendingEntity);

          // Then delete asynchronously the pending entity from the db
          repo.disposables.add(
              repo.pendingDao
                  .removePendingObject(messageID)
                  .subscribeOn(Schedulers.io())
                  .observeOn(AndroidSchedulers.mainThread())
                  .subscribe(
                      () -> Timber.tag(TAG).d("Removed successfully pending object from disk"),
                      err ->
                          Timber.tag(TAG)
                              .e(err, "Error in removing the pending object from disk")));
        }
      }

      // Reinsert the witness message with the new witness to update the subject
      add(witnessMessage);
      return true;
    }

    /**
     * This checks whether a given public key is a witness in the LAO.
     *
     * @param witness public key to check
     * @return true if the given public key has a match in the set of witnesses, false otherwise
     */
    public boolean isWitness(PublicKey witness) {
      // It may happen that this check is scheduled before the db retrieval, so to be sure we
      // perform a disk lookup. Trivially, by the compiler optimizations, if the first check is true
      // then the lookup is avoided.
      return witnesses.contains(witness) || repo.witnessDao.isWitness(laoId, witness) != 0;
    }

    public boolean isWitnessEmpty() {
      return witnesses.isEmpty();
    }

    /**
     * This function checks whether a witness message has been signed by enough witnesses. The
     * "enough" is established by the witnessing policy, which for now is just an hardcoded
     * threshold. The function is designed to return true only once, at the achievement of the
     * signature threshold, not before nor after. This choice is due to the fact that the function
     * is called every time a new signature is added.
     *
     * @param witnessMessage witness message to check if it has enough signatures
     * @return true if the signatures number is exactly matching the threshold, false otherwise
     */
    public boolean hasRequiredSignatures(WitnessMessage witnessMessage) {
      if (witnessMessage == null) {
        return false;
      }

      // In the future more complex policies can be defined, even at lao creation the organizer
      // could specify its own, s.t. we have personalized policies across laos. For now (25.05.2023)
      // the very simple rule consists of having around 2/3 of witnesses signed the message.
      int requiredSignatures = Math.round(witnesses.size() * WITNESSING_THRESHOLD);

      // Check if it does match exactly the threshold (so it will return true just once)
      return witnessMessage.getWitnesses().size() == requiredSignatures;
    }

    /**
     * This function deletes the witness messages that have already been accepted by the witnessing
     * policy (i.e. they have enough signatures to be considered valid). Since they become useless,
     * the user has the possibility of deleting them. For now this is only triggered by the user, in
     * the future this deletion could be automatic.
     */
    public void deleteAcceptedMessages() {
      // Find the messages to delete
      Set<MessageID> idsToDelete =
          witnessMessages.values().stream()
              .filter(this::hasRequiredSignatures)
              .map(WitnessMessage::getMessageId)
              .collect(Collectors.toSet());

      // Delete from db asynchronously
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

      // Delete from memory
      idsToDelete.forEach(witnessMessages::remove);

      // Publish the updated collection
      witnessMessagesSubject
          .toSerialized()
          .onNext(unmodifiableList(new ArrayList<>(witnessMessages.values())));
    }

    public void addPendingEntity(PendingEntity pendingEntity) {
      pendingEntities.put(pendingEntity.getMessageID(), pendingEntity);
    }

    public Set<PublicKey> getWitnesses() {
      return new HashSet<>(witnesses);
    }

    public Observable<Set<PublicKey>> getWitnessesSubject() {
      return witnessesSubject;
    }

    public Observable<List<WitnessMessage>> getWitnessMessagesSubject() {
      return witnessMessagesSubject;
    }

    /**
     * This function executes the action to trigger based on the type of pending entity.
     *
     * @param pendingEntity object (for now rollcall, election or meeting) which has now achieved
     *     the witnessing threshold
     */
    private void processPendingEntity(PendingEntity pendingEntity) {
      switch (pendingEntity.getObjectType()) {
        case ROLL_CALL:
          RollCallHandler.addRollCallRoutine(
              repo.rollCallRepository,
              repo.digitalCashRepository,
              laoId,
              pendingEntity.getRollCall());
          break;
        case ELECTION:
          ElectionHandler.addElectionRoutine(repo.electionRepository, pendingEntity.getElection());
          break;
        case MEETING:
          MeetingHandler.addMeetingRoutine(
              repo.meetingRepository, laoId, pendingEntity.getMeeting());
          break;
        default:
      }
    }

    /** This function loads the lao witnessing state from the disk at creation of the lao */
    private void loadFromDisk() {
      if (alreadyRetrieved) {
        return;
      }
      repo.disposables.addAll(
          // Load in parallel all the witnesses
          repo.witnessDao
              .getWitnessesByLao(laoId)
              .subscribeOn(Schedulers.io())
              .observeOn(AndroidSchedulers.mainThread())
              .subscribe(
                  witnessList -> addWitnesses(new HashSet<>(witnessList)),
                  err -> Timber.tag(TAG).e(err, "No witness messages found on the disk")),
          // And all the witness messages of a given lao
          repo.witnessingDao
              .getWitnessMessagesByLao(laoId)
              .subscribeOn(Schedulers.io())
              .observeOn(AndroidSchedulers.mainThread())
              .subscribe(
                  witnessMessageList -> witnessMessageList.forEach(this::add),
                  err -> Timber.tag(TAG).e(err, "No witness messages found on the disk")),
          // And finally also load the pending entities from the db
          repo.pendingDao
              .getPendingObjectsFromLao(laoId)
              .subscribeOn(Schedulers.io())
              .observeOn(AndroidSchedulers.mainThread())
              .subscribe(
                  pendingEntityList -> pendingEntityList.forEach(this::addPendingEntity),
                  err -> Timber.tag(TAG).e(err, "No pending entity found on the disk")));

      alreadyRetrieved = true;
    }
  }
}
