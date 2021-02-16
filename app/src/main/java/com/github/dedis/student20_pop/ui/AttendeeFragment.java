package com.github.dedis.student20_pop.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.ListView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.github.dedis.student20_pop.AttendeeActivity;
import com.github.dedis.student20_pop.PoPApplication;
import com.github.dedis.student20_pop.R;
import com.github.dedis.student20_pop.model.Lao;
import com.github.dedis.student20_pop.model.entities.LAOEntity;
import com.github.dedis.student20_pop.model.event.Event;
import com.github.dedis.student20_pop.utility.ui.adapter.AttendeeEventExpandableListViewAdapter;
import com.github.dedis.student20_pop.utility.ui.adapter.WitnessListViewAdapter;

import java.util.List;

/** Fragment used to display the Attendee UI */
public class AttendeeFragment extends Fragment {

  public static final String TAG = AttendeeFragment.class.getSimpleName();

  private final LAOEntity laoEntity;

  /*
  private AttendeeEventExpandableListViewAdapter listViewEventAdapter;
  private ExpandableListView expandableListView;
  private Button propertiesButton;
  private ListView witnessesListView;*/

  public AttendeeFragment(LAOEntity laoEntity) {
      this.laoEntity = laoEntity;
  }

  public static AttendeeFragment newInstance(LAOEntity laoEntity) {
      return new AttendeeFragment(laoEntity);
  }

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {

    View rootView = inflater.inflate(R.layout.fragment_attendee, container, false);
    /*
    PoPApplication app = (PoPApplication) (this.getActivity().getApplication());
    lao = app.getCurrentLaoUnsafe();
    */


    //TODO: Need get events List<Event> events = laoEntity.getEvents();
    /* Display Events
    expandableListView = rootView.findViewById(R.id.exp_list_view);
    listViewEventAdapter = new AttendeeEventExpandableListViewAdapter(this.getActivity(), events);
    expandableListView.setAdapter(listViewEventAdapter);
    expandableListView.expandGroup(0);
    expandableListView.expandGroup(1);*/

    /* Display Properties
    View propertiesView = rootView.findViewById(R.id.properties_view);
    ((TextView) propertiesView.findViewById(R.id.organization_name)).setText(laoEntity.lao.name);
    SwipeRefreshLayout swipeRefreshLayout = rootView.findViewById(R.id.swipe_refresh);

    final WitnessListViewAdapter witnessListViewAdapter =
        new WitnessListViewAdapter(getActivity(), laoEntity.getWitnesses());

    witnessesListView = propertiesView.findViewById(R.id.witness_list);
    witnessesListView.setAdapter(witnessListViewAdapter);
    propertiesButton = rootView.findViewById(R.id.tab_properties);

    propertiesButton.setOnClickListener(
        clicked -> {
          if (propertiesView.getVisibility() == View.GONE) {
            propertiesView.setVisibility(View.VISIBLE);
          } else {
            propertiesView.setVisibility(View.GONE);
          }
        });

    swipeRefreshLayout.setOnRefreshListener(
        () -> {
          witnessListViewAdapter.notifyDataSetChanged();
          listViewEventAdapter.notifyDataSetChanged();
          if (getFragmentManager() != null) {
            getFragmentManager().beginTransaction().detach(this).attach(this).commit();
          }
          swipeRefreshLayout.setRefreshing(false);
        });
    */

    return rootView;
  }
}
