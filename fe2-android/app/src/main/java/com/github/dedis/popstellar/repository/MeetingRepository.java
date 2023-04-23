package com.github.dedis.popstellar.repository;

import android.util.Log;

import androidx.annotation.NonNull;

import com.github.dedis.popstellar.model.objects.Meeting;
import com.github.dedis.popstellar.utility.error.UnknownMeetingException;

import java.util.*;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Observable;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;

import static java.util.Collections.emptySet;
import static java.util.Collections.unmodifiableSet;

/**
 * This class is the repository of the meetings events
 *
 * <p>Its main purpose is to store meetings
 */
@Singleton
public class MeetingRepository {
  public static final String TAG = RollCallRepository.class.getSimpleName();
  private final Map<String, LaoMeetings> meetingsByLao = new HashMap<>();

  @Inject
  public MeetingRepository() {
    // Constructor required by Hilt
  }

  /**
   * This either updates the meeting in the repository or adds it if absent
   *
   * @param meeting the meeting to update/add
   */
  public void updateMeeting(String laoId, Meeting meeting) {
    if (laoId == null) {
      throw new IllegalArgumentException("Lao id is null");
    }
    if (meeting == null) {
      throw new IllegalArgumentException("Meeting is null");
    }
    Log.d(TAG, "Updating meeting on lao " + laoId + " : " + meeting);

    // Retrieve Lao data and add the roll call to it
    getLaoMeetings(laoId).update(meeting);
  }

  /**
   * This provides an observable of a meeting that triggers an update when modified
   *
   * @param laoId the id of the Lao
   * @param persistentId the persistent id of the meeting
   * @return the observable wrapping the wanted meeting
   * @throws UnknownMeetingException if no meeting with the provided id could be found
   */
  public Observable<Meeting> getMeetingObservable(String laoId, String persistentId)
      throws UnknownMeetingException {
    return getLaoMeetings(laoId).getMeetingObservable(persistentId);
  }

  public Meeting getMeetingWithPersistentId(String laoId, String persistentId)
      throws UnknownMeetingException {
    return getLaoMeetings(laoId).getMeetingWithPersistentId(persistentId);
  }

  public Meeting getMeetingWithId(String laoId, String meetingId) throws UnknownMeetingException {
    return getLaoMeetings(laoId).getMeetingWithId(meetingId);
  }

  /**
   * @param laoId the id of the Lao whose roll calls we want to observe
   * @return an observable set of ids who correspond to the set of roll calls published on the given
   *     lao
   */
  public Observable<Set<Meeting>> getMeetingsObservableInLao(String laoId) {
    return getLaoMeetings(laoId).getMeetingsSubject();
  }

  @NonNull
  private synchronized LaoMeetings getLaoMeetings(String laoId) {
    return meetingsByLao.computeIfAbsent(laoId, lao -> new MeetingRepository.LaoMeetings());
  }

  private static final class LaoMeetings {
    private final Map<String, Meeting> meetingByPersistentId = new HashMap<>();

    // This maps a meeting id, which is state dependent,
    // to its persistentId which is fixed at creation
    private final Map<String, String> meetingIdAlias = new HashMap<>();

    // This allows to observe a specific meeting(s)
    private final Map<String, Subject<Meeting>> meetingSubjects = new HashMap<>();

    // This allows to observe the collection of meetings as a whole
    private final Subject<Set<Meeting>> meetingsSubject =
        BehaviorSubject.createDefault(unmodifiableSet(emptySet()));

    /**
     * This either updates the meeting in the repository or adds it if absent
     *
     * @param meeting the meeting to update/add
     */
    public synchronized void update(Meeting meeting) {
      // Updating repo data
      String persistentId = meeting.getPersistentId();
      meetingByPersistentId.put(persistentId, meeting);

      // We update the alias map with
      meetingIdAlias.put(meeting.getId(), meeting.getPersistentId());

      // Publish new values on subjects
      if (meetingSubjects.containsKey(persistentId)) {
        // If it exist we update the subject
        Log.d(TAG, "Updating existing meeting " + meeting.getName());
        meetingSubjects.get(persistentId).onNext(meeting);
      } else {
        // If it does not, we create a new subject
        Log.d(TAG, "New meeting, subject created for " + meeting.getName());
        meetingSubjects.put(persistentId, BehaviorSubject.createDefault(meeting));
      }

      meetingsSubject.onNext(unmodifiableSet(new HashSet<>(meetingByPersistentId.values())));
    }

    public Meeting getMeetingWithPersistentId(String persistentId) throws UnknownMeetingException {
      if (!meetingByPersistentId.containsKey(persistentId)) {
        throw new UnknownMeetingException(persistentId);
      }
      return meetingByPersistentId.get(persistentId);
    }

    public Meeting getMeetingWithId(String id) throws UnknownMeetingException {
      if (!meetingIdAlias.containsKey(id)) {
        throw new UnknownMeetingException(id);
      }
      String persistentId = meetingIdAlias.get(id);
      return getMeetingWithPersistentId(persistentId);
    }

    public Observable<Meeting> getMeetingObservable(String id) throws UnknownMeetingException {
      Observable<Meeting> observable = meetingSubjects.get(id);
      if (observable == null) {
        throw new UnknownMeetingException(id);
      } else {
        return observable;
      }
    }

    public Observable<Set<Meeting>> getMeetingsSubject() {
      return meetingsSubject;
    }
  }
}
