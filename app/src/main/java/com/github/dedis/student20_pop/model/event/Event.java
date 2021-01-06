package com.github.dedis.student20_pop.model.event;

import android.os.Build;

import androidx.annotation.RequiresApi;

import com.github.dedis.student20_pop.model.Keys;
import com.github.dedis.student20_pop.utility.security.Hash;
import com.github.dedis.student20_pop.utility.security.Signature;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * Class modeling an Event
 */
public class Event {

    private final String name;
    private final long time;
    private final String id;
    private final String lao;
    // Can use GeoLocation in the future
    private final String location;
    // Can use enums in the future
    private final EventType type;
    private final JSONObject other;
    private final List<String> attestation;
    private List<String> attendees;

    /**
     * Constructor for an Event
     *
     * @param name the name of the event, can be empty
     * @param time the creation time, can't be modified
     * @param lao  the public key of the associated LAO
     * @throws IllegalArgumentException if any of the parameters is null
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    public Event(String name, Date time, String lao, String location, EventType type) {
        if (name == null || time == null || lao == null || location == null || type == null) {
            throw new IllegalArgumentException("Trying to create an event with null parameters");
        }
        this.name = name;
        this.time = time.getTime() / 1000L;
        this.id = Hash.hash(name, time.getTime());
        this.lao = Hash.hash(lao);
        this.attendees = new ArrayList<>();
        this.location = location;
        this.type = type;
        this.other = new JSONObject();

        // Data to sign
        String data = name + time + lao + location;
        // Will get organizer's and witnesses' private keys in the future
        String organizer = new Keys().getPrivateKey();
        ArrayList<String> witnesses = new ArrayList<>();
        ArrayList<String> attestation = new ArrayList<>(Collections.singletonList(Signature.sign(organizer, data)));
        attestation.addAll(Signature.sign(witnesses, data));
        this.attestation = attestation;
    }

    public String getName() {
        return name;
    }

    /**
     * @return creation time of the LAO as Unix Timestamp, can't be modified
     */
    public long getTime() {
        return time;
    }

    /**
     * @return ID of the event, can't be modified
     */
    public String getId() {
        return id;
    }

    /**
     * @return ID of the associated LAO
     */
    public String getLao() {
        return lao;
    }

    /**
     * @return list of public keys of the attendees
     */
    public List<String> getAttendees() {
        return attendees;
    }

    /**
     * @param attendees list of public keys of attendees, can be empty
     * @throws IllegalArgumentException if the list is null or at least one public key is null
     */
    public void setAttendees(List<String> attendees) {
        if (attendees == null || attendees.contains(null)) {
            throw new IllegalArgumentException("Trying to add a null attendee to the event " + name);
        }
        this.attendees = attendees;
    }

    public String getLocation() {
        return location;
    }

    public EventType getType() {
        return type;
    }

    /**
     * @return list of signatures by the organizer and the witnesses of the corresponding LAO
     */
    public List<String> getAttestation() {
        return attestation;
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
                Objects.equals(type, event.type) &&
                Objects.equals(other, event.other) &&
                Objects.equals(attestation, event.attestation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, time, id, lao, attendees, location, type, other, attestation);
    }

    /**
     * Enum class for each event type
     */
    public enum EventType {
        MEETING, ROLL_CALL, POLL, DISCUSSION
    }

    /**
     * Enum class for each event category
     */
    public enum EventCategory {
        PAST, PRESENT, FUTURE
    }
}