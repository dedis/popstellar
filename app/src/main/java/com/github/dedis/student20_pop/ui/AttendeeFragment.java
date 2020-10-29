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
import com.github.dedis.student20_pop.utility.attendeeUI.ExpandableListViewEventAdapter;
import com.github.dedis.student20_pop.model.Event;
import com.github.dedis.student20_pop.model.Lao;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;


//How do we pass the info from Connect UI to Attendee UI ?
//What does an attendee listen for ? is it passed through the LAO?

/**
 * Fragment used to display the Attendee UI
 **/
public class AttendeeFragment extends Fragment {

    public static final String TAG = AttendeeFragment.class.getSimpleName();
    ExpandableListViewEventAdapter listViewEventAdapter;
    ExpandableListView expandableListView;
    Lao lao;  //should be given from intent or previous fragment



    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        //for testing purposes:
        lao = new Lao("LAO I just joined", new Date(), "OrganizerID");

        View rootView = inflater.inflate(R.layout.fragment_attendee, container, false);

        //Display Events
        expandableListView = rootView.findViewById(R.id.exp_list_view);
        listViewEventAdapter = new ExpandableListViewEventAdapter(this.getActivity(),getEvents());
        expandableListView.setAdapter(listViewEventAdapter);
        expandableListView.expandGroup(0);
        expandableListView.expandGroup(1);

        //Display Properties
        View properties = rootView.findViewById(R.id.properties_view);
        ((TextView) properties.findViewById(R.id.organization_name)).setText(lao.getName());
        ((TextView) properties.findViewById(R.id.witness_list)).setText("Witnesses: [id, id, id]");
        //later: witness list, other information?
        //((TextView) properties.findViewById(R.id.witness_list)).setText(lao.getWitnesses().toString());

        return rootView;
    }

    //retrieve events from LAO
    private List<Event> getEvents(){
        /*
        //Later:
        List<String> eventsIds = lao.getEvents();
        events = new ArrayList<>();
        for (String id: eventsIds){
            events.add(???.getEventFromId(id))
        }
         */

        //Now (for testing) :
        ArrayList<Event> events = new ArrayList<>();
        events.add(new Event("Past Event 1", new Date(10*1000L), "laoKey", "EPFL", "Poll"));
        events.add(new Event("Past Event 2", new Date(20*1000L), "laoKey", "CE-6", "Meeting"));
        events.add(new Event("Present Event 1", new Date(500*1000L),
                "laoKey", "Geneva", "Roll-Call"));
        events.add(new Event("Present Event 2", new Date(600*1000L),
                "laoKey", "Lausanne", "Discussion"));
        events.add(new Event("Future Event 1", new Date(5000*1000L),
                "laoKey", "i don't know where yet", "Poll"));

        return events;
    }
}
