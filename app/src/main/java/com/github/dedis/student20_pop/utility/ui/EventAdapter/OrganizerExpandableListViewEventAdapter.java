package com.github.dedis.student20_pop.utility.ui.EventAdapter;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import com.github.dedis.student20_pop.R;
import com.github.dedis.student20_pop.model.event.Event;
import com.github.dedis.student20_pop.utility.ui.organizer.OnEventTypeSelectedListener;

import java.util.List;

import static com.github.dedis.student20_pop.model.event.Event.EventCategory;
import static com.github.dedis.student20_pop.model.event.Event.EventCategory.FUTURE;
import static com.github.dedis.student20_pop.model.event.Event.EventType.*;

/**
 * Adapter to show events of an Organizer
 */
public class OrganizerExpandableListViewEventAdapter extends ExpandableListViewEventAdapter {
    private final OnEventTypeSelectedListener onEventTypeSelectedListener;

    /**
     * Constructor for the expandable list view adapter to display the events
     * in the organizer UI
     *
     * @param context
     * @param events  the list of events of the lao
     */
    public OrganizerExpandableListViewEventAdapter(Context context, List<Event> events, OnEventTypeSelectedListener onEventTypeSelectedListener) {
        super(context, events);
        this.onEventTypeSelectedListener = onEventTypeSelectedListener;
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
                eventCategory = context.getString(R.string.past_events);
                break;
            case PRESENT:
                eventCategory = context.getString(R.string.present_events);
                break;
            case FUTURE:
                eventCategory = context.getString(R.string.future_events);
                break;
        }

        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.layout_event_category, null);
        }

        TextView eventTextView = convertView.findViewById(R.id.event_category);
        eventTextView.setText(eventCategory);

        ImageButton addEvent = convertView.findViewById(R.id.add_future_event_button);
        addEvent.setVisibility((getGroup(groupPosition) == FUTURE) ? View.VISIBLE : View.GONE);
        addEvent.setFocusable(View.NOT_FOCUSABLE);
        addEvent.setOnClickListener(v -> {
            AlertDialog.Builder builderSingle = new AlertDialog.Builder(context);
            builderSingle.setTitle(R.string.select_event_type_dialog_title);

            final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(context, android.R.layout.select_dialog_singlechoice);

            arrayAdapter.insert(context.getString(R.string.meeting_event), MEETING.ordinal());
            arrayAdapter.insert(context.getString(R.string.roll_call_event), ROLL_CALL.ordinal());
            arrayAdapter.insert(context.getString(R.string.poll_event), POLL.ordinal());

            builderSingle.setNegativeButton(context.getString(R.string.button_cancel), (dialog, which) -> dialog.dismiss());
            builderSingle.setAdapter(arrayAdapter, (dialog, which) -> {
                onEventTypeSelectedListener.OnEventTypeSelectedListener(Event.EventType.values()[which]);
            });
            builderSingle.show();
        });


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
        //TODO : For the moment, events are displayed the same if user is attendee or organizer,
        // in the future it could be nice to have a pencil icon to allow organizer to modify an event
        Event event = ((Event) getChild(groupPosition, childPosition));
        String eventTitle = (event.getName() + " : " + event.getType());

        //For now, later: for each specific type of event, show the required content
        String eventDescription = "Time : " + event.getTime() + "\nLocation : " + event.getLocation();


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
