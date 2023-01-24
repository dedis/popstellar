package com.github.dedis.popstellar.ui.detail.event;

import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.model.objects.Election;
import com.github.dedis.popstellar.model.objects.RollCall;
import com.github.dedis.popstellar.model.objects.event.Event;
import com.github.dedis.popstellar.model.objects.event.EventType;
import com.github.dedis.popstellar.model.objects.security.PoPToken;
import com.github.dedis.popstellar.ui.detail.LaoDetailActivity;
import com.github.dedis.popstellar.ui.detail.LaoDetailViewModel;
import com.github.dedis.popstellar.ui.detail.event.election.fragments.ElectionFragment;
import com.github.dedis.popstellar.ui.detail.event.rollcall.RollCallFragment;
import com.github.dedis.popstellar.utility.error.ErrorUtils;
import com.github.dedis.popstellar.utility.error.UnknownLaoException;
import com.github.dedis.popstellar.utility.error.keys.KeyException;

import java.util.*;
import java.util.stream.Collectors;

import io.reactivex.Observable;

import static com.github.dedis.popstellar.ui.detail.LaoDetailActivity.setCurrentFragment;

public abstract class EventsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
  private List<Event> events;
  private final LaoDetailViewModel viewModel;
  private final FragmentActivity activity;
  private final String TAG;

  public EventsAdapter(
      Observable<Set<Event>> observable,
      LaoDetailViewModel viewModel,
      FragmentActivity activity,
      String TAG) {
    this.events = new ArrayList<>(events);
    this.viewModel = viewModel;
    this.activity = activity;
    this.TAG = TAG;
    subscribeToEventSet(observable);
  }

  private void subscribeToEventSet(Observable<Set<Event>> observable) {
    this.viewModel.addDisposable(
        observable
            .map(events -> events.stream().sorted().collect(Collectors.toList()))
            // No need to check for error as the events errors already handles them
            .subscribe(this::updateEventSet, err -> Log.e(TAG, "ERROR", err)));
  }

  public abstract void updateEventSet(List<Event> events);

  public List<Event> getEvents() {
    return new ArrayList<>(events);
  }

  public void setEvents(List<Event> events) {
    this.events = new ArrayList<>(events);
  }

  public LaoDetailViewModel getViewModel() {
    return viewModel;
  }

  public FragmentActivity getActivity() {
    return activity;
  }

  /**
   * Handle event content, that is setting icon based on RC/Election type, setting the name And
   * setting an appropriate listener based on the type
   */
  protected void handleEventContent(EventViewHolder eventViewHolder, Event event) {
    if (event.getType().equals(EventType.ELECTION)) {
      eventViewHolder.eventIcon.setImageResource(R.drawable.ic_vote);
      Election election = (Election) event;
      eventViewHolder.eventTitle.setText(election.getName());
      View.OnClickListener listener =
          view ->
              LaoDetailActivity.setCurrentFragment(
                  activity.getSupportFragmentManager(),
                  R.id.fragment_election,
                  () -> ElectionFragment.newInstance(election.getId()));
      eventViewHolder.eventCard.setOnClickListener(listener);

    } else if (event.getType().equals(EventType.ROLL_CALL)) {
      eventViewHolder.eventIcon.setImageResource(R.drawable.ic_roll_call);
      RollCall rollCall = (RollCall) event;
      eventViewHolder.eventTitle.setText(rollCall.getName());
      eventViewHolder.eventCard.setOnClickListener(
          view -> {
            if (viewModel.isWalletSetup()) {
              try {
                PoPToken token = viewModel.getCurrentPopToken(rollCall);
                setCurrentFragment(
                    activity.getSupportFragmentManager(),
                    R.id.fragment_roll_call,
                    () ->
                        RollCallFragment.newInstance(
                            token.getPublicKey(), rollCall.getPersistentId()));
              } catch (KeyException e) {
                ErrorUtils.logAndShow(activity, TAG, e, R.string.key_generation_exception);
              } catch (UnknownLaoException e) {
                ErrorUtils.logAndShow(activity, TAG, e, R.string.error_no_lao);
              }
            } else {
              showWalletNotSetupWarning();
            }
          });
      eventViewHolder.eventTitle.setText(rollCall.getName());
    }
  }

  public void showWalletNotSetupWarning() {
    AlertDialog.Builder builder = new AlertDialog.Builder(activity);
    builder.setTitle("You have to setup up your wallet before connecting.");
    builder.setPositiveButton("Ok", (dialog, which) -> dialog.dismiss());
    builder.show();
  }

  public static class EventViewHolder extends RecyclerView.ViewHolder {
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
}
