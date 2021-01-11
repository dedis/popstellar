package com.github.dedis.student20_pop.model;

import com.github.dedis.student20_pop.utility.security.Hash;
import com.github.dedis.student20_pop.utility.security.Signature;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Class modeling a Local Autonomous Organization (LAO)
 */
public final class Lao {

    private final String name;
    private final long time;
    private final String id;
    private final String organizer;
    private final String attestation;
    private List<String> witnesses;
    private List<String> members;
    private List<String> events;

    /**
     * Constructor for a LAO
     *
     * @param name      the name of the LAO, can be empty
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
        this.attestation = Signature.sign(organizer, name + time + organizer);
    }

    /**
     * Private constructor used to create new LAO when the name is modified,
     * forces to recompute the id and attestation using the new name
     *
     * @param name      the name of the LAO, can be empty
     * @param time      the creation time
     * @param id        the id of the LAO, Hash(name, creation time, organizer id)
     * @param organizer the public key of the organizer
     * @param witnesses the list of the public keys of the witnesses
     * @param members   the list of the public keys of the members
     * @param events    the list of the ids of the events
     */
    private Lao(String name, long time, String id, String organizer, List<String> witnesses,
                List<String> members, List<String> events) {
        this.name = name;
        this.time = time;
        this.id = id;
        this.organizer = organizer;
        this.witnesses = witnesses;
        this.members = members;
        this.events = events;
        this.attestation = Signature.sign(organizer, name + time + organizer);
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

    public String getName() {
        return name;
    }

    /**
     * Modifying the name of the LAO creates a new id and attestation
     *
     * @param name new name for the LAO, can be empty
     * @return new LAO with the new name, id and attestation
     * @throws IllegalArgumentException if the name is null
     */
    public Lao setName(String name) {
        if (name == null) {
            throw new IllegalArgumentException("Trying to set null as the name of the LAO");
        } else if (name.trim().isEmpty()) {
            throw new IllegalArgumentException("Trying to set an empty name for the LAO");
        }
        return new Lao(name, time, id, organizer, witnesses, members, events);
    }

    /**
     * @return creation time of the LAO as Unix Timestamp, can't be modified
     */
    public long getTime() {
        return time;
    }

    /**
     * @return ID of the LAO, can't be modified
     */
    public String getId() {
        return id;
    }

    /**
     * @return public key of the organizer, can't be modified
     */
    public String getOrganizer() {
        return organizer;
    }

    /**
     * @return list of public keys where each public key belongs to one witness
     */
    public List<String> getWitnesses() {
        return witnesses;
    }

    /**
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
     * @return list of public keys where each public key belongs to one member
     */
    public List<String> getMembers() {
        return members;
    }

    /**
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
     * @return list of public keys where each public key belongs to an event
     */
    public List<String> getEvents() {
        return events;
    }

    /**
     * @param events list of public keys of events, can be empty
     * @throws IllegalArgumentException if the list is null or at least one public key is null
     */
    public void setEvents(List<String> events) {
        if (events == null || events.contains(null)) {
            throw new IllegalArgumentException("Trying to add a null event to the LAO " + name);
        }
        this.events = events;
    }

    /**
     * @return signature by the organizer
     */
    public String getAttestation() {
        return attestation;
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
                && Objects.equals(events, lao.events)
                && Objects.equals(attestation, lao.attestation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, time, id, organizer, witnesses, members, events, attestation);
    }
}