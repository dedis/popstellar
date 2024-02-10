package com.github.dedis.popstellar.model.objects.event

/**
 * Indicates in which state the event is This is useful for example for displaying whether an event
 * is currently taking place or not Other states could be added if needed
 */
enum class EventState {
  CREATED,
  OPENED,
  CLOSED,
  RESULTS_READY
}
