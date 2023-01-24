package com.github.dedis.popstellar.ui.detail.event.eventlist;

import android.annotation.SuppressLint;
import android.view.*;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.model.objects.event.Event;
import com.github.dedis.popstellar.model.objects.event.EventCategory;
import com.github.dedis.popstellar.ui.detail.LaoDetailViewModel;
import com.github.dedis.popstellar.ui.detail.event.LaoDetailAnimation;

import java.util.*;

import io.reactivex.Observable;

import static com.github.dedis.popstellar.model.objects.event.EventCategory.*;

public class EventListAdapter extends EventsAdapter {

  private final EnumMap<EventCategory, List<Event>> eventsMap;

  private final boolean[] expanded = new boolean[3];
  public static final int TYPE_HEADER = 0;
  public static final int TYPE_EVENT = 1;
  public static final String TAG = EventListAdapter.class.getSimpleName();

  public EventListAdapter(
      LaoDetailViewModel viewModel, Observable<Set<Event>> events, FragmentActivity activity) {
    super(events, viewModel, activity, TAG);
    this.eventsMap = new EnumMap<>(EventCategory.class);
    this.eventsMap.put(PAST, new ArrayList<>());
    this.eventsMap.put(PRESENT, new ArrayList<>());

    expanded[PAST.ordinal()] = true;
    expanded[PRESENT.ordinal()] = true;
  }

  /**
   * A helper method that places the events in the correct key-value pair according to state Closed
   * events are put in the past section. Opened events in the current section. Created events that
   * are to be open in less than 24 hours are also in the current section. Created events that are
   * to be opened in more than 24 hours are not displayed here (They are displayed in a separate
   * view)
   */
  @SuppressLint("NotifyDataSetChanged")
  @Override
  public void updateEventSet(List<Event> events) {
    this.eventsMap.get(PAST).clear();
    this.eventsMap.get(PRESENT).clear();

    for (Event event : events) {
      switch (event.getState()) {
        case CREATED:
          if (event.isEventToday()) {
            eventsMap.get(PRESENT).add(event);
          }
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

    notifyDataSetChanged();
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

  @SuppressLint("NotifyDataSetChanged") // Warranted by our current implementation
  @Override
  public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
    // We handle the individual view of the recycler differently if it is a header or an event
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

      String numberOfEventsInCategory =
          "(" + Objects.requireNonNull(eventsMap.get(eventCategory)).size() + ")";
      ((HeaderViewHolder) holder).headerNumber.setText(numberOfEventsInCategory);

      // Expansion/Collapse part
      headerLayout.setOnClickListener(
          view -> {
            headerLayout.setEnabled(false);

            boolean value = expanded[eventCategory.ordinal()];
            expanded[eventCategory.ordinal()] = !value;
            LaoDetailAnimation.rotateExpand(expandIcon, !value);

            // When we expand/collapse the items changed so we need to recompute the view
            notifyDataSetChanged();

            headerLayout.setEnabled(true);
          });
    } else if (holder instanceof EventViewHolder) {
      EventViewHolder eventViewHolder = (EventViewHolder) holder;
      Event event = getEvent(position);
      handleEventContent(eventViewHolder, event);
    }
  }

  /**
   * Get the event at the indicated position. The computations are there to account for
   * expanded/collapsed sections and the fact that some elements are headers The caller must make
   * sure the position is occupied by an event or this will throw an exception
   */
  private Event getEvent(int position) {
    int nbrOfFutureEvents = Objects.requireNonNull(eventsMap.get(FUTURE)).size();
    int nbrOfPresentEvents = Objects.requireNonNull(eventsMap.get(PRESENT)).size();
    int nbrOfPastEvents = Objects.requireNonNull(eventsMap.get(PAST)).size();

    int eventAccumulator = 0;
    if (expanded[PRESENT.ordinal()]) {
      if (position <= nbrOfPresentEvents) {
        return Objects.requireNonNull(eventsMap.get(PRESENT))
            .get(position - 1); // position 0 is for the header
      }
      eventAccumulator += nbrOfPresentEvents;
    }
    if (expanded[FUTURE.ordinal()]) {
      if (position <= nbrOfFutureEvents + eventAccumulator + 1) {
        return Objects.requireNonNull(eventsMap.get(FUTURE)).get(position - eventAccumulator - 2);
      }
      eventAccumulator += nbrOfFutureEvents;
    }
    if (expanded[PAST.ordinal()] && position <= nbrOfPastEvents + eventAccumulator + 2) {
      return Objects.requireNonNull(eventsMap.get(PAST)).get(position - eventAccumulator - 3);
    }
    throw new IllegalStateException("no event matches");
  }

  /**
   * Get the header at the indicated position. The computations are there to account for
   * expanded/collapsed sections and the fact that some elements are headers The caller must make
   * sure the position is occupied by a header or this will throw an exception
   */
  private EventCategory getHeaderCategory(int position) {
    int nbrOfFutureEvents = Objects.requireNonNull(eventsMap.get(FUTURE)).size();
    int nbrOfPresentEvents = Objects.requireNonNull(eventsMap.get(PRESENT)).size();

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
    throw new IllegalStateException("No event category");
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
    return eventAccumulator + 3; // The number expanded of events + the 3 sub-headers
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

  public void showWalletNotSetupWarning() {
    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
    builder.setTitle("You have to setup up your wallet before connecting.");
    builder.setPositiveButton("Ok", (dialog, which) -> dialog.dismiss());
    builder.show();
  }

  public static class HeaderViewHolder extends RecyclerView.ViewHolder {
    private final TextView headerTitle;
    private final TextView headerNumber;
    private final ImageView expandIcon;
    private final ConstraintLayout headerLayout;

    public HeaderViewHolder(@NonNull View itemView) {
      super(itemView);
      headerTitle = itemView.findViewById(R.id.event_header_title);
      headerNumber = itemView.findViewById(R.id.event_header_number);
      expandIcon = itemView.findViewById(R.id.header_expand_icon);
      headerLayout = itemView.findViewById(R.id.header_layout);
    }
  }
}
