package com.github.dedis.student20_pop.model.network.method.message.data.election;

import com.github.dedis.student20_pop.model.network.method.message.data.Action;
import com.github.dedis.student20_pop.model.network.method.message.data.Data;
import com.github.dedis.student20_pop.model.network.method.message.data.Objects;
import com.github.dedis.student20_pop.utility.security.Hash;
import com.google.gson.annotations.SerializedName;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
public class ElectionSetup extends Data {

    private String id;
    private String name;
    private String lao;
    @SerializedName(value = "created_at")
    private long createdAt;
    @SerializedName(value = "start_time")
    private long startTime;
    @SerializedName(value = "end_time")
    private long endTime;
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
        this.createdAt = Instant.now().toEpochMilli();
        this.startTime = start;
        this.endTime = end;
        this.lao = laoId;
        this.version = "1.0.0";
        this.id = Hash.hash("E", laoId, Long.toString(createdAt), name);
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
        return createdAt;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getEndTime() { return endTime; }

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
                && startTime == that.getStartTime()
                && java.util.Objects.equals(getId(), that.getId())
                && createdAt == that.getCreation()
                && java.util.Objects.equals(getName(), that.getName())
                && endTime == that.getEndTime()
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
                + createdAt
                + '\''
                + ", start='"
                + startTime
                + '\''
                + ", end="
                + endTime
                + '\''
                + ", version='"
                + version
                + '\''
                + questions.get(0).toString()
                + '}';
    }

}
