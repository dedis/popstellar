package com.github.dedis.popstellar.ui.lao.event.eventlist

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.github.dedis.popstellar.R
import com.github.dedis.popstellar.model.objects.Election
import com.github.dedis.popstellar.model.objects.Meeting
import com.github.dedis.popstellar.model.objects.RollCall
import com.github.dedis.popstellar.model.objects.event.Event
import com.github.dedis.popstellar.model.objects.event.EventState
import com.github.dedis.popstellar.model.objects.event.EventType
import com.github.dedis.popstellar.ui.lao.LaoActivity.Companion.setCurrentFragment
import com.github.dedis.popstellar.ui.lao.LaoViewModel
import com.github.dedis.popstellar.ui.lao.event.election.fragments.ElectionFragment
import com.github.dedis.popstellar.ui.lao.event.meeting.MeetingFragment
import com.github.dedis.popstellar.ui.lao.event.rollcall.RollCallFragment
import io.reactivex.Observable
import java.util.Date
import java.util.stream.Collectors
import org.ocpsoft.prettytime.PrettyTime
import timber.log.Timber

abstract class EventsAdapter
protected constructor(
    observable: Observable<Set<Event>>,
    val laoViewModel: LaoViewModel,
    val activity: FragmentActivity,
    private val tag: String
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
  var events: List<Event> = ArrayList()
    get() = ArrayList(field)
    set(events) {
      field = ArrayList(events)
    }

  init {
    subscribeToEventSet(observable)
  }

  private fun subscribeToEventSet(observable: Observable<Set<Event>>) {
    laoViewModel.addDisposable(
        observable
            .map { eventList: Set<Event> ->
              eventList.stream().sorted().collect(Collectors.toList())
            }
            .subscribe(
                { events: List<Event> -> updateEventSet(events) },
                { err: Throwable -> Timber.tag(tag).e(err, "Error subscribing to event set") }))
  }

  abstract fun updateEventSet(events: List<Event>)

  /**
   * Handle event content, that is setting icon based on RC/Election type, setting the name And
   * setting an appropriate listener based on the type
   */
  protected fun handleEventContent(eventViewHolder: EventViewHolder, event: Event) {
    when (event.type) {
      EventType.ELECTION -> {
        handleElectionContent(eventViewHolder, event as Election)
      }
      EventType.ROLL_CALL -> {
        handleRollCallContent(eventViewHolder, event as RollCall)
      }
      EventType.MEETING -> {
        handleMeetingContent(eventViewHolder, event as Meeting)
      }
      else -> {}
    }

    eventViewHolder.eventTitle.text = event.name
    handleTimeAndLocation(eventViewHolder, event)
  }

  private fun handleElectionContent(eventViewHolder: EventViewHolder, election: Election) {
    eventViewHolder.eventIcon.setImageResource(R.drawable.ic_vote)
    eventViewHolder.eventCard.setOnClickListener {
      setCurrentFragment(activity.supportFragmentManager, R.id.fragment_election) {
        ElectionFragment.newInstance(election.id)
      }
    }
  }

  private fun handleRollCallContent(eventViewHolder: EventViewHolder, rollCall: RollCall) {
    eventViewHolder.eventIcon.setImageResource(R.drawable.ic_roll_call)
    eventViewHolder.eventCard.setOnClickListener {
      setCurrentFragment(activity.supportFragmentManager, R.id.fragment_roll_call) {
        RollCallFragment.newInstance(rollCall.persistentId)
      }
    }
  }

  private fun handleMeetingContent(eventViewHolder: EventViewHolder, meeting: Meeting) {
    eventViewHolder.eventIcon.setImageResource(R.drawable.ic_meeting)
    eventViewHolder.eventCard.setOnClickListener {
      setCurrentFragment(activity.supportFragmentManager, R.id.fragment_meeting) {
        MeetingFragment.newInstance(meeting.id)
      }
    }
  }

  private fun handleTimeAndLocation(viewHolder: EventViewHolder, event: Event) {
    var location = ""
    if (event is RollCall) {
      location = ", at " + event.location
    }

    if (event is Meeting && event.location.isNotEmpty()) {
      location = ", at " + event.location
    }

    val timeText: String =
        when (event.state) {
          EventState.CREATED ->
              if (event.isStartPassed) {
                activity.getString(R.string.start_anytime)
              } else {
                val eventTime = event.startTimestampInMillis
                String.format(
                    activity.getString(R.string.start_at), PrettyTime().format(Date(eventTime)))
              }
          EventState.OPENED -> activity.getString(R.string.ongoing)
          else -> {
            val eventTime = event.endTimestampInMillis
            String.format(
                activity.getString(R.string.close_at), PrettyTime().format(Date(eventTime)))
          }
        }

    val textToDisplay = timeText + location
    viewHolder.eventTimeAndLoc.text = textToDisplay
  }

  class EventViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val eventTitle: TextView = itemView.findViewById(R.id.event_card_text_view)
    val eventIcon: ImageView = itemView.findViewById(R.id.event_type_image)
    val eventCard: CardView = itemView.findViewById(R.id.event_card_view)
    val eventTimeAndLoc: TextView = itemView.findViewById(R.id.event_card_time_and_loc)
  }
}
