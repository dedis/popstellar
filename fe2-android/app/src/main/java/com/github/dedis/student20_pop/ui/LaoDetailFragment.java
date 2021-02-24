package com.github.dedis.student20_pop.ui;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.ListView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.github.dedis.student20_pop.R;
import com.github.dedis.student20_pop.databinding.FragmentLaoDetailBinding;
import com.github.dedis.student20_pop.detail.LaoDetailActivity;
import com.github.dedis.student20_pop.detail.LaoDetailViewModel;
import com.github.dedis.student20_pop.model.event.Event;
import com.github.dedis.student20_pop.utility.ui.adapter.OrganizerEventExpandableListViewAdapter;
import com.github.dedis.student20_pop.utility.ui.adapter.WitnessListViewAdapter;
import com.github.dedis.student20_pop.utility.ui.listener.OnAddWitnessListener;
import com.github.dedis.student20_pop.utility.ui.listener.OnEventTypeSelectedListener;
import java.util.ArrayList;
import java.util.List;

/** Fragment used to display the LAO Detail UI */
public class LaoDetailFragment extends Fragment {

  public static final String TAG = LaoDetailFragment.class.getSimpleName();

  private FragmentLaoDetailBinding mLaoDetailFragBinding;
  private LaoDetailViewModel mLaoDetailViewModel;
  private WitnessListViewAdapter mWitnessListViewAdapter;
  private OrganizerEventExpandableListViewAdapter mEventListViewEventAdapter;
  private OnEventTypeSelectedListener onEventTypeSelectedListener;
  private OnAddWitnessListener onAddWitnessListener;

  public LaoDetailFragment(Context context) {
    if (context instanceof OnEventTypeSelectedListener)
      onEventTypeSelectedListener = (OnEventTypeSelectedListener) context;
    else
      throw new ClassCastException(
          context.toString() + " must implement OnEventTypeSelectedListener");

    if (context instanceof OnAddWitnessListener)
      onAddWitnessListener = (OnAddWitnessListener) context;
    else throw new ClassCastException(context.toString() + " must implement OnAddWitnessListener");
  }

  public static LaoDetailFragment newInstance(Context context) {
    return new LaoDetailFragment(context);
  }

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    mLaoDetailFragBinding = FragmentLaoDetailBinding.inflate(inflater, container, false);

    mLaoDetailViewModel = LaoDetailActivity.obtainViewModel(getActivity());

    mLaoDetailFragBinding.setViewModel(mLaoDetailViewModel);
    mLaoDetailFragBinding.setLifecycleOwner(getActivity());

    return mLaoDetailFragBinding.getRoot();
  }

  @Override
  public void onActivityCreated(@Nullable Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);

    setupPropertiesButton();
    setupEditPropertiesButton();
    setupConfirmEditButton();
    setupCancelEditButton();

    setupEventListAdapter();
    setupWitnessListAdapter();

    setupSwipeRefresh();

    // Subscribe to "show/hide properties" event
    mLaoDetailViewModel
        .getShowPropertiesEvent()
        .observe(
            this,
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
            this,
            booleanEvent -> {
              Boolean action = booleanEvent.getContentIfNotHandled();
              if (action != null) {
                editProperties(action);
              }
            });
  }

  private void setupPropertiesButton() {
    Button propertiesButton = (Button) getActivity().findViewById(R.id.tab_properties);

    propertiesButton.setOnClickListener(clicked -> mLaoDetailViewModel.showHideProperties());
  }

  private void setupEditPropertiesButton() {
    mLaoDetailFragBinding.editButton.setOnClickListener(
        clicked -> {
          mLaoDetailViewModel.openEditProperties();
        });
  }

  private void setupConfirmEditButton() {
    mLaoDetailFragBinding.propertiesEditConfirm.setOnClickListener(
        clicked -> mLaoDetailViewModel.confirmEdit());
  }

  private void setupCancelEditButton() {
    mLaoDetailFragBinding.propertiesEditCancel.setOnClickListener(
        clicked -> {
          mLaoDetailViewModel.cancelEdit();
        });
  }

  private void setupWitnessListAdapter() {
    ListView listView = mLaoDetailFragBinding.witnessList;

    mWitnessListViewAdapter =
        new WitnessListViewAdapter(
            getActivity(),
            mLaoDetailViewModel.getCurrentLao().witness == null
                ? new ArrayList<>()
                : mLaoDetailViewModel.getCurrentLao().witness);

    listView.setAdapter(mWitnessListViewAdapter);
  }

  private void setupEventListAdapter() {
    ExpandableListView expandableListView = mLaoDetailFragBinding.expListView;

    List<Event> events = new ArrayList<>();

    // Get all events from the LAO
    //        events.addAll(transformRollCalls(mLaoDetailViewModel.getCurrentLao().rollCalls));
    //    events.addAll(transformMeetings(mLaoDetailViewModel.getCurrentLao().meetings));

    mEventListViewEventAdapter =
        new OrganizerEventExpandableListViewAdapter(
            getActivity(), events, onEventTypeSelectedListener, mLaoDetailViewModel.isOrganizer());

    expandableListView.setAdapter(mEventListViewEventAdapter);
    expandableListView.expandGroup(0);
    expandableListView.expandGroup(1);
  }

  private void setupSwipeRefresh() {
    mLaoDetailFragBinding.swipeRefresh.setOnRefreshListener(
        () -> {
          mWitnessListViewAdapter.notifyDataSetChanged();
          mEventListViewEventAdapter.notifyDataSetChanged();
          if (getFragmentManager() != null) {
            getFragmentManager().beginTransaction().detach(this).attach(this).commit();
          }
          mLaoDetailFragBinding.swipeRefresh.setRefreshing(false);
        });
  }

  private void showHideProperties(Boolean show) {
    mLaoDetailFragBinding.propertiesLinearLayout.setVisibility(show ? View.VISIBLE : View.GONE);
  }

  private void editProperties(Boolean edit) {
    mLaoDetailFragBinding.editPropertiesLinearLayout.setVisibility(edit ? View.VISIBLE : View.GONE);

    // Hide current LAO name and edit button while editing
    mLaoDetailFragBinding.editButton.setVisibility(edit ? View.GONE : View.VISIBLE);
    mLaoDetailFragBinding.organizationName.setVisibility(edit ? View.GONE : View.VISIBLE);
  }
}
