package com.github.dedis.student20_pop.model;

import android.os.Build;

import androidx.annotation.RequiresApi;

import com.github.dedis.student20_pop.utility.security.Signature;

import java.util.Objects;

/**
 * Class modeling a Vote
 */
public final class Vote {

    private final String person;
    private final String election;
    private final String vote;
    private final String attestation;

    /**
     * Constructor of a Vote
     *
     * @param person the public key of the person voting
     * @param election the id of the election
     * @param vote the encrypted vote
     * @throws IllegalArgumentException if any parameter is null
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    public Vote(String person, String election, String vote) throws IllegalArgumentException {
        if(person == null || election == null || vote == null) {
            throw new IllegalArgumentException("Trying to create a Vote with a null parameter");
        }
        this.person = person;
        this.election = election;
        this.vote = vote;
        // Get LAO ID in the future
        String lao = "";
        // Get person's private key in the future
        this.attestation = Signature.sign(person, election + lao + vote);
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