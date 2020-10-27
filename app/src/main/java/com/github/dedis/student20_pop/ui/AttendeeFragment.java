package com.github.dedis.student20_pop.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.dedis.student20_pop.R;
import com.github.dedis.student20_pop.attendeeUI.ExpandableListViewEventAdapter;
import com.github.dedis.student20_pop.model.Event;
import com.github.dedis.student20_pop.model.Lao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


//How do we pass the info from Connect UI to Attendee UI ?
//What does an attendee listen for ? is it passed through the LAO?

public class AttendeeFragment extends Fragment {

    public static final String TAG = AttendeeFragment.class.getSimpleName();
    ExpandableListViewEventAdapter listViewEventAdapter;
    ExpandableListView expandableListView;
    List<Event> events;
    Lao lao;  //given from intent or previous fragment



    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_attendee, container, false);

        //Display Events
        expandableListView = rootView.findViewById(R.id.exp_list_view);
        getEvents();
        listViewEventAdapter = new ExpandableListViewEventAdapter(this.getActivity(),events);
        expandableListView.setAdapter(listViewEventAdapter);
        expandableListView.expandGroup(0);
        expandableListView.expandGroup(1);

        //Display Properties
        View properties = rootView.findViewById(R.id.properties_view);
        ((TextView) properties.findViewById(R.id.organization_name)).setText(lao.getName());
        ((TextView) properties.findViewById(R.id.witness_list)).setText(lao.getWitnesses().toString());

        return rootView;
    }

    //retrieve events from LAO
    private void getEvents(){
        /*
        List<String> eventsIds = lao.getEvents();
        events = new ArrayList<>();
        for (String id: eventsIds){
            events.add(???.getEventFromId(id))
        }
         */
    }
}
