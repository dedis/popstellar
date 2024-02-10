package com.github.dedis.popstellar.model.objects.event

/** Enum class modeling the Event Types */
enum class EventType
/**
 * Constructor for the Event Type
 *
 * @param suffix the suffix used for the Event ID
 */
(
    /** Returns the suffix for an Event Type, used to compute the ID of an Event. */
    val suffix: String
) {
  ROLL_CALL("R"),
  ELECTION("Election"),
  MEETING("M"),
  POLL("P"),
  DISCUSSION("D")
}
