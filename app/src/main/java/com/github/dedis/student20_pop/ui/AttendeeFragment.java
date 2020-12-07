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

import com.github.dedis.student20_pop.R;
import com.github.dedis.student20_pop.model.Keys;
import com.github.dedis.student20_pop.model.Person;
import com.github.dedis.student20_pop.utility.ui.AttendeeExpandableListViewEventAdapter;
import com.github.dedis.student20_pop.model.Event;
import com.github.dedis.student20_pop.model.Lao;
import com.github.dedis.student20_pop.utility.ui.WitnessListAdapter;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Fragment used to display the Attendee UI
 **/
public class AttendeeFragment extends Fragment {

    public static final String TAG = AttendeeFragment.class.getSimpleName();
    AttendeeExpandableListViewEventAdapter listViewEventAdapter;
    ExpandableListView expandableListView;
    Lao lao;  //should be given from intent or previous fragment
    Button propertiesButton;
    private ListView witnessesListView;


    @RequiresApi(api = Build.VERSION_CODES.O)
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        //for testing purposes:
        lao = new Lao("LAO I just joined", new Date(), new Keys().getPublicKey());

        View rootView = inflater.inflate(R.layout.fragment_attendee, container, false);

        //Display Events
        expandableListView = rootView.findViewById(R.id.exp_list_view);
        listViewEventAdapter = new AttendeeExpandableListViewEventAdapter(this.getActivity(),getEvents());
        expandableListView.setAdapter(listViewEventAdapter);
        expandableListView.expandGroup(0);
        expandableListView.expandGroup(1);

        //Display Properties
        View propertiesView = rootView.findViewById(R.id.properties_view);
        ((TextView) propertiesView.findViewById(R.id.organization_name)).setText(lao.getName());
        //TODO : Connect to Backend and retrieve the list of witnesses
        final ArrayList<Person> witnesses = new ArrayList<>();
        witnesses.add(new Person("Alphonse"));
        witnesses.add(new Person("Barbara"));
        witnesses.add(new Person("Charles"));
        witnesses.add(new Person("Deborah"));
        final WitnessListAdapter adapter = new WitnessListAdapter(getActivity(), witnesses);
        witnessesListView = propertiesView.findViewById(R.id.witness_list);
        witnessesListView.setAdapter(adapter);
        propertiesButton = rootView.findViewById(R.id.tab_properties);

        propertiesButton.setOnClickListener(clicked -> {
            if (propertiesView.getVisibility() == View.GONE){
                propertiesView.setVisibility(View.VISIBLE);
            }
            else{
                propertiesView.setVisibility(View.GONE);
            }
        });

        return rootView;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
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
        events.add(new Event("Past Event 1", new Date(10*1000L), new Keys().getPublicKey(), "EPFL", "Poll"));
        events.add(new Event("Past Event 2", new Date(20*1000L), new Keys().getPublicKey(), "CE-6", "Meeting"));
        events.add(new Event("Present Event 1", new Date(500*1000L),
                new Keys().getPublicKey(), "Geneva", "Roll-Call"));
        events.add(new Event("Present Event 2", new Date(600*1000L),
                new Keys().getPublicKey(), "Lausanne", "Discussion"));
        events.add(new Event("Future Event 1", new Date(5000*1000L),
                new Keys().getPublicKey(), "i don't know where yet", "Poll"));

        return events;
    }
}
