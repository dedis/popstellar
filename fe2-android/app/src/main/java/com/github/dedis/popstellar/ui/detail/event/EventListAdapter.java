package com.github.dedis.popstellar.ui.detail.event;

import static com.github.dedis.popstellar.model.objects.event.EventCategory.FUTURE;
import static com.github.dedis.popstellar.model.objects.event.EventCategory.PAST;
import static com.github.dedis.popstellar.model.objects.event.EventCategory.PRESENT;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;

public class EventListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
  private final LaoDetailViewModel viewModel;
  private final LifecycleOwner lifecycleOwner;
  private final EnumMap<EventCategory, List<Event>> eventsMap;
  private final boolean[] expanded = new boolean[3];
  public static final int TYPE_HEADER = 0;
  public static final int TYPE_EVENT = 1;
  public static final String TAG = EventListAdapter.class.getSimpleName();

  public EventListAdapter(
      List<Event> events, LaoDetailViewModel viewModel, LifecycleOwner lifecycleOwner) {
    this.eventsMap = new EnumMap<>(EventCategory.class);
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
   * A helper method that places the events in the correct key-value pair according to state
   *
   * @param events the events to sort by state
   */
  private void putEventsInMap(List<Event> events) {
    Collections.sort(events);
    this.eventsMap.get(PAST).clear();
    this.eventsMap.get(FUTURE).clear();
    this.eventsMap.get(PRESENT).clear();

    for (Event event : events) {
      switch (event.getState().getValue()) {
        case CREATED:
          eventsMap.get(FUTURE).add(event);
          break;
        case OPENED:
          eventsMap.get(PRESENT).add(event);
          break;
        case CLOSED:
        case RESULTS_READY:
          eventsMap.get(PAST).add(event);
          break;
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
    throw new IllegalStateException("Illegal view type");
  }

  @Override
  public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
    if (holder instanceof HeaderViewHolder) {
      EventCategory eventCategory = getHeaderCategory(position);
      HeaderViewHolder headerViewHolder = (HeaderViewHolder) holder;
      TextView headerTitle = headerViewHolder.headerTitle;
      ImageView expandIcon = headerViewHolder.expandIcon;
      ConstraintLayout headerLayout = headerViewHolder.headerLayout;

      switch (eventCategory) {
        case PRESENT:
          headerTitle.setText(R.string.present_header_title);
          break;
        case FUTURE:
          headerTitle.setText(R.string.future_header_title);
          break;
        case PAST:
          headerTitle.setText(R.string.past_header_title);
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
            headerLayout.setEnabled(true);
          });
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
      View.OnClickListener listener =
          view -> {
            viewModel.setCurrentElection(election);
            viewModel.openElectionFragment(true);
          };
      eventViewHolder.eventCard.setOnClickListener(listener);

    } else if (event.getType().equals(EventType.ROLL_CALL)) {
      eventViewHolder.eventIcon.setImageResource(R.drawable.ic_roll_call);
      RollCall rollCall = (RollCall) event;
      View.OnClickListener listener =
          view -> {
            viewModel.setCurrentRollCall(rollCall);
            viewModel.enterRollCall(rollCall.getPersistentId());
          };
      eventViewHolder.eventCard.setOnClickListener(listener);
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
    if (expanded[PAST.ordinal()] && position <= nbrOfPastEvents + eventAccumulator + 2) {
      return eventsMap.get(PAST).get(position - eventAccumulator - 3);
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
    setList(events);
  }

  private void setList(List<Event> events) {
    for (EventCategory category : EventCategory.values()) {
      for (Event event : eventsMap.get(category)) {
        event.getState().removeObservers(lifecycleOwner);
      }
    }
    events.forEach(
        event -> event.getState().observe(lifecycleOwner, eventState -> notifyDataSetChanged()));
    putEventsInMap(events);
    notifyDataSetChanged();
  }

  private static class EventViewHolder extends RecyclerView.ViewHolder {
    private final TextView eventTitle;
    private final ImageView eventIcon;
    private final CardView eventCard;

    public EventViewHolder(@NonNull View itemView) {
      super(itemView);
      eventTitle = itemView.findViewById(R.id.event_card_text_view);
      eventIcon = itemView.findViewById(R.id.event_type_image);
      eventCard = itemView.findViewById(R.id.event_card_view);
    }
  }

  private static class HeaderViewHolder extends RecyclerView.ViewHolder {
    private final TextView headerTitle;
    private final ImageView expandIcon;
    private final ConstraintLayout headerLayout;

    public HeaderViewHolder(@NonNull View itemView) {
      super(itemView);
      headerTitle = itemView.findViewById(R.id.event_header_title);
      expandIcon = itemView.findViewById(R.id.expand_icon);
      headerLayout = itemView.findViewById(R.id.header_layout);
    }
  }
}
