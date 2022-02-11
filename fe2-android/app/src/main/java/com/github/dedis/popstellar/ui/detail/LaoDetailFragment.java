package com.github.dedis.popstellar.ui.detail;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.databinding.LaoDetailFragmentBinding;
import com.github.dedis.popstellar.model.objects.RollCall;
import com.github.dedis.popstellar.model.objects.event.Event;
import com.github.dedis.popstellar.model.objects.event.EventType;
import com.github.dedis.popstellar.model.qrcode.ConnectToLao;
import com.github.dedis.popstellar.repository.remote.LAORequestFactory;
import com.github.dedis.popstellar.ui.detail.event.EventExpandableListViewAdapter;
import com.github.dedis.popstellar.ui.detail.witness.WitnessListViewAdapter;
import com.github.dedis.popstellar.ui.qrcode.ScanningAction;
import com.google.gson.Gson;

import net.glxn.qrgen.android.QRCode;

import java.util.ArrayList;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

/** Fragment used to display the LAO Detail UI */
@AndroidEntryPoint(Fragment.class)
public class LaoDetailFragment extends Hilt_LaoDetailFragment {

  public static final String TAG = LaoDetailFragment.class.getSimpleName();

  @Inject Gson gson;
  @Inject LAORequestFactory requestFactory;

  private LaoDetailFragmentBinding mLaoDetailFragBinding;
  private LaoDetailViewModel mLaoDetailViewModel;
  private WitnessListViewAdapter mWitnessListViewAdapter;
  private EventExpandableListViewAdapter mEventListViewEventAdapter;

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

    return mLaoDetailFragBinding.getRoot();
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    setupWitnessMessageButton();
    setupPropertiesButton();
    setupEditPropertiesButton();
    setupConfirmEditButton();
    setupCancelEditButton();
    setupAddWitnessButton();

    setupEventListAdapter();
    setupEventListUpdates();
    setupWitnessListAdapter();
    setupWitnessListUpdates();

    // TODO: Add witness handler

    // Subscribe to "show/hide properties" event
    mLaoDetailViewModel
        .getShowPropertiesEvent()
        .observe(
            getViewLifecycleOwner(),
            booleanEvent -> {
              Boolean action = booleanEvent.getContentIfNotHandled();
              if (action != null) {
                showHideProperties(action);
              }
            });

    // Subscribe to "edit properties" event
    mLaoDetailViewModel
        .getEditPropertiesEvent()
        .observe(
            getViewLifecycleOwner(),
            booleanEvent -> {
              Boolean action = booleanEvent.getContentIfNotHandled();
              if (action != null) {
                editProperties(action);
              }
            });

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
              ConnectToLao data = new ConnectToLao(requestFactory.getUrl(), lao.getId());
              Bitmap myBitmap = QRCode.from(gson.toJson(data)).bitmap();
              mLaoDetailFragBinding.channelQrCode.setImageBitmap(myBitmap);
            });
  }

  private void setupWitnessMessageButton() {
    Button witnessMessageButton = requireActivity().findViewById(R.id.tab_witness_message_button);
    witnessMessageButton.setOnClickListener(v -> mLaoDetailViewModel.openWitnessMessage());
  }

  private void setupAddWitnessButton() {
    mLaoDetailFragBinding.addWitnessButton.setOnClickListener(
        v -> {
          mLaoDetailViewModel.setScanningAction(ScanningAction.ADD_WITNESS);
          mLaoDetailViewModel.openScanning();
        });
  }

  private void setupPropertiesButton() {
    Button propertiesButton = requireActivity().findViewById(R.id.tab_properties);

    propertiesButton.setOnClickListener(clicked -> mLaoDetailViewModel.toggleShowHideProperties());
  }

  private void setupEditPropertiesButton() {
    mLaoDetailFragBinding.editButton.setOnClickListener(
        clicked -> mLaoDetailViewModel.openEditProperties());
  }

  private void setupConfirmEditButton() {
    mLaoDetailFragBinding.propertiesEditConfirm.setOnClickListener(
        clicked -> mLaoDetailViewModel.confirmEdit());
  }

  private void setupCancelEditButton() {
    mLaoDetailFragBinding.propertiesEditCancel.setOnClickListener(
        clicked -> mLaoDetailViewModel.cancelEdit());
  }

  private void setupWitnessListAdapter() {
    ListView listView = mLaoDetailFragBinding.witnessList;

    mWitnessListViewAdapter =
        new WitnessListViewAdapter(new ArrayList<>(), mLaoDetailViewModel, getActivity());

    listView.setAdapter(mWitnessListViewAdapter);
  }

  private void setupWitnessListUpdates() {
    mLaoDetailViewModel
        .getWitnesses()
        .observe(
            requireActivity(),
            witnesses -> {
              Log.d(TAG, "witnesses updated");
              mWitnessListViewAdapter.replaceList(witnesses);
            });
  }

  private void setupEventListAdapter() {
    ExpandableListView expandableListView = mLaoDetailFragBinding.expListView;

    mEventListViewEventAdapter =
        new EventExpandableListViewAdapter(new ArrayList<>(), mLaoDetailViewModel, getActivity());
    Log.d(TAG, "created adapter");
    expandableListView.setAdapter(mEventListViewEventAdapter);
    expandableListView.expandGroup(0);
    expandableListView.expandGroup(1);
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
    mLaoDetailFragBinding.propertiesLinearLayout.setVisibility(
        Boolean.TRUE.equals(show) ? View.VISIBLE : View.GONE);
  }

  private void editProperties(Boolean edit) {
    mLaoDetailFragBinding.editPropertiesLinearLayout.setVisibility(
        Boolean.TRUE.equals(edit) ? View.VISIBLE : View.GONE);

    // Hide current LAO name and edit button while editing
    final int visibility = Boolean.TRUE.equals(edit) ? View.GONE : View.VISIBLE;
    mLaoDetailFragBinding.editButton.setVisibility(visibility);
    mLaoDetailFragBinding.organizationName.setVisibility(visibility);
  }
}
