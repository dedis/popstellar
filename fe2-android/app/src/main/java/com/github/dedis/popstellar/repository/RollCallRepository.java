package com.github.dedis.popstellar.repository;

import android.util.Log;

import androidx.annotation.NonNull;

import com.github.dedis.popstellar.model.objects.RollCall;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.utility.error.UnknownRollCallException;
import com.github.dedis.popstellar.utility.error.keys.NoRollCallException;

import java.util.*;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Observable;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;

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

  @Inject
  public RollCallRepository() {
    // Constructor required by Hilt
  }

  /**
   * This either updates the roll call in the repository or adds it if absent
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
    Log.d(TAG, "Updating roll call on lao " + laoId + " : " + rollCall);

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

  public RollCall getRollCallWithPersistentId(String laoId, String persistentId)
      throws UnknownRollCallException {
    return getLaoRollCalls(laoId).getRollCallWithPersistentId(persistentId);
  }

  public RollCall getRollCallWithId(String laoId, String rollCallId)
      throws UnknownRollCallException {
    return getLaoRollCalls(laoId).getRollCallWithId(rollCallId);
  }

  /**
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

  public RollCall getLastClosedRollCall(String laoId) throws NoRollCallException {
    return getLaoRollCalls(laoId).rollCallByPersistentId.values().stream()
        .filter(RollCall::isClosed)
        .max(Comparator.comparing(RollCall::getEnd))
        .orElseThrow(() -> new NoRollCallException(laoId));
  }

  @NonNull
  private synchronized LaoRollCalls getLaoRollCalls(String laoId) {
    return rollCallsByLao.computeIfAbsent(laoId, lao -> new LaoRollCalls());
  }

  private static final class LaoRollCalls {
    private final Map<String, RollCall> rollCallByPersistentId = new HashMap<>();

    // This maps a roll call id, which is state dependant,
    // to its persistentId which is fixed at creation
    private final Map<String, String> rollCallIdAlias = new HashMap<>();

    // This allows to observe a specific roll call(s)
    private final Map<String, Subject<RollCall>> rollCallSubjects = new HashMap<>();

    // This allows to observe the collection of roll calls as a whole
    private final Subject<Set<RollCall>> rollCallsSubject =
        BehaviorSubject.createDefault(unmodifiableSet(emptySet()));

    /**
     * This either updates the roll call in the repository or adds it if absent
     *
     * @param rollCall the roll call to update/add
     */
    public synchronized void update(RollCall rollCall) {
      // Updating repo data
      String persistentId = rollCall.getPersistentId();
      rollCallByPersistentId.put(persistentId, rollCall);

      // We update the alias map with
      rollCallIdAlias.put(rollCall.getId(), rollCall.getPersistentId());

      // Publish new values on subjects
      if (rollCallSubjects.containsKey(persistentId)) {
        // If it exist we update the subject
        Log.d(TAG, "Updating existing roll call " + rollCall.getName());
        rollCallSubjects.get(persistentId).onNext(rollCall);
      } else {
        // If it does not, we create a new subject
        Log.d(TAG, "New roll call, subject created for " + rollCall.getName());
        rollCallSubjects.put(persistentId, BehaviorSubject.createDefault(rollCall));
      }

      rollCallsSubject.onNext(unmodifiableSet(new HashSet<>(this.rollCallByPersistentId.values())));
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
  }
}
