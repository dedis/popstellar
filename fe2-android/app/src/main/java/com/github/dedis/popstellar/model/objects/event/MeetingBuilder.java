package com.github.dedis.popstellar.model.objects.event;

import com.github.dedis.popstellar.model.objects.Meeting;

import java.util.List;

public class MeetingBuilder {
  private String id;
  private String persistentId;
  private String name;
  private long creation;
  private long start;
  private long end;
  private String location;
  private long lastModified;
  private String modificationId;
  private List<String> modificationSignatures;

  public MeetingBuilder() {}

  public MeetingBuilder(Meeting meeting) {
    this.id = meeting.getId();
    this.persistentId = meeting.getPersistentId();
    this.name = meeting.getName();
    this.creation = meeting.getCreation();
    this.start = meeting.getStart();
    this.end = meeting.getEnd();
    this.location = meeting.getLocation();
    this.lastModified = meeting.getLastModified();
    this.modificationId = meeting.getModificationId();
    this.modificationSignatures = meeting.getModificationSignatures();
  }

  public MeetingBuilder setId(String id) {
    this.id = id;
    return this;
  }

  public MeetingBuilder setPersistentId(String id) {
    persistentId = id;
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
    this.modificationId = modificationId;
    if (modificationId == null) {
      throw new IllegalStateException("Modification Id is null");
    }
    return this;
  }

  public MeetingBuilder setModificationSignatures(List<String> modificationSignatures) {
    this.modificationSignatures = modificationSignatures;
    if (modificationSignatures == null) {
      throw new IllegalStateException("Modification Signatures are null");
    }
    return this;
  }

  public Meeting build() {
    if (location == null) {
      throw new IllegalStateException("Location is null");
    }
    if (modificationId == null) {
      throw new IllegalStateException("Modification Id is null");
    }
    if (modificationSignatures == null) {
      throw new IllegalStateException("Modification Signatures are null");
    }
    return new Meeting(
        id,
        persistentId,
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
