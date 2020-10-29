package com.github.dedis.student20_pop.utility.attendeeUI;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

import com.github.dedis.student20_pop.R;
import com.github.dedis.student20_pop.model.Event;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

public class ExpandableListViewEventAdapter extends BaseExpandableListAdapter {
    private Context context;
    private List<EventCategory> categories;
    private HashMap<EventCategory, List<Event>> eventsMap;

    private enum EventCategory{
        PAST, PRESENT, FUTURE
    }


    public ExpandableListViewEventAdapter(Context context, List<Event> events) {
        this.context = context;
        this.eventsMap = new HashMap<>();
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

    @Override
    public int getGroupCount() {
        return this.eventsMap.size();
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return this.eventsMap.get(this.categories.get(groupPosition)).size();
    }

    @Override
    public Object getGroup(int groupPosition) {
        if (groupPosition >= getGroupCount()){
            return null;
        }
        return this.categories.get(groupPosition);
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {

        if (groupPosition >= getGroupCount()){
            return null;
        }
        if (childPosition >= getChildrenCount(groupPosition)){
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
        return childPosition; //((Event) getChild(groupPosition, childPosition)).getUid();
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent){
        String eventCategory = "";
        switch ((EventCategory) getGroup(groupPosition)){
            case PAST:
                eventCategory = "Past Events";
                break;
            case PRESENT:
                eventCategory = "Present Events";
                break;
            case FUTURE:
                eventCategory = "Future Event";
                break;
        }

        if (convertView == null){
            LayoutInflater inflater = (LayoutInflater) this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.event_category_layout, null);
        }

        TextView eventTextView = convertView.findViewById(R.id.event_category);
        eventTextView.setText(eventCategory);

        return convertView;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {

        Event event = ((Event) getChild(groupPosition, childPosition));
        String eventTitle = (event.getName() + " : " + event.getType());

        //For now, later: for each specific type of event, show the required content
        String eventDescription = "Time : " + event.getTime() + "\nLocation : " + event.getLocation();



        if (convertView == null){
            LayoutInflater inflater = (LayoutInflater)this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
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

    private void putEventsInMap(List<Event> events, HashMap<EventCategory, List<Event>> eventsMap){
        //For now, the event are put in the different categories according to their time attribute
        //Later, according to the start/end-time
        for (Event event: events){
            if (event.getTime() < System.currentTimeMillis()/1000L){ //e.getEndTime < now
                eventsMap.get(EventCategory.PAST).add(event);
            }
            else if (event.getTime() < System.currentTimeMillis()/1000L){ //&&e.getEndTime > now
                eventsMap.get(EventCategory.PRESENT).add(event);
            }
            else{ //if e.getStartTime() > now
                eventsMap.get(EventCategory.FUTURE).add(event);
            }
        }
    }

    private void orderEventsInMap(HashMap<EventCategory, List<Event>> eventsMap){

        for (EventCategory category: categories){
            Collections.sort(eventsMap.get(category), new EventComparator());
        }
        //2 possibilities: B strictly after A or B nested within A
    }

    private class EventComparator implements Comparator<Event> {
        //later: compare start times
        @Override
        public int compare(Event event1, Event event2){
            return Long.compare(event1.getTime(), event2.getTime());
        }
    }


}
