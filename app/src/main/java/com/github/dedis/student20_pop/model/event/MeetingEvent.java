package com.github.dedis.student20_pop.model.event;

import java.util.Date;

import static com.github.dedis.student20_pop.model.event.Event.EventType.MEETING;

/**
 * Class modelling an Meeting Event
 */
public class MeetingEvent extends Event {
    private final long endTime;
    private final String description;

    /**
     * @param name
     * @param startTime
     * @param endTime
     * @param lao
     * @param location
     */
    public MeetingEvent(String name, long startTime, long endTime, String lao, String location, String description) {
        super(name, lao, startTime, location, MEETING);
        this.endTime = endTime;
        this.description = description;
    }

    public long getEndTime() {
        return endTime;
    }

    public String getDescription() {
        return description;
    }
}
