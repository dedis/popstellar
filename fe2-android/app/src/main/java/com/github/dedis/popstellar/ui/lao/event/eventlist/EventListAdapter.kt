package com.github.dedis.popstellar.ui.lao.event.eventlist

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.github.dedis.popstellar.R
import com.github.dedis.popstellar.model.objects.event.Event
import com.github.dedis.popstellar.model.objects.event.EventCategory
import com.github.dedis.popstellar.model.objects.event.EventState
import com.github.dedis.popstellar.ui.lao.LaoViewModel
import com.github.dedis.popstellar.ui.lao.event.LaoDetailAnimation.rotateExpand
import io.reactivex.Observable
import java.util.EnumMap
import timber.log.Timber

class EventListAdapter(
    viewModel: LaoViewModel,
    events: Observable<Set<Event>>,
    activity: FragmentActivity
) : EventsAdapter(events, viewModel, activity, TAG) {

  private val eventsMap: EnumMap<EventCategory, MutableList<Event>> =
      EnumMap(EventCategory::class.java)
  private val expanded = BooleanArray(2)

  init {
    eventsMap[EventCategory.PAST] = ArrayList()
    eventsMap[EventCategory.PRESENT] = ArrayList()
    expanded[EventCategory.PAST.ordinal] = true
    expanded[EventCategory.PRESENT.ordinal] = true
  }

  /**
   * A helper method that places the events in the correct key-value pair according to state Closed
   * events are put in the past section. Opened events in the current section. Created events that
   * are to be open in less than 24 hours are also in the current section. Created events that are
   * to be opened in more than 24 hours are not displayed here (They are displayed in a separate
   * view)
   */
  @SuppressLint("NotifyDataSetChanged")
  override fun updateEventSet(events: List<Event>) {
    eventsMap[EventCategory.PAST]?.clear()
    eventsMap[EventCategory.PRESENT]?.clear()

    for (event in events) {
      when (event.state) {
        EventState.CREATED ->
            if (event.isEventEndingToday) {
              eventsMap[EventCategory.PRESENT]?.add(event)
            }
        EventState.OPENED -> eventsMap[EventCategory.PRESENT]?.add(event)
        EventState.CLOSED,
        EventState.RESULTS_READY -> eventsMap[EventCategory.PAST]?.add(event)
        else -> {}
      }
    }

    notifyDataSetChanged()
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
    if (viewType == TYPE_HEADER) {
      val view =
          LayoutInflater.from(parent.context).inflate(R.layout.event_header_layout, parent, false)
      return HeaderViewHolder(view)
    }
    if (viewType == TYPE_EVENT) {
      val view = LayoutInflater.from(parent.context).inflate(R.layout.event_layout, parent, false)
      return EventViewHolder(view)
    }

    error("Illegal view type")
  }

  @SuppressLint("NotifyDataSetChanged") // Warranted by our current implementation
  override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
    // We handle the individual view of the recycler differently if it is a header or an event
    if (holder is HeaderViewHolder) {
      val eventCategory = getHeaderCategory(position)

      val headerTitle = holder.headerTitle
      val expandIcon = holder.expandIcon
      val headerLayout = holder.headerLayout

      if (eventCategory === EventCategory.PRESENT) {
        headerTitle.setText(R.string.present_header_title)
      } else if (eventCategory === EventCategory.PAST) {
        headerTitle.setText(R.string.past_header_title)
      }

      expandIcon.rotation = if (expanded[eventCategory.ordinal]) 180f else 0f

      val numberOfEventsInCategory = "(${eventsMap[eventCategory]?.size})"
      holder.headerNumber.text = numberOfEventsInCategory

      // Expansion/Collapse part
      headerLayout.setOnClickListener {
        headerLayout.isEnabled = false
        val value = expanded[eventCategory.ordinal]
        expanded[eventCategory.ordinal] = !value
        rotateExpand(expandIcon, !value)

        // When we expand/collapse the items changed so we need to recompute the view
        notifyDataSetChanged()
        headerLayout.isEnabled = true
      }
    } else if (holder is EventViewHolder) {
      val event = getEvent(position)
      handleEventContent(holder, event!!)
    }
  }

  /**
   * Get the event at the indicated position. The computations are there to account for
   * expanded/collapsed sections and the fact that some elements are headers The caller must make
   * sure the position is occupied by an event or this will throw an exception
   */
  private fun getEvent(position: Int): Event? {
    val nbrOfPresentEvents = eventsMap[EventCategory.PRESENT]?.size ?: 0
    val nbrOfPastEvents = eventsMap[EventCategory.PAST]?.size ?: 0

    var eventAccumulator = 0
    if (expanded[EventCategory.PRESENT.ordinal]) {
      if (position <= nbrOfPresentEvents) {
        return eventsMap[EventCategory.PRESENT]?.get(position - 1) // position 0 is for the header
      }
      eventAccumulator += nbrOfPresentEvents
    }

    if (expanded[EventCategory.PAST.ordinal] &&
        position <= nbrOfPastEvents + eventAccumulator + 1) {
      val secondSectionOffset = if (nbrOfPresentEvents > 0) 2 else 1
      return eventsMap[EventCategory.PAST]?.get(position - eventAccumulator - secondSectionOffset)
    }

    Timber.tag(TAG).e("position was %d", position)
    error("no event matches")
  }

  /**
   * Get the header at the indicated position. The computations are there to account for
   * expanded/collapsed sections and the fact that some elements are headers The caller must make
   * sure the position is occupied by a header or this will throw an exception
   */
  private fun getHeaderCategory(position: Int): EventCategory {
    val nbrOfPresentEvents = eventsMap[EventCategory.PRESENT]?.size ?: 0

    if (position == 0) {
      // If this function is called, it means that getSize() > 0. Therefore there are some events
      // in either past or present (or both). If there are none in the present the first item is the
      // past header
      return if (nbrOfPresentEvents > 0) EventCategory.PRESENT else EventCategory.PAST
    }

    var eventAccumulator = 0
    if (expanded[EventCategory.PRESENT.ordinal]) {
      eventAccumulator += nbrOfPresentEvents
    }
    if (position == eventAccumulator + 1) {
      return EventCategory.PAST
    }

    Timber.tag(TAG).e("Illegal position %d", position)
    error("No event category")
  }

  override fun getItemCount(): Int {
    val nbrOfPresentEvents = eventsMap[EventCategory.PRESENT]?.size ?: 0
    val nbrOfPastEvents = eventsMap[EventCategory.PAST]?.size ?: 0
    var eventAccumulator = 0

    if (nbrOfPresentEvents > 0) {
      if (expanded[EventCategory.PRESENT.ordinal]) {
        eventAccumulator = nbrOfPresentEvents
      }
      eventAccumulator++ // If there are present events, we want to display the header as well
    }

    if (nbrOfPastEvents > 0) {
      if (expanded[EventCategory.PAST.ordinal]) {
        eventAccumulator += nbrOfPastEvents
      }
      eventAccumulator++ // If there are past events, we want to display the header as well
    }

    return eventAccumulator
  }

  override fun getItemViewType(position: Int): Int {
    val nbrOfPresentEvents = eventsMap[EventCategory.PRESENT]?.size ?: 0
    var eventAccumulator = 0
    if (expanded[EventCategory.PRESENT.ordinal]) {
      eventAccumulator = nbrOfPresentEvents
    }

    if (position == 0) {
      return TYPE_HEADER
    }

    return if (position == eventAccumulator + 1 && nbrOfPresentEvents > 0) {
      TYPE_HEADER
    } else TYPE_EVENT
  }

  class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val headerTitle: TextView = itemView.findViewById(R.id.event_header_title)
    val headerNumber: TextView = itemView.findViewById(R.id.event_header_number)
    val expandIcon: ImageView = itemView.findViewById(R.id.header_expand_icon)
    val headerLayout: ConstraintLayout = itemView.findViewById(R.id.header_layout)
  }

  companion object {
    const val TYPE_HEADER = 0
    const val TYPE_EVENT = 1
    val TAG: String = EventListAdapter::class.java.simpleName
  }
}
