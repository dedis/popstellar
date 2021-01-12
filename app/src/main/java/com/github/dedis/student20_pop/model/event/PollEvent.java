package com.github.dedis.student20_pop.model.event;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Class modelling a poll event
 */
public final class PollEvent extends Event {
    private final long startTime;
    private final long endTime;
    private final List<String> choices;
    private final boolean oneOfN;

    /**
     * @param question
     * @param startTime
     * @param endTime
     * @param lao
     * @param location
     */
    public PollEvent(String question, List<String> choices, boolean oneOfN, long startTime, long endTime, String lao, String location) {
        super(question, lao, startTime, location, EventType.POLL);
        this.startTime = startTime;
        this.endTime = endTime;
        this.choices = choices;
        this.oneOfN = oneOfN;
    }

    public long getEndTime() {
        return endTime;
    }

    public boolean isOneOfN() {
        return oneOfN;
    }

    public List<String> getChoices() {
        return choices;
    }
}
