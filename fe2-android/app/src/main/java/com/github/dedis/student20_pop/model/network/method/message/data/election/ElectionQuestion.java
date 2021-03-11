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

public class ElectionQuestion extends Data {

    private String id;
    private String question;
    private String voting_method;
    private boolean write_in;
    private List<String> ballot_options;

    /**
     * Constructor for a data Question, for the election setup
     */
    public ElectionQuestion(
            String question,
            String voting_method,
            boolean write_in,
            List<String> ballot_options,
            String electionId) {

        this.question = question;
        this.ballot_options = ballot_options;
        this.write_in = write_in;
        this.voting_method = voting_method;
        this.id = Hash.hash("Question", electionId, question);
    }


    public String getId() {
        return id;
    }

    public String getQuestion() {
        return question;
    }

    public boolean getWriteIn() { return write_in; }

    public List<String> getBallotOptions() { return Collections.unmodifiableList(ballot_options); }

    public String getVotingMethod() { return voting_method; }

    @Override
    public String getObject() {
        return Objects.ELECTION_QUESTION.getObject();
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
        ElectionQuestion that = (ElectionQuestion) o;
        return getQuestion() == that.getQuestion()
                && getWriteIn() == that.getWriteIn()
                && java.util.Objects.equals(getId(), that.getId())
                && java.util.Objects.equals(getBallotOptions(), that.getBallotOptions())
                && getVotingMethod() == that.getVotingMethod();
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(
                getId(),
                getVotingMethod(),
                getWriteIn(),
                getBallotOptions(),
                getQuestion());
    }

    //TODO
    @Override
    public String toString() {
        return null;
    }


}

