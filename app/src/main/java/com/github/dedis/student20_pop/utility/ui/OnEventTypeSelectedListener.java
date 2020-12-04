package com.github.dedis.student20_pop.utility.ui;

public interface OnEventTypeSelectedListener {
    /**
     * Enum class for each event type
     */
    enum EventType {
        MEETING, ROLL_CALL, POLL
    }

    void OnEventTypeSelectedListener(EventType eventType);
}
