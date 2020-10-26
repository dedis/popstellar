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
    List<String> categories;
    HashMap<String, List<Event>> events;
    Lao lao;  //given from intent or previous fragment



    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_attendee, container, false);

        //Display Events
        expandableListView = rootView.findViewById(R.id.exp_list_view);
        instantiateEvents(lao);
        getEvents();
        listViewEventAdapter = new ExpandableListViewEventAdapter(this.getActivity(), categories, events);
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
        categories = new ArrayList<String>();
        categories.add("Past Events");
        categories.add("Present Events");
        categories.add("Future Events");

        events = new HashMap<String, List<Event>>();
        for (String s: categories){
            events.put(s, new ArrayList<Event>());
        }

        /*
        for (String id: lao.getEvents()){
            Event e = Event.idToEvent(id);
            switch (e.getTime()){
                case PAST:
                    events.get(EventCategory.PAST).add(e);
                    break;
                case PRESENT:
                    events.get(EventCategory.PRESENT).add(e);
                    break;
                case FUTURE:
                    events.get(EventCategory.FUTURE).add(e);
                    break;
                default:
            }
        }

         */

        //Still need to order the events in chronological order
    }

    private void instantiateEvents(Lao lao){
        /*Event e1 = new Event("Event 6", "Right here", Event.EventType.MEETING, EventCategory.FUTURE);
        Event e2 = new Event("Event 5", "Somewhere", Event.EventType.DISCUSSION, EventCategory.FUTURE);
        Event e3 = new Event("Event 4", "I don't know where", Event.EventType.POLL, EventCategory.PRESENT);
        Event e4 = new Event("Event 3", "Right here", Event.EventType.MEETING, EventCategory.PRESENT);
        Event e5 = new Event("Event 2", "Not here", Event.EventType.ROLL_CALL, EventCategory.PRESENT);
        Event e6 = new Event("Event 1", "EPFL", Event.EventType.MEETING, EventCategory.PAST);
*/
        /*
        lao.addEvent(e1);
        lao.addEvent(e2);
        lao.addEvent(e3);
        lao.addEvent(e4);
        lao.addEvent(e5);
        lao.addEvent(e6);
         */

    }



}
