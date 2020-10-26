package com.github.dedis.student20_pop.model;

import android.os.Build;

import androidx.annotation.RequiresApi;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * Class modeling a Local Autonomous Organization (LAO)
 */
public final class Lao {

    private String name;
    private long time;
    private String id;
    private String organizer;
    private List<String> witnesses;
    private List<String> members;
    private List<String> events;
    private String attestation; // use Sign API

    /**
     * Constructor for a LAO
     *
     * @param name the name of the LAO, can be empty
     * @param time the creation time, can't be modified
     * @param organizer the public key of the organizer in Hex
     *
     */
    public Lao(String name, Date time, String organizer) {
        this.name = name;
        this.time = time.getTime() / 1000L; // can modify to Instant instead of Date
        this.id = name + time; // have to hash
        this.organizer = organizer;
        this.witnesses = new ArrayList<>();
        this.members = new ArrayList<>();
        this.events = new ArrayList<>();
        this.attestation = name + time + organizer; // have to sign
    }

    public String getName() {
        return name;
    }

    /**
     *
     * @return creation time of the LAO as Unix Timestamp, can't be modified
     */
    public long getTime() {
        return time;
    }

    /**
     *
     * @return ID of the LAO, can't be modified
     */
    public String getId() {
        return id;
    }

    /**
     *
     * @return public key of the organizer, can't be modified
     */
    public String getOrganizer() {
        return organizer;
    }

    /**
     *
     * @return list of public keys where each public key belongs to one witness
     */
    public List<String> getWitnesses() {
        return witnesses;
    }

    /**
     *
     * @return list of public keys where each public key belongs to one member
     */
    public List<String> getMembers() {
        return members;
    }

    /**
     *
     * @return list of public keys where each public key belongs to an event
     */
    public List<String> getEvents() {
        return events;
    }

    /**
     *
     * @return signature by the organizer
     */
    public String getAttestation() {
        return attestation;
    }

    /**
     *
     * @param name new name for the LAO, can be empty
     * @throws IllegalArgumentException if the name is null
     */
    public void setName(String name) {
        if(name == null) {
            throw new IllegalArgumentException("Trying to set null as the name of the LAO");
        }
        this.name = name;
    }

    /**
     *
     * @param witnesses list of public keys of witnesses, can be empty
     * @throws IllegalArgumentException if at least one witness public key is null
     */
    public void setWitnesses(ArrayList<String> witnesses) {
        if(witnesses.contains(null)) {
            throw new IllegalArgumentException("Trying to add a null witness to the LAO " + name);
        }
        this.witnesses = witnesses;
    }

    /**
     *
     * @param members list of public keys of members, can be empty
     * @throws IllegalArgumentException if at least one member public key is null
     */
    public void setMembers(List<String> members) {
        if(members.contains(null)) {
            throw new IllegalArgumentException("Trying to add a null member to the LAO " + name);
        }
        this.members = members;
    }

    /**
     *
     * @param events list of public keys of events, can be empty
     * @throws IllegalArgumentException if at least one event public key is null
     */
    public void setEvents(List<String> events) {
        if(events.contains(null)) {
            throw new IllegalArgumentException("Trying to add a null event to the LAO " + name);
        }
        this.events = events;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Lao lao = (Lao) o;
        return name.equals(lao.name)
                && time == lao.time
                && id.equals(lao.id)
                && organizer.equals(lao.organizer)
                && witnesses.equals(lao.witnesses)
                && members.equals(lao.members)
                && events.equals(lao.events)
                && attestation.equals(lao.attestation);
    }

    // API problem in order to use Objects.hash
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public int hashCode() {
        return Objects.hash(name, time, id, organizer, witnesses, members, events, attestation);
    }
}