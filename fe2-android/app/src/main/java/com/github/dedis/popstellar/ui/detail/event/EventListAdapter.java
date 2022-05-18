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
    return null;
  }

  @Override
  public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
    holder.setIsRecyclable(false);
    if (holder instanceof HeaderViewHolder) {
      EventCategory eventCategory = getHeaderCategory(position);
      HeaderViewHolder headerViewHolder = (HeaderViewHolder) holder;
      TextView headerTitle = headerViewHolder.headerTitle;
      ImageView expandIcon = headerViewHolder.expandIcon;
      ConstraintLayout headerLayout = headerViewHolder.headerLayout;

      switch (eventCategory) {
        case PRESENT:
          headerTitle.setText("Current");
          break;
        case FUTURE:
          headerTitle.setText("Upcoming");
          break;
        case PAST:
          headerTitle.setText("Previous");
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
      Log.d(TAG, "getting into election " + election.getName());
      View.OnClickListener listener =
          view -> {
            Log.d(TAG, "election listener triggered");
            viewModel.setCurrentElection(election);
            viewModel.openElectionFragment(true);
          };
      eventViewHolder.eventCard.setOnClickListener(listener);
      //      eventViewHolder.eventTitle.setOnClickListener(listener);
      //      eventViewHolder.eventIcon.setOnClickListener(listener);

    } else if (event.getType().equals(EventType.ROLL_CALL)) {
      eventViewHolder.eventIcon.setImageResource(R.drawable.ic_roll_call);
      RollCall rollCall = (RollCall) event;
      View.OnClickListener listener =
          view -> {
            Log.d(TAG, "roll-call listener triggered");
            viewModel.enterRollCall(rollCall.getId());
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
