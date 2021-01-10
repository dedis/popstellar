package com.github.dedis.student20_pop.utility.ui.eventadapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.github.dedis.student20_pop.R;
import com.github.dedis.student20_pop.model.event.Event;

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
}
