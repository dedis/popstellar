package com.github.dedis.student20_pop.utility.ui.eventadapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.github.dedis.student20_pop.R;
import com.github.dedis.student20_pop.model.event.Event;

import java.text.SimpleDateFormat;
import java.util.List;

import static com.github.dedis.student20_pop.model.event.Event.EventCategory;

/**
 * Adapter to show events of an Attendee
 */
public class AttendeeExpandableListViewEventAdapter extends ExpandableListViewEventAdapter {

    /**
     * Constructor for the expandable list view adapter to display the events
     * in the attendee UI
     *
     * @param context
     * @param events  the list of events of the lao
     */
    public AttendeeExpandableListViewEventAdapter(Context context, List<Event> events) {
        super(context, events);
    }

    /**
     * @param groupPosition
     * @param isExpanded
     * @param convertView
     * @param parent
     * @return the view for a given category
     */
    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        String eventCategory = "";
        switch ((EventCategory) getGroup(groupPosition)) {
            case PAST:
                eventCategory = "Past Events";
                break;
            case PRESENT:
                eventCategory = "Present Events";
                break;
            case FUTURE:
                eventCategory = "Future Events";
                break;
        }

        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.layout_event_category, null);
        }

        TextView eventTextView = convertView.findViewById(R.id.event_category);
        eventTextView.setText(eventCategory);

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
        String eventTime = DATE_FORMAT.format(event.getTime()*1000L);

        //For now, later: for each specific type of event, show the required content
        String eventDescription = "Time : " + eventTime + "\nLocation : " + event.getLocation();


        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            //now:
            convertView = inflater.inflate(R.layout.layout_event, null);
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
}
