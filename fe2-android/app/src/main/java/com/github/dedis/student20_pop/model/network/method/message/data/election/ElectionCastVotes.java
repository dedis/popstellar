package com.github.dedis.student20_pop.model.network.method.message.data.election;

import androidx.annotation.Nullable;

import com.github.dedis.student20_pop.model.network.method.message.data.Action;
import com.github.dedis.student20_pop.model.network.method.message.data.Data;
import com.github.dedis.student20_pop.model.network.method.message.data.Objects;
import com.github.dedis.student20_pop.model.network.method.message.data.rollcall.CreateRollCall;
import com.github.dedis.student20_pop.utility.security.Hash;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class ElectionCastVotes extends Data {


    private long creation; // time the votes were submited
    private String laoId;
    private String electionId;
    private List<ElectionVote> votes;

    /**
     * Constructor for a data Cast Vote Election Event
     *
     * @param laoId id of the LAO
     * @param votes list of the Election Vote where an ElectionVote Object represents the corresponding votes for one question
     */
    public ElectionCastVotes(
            List<ElectionVote> votes,
            String electionId,
            String laoId) {
        this.creation = Instant.now().toEpochMilli();
        this.votes = new ArrayList<>();
        this.laoId = laoId;
        this.electionId = electionId;
       this.votes = votes;
    }


    public String getLaoId() {
        return laoId;
    }
    public String getElectionId(){return electionId;}

    public long getCreation() {
        return creation;
    }

    public List<ElectionVote> getVotes() { return Collections.unmodifiableList(votes); }


    @Override
    public String getObject() {
        return Objects.ELECTION.getObject();
    }

    @Override
    public String getAction() {
        return Action.CAST_VOTE.getAction();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ElectionCastVotes that = (ElectionCastVotes) o;
        return java.util.Objects.equals(getLaoId(), that.getLaoId())
                && creation == that.getCreation()
                && java.util.Objects.equals(votes, that.getVotes());
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(
                getLaoId(),
                getCreation(),
                getVotes());
    }

    //TODO
    @Override
    public String toString() {
        return null;
    }

}
