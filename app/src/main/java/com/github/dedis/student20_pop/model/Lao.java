package com.github.dedis.student20_pop.model;

import com.github.dedis.student20_pop.model.event.Event;
import com.github.dedis.student20_pop.utility.security.Hash;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Class modeling a Local Autonomous Organization (LAO) */
public final class Lao {

  private final long time;
  private final String id;
  private final String organizer;
  private String name;
  private List<String> witnesses;
  private List<String> members;
  private List<Event> events;

  /**
   * Constructor for a LAO
   *
   * @param name the name of the LAO, can be empty
   * @param organizer the public key of the organizer
   * @throws IllegalArgumentException if any of the parameters is null
   */
  public Lao(String name, String organizer) {
    if (name == null || organizer == null) {
      throw new IllegalArgumentException("Trying to  create a LAO with a null value");
    } else if (name.trim().isEmpty()) {
      throw new IllegalArgumentException("Trying to set an empty name for the LAO");
    }

    this.name = name.trim();
    this.time = Instant.now().getEpochSecond();
    this.id = Hash.hash(organizer, time, name);
    this.organizer = organizer;
    this.witnesses = new ArrayList<>();
    this.members = new ArrayList<>();
    this.events = new ArrayList<>();
  }

  /**
   * Private constructor used to create new LAO when the name is modified, forces to recompute the
   * id and attestation using the new name
   *
   * @param name the name of the LAO, can be empty
   * @param time the creation time
   * @param id the id of the LAO, Hash(name, creation time, organizer id)
   * @param organizer the public key of the organizer
   * @param witnesses the list of the public keys of the witnesses
   * @param members the list of the public keys of the members
   * @param events the list of the ids of the events
   */
  public Lao(
      String name,
      long time,
      String id,
      String organizer,
      List<String> witnesses,
      List<String> members,
      List<Event> events) {
    this.name = name;
    this.time = time;
    this.id = id;
    this.organizer = organizer;
    this.witnesses = witnesses;
    this.members = members;
    this.events = events;
  }

  /** Returns the name of the LAO. */
  public String getName() {
    return name;
  }

  /** Returns the creation time of the LAO as Unix Timestamp, can't be modified. */
  public long getTime() {
    return time;
  }

  /** Returns the ID of the LAO, can't be modified. */
  public String getId() {
    return id;
  }

  /**
   * Get the list of ids from a given list of LAOs
   *
   * @param laos the list of LAOs
   * @return list of ids of these LAOs
   */
  public static List<String> getIds(List<Lao> laos) {
    if (laos == null || laos.contains(null)) {
      throw new IllegalArgumentException("Cannot get ids of null LAOs");
    }
    List<String> ids = new ArrayList<>();
    for (Lao lao : laos) {
      ids.add(lao.id);
    }
    return ids;
  }

  /** Returns the public key of the organizer, can't be modified. */
  public String getOrganizer() {
    return organizer;
  }

  /** Returns the list of public keys where each public key belongs to one witness. */
  public List<String> getWitnesses() {
    return witnesses;
  }

  /** Returns the list of public keys where each public key belongs to one member. */
  public List<String> getMembers() {
    return members;
  }

  /** Returns the list of the events of the lao. */
  public List<Event> getEvents() {
    return events;
  }

  /**
   * Modifying the name of the LAO creates a new id and attestation
   *
   * @param name new name for the LAO, can be empty
   * @throws IllegalArgumentException if the name is null
   */
  public void setName(String name) {
    if (name == null) {
      throw new IllegalArgumentException("Trying to set null as the name of the LAO");
    } else if (name.trim().isEmpty()) {
      throw new IllegalArgumentException("Trying to set an empty name for the LAO");
    }

    this.name = name;
  }

  /**
   * Modify the LAO's list of witnesses
   *
   * @param witnesses list of public keys of witnesses, can be empty
   * @throws IllegalArgumentException if the list is null or at least one public key is null
   */
  public void setWitnesses(List<String> witnesses) {
    if (witnesses == null || witnesses.contains(null)) {
      throw new IllegalArgumentException("Trying to add a null witness to the LAO " + name);
    }
    this.witnesses = witnesses;
  }

  /**
   * Modify the LAO's list of members
   *
   * @param members list of public keys of members, can be empty
   * @throws IllegalArgumentException if the list is null or at least one public key is null
   */
  public void setMembers(List<String> members) {
    if (members == null || members.contains(null)) {
      throw new IllegalArgumentException("Trying to add a null member to the LAO " + name);
    }
    this.members = members;
  }

  /**
   * Modify the LAO's list of events
   *
   * @param events list of events, can be empty
   * @throws IllegalArgumentException if the list is null or at least one event is null
   */
  public void setEvents(List<Event> events) {
    if (events == null || events.contains(null)) {
      throw new IllegalArgumentException("Trying to add a null event to the LAO " + name);
    }
    this.events = new ArrayList<>(events);
  }

  /**
   * Add an event to the LAO
   *
   * @param event to add
   * @return true if the event was added
   * @throws IllegalArgumentException if event is null
   */
  public boolean addEvent(Event event) {
    if (event == null) {
      throw new IllegalArgumentException("Trying to add a null event to the LAO " + name);
    }

    if (events.contains(event)) return false;

    return events.add(event);
  }

  /**
   * Add a witness to the LAO
   *
   * @param witness to add
   * @return true if the witness was added
   * @throws IllegalArgumentException if witness is null
   */
  public boolean addWitness(String witness) {
    if (witness == null) {
      throw new IllegalArgumentException("Trying to add a null witness to the LAO " + name);
    }

    if (witnesses.contains(witness)) return false;

    return witnesses.add(witness);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Lao lao = (Lao) o;
    return Objects.equals(name, lao.name)
        && Objects.equals(time, lao.time)
        && Objects.equals(id, lao.id)
        && Objects.equals(organizer, lao.organizer)
        && Objects.equals(witnesses, lao.witnesses)
        && Objects.equals(members, lao.members)
        && Objects.equals(events, lao.events);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, time, id, organizer, witnesses, members, events);
  }
}
