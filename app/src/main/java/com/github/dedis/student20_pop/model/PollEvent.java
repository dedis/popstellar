package com.github.dedis.student20_pop.model;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Class modelling a poll event
 */
public final class PollEvent extends Event {
    private final Date startDate;
    private final Date endDate;
    private final Date startTime;
    private final Date endTime;
    private final List<String> choices;
    private final boolean oneOfN;

    /**
     * @param question
     * @param startDate
     * @param endDate
     * @param startTime
     * @param endTime
     * @param lao
     * @param location
     */
    public PollEvent(String question, List<String> choices, boolean oneOfN, Date startDate, Date endDate, Date startTime, Date endTime, String lao, String location){
        super(question, Calendar.getInstance().getTime(), lao, location, "Poll");
        this.startDate = startDate;
        this.endDate = endDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.choices = choices;
        this.oneOfN = oneOfN;
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

}
