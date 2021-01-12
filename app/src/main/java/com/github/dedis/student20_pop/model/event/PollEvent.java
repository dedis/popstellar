package com.github.dedis.student20_pop.model.event;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * Class modeling a Poll Event
 */
public final class PollEvent extends Event {

    private final Date startDate;
    private final Date endDate;
    private final Date startTime;
    private final Date endTime;
    private final List<String> choices;
    private final boolean oneOfN;

    /**
     * Constructor for a Poll Event
     *
     * @param question the question of the poll event, considered as the name
     * @param startDate the start date of the poll event
     * @param endDate the end date of the poll event
     * @param startTime the start time of the poll event
     * @param endTime the end time of the poll event
     * @param lao the ID of the associated LAO
     * @param location the location of the poll event
     * @param choices the list of possible choices
     * @param oneOfN true if it is a choose one type of poll
     * @throws IllegalArgumentException if any of the parameters is null
     */
    public PollEvent(String question, Date startDate, Date endDate, Date startTime, Date endTime,
                     String lao, String location, List<String> choices, boolean oneOfN) {
        super(question, Calendar.getInstance().getTime(), lao, location, EventType.POLL);
        if (startDate == null || endDate == null || startTime == null || endTime == null | choices == null || choices.contains(null)) {
            throw new IllegalArgumentException("Trying to create a meeting event with null parameters");
        }
        this.startDate = startDate;
        this.endDate = endDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.choices = choices;
        this.oneOfN = oneOfN;
    }

    /**
     * Returns the start date of the Poll.
     */
    public Date getStartDate() {
        return startDate;
    }

    /**
     * Returns the end date of the Poll.
     */
    public Date getEndDate() {
        return endDate;
    }

    /**
     * Returns the start time of the Poll.
     */
    public Date getStartTime() {
        return startTime;
    }

    /**
     * Returns the end time of the Poll.
     */
    public Date getEndTime() {
        return endTime;
    }

    /**
     * Returns the list of choices of the Poll.
     */
    public List<String> getChoices() {
        return choices;
    }

    /**
     * True if the Poll is one choice, false if multi-choice.
     */
    public boolean isOneOfN() {
        return oneOfN;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        PollEvent pollEvent = (PollEvent) o;
        return oneOfN == pollEvent.oneOfN &&
                Objects.equals(startDate, pollEvent.startDate) &&
                Objects.equals(endDate, pollEvent.endDate) &&
                Objects.equals(startTime, pollEvent.startTime) &&
                Objects.equals(endTime, pollEvent.endTime) &&
                Objects.equals(choices, pollEvent.choices);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), startDate, endDate, startTime, endTime, choices, oneOfN);
    }
}
