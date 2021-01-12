package com.github.dedis.student20_pop.model.event;

import androidx.databinding.ObservableArrayList;

import com.github.dedis.student20_pop.utility.security.Hash;

import java.time.Instant;
import java.util.Objects;

/**
 * Class modeling an Event
 */
public class Event {

    private final String name;
    private final long time;
    private final long startTime;
    private final String id;
    private final String lao;
    private ObservableArrayList<String> attendees;
    private final String location;
    private final EventType type;

    /**
     * Constructor for an Event
     *
     * @param name the name of the event, can be empty
     * @param lao  the public key of the associated LAO
     * @param startTime the event's start time
     * @throws IllegalArgumentException if any of the parameters is null
     */
    public Event(String name, String lao, long startTime, String location, EventType type) {
        if (name == null || lao == null || location == null || type == null) {
            throw new IllegalArgumentException("Trying to create an event with null parameters");
        }
        this.name = name;
        this.time = Instant.now().getEpochSecond();
        this.startTime = startTime;
        this.id = Hash.hash(type.getSuffix(), lao, time, name);
        this.lao = lao;
        this.attendees = new ObservableArrayList<>();
        this.location = location;
        this.type = type;
    }

    /**
     * Returns the name of the event.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the creation time of the LAO as Unix Timestamp, can't be modified.
     */
    public long getTime() {
        return time;
    }

    /**
     * Returns the start time of the event as Unix Timestamp
     */
    public long getStartTime() {
        return startTime;
    }

    /**
     * Returns the ID of the event, can't be modified.
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the ID of the associated LAO.
     */
    public String getLao() {
        return lao;
    }

    /**
     * Returns the list of public keys of the attendees.
     */
    public ObservableArrayList<String> getAttendees() {
        return attendees;
    }

    /**
     * Returns the location of the event.
     */
    public String getLocation() {
        return location;
    }

    /**
     * Returns the type of the event as an EventType.
     */
    public EventType getType() {
        return type;
    }

    /**
     * Modify the Event's list of attendees
     *
     * @param attendees list of public keys of attendees, can be empty
     * @throws IllegalArgumentException if the list is null or at least one public key is null
     */
    public void setAttendees(ObservableArrayList<String> attendees) {
        if (attendees == null || attendees.contains(null)) {
            throw new IllegalArgumentException("Trying to add a null attendee to the event " + name);
        }
        this.attendees = attendees;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Event event = (Event) o;
        return time == event.time &&
                Objects.equals(name, event.name) &&
                Objects.equals(id, event.id) &&
                Objects.equals(lao, event.lao) &&
                Objects.equals(attendees, event.attendees) &&
                Objects.equals(location, event.location) &&
                Objects.equals(type, event.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, time, id, lao, attendees, location, type);
    }
}