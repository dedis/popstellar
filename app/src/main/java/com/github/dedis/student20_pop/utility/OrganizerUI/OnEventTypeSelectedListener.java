package com.github.dedis.student20_pop.utility.OrganizerUI;

public interface OnEventTypeSelectedListener {
    /**
     * Enum class for each event type
     */
     enum EventType {
        MEETING, ROLL_CALL, POLL
    }
    public void OnEventTypeSelectedListener(EventType eventType);
}
