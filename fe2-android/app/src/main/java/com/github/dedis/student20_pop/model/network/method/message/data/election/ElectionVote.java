package com.github.dedis.student20_pop.model.network.method.message.data.election;

import androidx.annotation.Nullable;

import com.github.dedis.student20_pop.model.network.method.message.data.Action;
import com.github.dedis.student20_pop.model.network.method.message.data.Data;
import com.github.dedis.student20_pop.model.network.method.message.data.Objects;
import com.github.dedis.student20_pop.model.network.method.message.data.rollcall.CreateRollCall;
import com.github.dedis.student20_pop.utility.security.Hash;

import java.security.cert.PKIXRevocationChecker;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class ElectionVote extends Data {

    private String id;
    private String question_id; // id of the question
    private List<Long> vote_results;
    private Boolean write_in;

    /**
     * Constructor for a data Question, for the election setup
     */
    public ElectionVote(
            String question_id,
            List<Long> vote_results,
            Boolean write_in,
            String electionId) {

        this.question_id = question_id;
        this.write_in = write_in;
        this.vote_results = vote_results;
        this.id = Hash.hash("Vote", electionId, question_id, vote_results.toString(),   write_in.toString());
    }


    public String getId() {
        return id;
    }

    public String getQuestionId() {
        return question_id;
    }

    public Boolean getWriteIn() { return write_in; }

    public List<Long> getVote_results(){ return vote_results;}

    @Override
    public String getObject() {
        return Objects.ELECTION_VOTE.getObject();
    }

    @Override
    public String getAction() {
        return Action.CREATE.getAction();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ElectionVote that = (ElectionVote) o;
        return getQuestionId() == that.getQuestionId()
                && getWriteIn() == that.getWriteIn()
                && java.util.Objects.equals(getId(), that.getId())
                && java.util.Objects.equals(getVote_results(), that.getVote_results());

    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(
                getId(),
                getVote_results(),
                getWriteIn(),
                getQuestionId());
    }

    //TODO
    @Override
    public String toString() {
        return null;
    }


}

