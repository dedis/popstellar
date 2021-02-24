package com.github.dedis.student20_pop.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Class modeling a Local Autonomous Organization (LAO) */
public final class Lao {

  private String channel;
  private String id;
  private String name;
  private Long lastModified;
  private Long creation;
  private String organizer;
  private String modificationId;
  private List<String> witnesses;

  private Optional<String> updateMessageId;

  private Map<String, RollCall> rollCalls;

  public Lao(String id) {
    this.id = id;
    this.rollCalls = new HashMap<>();
  }

  public void updateRollCall(String prevId, RollCall rollCall) {
    if (rollCalls.containsKey(prevId)) {
      rollCalls.remove(prevId);
    }
    String newId = rollCall.getId();
    rollCalls.put(newId, rollCall);
  }

  public static class RollCall {
    private String id;
    private String name;
    private long creation;
    private long start;
    private long scheduled;
    private long end;
    private Set<String> attendees;

    private String location;
    private String description;

    public RollCall() {
      attendees = new HashSet<>();
    }

    public String getId() {
      return id;
    }

    public void setId(String id) {
      this.id = id;
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

    public long getScheduled() {
      return scheduled;
    }

    public void setScheduled(long scheduled) {
      this.scheduled = scheduled;
    }

    public long getEnd() {
      return end;
    }

    public void setEnd(long end) {
      this.end = end;
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
  }
}
