package com.github.dedis.popstellar.ui.detail.event;

import static com.github.dedis.popstellar.model.objects.event.EventCategory.FUTURE;
import static com.github.dedis.popstellar.model.objects.event.EventCategory.PAST;
import static com.github.dedis.popstellar.model.objects.event.EventCategory.PRESENT;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.lifecycle.LifecycleOwner;
import androidx.recyclerview.widget.RecyclerView;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.model.objects.Election;
import com.github.dedis.popstellar.model.objects.RollCall;
import com.github.dedis.popstellar.model.objects.event.Event;
import com.github.dedis.popstellar.model.objects.event.EventCategory;
import com.github.dedis.popstellar.model.objects.event.EventType;
import com.github.dedis.popstellar.ui.detail.LaoDetailViewModel;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class EventListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
  private final EventCategory[] categories = EventCategory.values();
  private final LaoDetailViewModel viewModel;
  private final LifecycleOwner lifecycleOwner;
  private final HashMap<EventCategory, List<Event>> eventsMap;
  private static final boolean[] expanded = new boolean[3];
  private static final int TYPE_HEADER = 0;
  private static final int TYPE_EVENT = 1;
  public static final String TAG = EventListAdapter.class.getSimpleName();

  public EventListAdapter(
          List<Event> events, LaoDetailViewModel viewModel, LifecycleOwner lifecycleOwner) {
    this.eventsMap = new HashMap<>();
    this.eventsMap.put(PAST, new ArrayList<>());
    this.eventsMap.put(PRESENT, new ArrayList<>());
    this.eventsMap.put(FUTURE, new ArrayList<>());
    this.lifecycleOwner = lifecycleOwner;
    this.viewModel = viewModel;
    expanded[PAST.ordinal()] = false;
    expanded[PRESENT.ordinal()] = true;
    expanded[FUTURE.ordinal()] = false;

    putEventsInMap(events);
  }

  /**
   * A helper method that places the events in the correct key-value pair according to their times
   *
   * @param events
   */
  private void putEventsInMap(List<Event> events) {
    Collections.sort(events);
    this.eventsMap.get(PAST).clear();
    this.eventsMap.get(FUTURE).clear();
    this.eventsMap.get(PRESENT).clear();
    long now = Instant.now().getEpochSecond();
    for (Event event : events) {
      if (event.getEndTimestamp() <= now) {
        eventsMap.get(PAST).add(event);
      } else if (event.getStartTimestamp() > now) {
        eventsMap.get(FUTURE).add(event);
      } else {
        eventsMap.get(PRESENT).add(event);
      }
    }
  }

  @NonNull
  @Override
  public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    if (viewType == TYPE_HEADER) {
      View view =
          LayoutInflater.from(parent.getContext())
              .inflate(R.layout.event_header_layout, parent, false);
      return new HeaderViewHolder(view);
    }
    if (viewType == TYPE_EVENT) {
      View view =
          LayoutInflater.from(parent.getContext()).inflate(R.layout.event_layout, parent, false);
      return new EventViewHolder(view);
    }
    return null;
  }

  @Override
  public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
    if (holder instanceof HeaderViewHolder) {
      HeaderViewHolder headerViewHolder = (HeaderViewHolder) holder;
    //  handleHeaderContent(headerViewHolder, position);
    } else if (holder instanceof EventViewHolder) {
      EventViewHolder eventViewHolder = (EventViewHolder) holder;
      Event event = getEvent(position);
      handleEventContent(eventViewHolder, event);
    }
  }

  private void handleEventContent(EventViewHolder eventViewHolder, Event event) {
    if (event.getType().equals(EventType.ELECTION)) {
      eventViewHolder.eventIcon.setImageResource(R.drawable.ic_vote);
      Election election = (Election) event;
      eventViewHolder.eventTitle.setText(election.getName());
    } else if (event.getType().equals(EventType.ROLL_CALL)) {
      eventViewHolder.eventIcon.setImageResource(R.drawable.ic_roll_call);
      RollCall rollCall = (RollCall) event;
      eventViewHolder.eventTitle.setText(rollCall.getName());
    }
  }

  private Event getEvent(int position) {
    int nbrOfFutureEvents = eventsMap.get(FUTURE).size();
    int nbrOfPresentEvents = eventsMap.get(PRESENT).size();
    int nbrOfPastEvents = eventsMap.get(PAST).size();

    int eventAccumulator = 0;
    if (expanded[PRESENT.ordinal()]) {
      if (position <= nbrOfPresentEvents) {
        return eventsMap.get(PRESENT).get(position - 1); // position 0 is for the header
      }
      eventAccumulator += nbrOfPresentEvents;
    }
    if (expanded[FUTURE.ordinal()]) {
      if (position <= nbrOfFutureEvents + eventAccumulator + 1) {
        return eventsMap.get(FUTURE).get(position - eventAccumulator - 2);
      }
      eventAccumulator += nbrOfFutureEvents;
    }
    if (expanded[PAST.ordinal()]) {
      if (position <= nbrOfPastEvents + eventAccumulator + 2) {
        return eventsMap.get(PAST).get(position - eventAccumulator - 3);
      }
    }
    return null;
  }

  private EventCategory getHeaderCategory(int position) {
    int nbrOfFutureEvents = eventsMap.get(FUTURE).size();
    int nbrOfPresentEvents = eventsMap.get(PRESENT).size();

    if (position == 0) {
      return PRESENT;
    }
    int eventAccumulator = 0;
    if (expanded[PRESENT.ordinal()]) {
      eventAccumulator += nbrOfPresentEvents;
    }

    if (position == eventAccumulator + 1) {
      return FUTURE;
    }
    if (expanded[FUTURE.ordinal()]) {
      eventAccumulator += nbrOfFutureEvents;
    }
    if (position == eventAccumulator + 2) {
      return PAST;
    }
    return null;
  }

  private void handleHeaderContent(HeaderViewHolder headerViewHolder, int position) {
    int adapterPosition = headerViewHolder.getAdapterPosition();
    ImageView expandIcon = headerViewHolder.expandIcon;
    ConstraintLayout headerLayout = headerViewHolder.headerLayout;
    int nbrOfFutureEvents = eventsMap.get(FUTURE).size();
    int nbrOfPresentEvents = eventsMap.get(PRESENT).size();
    int eventAccumulator = 0;
    String title = "";
    if (expanded[PRESENT.ordinal()]) {
      eventAccumulator = nbrOfPresentEvents;
    }
    if (adapterPosition == 0) {
      title = "Current";
      headerLayout.setOnClickListener(
          v -> {

          });
      if (expanded[PRESENT.ordinal()]) {
        LaoDetailAnimation.rotateExpand(expandIcon, true);
      }
    }
    if (adapterPosition == eventAccumulator + 1) {
      title = "Upcoming";
      if (expanded[FUTURE.ordinal()]) {
        LaoDetailAnimation.rotateExpand(expandIcon, true);
      }
      headerLayout.setOnClickListener(
          v -> {
            headerLayout.setEnabled(false);
            boolean value = expanded[FUTURE.ordinal()];
            expanded[FUTURE.ordinal()] = !value;
            LaoDetailAnimation.rotateExpand(expandIcon, !value);
            notifyDataSetChanged();
            Log.d(TAG, "click on adapterPosition " + adapterPosition);

            headerLayout.setEnabled(true);
          });
    }
    if (expanded[FUTURE.ordinal()]) {
      eventAccumulator += nbrOfFutureEvents;
    }
    if (adapterPosition == eventAccumulator + 2) {
      title = "Previous";
      if (expanded[PAST.ordinal()]) {
        LaoDetailAnimation.rotateExpand(expandIcon, true);
      }
      headerLayout.setOnClickListener(
          v -> {
            headerLayout.setEnabled(false);
            boolean value = expanded[PAST.ordinal()];
            expanded[PAST.ordinal()] = !value;
            LaoDetailAnimation.rotateExpand(expandIcon, !value);
            notifyDataSetChanged();
            Log.d(TAG, "click on adapterPosition " + adapterPosition);
            headerLayout.setEnabled(true);
          });
    }
    headerViewHolder.headerTitle.setText(title);
  }

  @Override
  public int getItemCount() {
    int nbrOfFutureEvents = eventsMap.get(FUTURE).size();
    int nbrOfPresentEvents = eventsMap.get(PRESENT).size();
    int nbrOfPastEvents = eventsMap.get(PAST).size();
    int eventAccumulator = 0;

    if (expanded[PRESENT.ordinal()]) {
      eventAccumulator = nbrOfPresentEvents;
    }
    if (expanded[FUTURE.ordinal()]) {
      eventAccumulator += nbrOfFutureEvents;
    }
    if (expanded[PAST.ordinal()]) {
      eventAccumulator += nbrOfPastEvents;
    }
    return eventAccumulator + 3; // The number of events + the 3 sub-headers
  }

  @Override
  public int getItemViewType(int position) {
    int nbrOfFutureEvents = eventsMap.get(FUTURE).size();
    int nbrOfPresentEvents = eventsMap.get(PRESENT).size();
    int eventAccumulator = 0;

    if (expanded[PRESENT.ordinal()]) {
      eventAccumulator = nbrOfPresentEvents;
    }
    if (position == 0) {
      return TYPE_HEADER;
    }
    if (position == eventAccumulator + 1) {
      return TYPE_HEADER;
    }
    if (expanded[FUTURE.ordinal()]) {
      eventAccumulator += nbrOfFutureEvents;
    }
    if (position == eventAccumulator + 2) {
      return TYPE_HEADER;
    }
    return TYPE_EVENT;
  }

  public void replaceList(List<Event> events) {

    Log.d(TAG, "Replace list called " + events.toString());
    setList(events);
  }

  private void setList(List<Event> events) {
    putEventsInMap(events);
    notifyDataSetChanged();
  }

  private static class EventViewHolder extends RecyclerView.ViewHolder {
    private final TextView eventTitle;
    private final ImageView eventIcon;

    public EventViewHolder(@NonNull View itemView) {
      super(itemView);
      eventTitle = itemView.findViewById(R.id.event_card_text_view);
      eventIcon = itemView.findViewById(R.id.event_type_image);
    }
  }

   class HeaderViewHolder extends RecyclerView.ViewHolder {
    private final TextView headerTitle;
    private final ImageView expandIcon;
    private final ConstraintLayout headerLayout;

    public HeaderViewHolder(@NonNull View itemView) {
      super(itemView);
      headerTitle = itemView.findViewById(R.id.event_header_title);
      expandIcon = itemView.findViewById(R.id.expand_icon);
      headerLayout = itemView.findViewById(R.id.header_layout);
      int position = getAdapterPosition();
      Log.d(TAG, "position is " + position);
      Log.d(TAG, "layout position is " + getLayoutPosition());
      EventCategory eventCategory = getHeaderCategory(position);
      if (eventCategory != null) {
        switch (eventCategory) {
          case PRESENT:
            headerTitle.setText(itemView.getContext().getString(R.string.present_header_title));
            break;
          case FUTURE:
            headerTitle.setText(itemView.getContext().getString(R.string.future_header_title));
            break;
          case PAST:
            headerTitle.setText(itemView.getContext().getString(R.string.past_header_title));
            break;
        }
        expandIcon.setRotation(expanded[eventCategory.ordinal()] ? 180f : 0f);
        headerLayout.setOnClickListener(
            view -> {
              headerLayout.setEnabled(false);
              boolean value = expanded[eventCategory.ordinal()];
              expanded[eventCategory.ordinal()] = !value;
              LaoDetailAnimation.rotateExpand(expandIcon, !value);
              notifyDataSetChanged();
              Log.d(TAG, "click on adapterPosition " + position);
              headerLayout.setEnabled(true);
            });
      }
    }
  }
}
