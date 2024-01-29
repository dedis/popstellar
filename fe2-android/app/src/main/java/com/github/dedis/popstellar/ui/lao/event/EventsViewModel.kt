package com.github.dedis.popstellar.ui.lao.event

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.github.dedis.popstellar.model.objects.Election
import com.github.dedis.popstellar.model.objects.Meeting
import com.github.dedis.popstellar.model.objects.RollCall
import com.github.dedis.popstellar.model.objects.event.Event
import com.github.dedis.popstellar.repository.ElectionRepository
import com.github.dedis.popstellar.repository.MeetingRepository
import com.github.dedis.popstellar.repository.RollCallRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class EventsViewModel
@Inject
constructor(
  application: Application,
  private val rollCallRepo: RollCallRepository,
  private val meetingRepo: MeetingRepository,
  private val electionRepo: ElectionRepository
) : AndroidViewModel(application) {
  var events: Observable<Set<Event>> = Observable.empty()
    private set

  fun setId(laoId: String) {
    events =
      Observable.combineLatest<Set<RollCall>, Set<Meeting>, Set<Election>, Set<Event>>(
          rollCallRepo.getRollCallsObservableInLao(laoId),
          meetingRepo.getMeetingsObservableInLao(laoId),
          electionRepo.getElectionsObservableInLao(laoId)
        ) { rcs: Set<RollCall>, meets: Set<Meeting>, elects: Set<Election> ->
          val union: MutableSet<Event> = HashSet(rcs)
          union.addAll(elects)
          union.addAll(meets)
          union
        }
        // Only dispatch the latest element once every 50 milliseconds
        // This avoids multiple updates in a short period of time
        .throttleLatest(50, TimeUnit.MILLISECONDS)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
  }
}
