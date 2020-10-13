package com.github.dedis.student20_pop.attendeeUI;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

import com.github.dedis.student20_pop.R;

import java.util.HashMap;
import java.util.List;

public class ExpandableListViewAdapter extends BaseExpandableListAdapter {
    private Context context;

    //Future: create enum type
    //        create types for properties/events/descriptions etc, update all methods
    private List<String> eventTypes; //Properties/Past Events/Present Events/Future Events
    private HashMap<String, List<String>> events; //events and properties tho


    public ExpandableListViewAdapter(Context context, List<String> eventTypes, HashMap<String, List<String>> events) {
        this.context = context;
        this.eventTypes = eventTypes;
        this.events = events;
    }

    @Override
    public int getGroupCount() {
        return this.eventTypes.size();
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return this.events.get(this.eventTypes.get(groupPosition)).size();
    }

    @Override
    public Object getGroup(int groupPosition) {
        if (groupPosition >= getGroupCount()){
            return null;
        }
        return this.eventTypes.get(groupPosition);
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {

        if (groupPosition >= getGroupCount()){
            return null;
        }
        if (childPosition >= getChildrenCount(groupPosition)){
            return null;
        }

        return this.events.get(this.eventTypes.get(groupPosition)).get(childPosition);
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

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent){
        String eventType = (String) getGroup(groupPosition);

        if (convertView == null){
            LayoutInflater inflater = (LayoutInflater) this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.event_type_layout, null);
        }

        TextView eventTextView = convertView.findViewById(R.id.event_type);
        eventTextView.setText(eventType);

        return convertView;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {

        String eventTitle = (String) getChild(groupPosition, childPosition);
        //String eventDescription = ...;

        if (convertView == null){
            LayoutInflater inflater = (LayoutInflater)this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.event_layout, null);
        }
        TextView eventTitleTextView = convertView.findViewById(R.id.event_title);
        //TextView descriptionTextView = convertView.findViewById(R.id.event_description);

        eventTitleTextView.setText(eventTitle);
        //descriptionTextView.setText(eventDescription);

        return convertView;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }
}
