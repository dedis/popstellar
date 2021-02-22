package com.github.dedis.student20_pop.model.event;

import static com.github.dedis.student20_pop.model.event.EventType.MEETING;

import java.util.Objects;

/** Class modeling a Meeting Event */
public class MeetingEvent extends Event {

  private final long endTime;
  private final String description;

  /**
   * Constructor for a Meeting Event
   *
   * @param name the name of the meeting event
   * @param startTime the start time of the meeting event
   * @param endTime the end time of the meeting event
   * @param lao the ID of the associated LAO
   * @param location the location of the meeting event
   * @param description the description of the meeting event
   * @throws IllegalArgumentException if any of the parameters is null
   */
  public MeetingEvent(
      String name, long startTime, long endTime, String lao, String location, String description) {
    super(name, lao, startTime, location, MEETING);
    if (description == null) {
      throw new IllegalArgumentException("Trying to create a meeting with a null description");
    }
    this.endTime = endTime;
    this.description = description;
  }

  /** Returns the end time of the Meeting. */
  public long getEndTime() {
    return endTime;
  }

  /** Returns the description of the Meeting. */
  public String getDescription() {
    return description;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    if (!super.equals(o)) return false;
    MeetingEvent that = (MeetingEvent) o;
    return Objects.equals(endTime, that.endTime) && Objects.equals(description, that.description);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), endTime, description);
  }
}
