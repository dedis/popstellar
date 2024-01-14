package com.github.dedis.popstellar.repository

import android.app.Activity
import android.app.Application
import androidx.lifecycle.Lifecycle
import com.github.dedis.popstellar.model.objects.RollCall
import com.github.dedis.popstellar.model.objects.security.PublicKey
import com.github.dedis.popstellar.repository.database.AppDatabase
import com.github.dedis.popstellar.repository.database.event.rollcall.RollCallDao
import com.github.dedis.popstellar.repository.database.event.rollcall.RollCallEntity
import com.github.dedis.popstellar.utility.GeneralUtils.buildLifecycleCallback
import com.github.dedis.popstellar.utility.error.UnknownRollCallException
import com.github.dedis.popstellar.utility.error.keys.NoRollCallException
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import java.util.Collections
import java.util.EnumMap
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Consumer
import java.util.stream.Collectors
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

/**
 * This class is the repository of the roll call events
 *
 * Its main purpose is to store roll calls and publish updates
 */
@Singleton
class RollCallRepository @Inject constructor(appDatabase: AppDatabase, application: Application) {
  private val rollCallsByLao: MutableMap<String, LaoRollCalls> = HashMap()
  private val rollCallDao: RollCallDao
  private val disposables = CompositeDisposable()

  init {
    rollCallDao = appDatabase.rollCallDao()

    val consumerMap: MutableMap<Lifecycle.Event, Consumer<Activity>> =
        EnumMap(Lifecycle.Event::class.java)
    consumerMap[Lifecycle.Event.ON_STOP] = Consumer { disposables.clear() }
    application.registerActivityLifecycleCallbacks(buildLifecycleCallback(consumerMap))
  }

  /**
   * This either updates the roll call in the repository or adds it if absent.
   *
   * @param rollCall the roll call to update/add
   */
  fun updateRollCall(laoId: String, rollCall: RollCall) {
    Timber.tag(TAG).d("Updating roll call on lao %s : %s", laoId, rollCall)

    // Persist the rollcall
    disposables.add(
        rollCallDao
            .insert(RollCallEntity(laoId, rollCall))
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { Timber.tag(TAG).d("Successfully persisted rollcall %s", rollCall.persistentId) },
                { err: Throwable ->
                  Timber.tag(TAG).e(err, "Error in persisting rollcall %s", rollCall.persistentId)
                }))

    // Retrieve Lao data and add the roll call to it
    getLaoRollCalls(laoId).update(rollCall)
  }

  /**
   * This provides an observable of a roll call that triggers an update when modified
   *
   * @param laoId the id of the Lao
   * @param persistentId the persistent id of the roll call
   * @return the observable wrapping the wanted roll call
   * @throws UnknownRollCallException if no roll call with the provided id could be found
   */
  @Throws(UnknownRollCallException::class)
  fun getRollCallObservable(laoId: String, persistentId: String): Observable<RollCall> {
    return getLaoRollCalls(laoId).getRollCallObservable(persistentId)
  }

  /**
   * This function retrieves a roll call from the repository given its persistent identifier.
   *
   * @param laoId the id of the Lao
   * @param persistentId the persistent id of the roll call
   * @return the roll call matching the identifier in the lao
   * @throws UnknownRollCallException if no roll call with the provided id could be found
   */
  @Throws(UnknownRollCallException::class)
  fun getRollCallWithPersistentId(laoId: String, persistentId: String): RollCall {
    return getLaoRollCalls(laoId).getRollCallWithPersistentId(persistentId)
  }

  /**
   * This function retrieves a roll call from the repository given its state-dependent identifier.
   *
   * @param laoId the id of the Lao
   * @param rollCallId the id of the roll call
   * @return the roll call matching the identifier in the lao
   * @throws UnknownRollCallException if no roll call with the provided id could be found
   */
  @Throws(UnknownRollCallException::class)
  fun getRollCallWithId(laoId: String, rollCallId: String): RollCall {
    return getLaoRollCalls(laoId).getRollCallWithId(rollCallId)
  }

  /**
   * Returns an observable over the set of the roll calls in a given lao.
   *
   * @param laoId the id of the Lao whose roll calls we want to observe
   * @return an observable set of ids who correspond to the set of roll calls published on the given
   *   lao
   */
  fun getRollCallsObservableInLao(laoId: String): Observable<Set<RollCall>> {
    return getLaoRollCalls(laoId).getRollCallsSubject()
  }

  /**
   * Returns the set of all attendees who have ever attended a roll call in the lao
   *
   * @param laoId the id of the considered lao
   * @return the set of all attendees who have ever attended a roll call in the lao
   */
  fun getAllAttendeesInLao(laoId: String): Set<PublicKey> {
    return getLaoRollCalls(laoId).allAttendees
  }

  /**
   * Returns the last closed roll call on a temporal basis.
   *
   * @param laoId the id of the considered lao
   * @return the roll call that has been the last one to be closed
   * @throws NoRollCallException if no roll call in the lao is found
   */
  @Throws(NoRollCallException::class)
  fun getLastClosedRollCall(laoId: String): RollCall {
    return getLaoRollCalls(laoId)
        .rollCallByPersistentId
        .values
        .stream()
        .filter { obj: RollCall -> obj.isClosed }
        .max(Comparator.comparing { obj: RollCall -> obj.end })
        .orElseThrow { NoRollCallException(laoId) }
  }

  /**
   * Check if no other roll call is in open/reopen state
   *
   * @param laoId the id of the considered lao
   * @return true if all roll calls in the given lao are not open yet, false otherwise
   */
  fun canOpenRollCall(laoId: String): Boolean {
    return getLaoRollCalls(laoId).rollCallByPersistentId.values.stream().noneMatch { obj: RollCall
      ->
      obj.isOpen
    }
  }

  fun addDisposable(disposable: Disposable) {
    disposables.add(disposable)
  }

  @Synchronized
  private fun getLaoRollCalls(laoId: String): LaoRollCalls {
    return rollCallsByLao.computeIfAbsent(laoId) { LaoRollCalls(this, laoId) }
  }

  private class LaoRollCalls(
      private val repository: RollCallRepository,
      private val laoId: String
  ) {
    /** Thread-safe mapping between a roll call persistent id and its object reference */
    val rollCallByPersistentId = ConcurrentHashMap<String, RollCall>()

    /**
     * Thread-safe mapping between the roll call ephemeral id (state dependant) and its persistent
     * id, fixed at creation instead
     */
    private val rollCallIdAlias = ConcurrentHashMap<String, String>()

    /** Thread-safe map which maps a roll call persistent id to an observable over itself */
    private val rollCallSubjects = ConcurrentHashMap<String, Subject<RollCall>>()

    /** Observable over the whole set of roll calls */
    private val rollCallsSubject: Subject<Set<RollCall>> =
        BehaviorSubject.createDefault(Collections.unmodifiableSet(emptySet()))

    init {
      loadStorage()
    }

    /**
     * This either updates the roll call in the repository or adds it if absent
     *
     * @param rollCall the roll call to update/add
     */
    fun update(rollCall: RollCall) {
      // Updating repo data
      val persistentId = rollCall.persistentId
      rollCallByPersistentId[persistentId] = rollCall

      // We update the alias map with
      rollCallIdAlias[rollCall.id] = rollCall.persistentId

      // Publish new values on subjects
      if (rollCallSubjects.containsKey(persistentId)) {
        // If it exist we update the subject
        Timber.tag(TAG).d("Updating existing roll call %s", rollCall.name)
        rollCallSubjects.getValue(persistentId).toSerialized().onNext(rollCall)
      } else {
        // If it does not, we create a new subject
        Timber.tag(TAG).d("New roll call, subject created for %s", rollCall.name)
        rollCallSubjects[persistentId] = BehaviorSubject.createDefault(rollCall)
      }
      rollCallsSubject
          .toSerialized()
          .onNext(Collections.unmodifiableSet(HashSet(rollCallByPersistentId.values)))
    }

    @Throws(UnknownRollCallException::class)
    fun getRollCallWithPersistentId(persistentId: String): RollCall {
      return rollCallByPersistentId[persistentId] ?: throw UnknownRollCallException(persistentId)
    }

    @Throws(UnknownRollCallException::class)
    fun getRollCallWithId(id: String): RollCall {
      if (!rollCallIdAlias.containsKey(id)) {
        throw UnknownRollCallException(id)
      }
      val persistentId = rollCallIdAlias.getValue(id)
      return getRollCallWithPersistentId(persistentId)
    }

    @Throws(UnknownRollCallException::class)
    fun getRollCallObservable(id: String): Observable<RollCall> {
      return rollCallSubjects[id] ?: throw UnknownRollCallException(id)
    }

    fun getRollCallsSubject(): Observable<Set<RollCall>> {
      return rollCallsSubject
    }

    val allAttendees: Set<PublicKey>
      get() = // For all roll calls we add all attendees to the returned set
      rollCallByPersistentId.values
              .stream()
              .map { obj: RollCall -> obj.attendees }
              .flatMap { obj: Set<PublicKey> -> obj.stream() }
              .collect(Collectors.toSet())

    /**
     * Load in memory the rollcalls from the disk only when the user clicks on the respective LAO,
     * just needed one time only at creation.
     */
    private fun loadStorage() {
      repository.disposables.add(
          repository.rollCallDao
              .getRollCallsByLaoId(laoId)
              .subscribeOn(Schedulers.io())
              .observeOn(AndroidSchedulers.mainThread())
              .subscribe(
                  { rollcalls: List<RollCall> ->
                    rollcalls.forEach(
                        Consumer { rollCall: RollCall ->
                          update(rollCall)
                          Timber.tag(TAG).d("Retrieved from db rollcall %s", rollCall.persistentId)
                        })
                  },
                  { err: Throwable ->
                    Timber.tag(TAG).e(err, "No rollcall found in the storage for lao %s", laoId)
                  }))
    }
  }

  companion object {
    val TAG: String = RollCallRepository::class.java.simpleName
  }
}
