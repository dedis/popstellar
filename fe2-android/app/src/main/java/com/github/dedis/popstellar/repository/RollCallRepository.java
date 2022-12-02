package com.github.dedis.popstellar.repository;

import android.util.Log;

import androidx.annotation.NonNull;

import com.github.dedis.popstellar.model.objects.RollCall;
import com.github.dedis.popstellar.utility.error.UnknownRollCallException;

import java.util.*;

import javax.inject.Singleton;

import io.reactivex.Observable;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;

/**
 * This class is the repository of the roll call events
 *
 * <p>Its main purpose is to store roll calls and publish updates
 */
@Singleton
public class RollCallRepository {
  public static final String TAG = RollCallRepository.class.getSimpleName();
  private final Map<String, LaoRollCalls> rollCallsByLao = new HashMap<>();

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
    Log.d(TAG, "Adding new roll call on lao " + laoId + " : " + rollCall);

    // Retrieve Lao data and add the roll call to it
    getLaoRollCalls(laoId).update(new RollCall(rollCall));
  }

  /**
   * This provides an observable of a roll call that triggers an update when modified
   *
   * @param laoId the id of the Lao
   * @param rollCallId the id of the roll call
   * @return the observable wrapping the wanted roll call
   * @throws UnknownRollCallException if no roll call with the provided id could be found
   */
  Observable<RollCall> getRollCall(String laoId, String rollCallId)
      throws UnknownRollCallException {
    return getLaoRollCalls(laoId).getRollCall(rollCallId);
  }

  /**
   * @param laoId the id of the Lao whose roll calls we want to observe
   * @return an observable set of ids who correspond to the set of roll calls published on the given
   *     lao
   */
  public Observable<Set<String>> getRollCallsInLao(String laoId) {
    return getLaoRollCalls(laoId).getRollCallsSubject();
  }

  @NonNull
  private synchronized LaoRollCalls getLaoRollCalls(String laoId) {
    return rollCallsByLao.computeIfAbsent(laoId, lao -> new LaoRollCalls());
  }

  private static final class LaoRollCalls {
    private final Map<String, RollCall> rollCalls = new HashMap<>();

    // This allows to observe a specific roll call(s)
    private final Map<String, Subject<RollCall>> rollCallSubjects = new HashMap<>();

    // This allows to observe the collection of roll calls as a whole
    private final Subject<Set<String>> rollCallsSubject =
        BehaviorSubject.createDefault(Collections.emptySet());

    /**
     * This either updates the roll call in the repository or adds it if absent
     *
     * @param rollCall the roll call to update/add
     */
    public synchronized void update(RollCall rollCall) {
      // Updating repo data
      String id = rollCall.getPersistentId();
      rollCalls.put(id, rollCall);

      // Publish new values on subjects
      if (rollCallSubjects.containsKey(id)) {
        // If it exist we update the subject
        Log.d(TAG, "Updating existing roll call " + rollCall.getName());
        rollCallSubjects.get(id).onNext(rollCall);
      } else {
        // If it does not, we create a new subject
        Log.d(TAG, "New roll call, subject created for " + rollCall.getName());
        rollCallSubjects.put(id, BehaviorSubject.createDefault(rollCall));
      }

      rollCallsSubject.onNext(rollCalls.keySet());
    }

    public Observable<RollCall> getRollCall(String id) throws UnknownRollCallException {
      Observable<RollCall> observable = rollCallSubjects.get(id);
      if (observable == null) {
        throw new UnknownRollCallException(id);
      } else {
        return observable.map(RollCall::new);
      }
    }

    public Observable<Set<String>> getRollCallsSubject() {
      return rollCallsSubject.map(HashSet::new);
    }
  }
}
