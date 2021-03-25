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

public class ElectionSetup extends Data {

    private String id;
    private String name;
    private long creation;
    private long start;
    private long end;
    private ElectionQuestion electionQuestion;

    /**
     * Constructor for a data setup Election Event
     *
     * @param name name of the Election
     * @param start of the Election
     * @param laoId id of the LAO
     */
    public ElectionSetup(
            String name,
            long start,
            long end,
            String voting_method,
            boolean write_in,
            List<String> ballot_options,
            String question,
            String laoId) {
        this.name = name;
        this.creation = Instant.now().toEpochMilli();
        this.start = start;
        this.end = end;
        this.id = Hash.hash("E", laoId, Long.toString(creation), name);
        this.electionQuestion = new ElectionQuestion(question, voting_method, write_in, ballot_options, this.id);
    }


    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public long getCreation() {
        return creation;
    }

    public long getStartTime() {
        return start;
    }

    public long getEndTime() { return end; }

    public ElectionQuestion getQuestion() { return electionQuestion; }


    @Override
    public String getObject() {
        return Objects.ELECTION.getObject();
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
        ElectionSetup that = (ElectionSetup) o;
        return getCreation() == that.getCreation()
                && start == that.getStartTime()
                && java.util.Objects.equals(getId(), that.getId())
                && creation == that.getCreation()
                && java.util.Objects.equals(getName(), that.getName())
                && end == that.getEndTime()
                && java.util.Objects.equals(electionQuestion, that.getQuestion());
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(
                getId(),
                getName(),
                getCreation(),
                getStartTime(),
                getEndTime(),
                getQuestion());
    }

    //TODO
    @Override
    public String toString() {
      return null;
    }

}
