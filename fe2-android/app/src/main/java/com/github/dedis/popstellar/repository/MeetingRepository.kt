package com.github.dedis.popstellar.repository

import android.app.Activity
import android.app.Application
import androidx.lifecycle.Lifecycle
import com.github.dedis.popstellar.model.objects.Meeting
import com.github.dedis.popstellar.repository.database.AppDatabase
import com.github.dedis.popstellar.repository.database.event.meeting.MeetingDao
import com.github.dedis.popstellar.repository.database.event.meeting.MeetingEntity
import com.github.dedis.popstellar.utility.ActivityUtils.buildLifecycleCallback
import com.github.dedis.popstellar.utility.error.UnknownMeetingException
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import java.util.Collections
import java.util.EnumMap
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Consumer
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

/**
 * This class is the repository of the meetings events
 *
 * Its main purpose is to store meetings by lao
 */
@Singleton
class MeetingRepository @Inject constructor(appDatabase: AppDatabase, application: Application) {
  private val meetingsByLao: MutableMap<String, LaoMeetings> = HashMap()
  private val meetingDao: MeetingDao
  private val disposables = CompositeDisposable()

  init {
    meetingDao = appDatabase.meetingDao()
    val consumerMap: MutableMap<Lifecycle.Event, Consumer<Activity>> =
        EnumMap(Lifecycle.Event::class.java)
    consumerMap[Lifecycle.Event.ON_STOP] = Consumer { disposables.clear() }
    application.registerActivityLifecycleCallbacks(buildLifecycleCallback(consumerMap))
  }

  /**
   * This add/replace the meeting in the repository
   *
   * @param meeting the meeting to add/replace
   */
  fun updateMeeting(laoId: String?, meeting: Meeting?) {
    Timber.tag(TAG).d("Adding meeting on lao %s : %s", laoId, meeting)
    requireNotNull(laoId) { "LaoId cannot be null" }
    requireNotNull(meeting) { "MeetingId cannot be null" }
    // Persist the meeting in the db
    disposables.add(
        meetingDao
            .insert(MeetingEntity(laoId, meeting))
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ Timber.tag(TAG).d("Successfully persisted meeting %s", meeting.id) }) {
                err: Throwable ->
              Timber.tag(TAG).e(err, "Error in persisting meeting %s", meeting.id)
            })

    // Retrieve Lao data and add the meeting to it
    getLaoMeetings(laoId).update(meeting)
  }

  /**
   * This provides an observable of a meeting that triggers an update when modified
   *
   * @param laoId the id of the Lao
   * @param meetingId the id of the meeting
   * @return the observable wrapping the wanted meeting
   * @throws UnknownMeetingException if no meeting with the provided id could be found
   */
  @Throws(UnknownMeetingException::class)
  fun getMeetingObservable(laoId: String, meetingId: String): Observable<Meeting> {
    return getLaoMeetings(laoId).getMeetingObservable(meetingId)
  }

  /**
   * This function returns the meeting object by the lao and meeting ids.
   *
   * @param laoId identifier of the lao where to search the meeting
   * @param meetingId meeting identifier
   * @return the meeting matching the identifier
   * @throws UnknownMeetingException if there's no meeting with the provided id in the lao
   */
  @Throws(UnknownMeetingException::class)
  fun getMeetingWithId(laoId: String, meetingId: String): Meeting {
    return getLaoMeetings(laoId).getMeetingWithId(meetingId)
  }

  /**
   * @param laoId the id of the Lao whose meetings we want to observe
   * @return observable set of ids, corresponding to the set of meetings published on the given lao
   */
  fun getMeetingsObservableInLao(laoId: String): Observable<Set<Meeting>> {
    return getLaoMeetings(laoId).getMeetingsSubject()
  }

  @Synchronized
  private fun getLaoMeetings(laoId: String): LaoMeetings {
    return meetingsByLao.computeIfAbsent(laoId) { LaoMeetings(this, laoId) }
  }

  private class LaoMeetings(private val repository: MeetingRepository, private val laoId: String) {
    /** Thread-safe map to store the meetings by their ids */
    private val meetingById = ConcurrentHashMap<String, Meeting>()

    /** Thread-safe map to map the meeting identifier to an observable over the meeting object */
    private val meetingSubjects = ConcurrentHashMap<String, Subject<Meeting>>()

    /** Observable over the whole collection of meetings */
    private val meetingsSubject: Subject<Set<Meeting>> =
        BehaviorSubject.createDefault(Collections.unmodifiableSet(emptySet()))

    init {
      loadStorage()
    }

    /**
     * This either updates the meeting in the repository or adds it if absent
     *
     * @param meeting the meeting to update/add
     */
    fun update(meeting: Meeting) {
      // Updating repo data
      val id = meeting.id
      meetingById[id] = meeting

      // Publish new values on subjects
      if (meetingSubjects.containsKey(id)) {
        // If it exist we update the subject
        Timber.tag(TAG).d("Updating existing meeting %s", meeting.name)
        meetingSubjects.getValue(id).toSerialized().onNext(meeting)
      } else {
        // If it does not, we create a new subject
        Timber.tag(TAG).d("New meeting, subject created for %s", meeting.name)
        meetingSubjects[id] = BehaviorSubject.createDefault(meeting)
      }
      meetingsSubject
          .toSerialized()
          .onNext(Collections.unmodifiableSet(HashSet(meetingById.values)))
    }

    @Throws(UnknownMeetingException::class)
    fun getMeetingWithId(id: String): Meeting {
      return meetingById[id] ?: throw UnknownMeetingException(id)
    }

    @Throws(UnknownMeetingException::class)
    fun getMeetingObservable(id: String): Observable<Meeting> {
      return meetingSubjects[id] ?: throw UnknownMeetingException(id)
    }

    fun getMeetingsSubject(): Observable<Set<Meeting>> {
      return meetingsSubject
    }

    /**
     * Load in memory the meetings from the disk only when the user clicks on the respective LAO.
     * This can be done one time only, as from then things are stored in memory.
     */
    private fun loadStorage() {
      repository.disposables.add(
          repository.meetingDao
              .getMeetingsByLaoId(laoId)
              .subscribeOn(Schedulers.io())
              .observeOn(AndroidSchedulers.mainThread())
              .subscribe({ meetings: List<Meeting> ->
                meetings.forEach(
                    Consumer { meeting: Meeting ->
                      update(meeting)
                      Timber.tag(TAG).d("Retrieved from db meeting %s", meeting.id)
                    })
              }) { err: Throwable ->
                Timber.tag(TAG).e(err, "No meeting found in the storage for lao %s", laoId)
              })
    }
  }

  companion object {
    val TAG: String = MeetingRepository::class.java.simpleName
  }
}
