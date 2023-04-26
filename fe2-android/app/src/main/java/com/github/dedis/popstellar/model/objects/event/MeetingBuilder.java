package com.github.dedis.popstellar.model.objects.event;

import com.github.dedis.popstellar.model.objects.Meeting;

import java.util.List;

/** This class is a builder for the Meeting object */
public class MeetingBuilder {
  private String id;
  private String name;
  private long creation;
  private long start;
  private long end;
  private String location;
  private long lastModified;
  private String modificationId;
  private List<String> modificationSignatures;

  public MeetingBuilder() {
    // Empty constructor required
  }

  public MeetingBuilder(Meeting meeting) {
    id = meeting.getId();
    name = meeting.getName();
    creation = meeting.getCreation();
    start = meeting.getStartTimestamp();
    end = meeting.getEndTimestamp();
    location = meeting.getLocation();
    lastModified = meeting.getLastModified();
    modificationId = meeting.getModificationId();
    modificationSignatures = meeting.getModificationSignatures();
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

  public MeetingBuilder setModificationId(String modificationId) {
    if (modificationId == null) {
      throw new IllegalArgumentException("Modification Id is null");
    }
    this.modificationId = modificationId;
    return this;
  }

  public MeetingBuilder setModificationSignatures(List<String> modificationSignatures) {
    if (modificationSignatures == null) {
      throw new IllegalArgumentException("Modification signatures list is null");
    }
    this.modificationSignatures = modificationSignatures;
    return this;
  }

  public Meeting build() {
    if (location == null) {
      throw new IllegalStateException("Location is null");
    }
    if (modificationId == null) {
      throw new IllegalArgumentException("Modification Id is null");
    }
    if (modificationSignatures == null) {
      throw new IllegalArgumentException("Modification signatures list is null");
    }
    return new Meeting(
        id,
        name,
        creation,
        start,
        end,
        location,
        lastModified,
        modificationId,
        modificationSignatures);
  }
}
