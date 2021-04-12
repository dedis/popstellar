package com.github.dedis.student20_pop.model.network.method.message.data.election;

import com.github.dedis.student20_pop.model.network.method.message.data.Action;
import com.github.dedis.student20_pop.model.network.method.message.data.Data;
import com.github.dedis.student20_pop.model.network.method.message.data.Objects;
import com.github.dedis.student20_pop.utility.security.Hash;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
public class ElectionSetup extends Data {

    private String id;
    private String name;
    private String lao;
    private long created_at;
    private long start_time;
    private long end_time;
    private String version;
    private List<ElectionQuestion> questions;

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
        this.created_at = Instant.now().toEpochMilli();
        this.start_time = start;
        this.end_time = end;
        this.lao = laoId;
        this.version = "1.0.0";
        this.id = Hash.hash("E", laoId, Long.toString(created_at), name);
        this.questions = new ArrayList<>();
        this.questions.add(new ElectionQuestion(question, voting_method, write_in, ballot_options, this.id));
    }


    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public long getCreation() {
        return created_at;
    }

    public long getStartTime() {
        return start_time;
    }

    public long getEndTime() { return end_time; }

    public List<ElectionQuestion> getQuestions() { return questions; }

    public String getLao() { return lao; }

    public String getVersion() { return version; }


    @Override
    public String getObject() {
        return Objects.ELECTION.getObject();
    }

    @Override
    public String getAction() {
        return Action.SETUP.getAction();
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
                && start_time == that.getStartTime()
                && java.util.Objects.equals(getId(), that.getId())
                && created_at == that.getCreation()
                && java.util.Objects.equals(getName(), that.getName())
                && end_time == that.getEndTime()
                && java.util.Objects.equals(questions, that.getQuestions());
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(
                getId(),
                getName(),
                getCreation(),
                getStartTime(),
                getEndTime(),
                getQuestions());
    }

    @Override
    public String toString() {
        return "ElectionSetup{"
                + "id='"
                + id
                + '\''
                + ", name='"
                + name
                + '\''
                + ", lao='"
                + lao
                + '\''
                + ", creation='"
                + created_at
                + '\''
                + ", start='"
                + start_time
                + '\''
                + ", end="
                + end_time
                + '\''
                + ", version='"
                + version
                + '\''
                + questions.get(0).toString()
                + '}';
    }

}
