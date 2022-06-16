package com.github.dedis.popstellar.ui.detail.event.election.fragments;

import static com.github.dedis.popstellar.utility.Constants.DISABLED_ALPHA;
import static com.github.dedis.popstellar.utility.Constants.ENABLED_ALPHA;
import static com.github.dedis.popstellar.utility.Constants.ID_NULL;

import android.app.AlertDialog;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.VisibleForTesting;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.widget.ImageViewCompat;
import androidx.fragment.app.Fragment;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.model.objects.Election;
import com.github.dedis.popstellar.model.objects.event.EventState;
import com.github.dedis.popstellar.ui.detail.LaoDetailActivity;
import com.github.dedis.popstellar.ui.detail.LaoDetailViewModel;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.EnumMap;
import java.util.Locale;

public class ElectionFragment extends Fragment {

  private final SimpleDateFormat dateFormat =
      new SimpleDateFormat("dd/MM/yyyy HH:mm z", Locale.ENGLISH);
  private LaoDetailViewModel laoDetailViewModel;
  private Election election;
  private View view;

  private Button managementButton;
  private Button actionButton;

  private final EnumMap<EventState, Integer> managementTextMap = buildManagementTextMap();
  private final EnumMap<EventState, Integer> statusTextMap = buildStatusTextMap();
  private final EnumMap<EventState, Integer> statusIconMap = buildStatusIconMap();
  private final EnumMap<EventState, Integer> managementIconMap = buildManagementIconMap();
  private EnumMap<EventState, Integer> managementVisibilityMap;

  private final EnumMap<EventState, Integer> statusColorMap = buildStatusColorMap();
  private final EnumMap<EventState, Integer> managementColorMap = buildManagementColorMap();
  private final EnumMap<EventState, Integer> actionIconMap = buildActionIconMap();
  private final EnumMap<EventState, Integer> actionTextMap = buildActionTextMap();
  private final EnumMap<EventState, Boolean> actionEnablingMap = buildActionEnablingMap();

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
    managementVisibilityMap = buildManagementVisibilityMap();

    if (election == null) {
      election = laoDetailViewModel.getCurrentElection();
    }

    setupElectionContent();
    setupTime(view);
    managementButton.setOnClickListener(
        v -> {
          EventState state = election.getState().getValue();
          switch (state) {
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
            default:
              throw new IllegalStateException(
                  "User should not be able to use the management button when in this state : "
                      + state);
          }
        });

    actionButton.setOnClickListener(
        v -> {
          EventState state = election.getState().getValue();
          switch (state) {
            case OPENED:
              laoDetailViewModel.openCastVotes();
              break;
            case RESULTS_READY:
              laoDetailViewModel.openElectionResults(true);
              break;
            default:
              throw new IllegalStateException(
                  "User should not be able to use the action button in this state :" + state);
          }
        });

    election.getState().observe(getViewLifecycleOwner(), eventState -> setupElectionContent());

    return view;
  }

  private void setupElectionContent() {
    EventState electionState = election.getState().getValue();

    TextView title = view.findViewById(R.id.election_fragment_title);
    title.setText(election.getName());

    // Fill action content
    TextView statusText = view.findViewById(R.id.election_fragment_status);
    Drawable imgAction = getDrawableFromContext(actionIconMap.getOrDefault(electionState, ID_NULL));
    actionButton.setCompoundDrawablesWithIntrinsicBounds(imgAction, null, null, null);
    setButtonEnabling(actionButton, actionEnablingMap.getOrDefault(electionState, false));
    actionButton.setText(actionTextMap.getOrDefault(electionState, ID_NULL));

    // Fill status content
    ImageView statusIcon = view.findViewById(R.id.election_fragment_status_icon);
    Drawable imgStatus = getDrawableFromContext(statusIconMap.getOrDefault(electionState, ID_NULL));
    setImageColor(statusIcon, statusColorMap.getOrDefault(electionState, ID_NULL));
    statusText.setTextColor(
        getResources().getColor(statusColorMap.getOrDefault(electionState, ID_NULL), null));
    statusText.setText(statusTextMap.getOrDefault(electionState, ID_NULL));
    statusIcon.setImageDrawable(imgStatus);

    // Fill management content
    Drawable imgManagement =
        getDrawableFromContext(managementIconMap.getOrDefault(electionState, ID_NULL));
    setButtonColor(managementButton, managementColorMap.getOrDefault(electionState, ID_NULL));
    managementButton.setText(managementTextMap.getOrDefault(electionState, ID_NULL));
    managementButton.setCompoundDrawablesWithIntrinsicBounds(imgManagement, null, null, null);
    managementButton.setVisibility(managementVisibilityMap.getOrDefault(electionState, View.GONE));
  }

  private void setupTime(View view) {
    TextView startTimeDisplay = view.findViewById(R.id.election_fragment_start_time);
    TextView endTimeDisplay = view.findViewById(R.id.election_fragment_end_time);

    Date startTime = new Date(election.getStartTimestampInMillis());
    Date endTime = new Date(election.getEndTimestampInMillis());

    startTimeDisplay.setText(dateFormat.format(startTime));
    endTimeDisplay.setText(dateFormat.format(endTime));
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

  private EnumMap<EventState, Integer> buildManagementTextMap() {
    EnumMap<EventState, Integer> map = new EnumMap<>(EventState.class);
    map.put(EventState.CREATED, R.string.open);
    map.put(EventState.OPENED, R.string.close);

    // Button will be invisible in those state
    map.put(EventState.CLOSED, R.string.close);
    map.put(EventState.RESULTS_READY, R.string.close);

    return map;
  }

  private EnumMap<EventState, Integer> buildStatusTextMap() {
    EnumMap<EventState, Integer> map = new EnumMap<>(EventState.class);
    map.put(EventState.CREATED, R.string.closed);
    map.put(EventState.OPENED, R.string.open);
    map.put(EventState.CLOSED, R.string.waiting_for_results);
    map.put(EventState.RESULTS_READY, R.string.finished);
    return map;
  }

  private EnumMap<EventState, Integer> buildStatusIconMap() {
    EnumMap<EventState, Integer> map = new EnumMap<>(EventState.class);
    map.put(EventState.CREATED, R.drawable.ic_lock);
    map.put(EventState.OPENED, R.drawable.ic_unlock);
    map.put(EventState.CLOSED, R.drawable.ic_wait);
    map.put(EventState.RESULTS_READY, R.drawable.ic_complete);
    return map;
  }

  private EnumMap<EventState, Integer> buildStatusColorMap() {
    EnumMap<EventState, Integer> map = new EnumMap<>(EventState.class);
    map.put(EventState.CREATED, R.color.red);
    map.put(EventState.OPENED, R.color.green);
    map.put(EventState.CLOSED, R.color.colorPrimary);
    map.put(EventState.RESULTS_READY, R.color.green);
    return map;
  }

  private EnumMap<EventState, Integer> buildManagementIconMap() {
    EnumMap<EventState, Integer> map = new EnumMap<>(EventState.class);
    map.put(EventState.CREATED, R.drawable.ic_unlock);
    map.put(EventState.OPENED, R.drawable.ic_lock);

    // Button will be invisible in those state
    map.put(EventState.CLOSED, R.drawable.ic_lock);
    map.put(EventState.RESULTS_READY, R.drawable.ic_lock);

    return map;
  }

  private EnumMap<EventState, Integer> buildManagementColorMap() {
    EnumMap<EventState, Integer> map = new EnumMap<>(EventState.class);
    map.put(EventState.CREATED, R.color.green);
    map.put(EventState.OPENED, R.color.red);

    // Button will be invisible in those state
    map.put(EventState.CLOSED, R.color.red);
    map.put(EventState.RESULTS_READY, R.color.red);

    return map;
  }

  private EnumMap<EventState, Integer> buildActionIconMap() {
    EnumMap<EventState, Integer> map = new EnumMap<>(EventState.class);
    map.put(EventState.CREATED, R.drawable.ic_voting_action);
    map.put(EventState.OPENED, R.drawable.ic_voting_action);
    map.put(EventState.CLOSED, R.drawable.ic_result);
    map.put(EventState.RESULTS_READY, R.drawable.ic_result);
    return map;
  }

  private EnumMap<EventState, Integer> buildActionTextMap() {
    EnumMap<EventState, Integer> map = new EnumMap<>(EventState.class);
    map.put(EventState.CREATED, R.string.vote);
    map.put(EventState.OPENED, R.string.vote);
    map.put(EventState.CLOSED, R.string.results);
    map.put(EventState.RESULTS_READY, R.string.results);
    return map;
  }

  private EnumMap<EventState, Integer> buildManagementVisibilityMap() {
    // Only the organizer may start or end an election
    int organizerVisibility =
        Boolean.TRUE.equals(laoDetailViewModel.isOrganizer().getValue()) ? View.VISIBLE : View.GONE;

    EnumMap<EventState, Integer> map = new EnumMap<>(EventState.class);
    map.put(EventState.CREATED, organizerVisibility);
    map.put(EventState.OPENED, organizerVisibility);
    map.put(EventState.CLOSED, View.GONE); // Button is invisible regardless of user's role
    map.put(EventState.RESULTS_READY, View.GONE); // Button is invisible regardless of user's role
    return map;
  }

  private EnumMap<EventState, Boolean> buildActionEnablingMap() {
    EnumMap<EventState, Boolean> map = new EnumMap<>(EventState.class);
    map.put(EventState.CREATED, false);
    map.put(EventState.OPENED, true);
    map.put(EventState.CLOSED, false);
    map.put(EventState.RESULTS_READY, true);
    return map;
  }

  private void setButtonEnabling(Button button, boolean enabled) {
    button.setAlpha(enabled ? ENABLED_ALPHA : DISABLED_ALPHA);
    button.setEnabled(enabled);
  }

  /**
   * The following is only for testing purposes. Production code should never pass arguments to a
   * fragment instantiation but should rather use arguments
   */
  @VisibleForTesting(otherwise = VisibleForTesting.NONE)
  public ElectionFragment(Election election) {
    this.election = election;
  }

  @VisibleForTesting(otherwise = VisibleForTesting.NONE)
  public static ElectionFragment newInstance(Election election) {
    return new ElectionFragment(election);
  }
}
