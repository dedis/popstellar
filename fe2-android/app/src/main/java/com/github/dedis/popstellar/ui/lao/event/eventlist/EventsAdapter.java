package com.github.dedis.popstellar.ui.lao.event.eventlist;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;
import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.model.objects.*;
import com.github.dedis.popstellar.model.objects.event.Event;
import com.github.dedis.popstellar.model.objects.event.EventType;
import com.github.dedis.popstellar.ui.lao.LaoActivity;
import com.github.dedis.popstellar.ui.lao.LaoViewModel;
import com.github.dedis.popstellar.ui.lao.event.election.fragments.ElectionFragment;
import com.github.dedis.popstellar.ui.lao.event.meeting.MeetingFragment;
import com.github.dedis.popstellar.ui.lao.event.rollcall.RollCallFragment;
import io.reactivex.Observable;
import java.util.*;
import java.util.stream.Collectors;
import org.ocpsoft.prettytime.PrettyTime;
import timber.log.Timber;

public abstract class EventsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

  private List<Event> events;
  private final LaoViewModel laoViewModel;
  private final FragmentActivity activity;
  private final String tag;

  protected EventsAdapter(
      Observable<Set<Event>> observable,
      LaoViewModel viewModel,
      FragmentActivity activity,
      String tag) {
    this.laoViewModel = viewModel;
    this.activity = activity;
    this.tag = tag;
    subscribeToEventSet(observable);
  }

  private void subscribeToEventSet(Observable<Set<Event>> observable) {
    this.laoViewModel.addDisposable(
        observable
            .map(eventList -> eventList.stream().sorted().collect(Collectors.toList()))
            .subscribe(
                this::updateEventSet,
                err -> Timber.tag(tag).e(err, "Error subscribing to event set")));
  }

  public abstract void updateEventSet(List<Event> events);

  public List<Event> getEvents() {
    if (events == null) {
      return new ArrayList<>();
    }
    return new ArrayList<>(events);
  }

  public void setEvents(List<Event> events) {
    this.events = new ArrayList<>(events);
  }

  public LaoViewModel getLaoViewModel() {
    return laoViewModel;
  }

  public FragmentActivity getActivity() {
    return activity;
  }

  /**
   * Handle event content, that is setting icon based on RC/Election type, setting the name And
   * setting an appropriate listener based on the type
   */
  protected void handleEventContent(EventViewHolder eventViewHolder, Event event) {
    if (event.type.equals(EventType.ELECTION)) {
      handleElectionContent(eventViewHolder, (Election) event);
    } else if (event.type.equals(EventType.ROLL_CALL)) {
      handleRollCallContent(eventViewHolder, (RollCall) event);
    } else if (event.type.equals(EventType.MEETING)) {
      handleMeetingContent(eventViewHolder, (Meeting) event);
    }
    eventViewHolder.eventTitle.setText(event.name);
    handleTimeAndLocation(eventViewHolder, event);
  }

  private void handleElectionContent(EventViewHolder eventViewHolder, Election election) {
    eventViewHolder.eventIcon.setImageResource(R.drawable.ic_vote);
    eventViewHolder.eventCard.setOnClickListener(
        view ->
            LaoActivity.setCurrentFragment(
                activity.getSupportFragmentManager(),
                R.id.fragment_election,
                () -> ElectionFragment.newInstance(election.id)));
  }

  private void handleRollCallContent(EventViewHolder eventViewHolder, RollCall rollCall) {
    eventViewHolder.eventIcon.setImageResource(R.drawable.ic_roll_call);
    eventViewHolder.eventCard.setOnClickListener(
        view ->
            LaoActivity.setCurrentFragment(
                activity.getSupportFragmentManager(),
                R.id.fragment_roll_call,
                () -> RollCallFragment.newInstance(rollCall.persistentId)));
  }

  private void handleMeetingContent(EventViewHolder eventViewHolder, Meeting meeting) {
    eventViewHolder.eventIcon.setImageResource(R.drawable.ic_meeting);
    eventViewHolder.eventCard.setOnClickListener(
        view ->
            LaoActivity.setCurrentFragment(
                activity.getSupportFragmentManager(),
                R.id.fragment_meeting,
                () -> MeetingFragment.newInstance(meeting.id)));
  }

  private void handleTimeAndLocation(EventViewHolder viewHolder, Event event) {
    String location = "";
    if (event instanceof RollCall) {
      location = ", at " + ((RollCall) event).location;
    }
    if (event instanceof Meeting && !((Meeting) event).location.isEmpty()) {
      location = ", at " + ((Meeting) event).location;
    }
    String timeText;
    switch (event.state) {
      case CREATED:
        if (event.isStartPassed()) {
          timeText = getActivity().getString(R.string.start_anytime);
        } else {
          long eventTime = event.getStartTimestampInMillis();
          timeText =
              String.format(
                  getActivity().getString(R.string.start_at),
                  new PrettyTime().format(new Date(eventTime)));
        }
        break;
      case OPENED:
        timeText = getActivity().getString(R.string.ongoing);
        break;

      case CLOSED:
      case RESULTS_READY:
      default:
        long eventTime = event.getEndTimestampInMillis();
        timeText =
            String.format(
                getActivity().getString(R.string.close_at),
                new PrettyTime().format(new Date(eventTime)));
    }
    String textToDisplay = timeText + location;
    viewHolder.eventTimeAndLoc.setText(textToDisplay);
  }

  public static class EventViewHolder extends RecyclerView.ViewHolder {
    private final TextView eventTitle;
    private final ImageView eventIcon;
    private final CardView eventCard;
    private final TextView eventTimeAndLoc;

    public EventViewHolder(@NonNull View itemView) {
      super(itemView);
      eventTitle = itemView.findViewById(R.id.event_card_text_view);
      eventIcon = itemView.findViewById(R.id.event_type_image);
      eventCard = itemView.findViewById(R.id.event_card_view);
      eventTimeAndLoc = itemView.findViewById(R.id.event_card_time_and_loc);
    }
  }
}
