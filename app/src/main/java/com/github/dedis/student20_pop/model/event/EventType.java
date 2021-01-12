package com.github.dedis.student20_pop.model.event;

/**
 * Enum class modeling the Event Types
 */
public enum EventType {
    MEETING("M"), ROLL_CALL("R"), POLL("P"), DISCUSSION("D");

    private final String suffix;

    /**
     * Constructor for the Event Type
     *
     * @param suffix the suffix used for the Event ID
     */
    EventType(String suffix) {
        this.suffix = suffix;
    }

    /**
     * Returns the suffix for an Event Type, used to compute the ID of an Event.
     */
    public String getSuffix() {
        return suffix;
    }
}
