package com.github.dedis.popstellar.model.objects.event;

import com.github.dedis.popstellar.model.objects.RollCall;
import com.github.dedis.popstellar.model.objects.security.PublicKey;

import java.util.HashSet;
import java.util.Set;

public class RollCallBuilder {

  private String id;
  private String persistentId;
  private String name;
  private long creation;
  private long start;
  private long end;
  private EventState state;
  private Set<PublicKey> attendees;

  private String location;
  private String description;

  public RollCallBuilder setId(String id) {
    this.id = id;
    return this;
  }

  public RollCallBuilder() {}

  public RollCallBuilder(RollCall rollCall) {
    this.id = rollCall.getId();
    this.persistentId = rollCall.getPersistentId();
    this.name = rollCall.getName();
    this.creation = rollCall.getCreation();
    this.start = rollCall.getStart();
    this.end = rollCall.getEnd();
    this.state = rollCall.getState();
    this.attendees = new HashSet<>(rollCall.getAttendees());
    this.location = rollCall.getLocation();
    this.description = rollCall.getLocation();
  }

  public RollCallBuilder setPersistentId(String id) {
    persistentId = id;
    return this;
  }

  public RollCallBuilder setName(String name) {
    this.name = name;
    return this;
  }

  public RollCallBuilder setCreation(long creation) {
    this.creation = creation;
    return this;
  }

  public RollCallBuilder setStart(long start) {
    this.start = start;
    return this;
  }

  public RollCallBuilder setEnd(long end) {
    this.end = end;
    return this;
  }

  public RollCallBuilder setState(EventState state) {
    this.state = state;
    return this;
  }

  public RollCallBuilder setAttendees(Set<PublicKey> attendees) {
    if (attendees == null) {
      throw new IllegalArgumentException("Attendee set is null");
    }
    this.attendees = attendees;
    return this;
  }

  public RollCallBuilder setEmptyAttendees() {
    attendees = new HashSet<>();
    return this;
  }

  public RollCallBuilder setLocation(String location) {
    if (location == null) {
      throw new IllegalArgumentException("Location is null");
    }
    this.location = location;
    return this;
  }

  public RollCallBuilder setDescription(String description) {
    if (description == null) {
      throw new IllegalArgumentException("Description is null");
    }
    this.description = description;
    return this;
  }

  public RollCall build() {
    if (description == null) {
      throw new IllegalStateException("Description is null");
    }
    if (location == null) {
      throw new IllegalStateException("Location is null");
    }
    if (attendees == null) {
      throw new IllegalStateException("Attendee set is null");
    }
    return new RollCall(
        id, persistentId, name, creation, start, end, state, attendees, location, description);
  }
}
