package com.github.dedis.student20_pop.ui;

import android.os.Build;
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
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;

import com.github.dedis.student20_pop.PoPApplication;
import com.github.dedis.student20_pop.R;
import com.github.dedis.student20_pop.model.Lao;
import com.github.dedis.student20_pop.model.event.Event;
import com.github.dedis.student20_pop.utility.ui.WitnessListAdapter;
import com.github.dedis.student20_pop.utility.ui.eventadapter.AttendeeExpandableListViewEventAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment used to display the Attendee UI
 **/
public class AttendeeFragment extends Fragment {

    public static final String TAG = AttendeeFragment.class.getSimpleName();
    private AttendeeExpandableListViewEventAdapter listViewEventAdapter;
    private ExpandableListView expandableListView;
    private Lao lao;
    private Button propertiesButton;
    private ListView witnessesListView;


    @RequiresApi(api = Build.VERSION_CODES.O)
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_attendee, container, false);
        PoPApplication app = (PoPApplication) (this.getActivity().getApplication());
        lao = app.getCurrentLao();
        List<Event> events = app.getEvents(lao);
        //Display Events
        expandableListView = rootView.findViewById(R.id.exp_list_view);
        listViewEventAdapter = new AttendeeExpandableListViewEventAdapter(this.getActivity(), events);
        expandableListView.setAdapter(listViewEventAdapter);
        expandableListView.expandGroup(0);
        expandableListView.expandGroup(1);

        //Display Properties
        View propertiesView = rootView.findViewById(R.id.properties_view);
        ((TextView) propertiesView.findViewById(R.id.organization_name)).setText(lao.getName());

        final WitnessListAdapter adapter = new WitnessListAdapter(getActivity(), (ArrayList<String>) app.getWitnesses(lao));

        witnessesListView = propertiesView.findViewById(R.id.witness_list);
        witnessesListView.setAdapter(adapter);
        propertiesButton = rootView.findViewById(R.id.tab_properties);

        propertiesButton.setOnClickListener(clicked -> {
            if (propertiesView.getVisibility() == View.GONE) {
                propertiesView.setVisibility(View.VISIBLE);
            } else {
                propertiesView.setVisibility(View.GONE);
            }
        });

        return rootView;
    }
}
