package com.github.dedis.popstellar.model.objects;

import com.github.dedis.popstellar.model.network.IdGenerator;
import com.github.dedis.popstellar.model.objects.event.Event;
import com.github.dedis.popstellar.model.objects.event.EventState;
import com.github.dedis.popstellar.model.objects.event.EventType;
import java.util.HashSet;
import java.util.Set;

public class RollCall extends Event {

  private String id;
  private final String persistentId;
  private String name;
  private long creation;
  private long start;
  private long end;
  private EventState state;
  private Set<String> attendees;

  private String location;
  private String description;

  public RollCall(String id) {
    this.id = id;
    this.persistentId = id;
    this.attendees = new HashSet<>();
  }

  public RollCall(String laoId, long creation, String name) {
    this(IdGenerator.generateCreateRollCallId(laoId, creation, name));
    if (name == null) {
      throw new IllegalArgumentException(" The name of the RollCall is null");
    }
    this.name = name;
    this.creation = creation;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getPersistentId() {
    return persistentId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public long getCreation() {
    return creation;
  }

  public void setCreation(long creation) {
    this.creation = creation;
  }

  public long getStart() {
    return start;
  }

  public void setStart(long start) {
    this.start = start;
  }

  public long getEnd() {
    return end;
  }

  public void setEnd(long end) {
    this.end = end;
  }

  public EventState getState() {
    return state;
  }

  public void setState(EventState state) {
    this.state = state;
  }

  public Set<String> getAttendees() {
    return attendees;
  }

  public void setAttendees(Set<String> attendees) {
    this.attendees = attendees;
  }

  public String getLocation() {
    return location;
  }

  public void setLocation(String location) {
    this.location = location;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  @Override
  public long getStartTimestamp() {
    return start;
  }

  @Override
  public EventType getType() {
    return EventType.ROLL_CALL;
  }

  @Override
  public long getEndTimestamp() {
    if (end == 0) {
      return Long.MAX_VALUE;
    }
    return end;
  }
}
