package com.github.dedis.popstellar.model.objects.event

import com.github.dedis.popstellar.utility.Constants

/** Class modeling an Event */
abstract class Event : Comparable<Event> {
  abstract val startTimestamp: Long
  abstract val endTimestamp: Long

  val startTimestampInMillis: Long
    get() = startTimestamp * 1000

  val endTimestampInMillis: Long
    get() = endTimestamp * 1000

  abstract val type: EventType
  abstract val state: EventState?
  abstract val name: String

  override fun compareTo(other: Event): Int {
    val start = other.startTimestamp.compareTo(startTimestamp)
    return if (start != 0) start else other.endTimestamp.compareTo(endTimestamp)
  }

  val isEventEndingToday: Boolean
    /** @return true if event the event takes place within 24 hours */
    get() {
      val currentTime = System.currentTimeMillis()
      return endTimestampInMillis - currentTime < Constants.MS_IN_A_DAY
    }

  val isStartPassed: Boolean
    get() = System.currentTimeMillis() >= startTimestampInMillis

  val isEndPassed: Boolean
    get() = System.currentTimeMillis() >= endTimestampInMillis
}
