package com.github.dedis.popstellar.ui.detail.event.election.fragments;

import android.app.AlertDialog;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.widget.ImageViewCompat;
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

public class ElectionFragment extends Fragment {

  private final SimpleDateFormat dateFormat =
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

  public ElectionFragment(Election election) {
    this.election = election;
  }

  public static ElectionFragment newInstance(Election election) {
    return new ElectionFragment(election);
  }

  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    // Inflate the layout for this fragment
    view = inflater.inflate(R.layout.election_fragment, container, false);

    managementButton = view.findViewById(R.id.election_management_button);
    actionButton = view.findViewById(R.id.election_action_button);

    laoDetailViewModel = LaoDetailActivity.obtainViewModel(requireActivity());

    if (election == null) {
      election = laoDetailViewModel.getCurrentElection();
    }

    setupElectionContent();
    setupTime(view);

    managementButton.setOnClickListener(
        v -> {
          switch (election.getState().getValue()) {
            case CREATED:
              // When implemented across all subsystems go into start election fragment which
              // implements consensus
              new AlertDialog.Builder(getContext())
                  .setTitle(R.string.confirm_title)
                  .setMessage(R.string.election_confirm_open)
                  .setPositiveButton(
                      R.string.yes,
                      (dialogInterface, i) -> laoDetailViewModel.openElection(election))
                  .setNegativeButton(R.string.no, null)
                  .show();
              break;
            case OPENED:
              new AlertDialog.Builder(getContext())
                  .setTitle(R.string.confirm_title)
                  .setMessage(R.string.election_confirm_close)
                  .setPositiveButton(
                      R.string.yes,
                      (dialogInterface, i) -> laoDetailViewModel.endElection(election))
                  .setNegativeButton(R.string.no, null)
                  .show();
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

    election.getState().observe(getViewLifecycleOwner(), eventState -> setupElectionContent());

    return view;
  }

  private void setupElectionContent() {
    EventState electionState = election.getState().getValue();

    TextView title = view.findViewById(R.id.election_fragment_title);
    title.setText(election.getName());

    // Only the organizer may start or end an election
    managementButton.setVisibility(
        Boolean.TRUE.equals(laoDetailViewModel.isOrganizer().getValue())
            ? View.VISIBLE
            : View.GONE);

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
        imgAction = getDrawableFromContext(R.drawable.ic_voting_action);

        imgStatus = getDrawableFromContext(R.drawable.ic_lock);
        statusText.setTextColor(getResources().getColor(R.color.red, null));
        setImageColor(statusIcon, R.color.red);

        imgManagement = getDrawableFromContext(R.drawable.ic_unlock);
        setButtonColor(managementButton, R.color.green);
        break;
      case OPENED:
        actionTextId = R.string.vote;
        statusTextId = R.string.open;
        managementTextId = R.string.close;

        setButtonEnabling(actionButton, true);
        imgAction = getDrawableFromContext(R.drawable.ic_voting_action);

        imgStatus = getDrawableFromContext(R.drawable.ic_unlock);
        setImageColor(statusIcon, R.color.green);
        statusText.setTextColor(getResources().getColor(R.color.green, null));

        imgManagement = getDrawableFromContext(R.drawable.ic_lock);
        setButtonColor(managementButton, R.color.red);

        break;
      case CLOSED:
        statusTextId = R.string.waiting_for_results;
        actionTextId = R.string.results;

        setButtonEnabling(actionButton, false);
        imgAction = getDrawableFromContext(R.drawable.ic_result);

        imgStatus = getDrawableFromContext(R.drawable.ic_wait);
        setImageColor(statusIcon, R.color.colorPrimary);
        statusText.setTextColor(getResources().getColor(R.color.colorPrimary, null));

        managementButton.setVisibility(View.GONE);
        break;
      case RESULTS_READY:
        statusTextId = R.string.finished;
        actionTextId = R.string.results;

        setButtonEnabling(actionButton, true);
        imgAction = getDrawableFromContext(R.drawable.ic_result);

        imgStatus = getDrawableFromContext(R.drawable.ic_complete);
        setImageColor(statusIcon, R.color.green);
        statusText.setTextColor(getResources().getColor(R.color.green, null));

        managementButton.setVisibility(View.GONE);
    }

    actionButton.setCompoundDrawablesWithIntrinsicBounds(imgAction, null, null, null);
    actionButton.setText(actionTextId);

    statusIcon.setImageDrawable(imgStatus);
    statusText.setText(statusTextId);

    if (imgManagement != null) {
      managementButton.setCompoundDrawablesWithIntrinsicBounds(imgManagement, null, null, null);
      managementButton.setText(managementTextId);
    }
  }

  private void setupTime(View view) {
    TextView startTimeDisplay = view.findViewById(R.id.election_fragment_start_time);
    TextView endTimeDisplay = view.findViewById(R.id.election_fragment_end_time);

    Date startTime = new Date(election.getStartTimestampInMillis());
    Date endTime = new Date(election.getEndTimestampInMillis());

    startTimeDisplay.setText(dateFormat.format(startTime));
    endTimeDisplay.setText(dateFormat.format(endTime));
  }

  private void setButtonEnabling(Button button, boolean enabled) {
    button.setAlpha(
        enabled ? Constants.ENABLED_OPAQUE_ALPHA : Constants.DISABLED_TRANSPARENCY_ALPHA);
    button.setEnabled(enabled);
  }

  private Drawable getDrawableFromContext(int id) {
    return AppCompatResources.getDrawable(getContext(), id);
  }

  private void setButtonColor(View v, int colorId) {
    v.setBackgroundTintList(getResources().getColorStateList(colorId, null));
  }

  private void setImageColor(ImageView imageView, int colorId) {
    ImageViewCompat.setImageTintList(imageView, getResources().getColorStateList(colorId, null));
  }
}
