package com.github.dedis.student20_pop.model.event;

import java.util.Calendar;
import java.util.Date;
import java.util.Objects;

import static com.github.dedis.student20_pop.model.event.EventType.MEETING;

/**
 * Class modeling a Meeting Event
 */
public class MeetingEvent extends Event {

    private final Date startDate;
    private final Date endDate;
    private final Date startTime;
    private final Date endTime;
    private final String description;

    /**
     * Constructor for a Meeting Event
     *
     * @param name the name of the meeting event
     * @param startDate the start date of the meeting event
     * @param endDate the end date of the meeting event
     * @param startTime the start time of the meeting event
     * @param endTime the end time of the meeting event
     * @param lao the ID of the associated LAO
     * @param location the location of the meeting event
     * @param description the description of the meeting event
     * @throws IllegalArgumentException if any of the parameters is null
     */
    public MeetingEvent(String name, Date startDate, Date endDate, Date startTime, Date endTime,
                        String lao, String location, String description) {
        super(name, Calendar.getInstance().getTime(), lao, location, MEETING);
        if (startDate == null || endDate == null || startTime == null || endTime == null | description == null) {
            throw new IllegalArgumentException("Trying to create a meeting event with null parameters");
        }
        this.startDate = startDate;
        this.endDate = endDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.description = description;
    }

    /**
     * Returns the start date of the Meeting.
     */
    public Date getStartDate() {
        return startDate;
    }

    /**
     * Returns the end date of the Meeting.
     */
    public Date getEndDate() {
        return endDate;
    }

    /**
     * Returns the start time of the Meeting.
     */
    public Date getStartTime() {
        return startTime;
    }

    /**
     * Returns the end time of the Meeting.
     */
    public Date getEndTime() {
        return endTime;
    }

    /**
     * Returns the description of the Meeting.
     */
    public String getDescription() {
        return description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        MeetingEvent that = (MeetingEvent) o;
        return Objects.equals(startDate, that.startDate) &&
                Objects.equals(endDate, that.endDate) &&
                Objects.equals(startTime, that.startTime) &&
                Objects.equals(endTime, that.endTime) &&
                Objects.equals(description, that.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), startDate, endDate, startTime, endTime, description);
    }
}
