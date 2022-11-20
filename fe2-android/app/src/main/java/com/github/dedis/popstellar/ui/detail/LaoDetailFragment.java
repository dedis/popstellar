package com.github.dedis.popstellar.ui.detail;

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
import com.github.dedis.popstellar.databinding.LaoDetailFragmentBinding;
import com.github.dedis.popstellar.model.objects.RollCall;
import com.github.dedis.popstellar.model.objects.event.Event;
import com.github.dedis.popstellar.model.objects.event.EventType;
import com.github.dedis.popstellar.repository.remote.GlobalNetworkManager;
import com.github.dedis.popstellar.ui.detail.event.*;
import com.github.dedis.popstellar.ui.detail.event.election.fragments.ElectionSetupFragment;
import com.github.dedis.popstellar.ui.detail.event.rollcall.RollCallCreationFragment;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;

import java.util.ArrayList;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

/** Fragment used to display the LAO Detail UI */
@AndroidEntryPoint
public class LaoDetailFragment extends Fragment {

  public static final String TAG = LaoDetailFragment.class.getSimpleName();

  @Inject Gson gson;
  @Inject GlobalNetworkManager networkManager;

  private LaoDetailFragmentBinding binding;
  private LaoDetailViewModel viewModel;
  private EventListAdapter mEventListViewEventAdapter;
  private boolean isRotated = false;

  public static LaoDetailFragment newInstance() {
    return new LaoDetailFragment();
  }

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    binding = LaoDetailFragmentBinding.inflate(inflater, container, false);

    viewModel = LaoDetailActivity.obtainViewModel(requireActivity());
    binding.setViewModel(viewModel);
    binding.setLifecycleOwner(requireActivity());

    FloatingActionButton addButton = binding.addEvent;
    addButton.setOnClickListener(fabListener);

    binding.addElection.setOnClickListener(openCreateEvent(EventType.ELECTION));
    binding.addElectionText.setOnClickListener(openCreateEvent(EventType.ELECTION));
    binding.addRollCall.setOnClickListener(openCreateEvent(EventType.ROLL_CALL));
    binding.addRollCallText.setOnClickListener(openCreateEvent(EventType.ROLL_CALL));

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
        return v -> {
          LaoDetailActivity.setCurrentFragment(
              getParentFragmentManager(),
              R.id.fragment_create_roll_call_event,
              RollCallCreationFragment::newInstance);
          viewModel.setPageTitle(getString(R.string.roll_call_setup_title));
        };
      case ELECTION:
        return v -> {
          LaoDetailActivity.setCurrentFragment(
              getParentFragmentManager(),
              R.id.fragment_setup_election_event,
              ElectionSetupFragment::newInstance);
          viewModel.setPageTitle(getString(R.string.election_setup_title));
        };
      default:
        return v -> Log.d(TAG, "unknown event type: " + type);
    }
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    setupEventListAdapter();
    setupEventListUpdates();

    viewModel
        .getLaoEvents()
        .observe(
            requireActivity(),
            events -> {
              Log.d(TAG, "Got a list update for LAO events");
              mEventListViewEventAdapter.replaceList(events);
            });
  }

  private void setupEventListAdapter() {
    RecyclerView eventList = binding.eventList;

    mEventListViewEventAdapter =
        new EventListAdapter(new ArrayList<>(), viewModel, requireActivity());
    Log.d(TAG, "created adapter");
    LinearLayoutManager mLayoutManager = new LinearLayoutManager(getContext());
    eventList.setLayoutManager(mLayoutManager);

    EventListDivider divider = new EventListDivider(getContext());
    eventList.addItemDecoration(divider);
    eventList.setAdapter(mEventListViewEventAdapter);
  }

  private void setupEventListUpdates() {
    viewModel
        .getLaoEvents()
        .observe(
            requireActivity(),
            events -> {
              Log.d(TAG, "Got an event list update");
              for (Event event : events) {
                if (event.getType() == EventType.ROLL_CALL) {
                  Log.d(TAG, ((RollCall) event).getDescription());
                }
              }
              mEventListViewEventAdapter.replaceList(events);
            });
  }
}
