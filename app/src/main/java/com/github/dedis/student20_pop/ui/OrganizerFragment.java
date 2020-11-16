package com.github.dedis.student20_pop.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseExpandableListAdapter;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;

import com.github.dedis.student20_pop.R;
import com.github.dedis.student20_pop.model.Event;
import com.github.dedis.student20_pop.model.Keys;
import com.github.dedis.student20_pop.model.Lao;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class OrganizerFragment extends Fragment {


    public static final String TAG = AttendeeFragment.class.getSimpleName();
    private OrganizerExpandableListViewEventAdapter listViewEventAdapter;
    private ExpandableListView expandableListView;
    private Lao lao;  //should be given from intent or previous fragment
    private Button propertiesButton;

    /**
     * Enum class for each event category
     */
    private enum EventCategory {
        PAST, PRESENT, FUTURE
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        //for testing purposes:
        lao = new Lao("LAO I just joined", new Date(), new Keys().getPublicKey());

        View rootView = inflater.inflate(R.layout.fragment_organizer, container, false);

        //Display Events
        expandableListView = rootView.findViewById(R.id.organizer_expandable_list_view);
        listViewEventAdapter = new OrganizerExpandableListViewEventAdapter(this.getActivity(), getEvents(), true);
        expandableListView.setAdapter(listViewEventAdapter);
        expandableListView.expandGroup(0);
        expandableListView.expandGroup(1);

        //Display Properties
        View properties = rootView.findViewById(R.id.properties_view);
        ((TextView) properties.findViewById(R.id.organization_name)).setText(lao.getName());
        ((TextView) properties.findViewById(R.id.witness_list)).setText("Witnesses: [id, id, id]");

        propertiesButton = rootView.findViewById(R.id.tab_properties);
        propertiesButton.setOnClickListener(
                clicked -> {
                    properties.setVisibility((properties.getVisibility() == View.GONE) ? View.VISIBLE : View.GONE);
                });

        return rootView;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private List<Event> getEvents() {
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
        events.add(new Event("Past Event 1", new Date(10 * 1000L), new Keys().getPublicKey(), "EPFL", "Poll"));
        events.add(new Event("Past Event 2", new Date(20 * 1000L), new Keys().getPublicKey(), "CE-6", "Meeting"));
        events.add(new Event("Present Event 1", new Date(500 * 1000L),
                new Keys().getPublicKey(), "Geneva", "Roll-Call"));
        events.add(new Event("Present Event 2", new Date(600 * 1000L),
                new Keys().getPublicKey(), "Lausanne", "Discussion"));
        events.add(new Event("Future Event 1", new Date(5000 * 1000L),
                new Keys().getPublicKey(), "i don't know where yet", "Poll"));

        return events;
    }

    public void launchEventCreationFragment(int eventType) {
        switch (eventType) {
            case 0:
                //TODO
                Log.d("Meeting Event Type ", "Launch here Meeting Event Creation Fragment");
                break;
            case 1:
                //TODO
                Log.d("Roll-Call Event Type ", "Launch here Roll-Call Event Creation Fragment");
                break;
            case 2:
                //TODO
                Log.d("Poll Event Type ", "Launch here Poll Event Creation Fragment");
                break;
            default:
                Log.d("Default Event Type :", "Default Behaviour TBD");
                break;
        }
    }

    public class OrganizerExpandableListViewEventAdapter extends BaseExpandableListAdapter {
        private final Context context;
        private final List<EventCategory> categories;
        private final HashMap<EventCategory, List<Event>> eventsMap;
        private final Boolean isOrganizer;


        /**
         * Constructor for the expandable list view adapter to display the events
         * in the attendee UI
         *
         * @param context
         * @param events  the list of events of the lao
         */
        public OrganizerExpandableListViewEventAdapter(Context context, List<Event> events, boolean isOrganizer) {
            this.context = context;
            this.eventsMap = new HashMap<>();
            this.isOrganizer = isOrganizer;
            this.categories = new ArrayList<>();
            this.categories.add(EventCategory.PAST);
            this.categories.add(EventCategory.PRESENT);
            this.categories.add(EventCategory.FUTURE);
            this.eventsMap.put(EventCategory.PAST, new ArrayList<>());
            this.eventsMap.put(EventCategory.PRESENT, new ArrayList<>());
            this.eventsMap.put(EventCategory.FUTURE, new ArrayList<>());

            putEventsInMap(events, this.eventsMap);
            orderEventsInMap(this.eventsMap);

        }

        /**
         * @return the amount of categories
         */
        @Override
        public int getGroupCount() {
            return this.eventsMap.size();
        }

        /**
         * @param groupPosition
         * @return the amount of events in a given group
         */
        @Override
        public int getChildrenCount(int groupPosition) {
            return this.eventsMap.get(this.categories.get(groupPosition)).size();
        }

        /**
         * @param groupPosition
         * @return the category of a given position
         */
        @Override
        public Object getGroup(int groupPosition) {
            if (groupPosition >= getGroupCount()) {
                return null;
            }
            return this.categories.get(groupPosition);
        }

        /**
         * @param groupPosition
         * @param childPosition
         * @return the event for a given position in a given category
         */
        @Override
        public Object getChild(int groupPosition, int childPosition) {

            if (groupPosition >= getGroupCount()) {
                return null;
            }
            if (childPosition >= getChildrenCount(groupPosition)) {
                return null;
            }

            return this.eventsMap.get(this.categories.get(groupPosition)).get(childPosition);
        }

        @Override
        public long getGroupId(int groupPosition) {
            return groupPosition;
        }

        @Override
        public long getChildId(int groupPosition, int childPosition) {
            return childPosition;
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }

        /**
         * @param groupPosition
         * @param isExpanded
         * @param convertView
         * @param parent
         * @return the view for a given category
         */
        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
            String eventCategory = "";
            switch ((EventCategory) getGroup(groupPosition)) {
                case PAST:
                    eventCategory = getString(R.string.past_events);
                    break;
                case PRESENT:
                    eventCategory = getString(R.string.present_events);
                    break;
                case FUTURE:
                    eventCategory = getString(R.string.future_events);
                    break;
            }

            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater) this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.event_category_layout, null);
            }

            TextView eventTextView = convertView.findViewById(R.id.event_category);
            eventTextView.setText(eventCategory);

            if (isOrganizer) {
                ImageButton addEvent = convertView.findViewById(R.id.add_future_event_button);
                addEvent.setVisibility((getGroup(groupPosition) == EventCategory.FUTURE) ? View.VISIBLE : View.GONE);
                addEvent.setFocusable(View.NOT_FOCUSABLE);
                addEvent.setOnClickListener(v -> {
                    AlertDialog.Builder builderSingle = new AlertDialog.Builder(context);
                    builderSingle.setTitle(R.string.select_event_type_dialog_title);

                    final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(context, android.R.layout.select_dialog_singlechoice);
                    arrayAdapter.add(context.getString(R.string.meeting_event));
                    arrayAdapter.add(context.getString(R.string.roll_call_event));
                    arrayAdapter.add(context.getString(R.string.poll_event));

                    builderSingle.setNegativeButton(context.getString(R.string.button_cancel), (dialog, which) -> dialog.dismiss());
                    builderSingle.setAdapter(arrayAdapter, (dialog, which) -> {
                        launchEventCreationFragment(which);
                    });
                    builderSingle.show();
                });
            }

            return convertView;
        }

        /**
         * @param groupPosition
         * @param childPosition
         * @param isLastChild
         * @param convertView
         * @param parent
         * @return the view for a given event
         */
        @Override
        public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {

            Event event = ((Event) getChild(groupPosition, childPosition));
            String eventTitle = (event.getName() + " : " + event.getType());

            //For now, later: for each specific type of event, show the required content
            String eventDescription = "Time : " + event.getTime() + "\nLocation : " + event.getLocation();


            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater) this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                //now:
                convertView = inflater.inflate(R.layout.event_layout, null);
                //later:
            /*
            switch (event.getType()) {
                case MEETING:
                    convertView = inflater.inflate(R.layout.meeting_layout, null);
                    break;
                case ROLL_CALL:
                    convertView = inflater.inflate(R.layout.rollcall_layout, null);
                    break;
                case POLL:
                    convertView = inflater.inflate(R.layout.poll_layout, null);
                    break;
                case DISCUSSION:
                    convertView = inflater.inflate(R.layout.discussion_layout, null);
                    break;
                default:
                    convertView = inflater.inflate(R.layout.event_layout, null);
                    break;
            }
             */
            }

            TextView eventTitleTextView = convertView.findViewById(R.id.event_title);
            eventTitleTextView.setText(eventTitle);
            //later: put this in the switch, depending on what else to display
            TextView descriptionTextView = convertView.findViewById(R.id.event_description);
            descriptionTextView.setText(eventDescription);
            return convertView;
        }

        @Override
        public boolean isChildSelectable(int groupPosition, int childPosition) {
            return true;
        }

        /**
         * A helper method that places the events in the correct key-value pair
         * according to their times
         *
         * @param events
         * @param eventsMap
         */
        private void putEventsInMap(List<Event> events, HashMap<EventCategory, List<Event>> eventsMap) {
            //For now, the event are put in the different categories according to their time attribute
            //Later, according to the start/end-time
            for (Event event : events) {
                //for now (testing purposes)
                //later: event.getEndTime() < now
                if (event.getTime() < 50) {
                    eventsMap.get(EventCategory.PAST).add(event);
                }
                //later: event.getStartTime()<now && event.getEndTime() > now
                else if (event.getTime() < 1000) {
                    eventsMap.get(EventCategory.PRESENT).add(event);
                } else { //if e.getStartTime() > now
                    eventsMap.get(EventCategory.FUTURE).add(event);
                }
            }
        }

        /**
         * A helper method that orders the events according to their times
         *
         * @param eventsMap
         */
        private void orderEventsInMap(HashMap<EventCategory, List<Event>> eventsMap) {

            for (EventCategory category : categories) {
                Collections.sort(eventsMap.get(category), new EventComparator());
            }
            //2 possibilities: B strictly after A or B nested within A
        }

        private class EventComparator implements Comparator<Event> {
            //later: compare start times
            @Override
            public int compare(Event event1, Event event2) {
                return Long.compare(event1.getTime(), event2.getTime());
            }
        }
    }
}
