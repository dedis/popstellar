package com.github.dedis.popstellar.model.objects.event;

import com.github.dedis.popstellar.utility.Constants;

/** Class modeling an Event */
public abstract class Event implements Comparable<Event> {

  public abstract long getStartTimestamp();

  public long getStartTimestampInMillis() {
    return getStartTimestamp() * 1000;
  }

  public abstract EventType getType();

  public abstract long getEndTimestamp();

  public abstract EventState getState();

  public abstract String getName();

  public long getEndTimestampInMillis() {
    return getEndTimestamp() * 1000;
  }

  @Override
  public int compareTo(Event o) {
    int start = Long.compare(o.getStartTimestamp(), this.getStartTimestamp());
    if (start != 0) {
      return start;
    }

    return Long.compare(o.getEndTimestamp(), this.getEndTimestamp());
  }

  /**
   * @return true if event the event takes place within 24 hours
   */
  public boolean isEventEndingToday() {
    long currentTime = System.currentTimeMillis();
    return getEndTimestampInMillis() - currentTime < Constants.MS_IN_A_DAY;
  }

  public boolean isStartPassed() {
    return System.currentTimeMillis() >= getStartTimestampInMillis();
  }
}
