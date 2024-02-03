package com.github.dedis.popstellar.ui.lao.event.eventlist

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.github.dedis.popstellar.R
import com.github.dedis.popstellar.model.objects.event.Event
import com.github.dedis.popstellar.model.objects.event.EventState
import com.github.dedis.popstellar.ui.lao.LaoViewModel
import io.reactivex.Observable
import java.util.stream.Collectors

class UpcomingEventsAdapter(
    observable: Observable<Set<Event>>,
    viewModel: LaoViewModel,
    activity: FragmentActivity,
    tag: String
) : EventsAdapter(observable, viewModel, activity, tag) {

  @SuppressLint("NotifyDataSetChanged")
  override fun updateEventSet(events: List<Event>) {
    this.events =
        events
            .stream()
            .filter { event: Event ->
              event.state == EventState.CREATED && !event.isEventEndingToday
            }
            .collect(Collectors.toList())

    notifyDataSetChanged()
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
    return EventViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.event_layout, parent, false))
  }

  override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
    val eventViewHolder = holder as EventViewHolder
    val event = events[position]

    handleEventContent(eventViewHolder, event)
  }

  override fun getItemCount(): Int {
    return events.size
  }
}
