package com.github.dedis.student20_pop.model.network.method.message.data.election;

import com.github.dedis.student20_pop.model.network.method.message.data.Action;
import com.github.dedis.student20_pop.model.network.method.message.data.Data;
import com.github.dedis.student20_pop.model.network.method.message.data.Objects;
import com.google.gson.annotations.SerializedName;

public class ElectionEnd extends Data {

    @SerializedName(value = "election")
    private String electionId;
    @SerializedName(value = "created_at")
    private long createdAt;
    @SerializedName(value = "lao")
    private String laoId;
    @SerializedName(value = "registered_votes")
    private String registeredVotes; //hashed


    @Override
    public String getObject() {
        return Objects.ELECTION.getObject();
    }

    @Override
    public String getAction() {
        return Action.END.getAction();
    }

    public String getLaoId() {
        return laoId;
    }

    public String getElectionId() {
        return electionId;
    }

    public String getRegisteredVotes() {
        return registeredVotes;
    }

    public long getCreatedAt() {
        return createdAt;
    }
}
