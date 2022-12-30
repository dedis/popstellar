package com.github.dedis.popstellar.ui.detail.event;

import android.view.View;

import androidx.appcompat.app.AlertDialog;
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

import java.util.ArrayList;
import java.util.List;

import static com.github.dedis.popstellar.ui.detail.LaoDetailActivity.setCurrentFragment;

public abstract class EventsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
  private List<Event> events;
  private LaoDetailViewModel viewModel;
  private FragmentActivity activity;

  public EventsAdapter(
      List<Event> events, LaoDetailViewModel viewModel, FragmentActivity activity) {
    this.events = new ArrayList<>(events);
    this.viewModel = viewModel;
    this.activity = activity;
  }

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
  protected void handleEventContent(EventViewHolder eventViewHolder, Event event, String TAG) {
    if (event.getType().equals(EventType.ELECTION)) {
      eventViewHolder.getEventIcon().setImageResource(R.drawable.ic_vote);
      Election election = (Election) event;
      eventViewHolder.getEventTitle().setText(election.getName());
      View.OnClickListener listener =
          view -> {
            viewModel.setCurrentElection(election);
            LaoDetailActivity.setCurrentFragment(
                activity.getSupportFragmentManager(),
                R.id.fragment_election,
                ElectionFragment::newInstance);
          };
      eventViewHolder.getEventCard().setOnClickListener(listener);

    } else if (event.getType().equals(EventType.ROLL_CALL)) {
      eventViewHolder.getEventIcon().setImageResource(R.drawable.ic_roll_call);
      RollCall rollCall = (RollCall) event;
      View.OnClickListener listener =
          view -> {
            if (viewModel.isWalletSetup()) {
              viewModel.setCurrentRollCall(rollCall);
              try {
                PoPToken token = viewModel.getCurrentPopToken();
                setCurrentFragment(
                    activity.getSupportFragmentManager(),
                    R.id.fragment_roll_call,
                    () -> RollCallFragment.newInstance(token.getPublicKey()));
              } catch (KeyException e) {
                ErrorUtils.logAndShow(activity, TAG, e, R.string.key_generation_exception);
              } catch (UnknownLaoException e) {
                ErrorUtils.logAndShow(activity, TAG, e, R.string.error_no_lao);
              }
            } else {
              showWalletNotSetupWarning();
            }
          };
      eventViewHolder.getEventCard().setOnClickListener(listener);
      eventViewHolder.getEventTitle().setText(rollCall.getName());
    }
  }

  public void showWalletNotSetupWarning() {
    AlertDialog.Builder builder = new AlertDialog.Builder(activity);
    builder.setTitle("You have to setup up your wallet before connecting.");
    builder.setPositiveButton("Ok", (dialog, which) -> dialog.dismiss());
    builder.show();
  }
}
