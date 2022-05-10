package com.github.dedis.popstellar.ui.detail;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.databinding.LaoDetailFragmentBinding;
import com.github.dedis.popstellar.model.objects.RollCall;
import com.github.dedis.popstellar.model.objects.event.Event;
import com.github.dedis.popstellar.model.objects.event.EventType;
import com.github.dedis.popstellar.model.qrcode.ConnectToLao;
import com.github.dedis.popstellar.repository.remote.GlobalNetworkManager;
import com.github.dedis.popstellar.ui.detail.event.EventListAdapter;
import com.github.dedis.popstellar.ui.detail.event.LaoDetailAnimation;
import com.github.dedis.popstellar.ui.detail.witness.WitnessListViewAdapter;
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
  private WitnessListViewAdapter mWitnessListViewAdapter;
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

    mLaoDetailFragBinding.addElection.setOnClickListener(addEventLister(EventType.ELECTION));
    mLaoDetailFragBinding.addElectionText.setOnClickListener(addEventLister(EventType.ELECTION));
    mLaoDetailFragBinding.addRollCall.setOnClickListener(addEventLister(EventType.ROLL_CALL));
    mLaoDetailFragBinding.addRollCallText.setOnClickListener(addEventLister(EventType.ROLL_CALL));


    return mLaoDetailFragBinding.getRoot();
  }


  View.OnClickListener fabListener = view ->{
    ConstraintLayout laoContainer = mLaoDetailFragBinding.laoContainer;
    isRotated = LaoDetailAnimation.rotateFab(view, !isRotated);
    if (isRotated){
      LaoDetailAnimation.showIn(mLaoDetailFragBinding.addRollCall);
      LaoDetailAnimation.showIn(mLaoDetailFragBinding.addElection);
      LaoDetailAnimation.showIn(mLaoDetailFragBinding.addElectionText);
      LaoDetailAnimation.showIn(mLaoDetailFragBinding.addRollCallText);
      LaoDetailAnimation.fadeOut(laoContainer, 1.0f, 0.2f, 300);
      laoContainer.setEnabled(false);
    }
    else {
      LaoDetailAnimation.showOut(mLaoDetailFragBinding.addRollCall);
      LaoDetailAnimation.showOut(mLaoDetailFragBinding.addElection);
      LaoDetailAnimation.showOut(mLaoDetailFragBinding.addElectionText);
      LaoDetailAnimation.showOut(mLaoDetailFragBinding.addRollCallText);
      LaoDetailAnimation.fadeIn(laoContainer, 0.2f, 1.0f, 300);
      laoContainer.setEnabled(true);

    }
  };

  private View.OnClickListener addEventLister(EventType type){
    View.OnClickListener listener = v -> {
      mLaoDetailViewModel.chooseEventType(type);
    };
    return listener;
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    // setupWitnessMessageButton();
    setupQrCodeIconButton();
    setUpQrCloseButton();
    //   setupEditPropertiesButton();
    //   setupConfirmEditButton();
    //  setupCancelEditButton();
    //  setupAddWitnessButton();

    setupEventListAdapter();
    setupEventListUpdates();
    //  setupWitnessListAdapter();
    //  setupWitnessListUpdates();

    // TODO: Add witness handler

    // Subscribe to "show/hide properties" event
    mLaoDetailViewModel
        .getShowPropertiesEvent()
        .observe(
            getViewLifecycleOwner(),
            booleanEvent -> {
              Log.d(TAG, "toggle happened");
              Boolean action = booleanEvent.getContentIfNotHandled();
              if (action != null) {
                showHideProperties(action);
              }
            });

    // Subscribe to "edit properties" event
    //    mLaoDetailViewModel
    //        .getEditPropertiesEvent()
    //        .observe(
    //            getViewLifecycleOwner(),
    //            booleanEvent -> {
    //              Boolean action = booleanEvent.getContentIfNotHandled();
    //              if (action != null) {
    //                editProperties(action);
    //              }
    //            });

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

  //  private void setupWitnessMessageButton() {
  //    Button witnessMessageButton =
  // requireActivity().findViewById(R.id.tab_witness_message_button);
  //    witnessMessageButton.setOnClickListener(v -> mLaoDetailViewModel.openWitnessMessage());
  //  }

  //  private void setupAddWitnessButton() {
  //    mLaoDetailFragBinding.addWitnessButton.setOnClickListener(
  //        v -> {
  //          mLaoDetailViewModel.setScanningAction(ScanningAction.ADD_WITNESS);
  //          mLaoDetailViewModel.openScanning();
  //        });
  //  }

  private void setupQrCodeIconButton() {
    ImageView propertiesButton = requireActivity().findViewById(R.id.qr_code_icon);
    propertiesButton.setOnClickListener(clicked -> mLaoDetailViewModel.toggleShowHideProperties());
  }

  private void setUpQrCloseButton() {
    ImageView closeButton = requireActivity().findViewById(R.id.qr_icon_close);
    closeButton.setOnClickListener(view -> mLaoDetailViewModel.toggleShowHideProperties());
  }

  //  private void setupEditPropertiesButton() {
  //    mLaoDetailFragBinding.editButton.setOnClickListener(
  //        clicked -> mLaoDetailViewModel.openEditProperties());
  //  }

  //  private void setupConfirmEditButton() {
  //    mLaoDetailFragBinding.propertiesEditConfirm.setOnClickListener(
  //        clicked -> mLaoDetailViewModel.confirmEdit());
  //  }

  //  private void setupCancelEditButton() {
  //    mLaoDetailFragBinding.propertiesEditCancel.setOnClickListener(
  //        clicked -> mLaoDetailViewModel.cancelEdit());
  //  }

  //  private void setupWitnessListAdapter() {
  //    ListView listView = mLaoDetailFragBinding.witnessList;
  //
  //    mWitnessListViewAdapter =
  //        new WitnessListViewAdapter(new ArrayList<>(), mLaoDetailViewModel, getActivity());
  //
  //    listView.setAdapter(mWitnessListViewAdapter);
  //  }

  //  private void setupWitnessListUpdates() {
  //    mLaoDetailViewModel
  //        .getWitnesses()
  //        .observe(
  //            requireActivity(),
  //            witnesses -> {
  //              Log.d(TAG, "witnesses updated");
  //              mWitnessListViewAdapter.replaceList(witnesses);
  //            });
  //  }

  private void setupEventListAdapter() {
    RecyclerView eventList = mLaoDetailFragBinding.eventList;

    mEventListViewEventAdapter =
        new EventListAdapter(new ArrayList<>(), mLaoDetailViewModel, getActivity());
    Log.d(TAG, "created adapter");
    LinearLayoutManager mLayoutManager = new LinearLayoutManager(getContext());
    eventList.setLayoutManager(mLayoutManager);

    DividerItemDecoration dividerItemDecoration =
        new DividerItemDecoration(getContext(), mLayoutManager.getOrientation());
    eventList.addItemDecoration(dividerItemDecoration);
    eventList.setAdapter(mEventListViewEventAdapter);
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

  private void setupSwipeRefresh() {
    //    mLaoDetailFragBinding.swipeRefresh.setOnRefreshListener(
    //        () -> {
    //          mWitnessListViewAdapter.notifyDataSetChanged();
    //          mEventListViewEventAdapter.notifyDataSetChanged();
    //          if (getFragmentManager() != null) {
    //            getFragmentManager().beginTransaction().detach(this).attach(this).commit();
    //          }
    //          mLaoDetailFragBinding.swipeRefresh.setRefreshing(false);
    //        });
  }

  private void showHideProperties(Boolean show) {
    ConstraintLayout laoDetailQrLayout = mLaoDetailFragBinding.laoDetailQrLayout;
//    mLaoDetailFragBinding.laoDetailQrLayout.setVisibility(
//        Boolean.TRUE.equals(show) ? View.VISIBLE : View.GONE);
    if (Boolean.TRUE.equals(show)){
      LaoDetailAnimation.fadeIn(laoDetailQrLayout, 0.0f, 1.0f, 500);
      laoDetailQrLayout.setVisibility(View.VISIBLE);
    }
    else {
      LaoDetailAnimation.fadeOut(laoDetailQrLayout, 1.0f, 0.0f, 500);
    }

  }

  //  private void editProperties(Boolean edit) {
  //    mLaoDetailFragBinding.editPropertiesLinearLayout.setVisibility(
  //        Boolean.TRUE.equals(edit) ? View.VISIBLE : View.GONE);
  //
  //    // Hide current LAO name and edit button while editing
  //    final int visibility = Boolean.TRUE.equals(edit) ? View.GONE : View.VISIBLE;
  //    mLaoDetailFragBinding.editButton.setVisibility(visibility);
  //    mLaoDetailFragBinding.organizationName.setVisibility(visibility);
  //  }
}
