package com.github.dedis.student20_pop.model;

import com.github.dedis.student20_pop.utility.security.Signature;

import java.util.Objects;

/**
 * Class modeling a Vote
 */
public final class Vote {

    private final String person;
    private final String election;
    private final String vote;

    /**
     * Constructor of a Vote
     *
     * @param person   the public key of the person voting
     * @param election the id of the election
     * @param vote     the encrypted vote
     * @throws IllegalArgumentException if any parameter is null
     */
    public Vote(String person, String election, String vote) {
        if (person == null || election == null || vote == null) {
            throw new IllegalArgumentException("Trying to create a Vote with a null parameter");
        }
        this.person = person;
        this.election = election;
        this.vote = vote;
        // TODO: Get LAO ID in the future
        String lao = "";
    }

    /**
     * @return public key of the person
     */
    public String getPerson() {
        return person;
    }

    /**
     * @return id of the election
     */
    public String getElection() {
        return election;
    }

    /**
     * @return encrypted vote
     */
    public String getVote() {
        return vote;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Vote vote1 = (Vote) o;
        return Objects.equals(person, vote1.person) &&
                Objects.equals(election, vote1.election) &&
                Objects.equals(vote, vote1.vote);
    }

    @Override
    public int hashCode() {
        return Objects.hash(person, election, vote);
    }
}