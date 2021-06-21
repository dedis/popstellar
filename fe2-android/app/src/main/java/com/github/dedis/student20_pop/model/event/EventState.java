package com.github.dedis.student20_pop.model.event;

/**
 * Indicates in which state the event is
 * This is useful for example for displaying whether an event is currently taking place or not
 * Other states could be added if needed
 */
public enum EventState {
    CREATED,
    OPENED,
    CLOSED,
    RESULTS_READY
}
