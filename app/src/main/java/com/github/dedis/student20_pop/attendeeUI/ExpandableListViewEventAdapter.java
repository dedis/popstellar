package com.github.dedis.student20_pop.attendeeUI;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

import com.github.dedis.student20_pop.R;
import com.github.dedis.student20_pop.model.Event;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExpandableListViewEventAdapter extends BaseExpandableListAdapter {
    private Context context;
    private List<EventCategory> categories; //past, present or future
    private HashMap<EventCategory, List<Event>> events;

    private enum EventCategory{
        PAST, PRESENT, FUTURE
    }


    public ExpandableListViewEventAdapter(Context context, List<EventCategory> categories,
                                          HashMap<EventCategory, List<Event>> events) {
        this.categories = categories;
        this.context = context;
        this.events = events;
    }

    @Override
    public int getGroupCount() {
        return this.categories.size();
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return this.events.get(this.categories.get(groupPosition)).size();
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

        return this.events.get(this.categories.get(groupPosition)).get(childPosition);
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
            convertView = inflater.inflate(R.layout.event_type_layout, null);
        }

        TextView eventTextView = convertView.findViewById(R.id.event_type);
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
            convertView = inflater.inflate(R.layout.event_layout, null);
        }
        TextView eventTitleTextView = convertView.findViewById(R.id.event_title);
        TextView descriptionTextView = convertView.findViewById(R.id.event_description);

        eventTitleTextView.setText(eventTitle);
        descriptionTextView.setText(eventDescription);

        return convertView;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }
}
