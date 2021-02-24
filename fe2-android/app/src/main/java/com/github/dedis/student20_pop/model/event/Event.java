package com.github.dedis.student20_pop.model.event;

/** Class modeling an Event */
public abstract class Event implements Comparable<Event> {
  public abstract long getTimestamp();

  @Override
  public int compareTo(Event o) {
    return Long.compare(this.getTimestamp(), o.getTimestamp());
  }
}
