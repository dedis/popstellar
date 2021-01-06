package com.github.dedis.student20_pop.model.event;

import java.util.Date;

import static com.github.dedis.student20_pop.model.event.Event.EventType.MEETING;

public class MeetingEvent extends Event {
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
    public MeetingEvent(String name, Date startDate, Date endDate, Date startTime, Date endTime, String lao, String location, String description) {
        super(name, startDate, lao, location, MEETING);

        this.startDate = startDate;
        this.endDate = endDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.description = description;
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
}
