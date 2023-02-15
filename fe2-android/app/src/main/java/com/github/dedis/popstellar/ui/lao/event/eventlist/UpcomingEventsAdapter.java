package com.github.dedis.popstellar.ui.lao.event.eventlist;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.model.objects.event.Event;
import com.github.dedis.popstellar.model.objects.event.EventState;
import com.github.dedis.popstellar.ui.lao.LaoViewModel;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import io.reactivex.Observable;

public class UpcomingEventsAdapter extends EventsAdapter {

  public UpcomingEventsAdapter(
      Observable<Set<Event>> observable,
      LaoViewModel viewModel,
      FragmentActivity activity,
      String tag) {
    super(observable, viewModel, activity, tag);
  }

  @SuppressLint("NotifyDataSetChanged")
  @Override
  public void updateEventSet(List<Event> events) {
    this.setEvents(
        events.stream()
            .filter(
                event -> event.getState().equals(EventState.CREATED) && !event.isEventEndingToday())
            .collect(Collectors.toList()));
    notifyDataSetChanged();
  }

  @NonNull
  @Override
  public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    return new EventViewHolder(
        LayoutInflater.from(parent.getContext()).inflate(R.layout.event_layout, parent, false));
  }

  @Override
  public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
    EventViewHolder eventViewHolder = (EventViewHolder) holder;
    Event event = this.getEvents().get(position);
    this.handleEventContent(eventViewHolder, event);
  }

  @Override
  public int getItemCount() {
    return getEvents().size();
  }
}
