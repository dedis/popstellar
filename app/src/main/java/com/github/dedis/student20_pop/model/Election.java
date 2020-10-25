package com.github.dedis.student20_pop.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * Class modeling an Election
 */
public class Election {

    private String name;
    private long time;
    private String id;
    private String lao;
    private List<String> options;
    private List<String> attestation;

    /**
     * Constructor of an Election
     *
     * @param name the name of the election, can be empty
     * @param time the creation time, can't be modified
     * @param lao the LAO associated to the election
     * @param options the default ballot options
     * @throws IllegalArgumentException if any of the parameters is null
     */
    public Election(String name, Date time, String lao, List<String> options) {
        if(name == null || time == null || lao == null || options == null || options.contains(null)) {
            throw new IllegalArgumentException("Trying to  create an Election with a null value");
        }
        this.name = name;
        this.time = time.getTime() / 1000L;
        // simple for now, will hash later
        this.id = name + time;
        this.lao = lao;
        this.options = options;
        // Will get list of organizer and witnesses ids, hash and sign later
        this.attestation = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    /**
     *
     * @return creation time of the Election as Unix Timestamp, can't be modified
     */
    public long getTime() {
        return time;
    }

    /**
     *
     * @return ID of the Election, can't be modified
     */
    public String getId() {
        return id;
    }

    /**
     *
     * @return associated LAO
     */
    public String getLao() {
        return lao;
    }

    /**
     *
     * @return the default ballot options
     */
    public List<String> getOptions() {
        return options;
    }

    /**
     *
     * @return list of signatures by the organizer and the witnesses of the corresponding LAO
     */
    public List<String> getAttestation() {
        return attestation;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Election election = (Election) o;
        return time == election.time &&
                Objects.equals(name, election.name) &&
                Objects.equals(id, election.id) &&
                Objects.equals(lao, election.lao) &&
                Objects.equals(options, election.options) &&
                Objects.equals(attestation, election.attestation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, time, id, lao, options, attestation);
    }
}
