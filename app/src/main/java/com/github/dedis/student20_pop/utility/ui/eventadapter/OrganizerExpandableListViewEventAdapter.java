package com.github.dedis.student20_pop.utility.ui.eventadapter;

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
import static com.github.dedis.student20_pop.model.event.Event.EventType.MEETING;
import static com.github.dedis.student20_pop.model.event.Event.EventType.POLL;
import static com.github.dedis.student20_pop.model.event.Event.EventType.ROLL_CALL;

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
}
