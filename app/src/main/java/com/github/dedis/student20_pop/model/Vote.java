package com.github.dedis.student20_pop.model;

import java.util.Objects;

/**
 * Class modeling a Vote
 */
public class Vote {

    private String person;
    private String election;
    private String vote; // TODO: ElGamal encryption of a message
    private String attestation; // TODO: sign SHA1

    /**
     * Constructor of a Vote
     *
     * @param person the public key of the person voting
     * @param election the id of the election
     * @param vote the encrypted vote
     */
    public Vote(String person, String election, String vote) {
        if(person == null || election == null || vote == null) {
            throw new IllegalArgumentException("Trying to create a Vote with a null parameter");
        }
        this.person = person;
        this.election = election;
        this.vote = vote;
        this.attestation = election + vote; // TODO: need LAO ID too, then use SHA1
    }

    /**
     *
     * @return public key of the person
     */
    public String getPerson() {
        return person;
    }

    /**
     *
     * @return id of the election
     */
    public String getElection() {
        return election;
    }

    /**
     *
     * @return encrypted vote
     */
    public String getVote() {
        return vote;
    }

    /**
     *
     * @return signature by the voter
     */
    public String getAttestation() {
        return attestation;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Vote vote1 = (Vote) o;
        return Objects.equals(person, vote1.person) &&
                Objects.equals(election, vote1.election) &&
                Objects.equals(vote, vote1.vote) &&
                Objects.equals(attestation, vote1.attestation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(person, election, vote, attestation);
    }
}
