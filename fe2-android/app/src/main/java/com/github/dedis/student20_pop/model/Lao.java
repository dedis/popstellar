package com.github.dedis.student20_pop.model;

import com.github.dedis.student20_pop.model.event.Event;
import java.util.HashMap;
import java.util.HashSet;
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
  private Set<String> witnesses;

  private Set<PendingUpdate> pendingUpdates;

  private Map<String, RollCall> rollCalls;

  public Lao(String id) {
    this.channel = id;
    this.id = id;
    this.rollCalls = new HashMap<>();
    this.witnesses = new HashSet<>();
    this.pendingUpdates = new HashSet<>();
  }

  public Lao(String id, String name) {
    this(id);
    this.name = name;
  }

  public void updateRollCall(String prevId, RollCall rollCall) {
    if (rollCalls.containsKey(prevId)) {
      rollCalls.remove(prevId);
    }
    String newId = rollCall.getId();
    rollCalls.put(newId, rollCall);
  }

  public Optional<RollCall> getRollCall(String id) {
    return Optional.ofNullable(rollCalls.get(id));
  }

  public Long getLastModified() {
    return lastModified;
  }

  public Set<String> getWitnesses() {
    return witnesses;
  }

  public Set<PendingUpdate> getPendingUpdates() {
    return pendingUpdates;
  }

  public String getOrganizer() {
    return organizer;
  }

  public String getChannel() {
    return channel;
  }

  public void setChannel(String channel) {
    this.channel = channel;
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

  public void setLastModified(Long lastModified) {
    this.lastModified = lastModified;
  }

  public Long getCreation() {
    return creation;
  }

  public void setCreation(Long creation) {
    this.creation = creation;
  }

  public void setOrganizer(String organizer) {
    this.organizer = organizer;
  }

  public String getModificationId() {
    return modificationId;
  }

  public void setModificationId(String modificationId) {
    this.modificationId = modificationId;
  }

  public void setWitnesses(Set<String> witnesses) {
    this.witnesses = witnesses;
  }

  public void setPendingUpdates(Set<PendingUpdate> pendingUpdates) {
    this.pendingUpdates = pendingUpdates;
  }

  public Map<String, RollCall> getRollCalls() {
    return rollCalls;
  }

  public void setRollCalls(Map<String, RollCall> rollCalls) {
    this.rollCalls = rollCalls;
  }

  public static class PendingUpdate implements Comparable<PendingUpdate> {
    private long modificationTime;
    private String messageId;

    public PendingUpdate(long modificationTime, String messageId) {
      this.modificationTime = modificationTime;
      this.messageId = messageId;
    }

    public long getModificationTime() {
      return modificationTime;
    }

    @Override
    public int compareTo(PendingUpdate o) {
      return Long.compare(modificationTime, o.modificationTime);
    }
  }

  public static class RollCall extends Event {
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

    @Override
    public long getTimestamp() {
      if (end != 0) {
        return end;
      }

      if (start == 0) {
        return scheduled;
      }

      return start;
    }
  }
}
