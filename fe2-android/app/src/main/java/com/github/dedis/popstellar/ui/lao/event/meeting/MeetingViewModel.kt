package com.github.dedis.popstellar.ui.lao.event.meeting

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.github.dedis.popstellar.R
import com.github.dedis.popstellar.model.network.method.message.data.meeting.CreateMeeting
import com.github.dedis.popstellar.model.objects.Meeting
import com.github.dedis.popstellar.model.objects.view.LaoView
import com.github.dedis.popstellar.repository.LAORepository
import com.github.dedis.popstellar.repository.MeetingRepository
import com.github.dedis.popstellar.repository.remote.GlobalNetworkManager
import com.github.dedis.popstellar.utility.error.ErrorUtils.logAndShow
import com.github.dedis.popstellar.utility.error.UnknownLaoException
import com.github.dedis.popstellar.utility.error.UnknownMeetingException
import com.github.dedis.popstellar.utility.scheduler.SchedulerProvider
import com.github.dedis.popstellar.utility.security.KeyManager
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.Observable
import io.reactivex.Single
import javax.inject.Inject
import timber.log.Timber

@HiltViewModel
class MeetingViewModel
@Inject
constructor(
    application: Application,
    private val laoRepo: LAORepository,
    private val meetingRepo: MeetingRepository,
    private val networkManager: GlobalNetworkManager,
    private val keyManager: KeyManager,
    private val schedulerProvider: SchedulerProvider
) : AndroidViewModel(application) {
  private lateinit var laoId: String

  fun setLaoId(laoId: String) {
    this.laoId = laoId
  }

  fun getMeetingObservable(id: String): Observable<Meeting> {
    return try {
      meetingRepo.getMeetingObservable(laoId, id).observeOn(schedulerProvider.mainThread())
    } catch (e: UnknownMeetingException) {
      Timber.tag(TAG).d(e)
      Observable.error(UnknownMeetingException(id))
    }
  }

  @get:Throws(UnknownLaoException::class)
  private val lao: LaoView
    get() = laoRepo.getLaoView(laoId)

  fun createNewMeeting(
      title: String,
      location: String?,
      creation: Long,
      proposedStart: Long,
      proposedEnd: Long
  ): Single<String> {
    Timber.tag(TAG).d("creating a new meeting with title %s", title)

    val laoView: LaoView =
        try {
          lao
        } catch (e: UnknownLaoException) {
          logAndShow(getApplication(), TAG, e, R.string.unknown_lao_exception)
          return Single.error(UnknownLaoException())
        }

    val createMeeting =
        CreateMeeting(laoView.id, title, creation, location, proposedStart, proposedEnd)

    return networkManager.messageSender
        .publish(keyManager.mainKeyPair, laoView.channel, createMeeting)
        .toSingleDefault(createMeeting.id)
  }

  companion object {
    val TAG: String = MeetingViewModel::class.java.simpleName
  }
}
