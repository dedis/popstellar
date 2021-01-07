package com.github.dedis.student20_pop.model.event;

import androidx.databinding.ObservableArrayList;

import java.util.Date;
import java.util.Objects;

import static com.github.dedis.student20_pop.model.event.Event.EventType.ROLL_CALL;
import static com.github.dedis.student20_pop.model.event.RollCallEvent.AddAttendeeResult.ADD_ATTENDEE_ALREADY_EXISTS;
import static com.github.dedis.student20_pop.model.event.RollCallEvent.AddAttendeeResult.ADD_ATTENDEE_SUCCESSFUL;

public class RollCallEvent extends Event {
    private final Date startDate;
    private final Date endDate;
    private final Date startTime;
    private final Date endTime;
    private final String description;

    /**
     * @param name
     * @param startDate
     * @param endDate
     * @param startTime
     * @param endTime
     * @param lao
     * @param location
     */
    public RollCallEvent(String name, Date startDate, Date endDate, Date startTime, Date endTime, String lao, String location, String description, ObservableArrayList<String> attendees) {
        super(name, startDate, lao, location, ROLL_CALL);

        this.startDate = startDate;
        this.endDate = endDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.description = description;
        this.setAttendees(attendees);
    }

    public Date getStartDate() {
        return startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public Date getStartTime() {
        return startTime;
    }

    public Date getEndTime() {
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
