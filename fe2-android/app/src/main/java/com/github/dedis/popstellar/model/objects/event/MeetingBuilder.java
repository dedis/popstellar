package com.github.dedis.popstellar.model.objects.event;

import com.github.dedis.popstellar.model.objects.Meeting;

/** This class is a builder for the Meeting object */
public class MeetingBuilder {
  private String id;
  private String name;
  private long creation;
  private long start;
  private long end;
  private String location;
  private long lastModified;

  // TODO: this class could be extended with signatures when implementing StateMeeting

  public MeetingBuilder() {
    // Empty constructor required
  }

  public MeetingBuilder(Meeting meeting) {
    this.id = meeting.getId();
    this.name = meeting.getName();
    this.creation = meeting.getCreation();
    this.start = meeting.getStartTimestamp();
    this.end = meeting.getEndTimestamp();
    this.location = meeting.getLocation();
    this.lastModified = meeting.getLastModified();
  }

  public MeetingBuilder setId(String id) {
    this.id = id;
    return this;
  }

  public MeetingBuilder setName(String name) {
    this.name = name;
    return this;
  }

  public MeetingBuilder setCreation(long creation) {
    this.creation = creation;
    return this;
  }

  public MeetingBuilder setStart(long start) {
    this.start = start;
    return this;
  }

  public MeetingBuilder setEnd(long end) {
    this.end = end;
    return this;
  }

  public MeetingBuilder setLocation(String location) {
    if (location == null) {
      throw new IllegalArgumentException("Location is null");
    }
    this.location = location;
    return this;
  }

  public MeetingBuilder setLastModified(long lastModified) {
    this.lastModified = lastModified;
    return this;
  }

  public Meeting build() {
    if (location == null) {
      throw new IllegalStateException("Location is null");
    }
    return new Meeting(id, name, creation, start, end, location, lastModified);
  }
}
