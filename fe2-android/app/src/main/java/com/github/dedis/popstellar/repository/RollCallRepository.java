package com.github.dedis.popstellar.repository;

import android.app.Activity;
import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.Lifecycle;

import com.github.dedis.popstellar.model.objects.RollCall;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.repository.database.AppDatabase;
import com.github.dedis.popstellar.repository.database.event.rollcall.RollCallDao;
import com.github.dedis.popstellar.repository.database.event.rollcall.RollCallEntity;
import com.github.dedis.popstellar.utility.ActivityUtils;
import com.github.dedis.popstellar.utility.error.UnknownRollCallException;
import com.github.dedis.popstellar.utility.error.keys.NoRollCallException;

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

import static java.util.Collections.emptySet;
import static java.util.Collections.unmodifiableSet;

/**
 * This class is the repository of the roll call events
 *
 * <p>Its main purpose is to store roll calls and publish updates
 */
@Singleton
public class RollCallRepository {

  public static final String TAG = RollCallRepository.class.getSimpleName();
  private final Map<String, LaoRollCalls> rollCallsByLao = new HashMap<>();

  private final RollCallDao rollCallDao;

  private final CompositeDisposable disposables = new CompositeDisposable();

  @Inject
  public RollCallRepository(AppDatabase appDatabase, Application application) {
    rollCallDao = appDatabase.rollCallDao();
    Map<Lifecycle.Event, Consumer<Activity>> consumerMap = new EnumMap<>(Lifecycle.Event.class);
    consumerMap.put(Lifecycle.Event.ON_STOP, activity -> disposables.clear());
    application.registerActivityLifecycleCallbacks(
        ActivityUtils.buildLifecycleCallback(consumerMap));
  }

  /**
   * This either updates the roll call in the repository or adds it if absent.
   *
   * @param rollCall the roll call to update/add
   */
  public void updateRollCall(String laoId, RollCall rollCall) {
    if (laoId == null) {
      throw new IllegalArgumentException("Lao id is null");
    }
    if (rollCall == null) {
      throw new IllegalArgumentException("Roll call is null");
    }
    Timber.tag(TAG).d("Updating roll call on lao %s : %s", laoId, rollCall);

    // Persist the rollcall
    disposables.add(
        rollCallDao
            .insert(new RollCallEntity(laoId, rollCall))
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                () ->
                    Timber.tag(TAG)
                        .d("Successfully persisted rollcall %s", rollCall.getPersistentId()),
                err ->
                    Timber.tag(TAG)
                        .e(err, "Error in persisting rollcall %s", rollCall.getPersistentId())));

    // Retrieve Lao data and add the roll call to it
    getLaoRollCalls(laoId).update(rollCall);
  }

  /**
   * This provides an observable of a roll call that triggers an update when modified
   *
   * @param laoId the id of the Lao
   * @param persistentId the persistent id of the roll call
   * @return the observable wrapping the wanted roll call
   * @throws UnknownRollCallException if no roll call with the provided id could be found
   */
  public Observable<RollCall> getRollCallObservable(String laoId, String persistentId)
      throws UnknownRollCallException {
    return getLaoRollCalls(laoId).getRollCallObservable(persistentId);
  }

  /**
   * This function retrieves a roll call from the repository given its persistent identifier.
   *
   * @param laoId the id of the Lao
   * @param persistentId the persistent id of the roll call
   * @return the roll call matching the identifier in the lao
   * @throws UnknownRollCallException if no roll call with the provided id could be found
   */
  public RollCall getRollCallWithPersistentId(String laoId, String persistentId)
      throws UnknownRollCallException {
    return getLaoRollCalls(laoId).getRollCallWithPersistentId(persistentId);
  }

  /**
   * This function retrieves a roll call from the repository given its state-dependent identifier.
   *
   * @param laoId the id of the Lao
   * @param rollCallId the id of the roll call
   * @return the roll call matching the identifier in the lao
   * @throws UnknownRollCallException if no roll call with the provided id could be found
   */
  public RollCall getRollCallWithId(String laoId, String rollCallId)
      throws UnknownRollCallException {
    return getLaoRollCalls(laoId).getRollCallWithId(rollCallId);
  }

  /**
   * Returns an observable over the set of the roll calls in a given lao.
   *
   * @param laoId the id of the Lao whose roll calls we want to observe
   * @return an observable set of ids who correspond to the set of roll calls published on the given
   *     lao
   */
  public Observable<Set<RollCall>> getRollCallsObservableInLao(String laoId) {
    return getLaoRollCalls(laoId).getRollCallsSubject();
  }

  /**
   * Returns the set of all attendees who have ever attended a roll call in the lao
   *
   * @param laoId the id of the considered lao
   * @return the set of all attendees who have ever attended a roll call in the lao
   */
  public Set<PublicKey> getAllAttendeesInLao(String laoId) {
    return getLaoRollCalls(laoId).getAllAttendees();
  }

  /**
   * Returns the last closed roll call on a temporal basis.
   *
   * @param laoId the id of the considered lao
   * @return the roll call that has been the last one to be closed
   * @throws NoRollCallException if no roll call in the lao is found
   */
  public RollCall getLastClosedRollCall(String laoId) throws NoRollCallException {
    return getLaoRollCalls(laoId).rollCallByPersistentId.values().stream()
        .filter(RollCall::isClosed)
        .max(Comparator.comparing(RollCall::getEnd))
        .orElseThrow(() -> new NoRollCallException(laoId));
  }

  /**
   * Check if no other roll call is in open/reopen state
   *
   * @param laoId the id of the considered lao
   * @return true if all roll calls in the given lao are not open yet, false otherwise
   */
  public boolean canOpenRollCall(String laoId) {
    return getLaoRollCalls(laoId).rollCallByPersistentId.values().stream()
        .noneMatch(RollCall::isOpen);
  }

  @NonNull
  private synchronized LaoRollCalls getLaoRollCalls(String laoId) {
    return rollCallsByLao.computeIfAbsent(laoId, lao -> new LaoRollCalls(this, laoId));
  }

  private static final class LaoRollCalls {
    private final RollCallRepository repository;
    private final String laoId;

    /** Thread-safe mapping between a roll call persistent id and its object reference */
    private final ConcurrentHashMap<String, RollCall> rollCallByPersistentId =
        new ConcurrentHashMap<>();

    /**
     * Thread-safe mapping between the roll call ephemeral id (state dependant) and its persistent
     * id, fixed at creation instead
     */
    private final ConcurrentHashMap<String, String> rollCallIdAlias = new ConcurrentHashMap<>();

    /** Thread-safe map which maps a roll call persistent id to an observable over itself */
    private final ConcurrentHashMap<String, Subject<RollCall>> rollCallSubjects =
        new ConcurrentHashMap<>();

    /** Observable over the whole set of roll calls */
    private final Subject<Set<RollCall>> rollCallsSubject =
        BehaviorSubject.createDefault(unmodifiableSet(emptySet()));

    public LaoRollCalls(RollCallRepository repository, String laoId) {
      this.repository = repository;
      this.laoId = laoId;
      loadStorage();
    }

    /**
     * This either updates the roll call in the repository or adds it if absent
     *
     * @param rollCall the roll call to update/add
     */
    public void update(RollCall rollCall) {
      // Updating repo data
      String persistentId = rollCall.getPersistentId();
      rollCallByPersistentId.put(persistentId, rollCall);

      // We update the alias map with
      rollCallIdAlias.put(rollCall.getId(), rollCall.getPersistentId());

      // Publish new values on subjects
      if (rollCallSubjects.containsKey(persistentId)) {
        // If it exist we update the subject
        Timber.tag(TAG).d("Updating existing roll call %s", rollCall.getName());
        Objects.requireNonNull(rollCallSubjects.get(persistentId)).toSerialized().onNext(rollCall);
      } else {
        // If it does not, we create a new subject
        Timber.tag(TAG).d("New roll call, subject created for %s", rollCall.getName());
        rollCallSubjects.put(persistentId, BehaviorSubject.createDefault(rollCall));
      }

      rollCallsSubject
          .toSerialized()
          .onNext(unmodifiableSet(new HashSet<>(this.rollCallByPersistentId.values())));
    }

    public RollCall getRollCallWithPersistentId(String persistentId)
        throws UnknownRollCallException {
      if (!rollCallByPersistentId.containsKey(persistentId)) {
        throw new UnknownRollCallException(persistentId);
      }
      return rollCallByPersistentId.get(persistentId);
    }

    public RollCall getRollCallWithId(String id) throws UnknownRollCallException {
      if (!rollCallIdAlias.containsKey(id)) {
        throw new UnknownRollCallException(id);
      }
      String persistentId = rollCallIdAlias.get(id);
      return getRollCallWithPersistentId(persistentId);
    }

    public Observable<RollCall> getRollCallObservable(String id) throws UnknownRollCallException {
      Observable<RollCall> observable = rollCallSubjects.get(id);
      if (observable == null) {
        throw new UnknownRollCallException(id);
      } else {
        return observable;
      }
    }

    public Observable<Set<RollCall>> getRollCallsSubject() {
      return rollCallsSubject;
    }

    public Set<PublicKey> getAllAttendees() {
      // For all roll calls we add all attendees to the returned set
      return rollCallByPersistentId.values().stream()
          .map(RollCall::getAttendees)
          .flatMap(Collection::stream)
          .collect(Collectors.toSet());
    }

    /**
     * Load in memory the rollcalls from the disk only when the user clicks on the respective LAO,
     * just needed one time only at creation.
     */
    private void loadStorage() {
      repository.disposables.add(
          repository
              .rollCallDao
              .getRollCallsByLaoId(laoId)
              .subscribeOn(Schedulers.io())
              .observeOn(AndroidSchedulers.mainThread())
              .subscribe(
                  rollcalls ->
                      rollcalls.forEach(
                          rollCall -> {
                            update(rollCall);
                            Timber.tag(TAG)
                                .d("Retrieved from db rollcall %s", rollCall.getPersistentId());
                          }),
                  err ->
                      Timber.tag(TAG)
                          .e(err, "No rollcall found in the storage for lao %s", laoId)));
    }
  }
}
