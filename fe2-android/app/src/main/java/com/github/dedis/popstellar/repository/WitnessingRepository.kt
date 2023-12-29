package com.github.dedis.popstellar.repository

import android.app.Activity
import android.app.Application
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.Lifecycle
import com.github.dedis.popstellar.model.network.method.message.data.Objects
import com.github.dedis.popstellar.model.objects.WitnessMessage
import com.github.dedis.popstellar.model.objects.security.MessageID
import com.github.dedis.popstellar.model.objects.security.PublicKey
import com.github.dedis.popstellar.repository.database.AppDatabase
import com.github.dedis.popstellar.repository.database.witnessing.PendingDao
import com.github.dedis.popstellar.repository.database.witnessing.PendingEntity
import com.github.dedis.popstellar.repository.database.witnessing.WitnessDao
import com.github.dedis.popstellar.repository.database.witnessing.WitnessEntity
import com.github.dedis.popstellar.repository.database.witnessing.WitnessingDao
import com.github.dedis.popstellar.repository.database.witnessing.WitnessingEntity
import com.github.dedis.popstellar.utility.ActivityUtils.buildLifecycleCallback
import com.github.dedis.popstellar.utility.handler.data.ElectionHandler.Companion.addElectionRoutine
import com.github.dedis.popstellar.utility.handler.data.MeetingHandler.Companion.addMeetingRoutine
import com.github.dedis.popstellar.utility.handler.data.RollCallHandler.Companion.addRollCallRoutine
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import java.util.Collections
import java.util.EnumMap
import java.util.Optional
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Consumer
import java.util.stream.Collectors
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt
import timber.log.Timber

/**
 * This class is the repository for witnesses and witness messages.
 *
 * In particular, this repository:
 * * Stores the public keys of the witnesses of a given LAO.
 * * Stores the witness messages of a given LAO.
 * * Stores the pending objects of a given LAO.
 * * Process the pending objects when they are approved by "enough" witnesses
 *
 * *- What is a pending object?*<br></br> When we receive a message which is subject to the
 * witnessing policy (it has to be witnessed by a given number of witnesses before being accepted),
 * the underlying object of this message is called pending object, as it cannot be directly added to
 * the repository before being approved. This represents a transient state, where the object waits
 * to receive enough signature to be finally processed and added to its repository. A pending object
 * consists of the object itself plus the message identifier from which it comes. This allows upon
 * the reception of a witness signature to know the pending object only from the message
 * id.<br></br> *- What does "enough" mean?*<br></br> The number of signatures to make a message
 * valid (and consequently its relative pending object) are established by the witnessing policy.
 * This repository is in charge of checking the witnessing policy through the method
 * hasRequiredSignatures().<br></br> *- What does processing a pending object mean?*<br></br> It
 * means considering that object as valid, adding it to its own repository and doing other
 * processing job involved depending the object's type. <br></br> <br></br> *Example*<br></br> We
 * receive an OpenRollCall, which contains the data of a RollCall object. We create an empty
 * witnessing message (i.e. with no witnesses) and add it to the repo. That RollCall object is also
 * put in the pending state. When we receive witnessing signatures matching the id of the
 * OpenRollCall message, the witnessing message is updated and each time we check if the witnessing
 * policy is passing. As soon as it passes, the pending RollCall is removed from the pending state
 * and processed. In the case of OpenRollCall processing means simply adding it to the
 * RollCallRepository. In some other cases other actions are performed depending on the object.
 */
@Singleton
class WitnessingRepository
@Inject
constructor(
    appDatabase: AppDatabase,
    application: Application,
    /**
     * Dependencies for other repositories, as they're needed to process the pending objects (roll
     * calls, meetings and elections) and insert those in their respective repos.
     */
    private val rollCallRepository: RollCallRepository,
    private val electionRepository: ElectionRepository,
    private val meetingRepository: MeetingRepository,
    private val digitalCashRepository: DigitalCashRepository
) {
  private val witnessByLao: MutableMap<String, LaoWitness> = HashMap()
  private val witnessingDao: WitnessingDao
  private val witnessDao: WitnessDao
  private val pendingDao: PendingDao
  private val disposables = CompositeDisposable()

  init {
    witnessingDao = appDatabase.witnessingDao()
    witnessDao = appDatabase.witnessDao()
    pendingDao = appDatabase.pendingDao()
    val consumerMap: MutableMap<Lifecycle.Event, Consumer<Activity>> =
        EnumMap(Lifecycle.Event::class.java)
    consumerMap[Lifecycle.Event.ON_STOP] = Consumer { disposables.clear() }
    application.registerActivityLifecycleCallbacks(buildLifecycleCallback(consumerMap))
  }

  /**
   * This adds to the repository a witness message. This is done at creation of a particular
   * message, such that the witnesses of the message are empty.
   *
   * @param laoId identifier of the lao where the message belongs
   * @param witnessMessage witness message containing the message id to sign
   */
  fun addWitnessMessage(laoId: String, witnessMessage: WitnessMessage) {
    Timber.tag(TAG).d("Adding a witness message on lao %s : %s", laoId, witnessMessage)

    // Persist the message
    val witnessingEntity = WitnessingEntity(laoId, witnessMessage)
    disposables.add(
        witnessingDao
            .insert(witnessingEntity)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
              Timber.tag(TAG).d("Successfully persisted witness message %s", witnessMessage)
            }) { err: Throwable ->
              Timber.tag(TAG).e(err, "Error in persisting witness message")
            })

    // Retrieve Lao data and add the witness message to it
    getLaoWitness(laoId).add(witnessMessage)
  }

  /**
   * This adds a set of witnesses to a lao.
   *
   * @param laoId identifier of the lao
   * @param witnesses set of public keys of the witnesses to add
   */
  fun addWitnesses(laoId: String, witnesses: Set<PublicKey>) {
    Timber.tag(TAG).d("Adding witnesses to lao %s : %s", laoId, witnesses)

    // Persist all the witnesses at once
    val witnessEntities =
        witnesses
            .stream()
            .map { pk: PublicKey -> WitnessEntity(laoId, pk) }
            .collect(Collectors.toList())
    disposables.add(
        witnessDao
            .insertAll(witnessEntities)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ Timber.tag(TAG).d("Successfully persisted witnesses %s", witnesses) }) {
                err: Throwable ->
              Timber.tag(TAG).e(err, "Error in persisting witnesses")
            })

    // Retrieve Lao data and add the witnesses to it
    getLaoWitness(laoId).addWitnesses(witnesses)
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
  fun addWitnessToMessage(laoId: String, messageID: MessageID, witness: PublicKey): Boolean {
    Timber.tag(TAG).d("Adding a witness to a witness message on lao %s : %s", laoId, messageID)
    // Retrieve Lao data and add the witness message to it
    return getLaoWitness(laoId).addWitnessToMessage(messageID, witness)
  }

  /**
   * It checks if a public keys belongs to the set of witnesses for a given lao.
   *
   * @param laoId lao identifier
   * @param witness public key to check
   * @return true if present, false otherwise
   */
  fun isWitness(laoId: String, witness: PublicKey): Boolean {
    return getLaoWitness(laoId).isWitness(witness)
  }

  /** Check whether the witness set is empty for a given lao. */
  fun areWitnessesEmpty(laoId: String): Boolean {
    return getLaoWitness(laoId).isWitnessEmpty
  }

  /**
   * This returns the set of public keys of the witnesses of a lao.
   *
   * @param laoId lao identifier whose witnesses are being retrieved
   * @return set of public keys of the witnesses
   */
  fun getWitnesses(laoId: String): MutableSet<PublicKey> {
    return getLaoWitness(laoId).getWitnesses()
  }

  /**
   * Returns an observable over the set of witnesses in a lao.
   *
   * @param laoId the id of the Lao whose witnesses we want to observe
   * @return an observable over the set of public keys which corresponds to the set of witnesses on
   *   the given lao
   */
  fun getWitnessesObservableInLao(laoId: String): Observable<Set<PublicKey>> {
    return getLaoWitness(laoId).getWitnessesSubject()
  }

  /**
   * Returns an observable of the list of witness messages. Each time a witness is added to a
   * witness message the observable is notified with the updated list.
   *
   * @param laoId identifier of the lao whose witness messages are observed
   * @return an observable list of witness messages (message id and witnesses having signed it)
   */
  fun getWitnessMessagesObservableInLao(laoId: String): Observable<List<WitnessMessage>> {
    return getLaoWitness(laoId).getWitnessMessagesSubject()
  }

  /**
   * This function deletes all the accepted messages (signed by the threshold of witnesses)
   *
   * @param laoId the id of the Lao
   */
  fun deleteSignedMessages(laoId: String) {
    getLaoWitness(laoId).deleteAcceptedMessages()
  }

  /**
   * This function adds a pending entity.
   *
   * @param pendingEntity object (rollcall, election or meeting) that still needs to be approved by
   *   the witnesses
   */
  fun addPendingEntity(pendingEntity: PendingEntity) {
    val messageId = pendingEntity.messageID
    // Persist the pending entity
    disposables.add(
        pendingDao
            .insert(pendingEntity)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
              Timber.tag(TAG)
                  .d("Persisted the pending entity corresponding to message %s", messageId)
            }) { err: Throwable ->
              Timber.tag(TAG)
                  .e(err, "Error in persisting the pending entity of message %s", messageId)
            })
    // Add it to the memory for faster lookups
    getLaoWitness(pendingEntity.laoId).addPendingEntity(pendingEntity)
  }

  /** Get in a thread-safe fashion the witness object for the lao, computes it if absent. */
  @Synchronized
  private fun getLaoWitness(laoId: String): LaoWitness {
    // Create the lao witness object if it is not present yet
    return witnessByLao.computeIfAbsent(laoId) { LaoWitness(laoId, this) }
  }

  @VisibleForTesting
  fun getWitnessMessage(laoId: String, messageID: MessageID): Optional<WitnessMessage> {
    return Optional.ofNullable(getLaoWitness(laoId).witnessMessages[messageID])
  }

  @VisibleForTesting
  fun areWitnessMessagesEmpty(laoId: String): Boolean {
    return getLaoWitness(laoId).witnessMessages.isEmpty()
  }

  private class LaoWitness(private val laoId: String, private val repo: WitnessingRepository) {
    private var alreadyRetrieved = false

    /** Thread-safe structure for saving the witnesses of a given lao */
    private val witnesses: MutableSet<PublicKey> = ConcurrentHashMap.newKeySet()

    /** Subject to observe the witnesses collection as a whole */
    private val witnessesSubject: Subject<Set<PublicKey>> =
        BehaviorSubject.createDefault(Collections.unmodifiableSet(emptySet()))

    /** Thread-safe map to save witness messages by their ids */
    val witnessMessages = ConcurrentHashMap<MessageID, WitnessMessage>()

    /** Subject to observe the witness messages collection as a whole */
    private val witnessMessagesSubject: Subject<List<WitnessMessage>> =
        BehaviorSubject.createDefault(Collections.unmodifiableList(emptyList()))

    /** Thread-safe map to save pending entities by their message id */
    private val pendingEntities = ConcurrentHashMap<MessageID, PendingEntity>()

    init {
      loadFromDisk()
    }

    /**
     * This function adds (or replaces if there's already a witness message with the same id) a
     * WitnessMessage to the in-memory data structure and updates the subject containing the whole
     * collection of witness messages.
     *
     * @param witnessMessage the new witness message to add/replace
     */
    fun add(witnessMessage: WitnessMessage) {
      val messageID = witnessMessage.messageId
      witnessMessages[messageID] = witnessMessage

      // Publish the updated collection in a thread-safe fashion
      witnessMessagesSubject
          .toSerialized()
          .onNext(Collections.unmodifiableList(ArrayList(witnessMessages.values)))
    }

    /**
     * This adds a set of public keys representing the witnesses to a LAO. For now this is done only
     * at creation, as the witnesses set is not dynamic due to consensus missing.
     *
     * @param witnesses set of public keys representing the witnesses' public keys
     */
    fun addWitnesses(witnesses: Set<PublicKey>) {
      this.witnesses.addAll(witnesses)
      // Publish the update collection in a thread-safe fashion
      witnessesSubject.toSerialized().onNext(Collections.unmodifiableSet(HashSet(this.witnesses)))
    }

    /**
     * This function is triggered when a witness signs a given message, identified by the message
     * id, and adds such witness to the list of witnesses of that message.
     *
     * @param messageID identifier of the message signed by the witness
     * @param witness public key
     * @return false if there's no message matching the given id, true otherwise
     */
    fun addWitnessToMessage(messageID: MessageID, witness: PublicKey): Boolean {
      val witnessMessage = witnessMessages[messageID] ?: return false
      // Add the witness to the witness message
      witnessMessage.addWitness(witness)

      // Persist the new message
      repo.disposables.add(
          repo.witnessingDao
              .insert(WitnessingEntity(laoId, witnessMessage))
              .subscribeOn(Schedulers.io())
              .observeOn(AndroidSchedulers.mainThread())
              .subscribe({
                Timber.tag(TAG).d("Successfully persisted witness message %s", witnessMessage)
              }) { err: Throwable ->
                Timber.tag(TAG).e(err, "Error in persisting witness message")
              })

      // Upon reception of a new signature check that the witnessing policy is passing.
      // The following function is designed to return true only once (when it just achieves the
      // minimum threshold).
      if (hasRequiredSignatures(witnessMessage)) {
        // Get and remove the pending entity
        val pendingEntity = pendingEntities.remove(messageID)
        if (pendingEntity != null) {
          // Process the entity by calling its action: the pending object can be now be considered
          // valid and added to its repository
          processPendingEntity(pendingEntity)

          // Then delete asynchronously the pending entity from the db
          repo.disposables.add(
              repo.pendingDao
                  .removePendingObject(messageID)
                  .subscribeOn(Schedulers.io())
                  .observeOn(AndroidSchedulers.mainThread())
                  .subscribe({
                    Timber.tag(TAG).d("Removed successfully pending object from disk")
                  }) { err: Throwable ->
                    Timber.tag(TAG).e(err, "Error in removing the pending object from disk")
                  })
        }
      }

      // Reinsert the witness message with the new witness to update the subject
      add(witnessMessage)
      return true
    }

    /**
     * This checks whether a given public key is a witness in the LAO.
     *
     * @param witness public key to check
     * @return true if the given public key has a match in the set of witnesses, false otherwise
     */
    fun isWitness(witness: PublicKey): Boolean {
      // It may happen that this check is scheduled before the db retrieval, so to be sure we
      // perform a disk lookup. Trivially, by the compiler optimizations, if the first check is true
      // then the lookup is avoided.
      return witnesses.contains(witness) || repo.witnessDao.isWitness(laoId, witness) != 0
    }

    val isWitnessEmpty: Boolean
      get() = witnesses.isEmpty()

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
    fun hasRequiredSignatures(witnessMessage: WitnessMessage?): Boolean {
      if (witnessMessage == null) {
        return false
      }

      // In the future more complex policies can be defined, even at lao creation the organizer
      // could specify its own, s.t. we have personalized policies across laos. For now (25.05.2023)
      // the very simple rule consists of having around 2/3 of witnesses signed the message.
      val requiredSignatures = (witnesses.size * WITNESSING_THRESHOLD).roundToInt()

      // Check if it does match exactly the threshold (so it will return true just once)
      return witnessMessage.witnesses.size == requiredSignatures
    }

    /**
     * This function deletes the witness messages that have already been accepted by the witnessing
     * policy (i.e. they have enough signatures to be considered valid). Since they become useless,
     * the user has the possibility of deleting them. For now this is only triggered by the user, in
     * the future this deletion could be automatic.
     */
    fun deleteAcceptedMessages() {
      // Find the messages to delete
      val idsToDelete =
          witnessMessages.values
              .stream()
              .filter { witnessMessage: WitnessMessage? -> hasRequiredSignatures(witnessMessage) }
              .map { obj: WitnessMessage -> obj.messageId }
              .collect(Collectors.toSet())

      // Delete from db asynchronously
      repo.disposables.add(
          repo.witnessingDao
              .deleteMessagesByIds(laoId, idsToDelete)
              .subscribeOn(Schedulers.io())
              .observeOn(AndroidSchedulers.mainThread())
              .subscribe({
                Timber.tag(TAG)
                    .d("Deleted accepted witness messages of lao %s from the disk", laoId)
              }) { err: Throwable ->
                Timber.tag(TAG).e(err, "Error deleting witness messages in lao %s", laoId)
              })

      // Delete from memory
      idsToDelete.forEach(Consumer { key: MessageID? -> witnessMessages.remove(key) })

      // Publish the updated collection
      witnessMessagesSubject
          .toSerialized()
          .onNext(Collections.unmodifiableList(ArrayList(witnessMessages.values)))
    }

    fun addPendingEntity(pendingEntity: PendingEntity) {
      pendingEntities[pendingEntity.messageID] = pendingEntity
    }

    fun getWitnesses(): MutableSet<PublicKey> {
      return HashSet(witnesses)
    }

    fun getWitnessesSubject(): Observable<Set<PublicKey>> {
      return witnessesSubject
    }

    fun getWitnessMessagesSubject(): Observable<List<WitnessMessage>> {
      return witnessMessagesSubject
    }

    /**
     * This function executes the action to trigger based on the type of pending entity.
     *
     * @param pendingEntity object (for now rollcall, election or meeting) which has now achieved
     *   the witnessing threshold
     */
    private fun processPendingEntity(pendingEntity: PendingEntity) {
      when (pendingEntity.objectType) {
        Objects.ROLL_CALL ->
            addRollCallRoutine(
                repo.rollCallRepository,
                repo.digitalCashRepository,
                laoId,
                pendingEntity.rollCall!!)
        Objects.ELECTION -> addElectionRoutine(repo.electionRepository, pendingEntity.election!!)
        Objects.MEETING -> addMeetingRoutine(repo.meetingRepository, laoId, pendingEntity.meeting!!)
        else -> {}
      }
    }

    /** This function loads the lao witnessing state from the disk at creation of the lao */
    private fun loadFromDisk() {
      if (alreadyRetrieved) {
        return
      }
      repo.disposables.addAll( // Load in parallel all the witnesses
          repo.witnessDao
              .getWitnessesByLao(laoId)
              .subscribeOn(Schedulers.io())
              .observeOn(AndroidSchedulers.mainThread())
              .subscribe({ witnessList: List<PublicKey> -> addWitnesses(HashSet(witnessList)) }) {
                  err: Throwable ->
                Timber.tag(TAG).e(err, "No witness messages found on the disk")
              }, // And all the witness messages of a given lao
          repo.witnessingDao
              .getWitnessMessagesByLao(laoId)
              .subscribeOn(Schedulers.io())
              .observeOn(AndroidSchedulers.mainThread())
              .subscribe({ witnessMessageList: List<WitnessMessage> ->
                witnessMessageList.forEach(
                    Consumer { witnessMessage: WitnessMessage -> add(witnessMessage) })
              }) { err: Throwable ->
                Timber.tag(TAG).e(err, "No witness messages found on the disk")
              }, // And finally also load the pending entities from the db
          repo.pendingDao
              .getPendingObjectsFromLao(laoId)
              .subscribeOn(Schedulers.io())
              .observeOn(AndroidSchedulers.mainThread())
              .subscribe({ pendingEntityList: List<PendingEntity> ->
                pendingEntityList.forEach(
                    Consumer { pendingEntity: PendingEntity -> addPendingEntity(pendingEntity) })
              }) { err: Throwable ->
                Timber.tag(TAG).e(err, "No pending entity found on the disk")
              })
      alreadyRetrieved = true
    }
  }

  companion object {
    private val TAG = WitnessingRepository::class.java.simpleName

    /** Constant used to decide the percentage of witness signatures required */
    private const val WITNESSING_THRESHOLD = 2.0f / 3.0f
  }
}
