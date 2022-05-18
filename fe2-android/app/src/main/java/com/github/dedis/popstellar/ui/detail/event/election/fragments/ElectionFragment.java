package com.github.dedis.popstellar.ui.detail.event.election.fragments;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.fragment.app.Fragment;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.model.objects.Election;
import com.github.dedis.popstellar.model.objects.event.EventState;
import com.github.dedis.popstellar.ui.detail.LaoDetailActivity;
import com.github.dedis.popstellar.ui.detail.LaoDetailViewModel;
import com.github.dedis.popstellar.utility.Constants;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * A simple {@link Fragment} subclass. Use the {@link ElectionFragment#newInstance} factory method
 * to create an instance of this fragment.
 */
public class ElectionFragment extends Fragment {

  protected final SimpleDateFormat DATE_FORMAT =
      new SimpleDateFormat("dd/MM/yyyy HH:mm z", Locale.ENGLISH);
  private LaoDetailViewModel laoDetailViewModel;
  private Election election;
  private View view;

  private Button managementButton;
  private Button actionButton;

  public static ElectionFragment newInstance() {
    return new ElectionFragment();
  }

  public ElectionFragment() { // required empty constructor
  }

  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    // Inflate the layout for this fragment
    view = inflater.inflate(R.layout.election_fragment, container, false);

    managementButton = view.findViewById(R.id.election_management_button);
    actionButton = view.findViewById(R.id.election_action_button);

    laoDetailViewModel = LaoDetailActivity.obtainViewModel(requireActivity());
    election = laoDetailViewModel.getCurrentElection();

    setupElectionContent();
    setupTime(view);

    managementButton.setOnClickListener(
        v -> {
          switch (election.getState().getValue()) {
            case CREATED:
              laoDetailViewModel.openStartElection(true);
              break;
            case OPENED:
              laoDetailViewModel.endElection(election);
              break;
            case CLOSED:
            case RESULTS_READY:
              throw new IllegalStateException(
                  "User should not be able to use the management button when in this state");
          }
        });
    actionButton.setOnClickListener(
        v -> {
          switch (election.getState().getValue()) {
            case CLOSED:
            case CREATED:
              throw new IllegalStateException(
                  "User should not be able to use the action button in this state");
            case OPENED:
              laoDetailViewModel.openCastVotes();
              break;
            case RESULTS_READY:
              laoDetailViewModel.openElectionResults(true);
          }
        });

    return view;
  }

  private void setupElectionContent() {
    EventState electionState = election.getState().getValue();
    boolean isOrganizer = laoDetailViewModel.isOrganizer().getValue();

    TextView title = view.findViewById(R.id.election_fragment_title);
    title.setText(election.getName());

    // Only the organizer may start or end an election
    managementButton.setVisibility(isOrganizer ? View.VISIBLE : View.GONE);

    ImageView statusIcon = view.findViewById(R.id.election_fragment_status_icon);
    TextView statusText = view.findViewById(R.id.election_fragment_status);

    Drawable imgStatus = null;
    Drawable imgAction = null;
    Drawable imgManagement = null;

    int actionTextId = 0;
    int statusTextId = 0;
    int managementTextId = 0;

    switch (electionState) {
      case CREATED:
        actionTextId = R.string.vote;
        statusTextId = R.string.closed;
        managementTextId = R.string.open;

        setButtonEnabling(actionButton, false);
        imgAction = getDrawableFromContext(R.drawable.ic_voting_action, R.color.white);

        imgStatus = getDrawableFromContext(R.drawable.ic_lock, R.color.red);
        statusText.setTextColor(getResources().getColor(R.color.red, null));

        imgManagement = getDrawableFromContext(R.drawable.ic_unlock, R.color.white);
        managementButton.setBackgroundColor(getResources().getColor(R.color.green, null));
        break;
      case OPENED:
        actionTextId = R.string.vote;
        statusTextId = R.string.open;
        managementTextId = R.string.close;

        setButtonEnabling(actionButton, true);
        imgAction = getDrawableFromContext(R.drawable.ic_voting_action, R.color.white);

        imgStatus = getDrawableFromContext(R.drawable.ic_unlock, R.color.green);
        statusText.setTextColor(getResources().getColor(R.color.green, null));

        imgManagement = getDrawableFromContext(R.drawable.ic_lock, R.color.white);
        managementButton.setBackgroundColor(getResources().getColor(R.color.red, null));

        break;
      case CLOSED:
        statusTextId = R.string.waiting_for_results;
        actionTextId = R.string.results;

        setButtonEnabling(actionButton, false);
        imgAction = getDrawableFromContext(R.drawable.ic_result, R.color.white);

        imgStatus = getDrawableFromContext(R.drawable.ic_wait, R.color.colorPrimary);
        statusText.setTextColor(getResources().getColor(R.color.colorPrimary, null));

        managementButton.setVisibility(View.GONE);
        break;
      case RESULTS_READY:
        statusTextId = R.string.finished;
        actionTextId = R.string.results;

        setButtonEnabling(actionButton, true);
        imgAction = getDrawableFromContext(R.drawable.ic_result, R.color.white);

        imgStatus = getDrawableFromContext(R.drawable.ic_complete, R.color.green);
        statusText.setTextColor(getResources().getColor(R.color.green, null));

        managementButton.setVisibility(View.GONE);
    }

    actionButton.setCompoundDrawables(imgAction, null, null, null);
    actionButton.setText(actionTextId);

    statusIcon.setImageDrawable(imgStatus);
    statusText.setText(statusTextId);

    if (imgManagement != null) {
      managementButton.setCompoundDrawables(imgManagement, null, null, null);
      managementButton.setText(managementTextId);
    }
  }

  private void setupTime(View view) {
    TextView startTimeDisplay = view.findViewById(R.id.election_fragment_start_time);
    TextView endTimeDisplay = view.findViewById(R.id.election_fragment_end_time);

    Date startTime = new Date(election.getStartTimestampInMillis());
    Date endTime = new Date(election.getStartTimestampInMillis());

    startTimeDisplay.setText(DATE_FORMAT.format(startTime));
    endTimeDisplay.setText(DATE_FORMAT.format(endTime));
  }

  private void setButtonEnabling(Button button, boolean enabled) {
    button.setAlpha(
        enabled ? Constants.ENABLED_OPAQUE_ALPHA : Constants.DISABLED_TRANSPARENCY_ALPHA);
    button.setEnabled(enabled);
  }

  private Drawable getDrawableFromContext(int id, int colorId) {
    Drawable unWrappedDrawable = AppCompatResources.getDrawable(getContext(), id);
    Drawable wrappedDrawable = DrawableCompat.wrap(unWrappedDrawable);
    DrawableCompat.setTint(wrappedDrawable, colorId);
    return wrappedDrawable;
  }
}
