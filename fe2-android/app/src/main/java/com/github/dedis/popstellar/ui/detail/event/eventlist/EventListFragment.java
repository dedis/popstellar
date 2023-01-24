package com.github.dedis.popstellar.ui.detail.event.eventlist;

import android.os.Bundle;
import android.util.Log;
import android.view.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.databinding.EventListFragmentBinding;
import com.github.dedis.popstellar.model.Role;
import com.github.dedis.popstellar.model.objects.event.EventState;
import com.github.dedis.popstellar.model.objects.event.EventType;
import com.github.dedis.popstellar.ui.detail.LaoDetailActivity;
import com.github.dedis.popstellar.ui.detail.LaoDetailViewModel;
import com.github.dedis.popstellar.ui.detail.event.LaoDetailAnimation;
import com.github.dedis.popstellar.ui.detail.event.election.fragments.ElectionSetupFragment;
import com.github.dedis.popstellar.ui.detail.event.rollcall.RollCallCreationFragment;
import com.github.dedis.popstellar.utility.error.ErrorUtils;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

/** Fragment used to display the list of events */
@AndroidEntryPoint
public class EventListFragment extends Fragment {

  public static final String TAG = EventListFragment.class.getSimpleName();

  @Inject Gson gson;

  private EventListFragmentBinding binding;
  private LaoDetailViewModel viewModel;
  private boolean isRotated = false;

  public static EventListFragment newInstance() {
    return new EventListFragment();
  }

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    binding = EventListFragmentBinding.inflate(inflater, container, false);

    viewModel = LaoDetailActivity.obtainViewModel(requireActivity());
    binding.setLifecycleOwner(requireActivity());

    FloatingActionButton addButton = binding.addEvent;
    addButton.setOnClickListener(fabListener);
    viewModel
        .getRole()
        .observe(
            requireActivity(),
            role ->
                addButton.setVisibility(role.equals(Role.ORGANIZER) ? View.VISIBLE : View.GONE));

    binding.addElection.setOnClickListener(openCreateEvent(EventType.ELECTION));
    binding.addElectionText.setOnClickListener(openCreateEvent(EventType.ELECTION));
    binding.addRollCall.setOnClickListener(openCreateEvent(EventType.ROLL_CALL));
    binding.addRollCallText.setOnClickListener(openCreateEvent(EventType.ROLL_CALL));

    // Observing events so that we know when to display the upcoming events card
    viewModel.addDisposable(
        viewModel
            .getEvents()
            .subscribe(
                events ->
                    binding.upcomingEventsCard.setVisibility(
                        events.stream()
                                .anyMatch( // We are looking for any event that is in future section
                                    event ->
                                        // We want created events that are in more than 24 hours
                                        event.getState().equals(EventState.CREATED)
                                            && !event.isEventToday())
                            ? View.VISIBLE
                            : View.GONE),
                error ->
                    ErrorUtils.logAndShow(requireContext(), TAG, R.string.error_event_observed)));

    return binding.getRoot();
  }

  View.OnClickListener fabListener =
      view -> {
        ConstraintLayout laoContainer = binding.laoContainer;
        isRotated = LaoDetailAnimation.rotateFab(view, !isRotated);
        if (isRotated) {
          LaoDetailAnimation.showIn(binding.addRollCall);
          LaoDetailAnimation.showIn(binding.addElection);
          LaoDetailAnimation.showIn(binding.addElectionText);
          LaoDetailAnimation.showIn(binding.addRollCallText);
          LaoDetailAnimation.fadeOut(laoContainer, 1.0f, 0.2f, 300);
          laoContainer.setEnabled(false);
        } else {
          LaoDetailAnimation.showOut(binding.addRollCall);
          LaoDetailAnimation.showOut(binding.addElection);
          LaoDetailAnimation.showOut(binding.addElectionText);
          LaoDetailAnimation.showOut(binding.addRollCallText);
          LaoDetailAnimation.fadeIn(laoContainer, 0.2f, 1.0f, 300);
          laoContainer.setEnabled(true);
        }
      };

  private View.OnClickListener openCreateEvent(EventType type) {
    switch (type) {
      case ROLL_CALL:
        return v ->
            LaoDetailActivity.setCurrentFragment(
                getParentFragmentManager(),
                R.id.fragment_create_roll_call_event,
                RollCallCreationFragment::newInstance);
      case ELECTION:
        return v ->
            LaoDetailActivity.setCurrentFragment(
                getParentFragmentManager(),
                R.id.fragment_setup_election_event,
                ElectionSetupFragment::newInstance);
      default:
        return v -> Log.d(TAG, "unknown event type: " + type);
    }
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    setupEventListAdapter();
  }

  @Override
  public void onResume() {
    super.onResume();
    viewModel.setPageTitle(R.string.event_list);
    viewModel.setIsTab(true);
  }

  private void setupEventListAdapter() {
    RecyclerView eventList = binding.eventList;

    EventListAdapter eventListAdapter =
        new EventListAdapter(viewModel, viewModel.getEvents(), requireActivity());
    Log.d(TAG, "created adapter");
    LinearLayoutManager mLayoutManager = new LinearLayoutManager(getContext());
    eventList.setLayoutManager(mLayoutManager);

    eventList.setAdapter(eventListAdapter);
  }
}
