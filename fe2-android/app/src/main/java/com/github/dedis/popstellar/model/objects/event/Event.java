package com.github.dedis.popstellar.model.objects.event;

import androidx.lifecycle.MutableLiveData;

/** Class modeling an Event */
public abstract class Event implements Comparable<Event> {

  public abstract long getStartTimestamp();

  public long getStartTimestampInMillis() {
    return getStartTimestamp() * 1000;
  }

  public abstract EventType getType();

  public abstract long getEndTimestamp();

  public abstract MutableLiveData<EventState> getState();

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
}
