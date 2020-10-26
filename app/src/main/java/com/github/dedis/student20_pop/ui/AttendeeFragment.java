package com.github.dedis.student20_pop.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.dedis.student20_pop.R;
import com.github.dedis.student20_pop.attendeeUI.ExpandableListViewEventAdapter;
import com.github.dedis.student20_pop.model.Event;
import com.github.dedis.student20_pop.model.Event.EventCategory;
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
    List<EventCategory> categories;
    HashMap<EventCategory, List<Event>> events;
    Lao lao;  //given from intent or previous fragment



    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_attendee, container, false);

        expandableListView = rootView.findViewById(R.id.exp_list_view);
        instantiateProperties(lao);
        instantiateEvents(lao);
        getEvents();

        listViewEventAdapter = new ExpandableListViewEventAdapter(this.getActivity(), categories, events);
        expandableListView.setAdapter(listViewEventAdapter);
        expandableListView.expandGroup(0);
        expandableListView.expandGroup(1);


        return rootView;


    }

    //retrieve events from LAO
    private void getEvents(){
        categories = new ArrayList<EventCategory>();
        categories.add(EventCategory.PAST);
        categories.add(EventCategory.PRESENT);
        categories.add(EventCategory.FUTURE);

        events = new HashMap<EventCategory, List<Event>>();
        events.put(EventCategory.PAST, new ArrayList<Event>());
        events.put(EventCategory.PRESENT, new ArrayList<Event>());
        events.put(EventCategory.FUTURE, new ArrayList<Event>());

        /*for (Event e: lao.getEvents()){
            switch (e.getCategory()){
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
        Event e1 = new Event("Event 6", "Right here", Event.EventType.MEETING, EventCategory.FUTURE);
        Event e2 = new Event("Event 5", "Somewhere", Event.EventType.DISCUSSION, EventCategory.FUTURE);
        Event e3 = new Event("Event 4", "I don't know where", Event.EventType.POLL, EventCategory.PRESENT);
        Event e4 = new Event("Event 3", "Right here", Event.EventType.MEETING, EventCategory.PRESENT);
        Event e5 = new Event("Event 2", "Not here", Event.EventType.ROLL_CALL, EventCategory.PRESENT);
        Event e6 = new Event("Event 1", "EPFL", Event.EventType.MEETING, EventCategory.PAST);

        /*
        lao.addEvent(e1);
        lao.addEvent(e2);
        lao.addEvent(e3);
        lao.addEvent(e4);
        lao.addEvent(e5);
        lao.addEvent(e6);
         */

    }

    private void instantiateProperties(Lao lao){

    }

}
