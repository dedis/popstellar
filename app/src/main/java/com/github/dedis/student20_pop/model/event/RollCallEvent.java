package com.github.dedis.student20_pop.model.event;

import androidx.databinding.ObservableArrayList;

import java.util.Objects;

import static com.github.dedis.student20_pop.model.event.Event.EventType.ROLL_CALL;
import static com.github.dedis.student20_pop.model.event.RollCallEvent.AddAttendeeResult.ADD_ATTENDEE_ALREADY_EXISTS;
import static com.github.dedis.student20_pop.model.event.RollCallEvent.AddAttendeeResult.ADD_ATTENDEE_SUCCESSFUL;

/**
 * Class modelling a Roll-Call event
 */
public final class RollCallEvent extends Event {
    private final long startTime;
    private final long endTime;
    private final String description;

    /**
     * @param name
     * @param startTime
     * @param endTime
     * @param lao
     * @param location
     */
    public RollCallEvent(String name, long startTime, long endTime, String lao, String location, String description, ObservableArrayList<String> attendees) {
        super(name, lao, startTime, location, ROLL_CALL);
        this.startTime = startTime;
        this.endTime = endTime;
        this.description = description;
        this.setAttendees(attendees);
    }

    public long getEndTime() {
        return endTime;
    }

    public String getDescription() {
        return description;
    }

    public AddAttendeeResult addAttendee(String attendeeId) {
        if (Objects.requireNonNull(getAttendees()).contains(attendeeId)) {
            return ADD_ATTENDEE_ALREADY_EXISTS;
        } else {
            getAttendees().add(attendeeId);
            return ADD_ATTENDEE_SUCCESSFUL;
        }
    }

    /**
     * Type of results when adding an attendee
     */
    public enum AddAttendeeResult {
        ADD_ATTENDEE_SUCCESSFUL,
        ADD_ATTENDEE_ALREADY_EXISTS,
        ADD_ATTENDEE_UNSUCCESSFUL
    }
}
