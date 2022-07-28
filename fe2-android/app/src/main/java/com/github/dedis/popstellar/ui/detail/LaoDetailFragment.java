package com.github.dedis.popstellar.ui.detail;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.widget.ImageView;

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
import com.github.dedis.popstellar.model.qrcode.ConnectToLao;
import com.github.dedis.popstellar.repository.remote.GlobalNetworkManager;
import com.github.dedis.popstellar.ui.detail.event.*;
import com.github.dedis.popstellar.ui.detail.event.election.fragments.ElectionSetupFragment;
import com.github.dedis.popstellar.ui.detail.event.rollcall.RollCallCreationFragment;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;

import net.glxn.qrgen.android.QRCode;

import java.util.ArrayList;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

/** Fragment used to display the LAO Detail UI */
@AndroidEntryPoint
public class LaoDetailFragment extends Fragment {

  public static final String TAG = LaoDetailFragment.class.getSimpleName();

  @Inject Gson gson;
  @Inject GlobalNetworkManager networkManager;

  private LaoDetailFragmentBinding mLaoDetailFragBinding;
  private LaoDetailViewModel mLaoDetailViewModel;
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
    mLaoDetailFragBinding = LaoDetailFragmentBinding.inflate(inflater, container, false);

    mLaoDetailViewModel = LaoDetailActivity.obtainViewModel(requireActivity());
    mLaoDetailFragBinding.setViewModel(mLaoDetailViewModel);
    mLaoDetailFragBinding.setLifecycleOwner(requireActivity());

    FloatingActionButton addButton = mLaoDetailFragBinding.addEvent;
    addButton.setOnClickListener(fabListener);

    mLaoDetailFragBinding.addElection.setOnClickListener(openCreateEvent(EventType.ELECTION));
    mLaoDetailFragBinding.addElectionText.setOnClickListener(openCreateEvent(EventType.ELECTION));
    mLaoDetailFragBinding.addRollCall.setOnClickListener(openCreateEvent(EventType.ROLL_CALL));
    mLaoDetailFragBinding.addRollCallText.setOnClickListener(openCreateEvent(EventType.ROLL_CALL));

    return mLaoDetailFragBinding.getRoot();
  }

  View.OnClickListener fabListener =
      view -> {
        ConstraintLayout laoContainer = mLaoDetailFragBinding.laoContainer;
        isRotated = LaoDetailAnimation.rotateFab(view, !isRotated);
        if (isRotated) {
          LaoDetailAnimation.showIn(mLaoDetailFragBinding.addRollCall);
          LaoDetailAnimation.showIn(mLaoDetailFragBinding.addElection);
          LaoDetailAnimation.showIn(mLaoDetailFragBinding.addElectionText);
          LaoDetailAnimation.showIn(mLaoDetailFragBinding.addRollCallText);
          LaoDetailAnimation.fadeOut(laoContainer, 1.0f, 0.2f, 300);
          laoContainer.setEnabled(false);
        } else {
          LaoDetailAnimation.showOut(mLaoDetailFragBinding.addRollCall);
          LaoDetailAnimation.showOut(mLaoDetailFragBinding.addElection);
          LaoDetailAnimation.showOut(mLaoDetailFragBinding.addElectionText);
          LaoDetailAnimation.showOut(mLaoDetailFragBinding.addRollCallText);
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
    setupQrCodeIconButton();
    setUpQrCloseButton();
    setupEventListAdapter();
    setupEventListUpdates();

    // TODO: Add witness handler

    mLaoDetailViewModel
        .getLaoEvents()
        .observe(
            requireActivity(),
            events -> {
              Log.d(TAG, "Got a list update for LAO events");
              mEventListViewEventAdapter.replaceList(events);
            });

    mLaoDetailViewModel
        .getCurrentLao()
        .observe(
            requireActivity(),
            lao -> {
              ConnectToLao data = new ConnectToLao(networkManager.getCurrentUrl(), lao.getId());
              Bitmap myBitmap = QRCode.from(gson.toJson(data)).bitmap();
              mLaoDetailFragBinding.channelQrCode.setImageBitmap(myBitmap);
            });
  }

  private void setupQrCodeIconButton() {
    ImageView propertiesButton = requireActivity().findViewById(R.id.qr_code_icon);
    propertiesButton.setOnClickListener(clicked -> toggleProperties());
  }

  private void setUpQrCloseButton() {
    ImageView closeButton = requireActivity().findViewById(R.id.qr_icon_close);
    closeButton.setOnClickListener(view -> toggleProperties());
  }

  private void setupEventListAdapter() {
    RecyclerView eventList = mLaoDetailFragBinding.eventList;

    mEventListViewEventAdapter =
        new EventListAdapter(new ArrayList<>(), mLaoDetailViewModel, requireActivity());
    Log.d(TAG, "created adapter");
    LinearLayoutManager mLayoutManager = new LinearLayoutManager(getContext());
    eventList.setLayoutManager(mLayoutManager);

    EventListDivider divider = new EventListDivider(getContext());
    eventList.addItemDecoration(divider);
    eventList.setAdapter(mEventListViewEventAdapter);
  }

  private void setupEventListUpdates() {
    mLaoDetailViewModel
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

  private void toggleProperties() {
    ConstraintLayout laoDetailQrLayout = mLaoDetailFragBinding.laoDetailQrLayout;
    if (Boolean.TRUE.equals(mLaoDetailViewModel.getShowProperties().getValue())) {
      LaoDetailAnimation.fadeIn(laoDetailQrLayout, 0.0f, 1.0f, 500);
      laoDetailQrLayout.setVisibility(View.VISIBLE);
      mLaoDetailViewModel.setShowProperties(true);
    } else {
      LaoDetailAnimation.fadeOut(laoDetailQrLayout, 1.0f, 0.0f, 500);
      laoDetailQrLayout.setVisibility(View.GONE);
      mLaoDetailViewModel.setShowProperties(false);
    }
  }
}
