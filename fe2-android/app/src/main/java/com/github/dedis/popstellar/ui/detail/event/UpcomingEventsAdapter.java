package com.github.dedis.popstellar.ui.detail.event;

import android.annotation.SuppressLint;
import android.view.*;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.model.objects.event.Event;
import com.github.dedis.popstellar.model.objects.event.EventState;
import com.github.dedis.popstellar.ui.detail.LaoDetailViewModel;

import java.util.List;
import java.util.stream.Collectors;

public class UpcomingEventsAdapter extends EventsAdapter {
  public static final String TAG = UpcomingEventsAdapter.class.getSimpleName();

  public UpcomingEventsAdapter(
      List<Event> events, LaoDetailViewModel viewModel, FragmentActivity activity) {
    super(events, viewModel, activity);
  }

  @SuppressLint("NotifyDataSetChanged")
  public void replaceList(List<Event> events) {
    this.setEvents(
        events.stream()
            .filter(event -> event.getState().equals(EventState.CREATED))
            .sorted()
            .collect(Collectors.toList()));
    notifyDataSetChanged();
  }

  @NonNull
  @Override
  public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    View view =
        LayoutInflater.from(parent.getContext()).inflate(R.layout.event_layout, parent, false);
    return new EventViewHolder(view);
  }

  @Override
  public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
    EventViewHolder eventViewHolder = (EventViewHolder) holder;
    Event event = this.getEvents().get(position);
    this.handleEventContent(eventViewHolder, event, TAG);
  }

  @Override
  public int getItemCount() {
    return getEvents().size();
  }
}
